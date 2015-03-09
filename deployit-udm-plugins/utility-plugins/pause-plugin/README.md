# DEPRECATED #
Source code is now hosted here : https://github.com/xebialabs-community/xld-pause-plugin

## Description

A Deployit 3.8 plugin that generate pause steps.

## Installation

Place the 'pause-plugin-&lt;version&gt;.jar' file into your SERVER_HOME/plugins directory.

## Configuration
The udm.Environment ci has 2 new properties:
* pausable (boolean)
* pauseOrder
## Usage

If pause property is true, the plugin will add a pause step a the given order for all deployments targeting this environment.
