woafr-song-log
==============

Current version: 2.0

Java widget for logging songs to [Log](https://github.com/WMTU/Log) app from WO Automation for Radio

Required Java libraries: Apache Commons Lang 3.3.2, Log4J 1.2.16.

Widget package structure: (.zip file)

```
+---classes
|   +---edu
|       +---mtu
|           +---wmtu
|               +---resources
|               |   \---Logging.png
|               |   \---messages.properties
|               \---LoggingWidget.class (make sure to include all subclass files here too)
+---lib
|   /---commons-lang3-3.3.2.jar
|   /---log4j-1.2.16.jar
+---resources
|   /---config.properties
/---plugin.xml
```

===
#### LoggingWidget.java

The java source of the widget.

===
#### plugin.xml

An xml file describing the widget package.

===
#### resources/config.properties.example

Example database configuration.

===
#### classes/edu/mtu/wmtu/resources/Logging.png

An icon for the widget in the WO Automation for Radio interface.
