package ext.deployit.community.plugin.changemgmt;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.collect.Multimap;
import com.xebialabs.deployit.plugin.api.boot.PluginBooter;
import com.xebialabs.deployit.plugin.api.reflect.Descriptor;
import com.xebialabs.deployit.plugin.api.reflect.DescriptorRegistry;
import com.xebialabs.deployit.plugin.api.reflect.Type;

public class OverrideTestSynthetics implements TestRule {
	// format strings for String.format
	private static final String SYNTHETIC_OVERRIDE_FILENAME_FORMAT = "synthetic-%s.xml";
	private final String syntheticOverridePathFormat;

	public OverrideTestSynthetics(String testSyntheticOverridesDir) {
		this.syntheticOverridePathFormat = testSyntheticOverridesDir 
			+ (!testSyntheticOverridesDir.endsWith(File.separator) ? File.separator : "")
			+ SYNTHETIC_OVERRIDE_FILENAME_FORMAT; 
	}

        @Override
        public Statement apply(final Statement base, final Description description) {
		final ClassLoader originalContextClassLoader = 
			Thread.currentThread().getContextClassLoader();
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				final boolean overrideApplied = 
					overrideTestSynthetic(description.getMethodName(), originalContextClassLoader);
				try {
					base.evaluate();
				} finally {
					Thread.currentThread().setContextClassLoader(originalContextClassLoader);
					if (overrideApplied) {
						forcePluginReboot();
					}
				}
			}
		};
	}

	private boolean overrideTestSynthetic(String testName, ClassLoader originalContextClassLoader) {
		File testSyntheticOverride = new File(format(syntheticOverridePathFormat, testName));
		if (!testSyntheticOverride.exists()) {
			return false;
		}

		Thread.currentThread().setContextClassLoader(
				new TestSyntheticsOverrideClassloader(originalContextClassLoader, testSyntheticOverride));
		forcePluginReboot();
		return true;
	}

        @SuppressWarnings("unchecked")
        private static void forcePluginReboot() throws IllegalArgumentException {
		try {
		        // private static fields
			Field isBooted = getAccessibleField(PluginBooter.class, "isBooted");
			((AtomicBoolean) isBooted.get(null)).set(false);
			Field descriptors = getAccessibleField(DescriptorRegistry.class, "descriptors");
			((Map<Type, Descriptor>) descriptors.get(null)).clear();
                        Field subtypes = getAccessibleField(DescriptorRegistry.class, "subtypes");
                        ((Multimap<Type, Type>) subtypes.get(null)).clear();
		} catch (Exception exception) {
			throw new IllegalArgumentException("Unable to reset plugin booter", exception);
		}
	}

	private static Field getAccessibleField(Class<?> clazz, String fieldName) throws SecurityException, NoSuchFieldException {
	    Field field = clazz.getDeclaredField(fieldName);
	    field.setAccessible(true);
	    return field;
	}

	private static class TestSyntheticsOverrideClassloader extends ClassLoader {
		private static final String TEST_SYNTHETIC_RESOURCE_NAME = "synthetic-test.xml";
		private final Enumeration<URL> testSynthetics;
		
		private TestSyntheticsOverrideClassloader(ClassLoader parent, File... syntheticsFiles) {
			super(parent);
			testSynthetics = toUrlEnumeration(syntheticsFiles);
		}

		private static Enumeration<URL> toUrlEnumeration(File[] syntheticsFiles) {
			// don't you just love Enumerations...
			Vector<URL> synthetics = new Vector<URL>(syntheticsFiles.length);
			for (File syntheticsFile : syntheticsFiles) {
				try {
					synthetics.add(syntheticsFile.toURI().toURL());
				} catch (MalformedURLException exception) {
					throw new IllegalArgumentException(
							format("Unable to convert '%s' to URL", syntheticsFile), exception);
				}
			}
			return synthetics.elements();
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return (name.equals(TEST_SYNTHETIC_RESOURCE_NAME) ? testSynthetics : super.getResources(name));
		}
	}
}