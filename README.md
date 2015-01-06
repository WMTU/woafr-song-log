woafr-song-log
==============

Java widget for logging songs to WMTU db from WO Automation for Radio

Required Java libraries: Apache Commons Lang, MySQL Connector/J.

Widget package structure: (.zip file)

```+---classes
|   +---edu
|       +---mtu
|           +---wmtu
|               +---resources
|               |   \---Logging.png
|               |   \---messages.properties
|               \---LoggingWidget.class
+---lib
|   /---commons-lang3-3.3.2.jar
|   /---mysql-connector-java-5.1.34-bin.jar
/---plugin.xml
```

===
#### LoggingWidget.java

This is the java source of the widget, with the database credentials removed.

===
#### plugin.xml

This is an xml file that describes the packaged widget.

===
#### classes/edu/mtu/wmtu/resources/Logging.png

This is an icon for the widget in the WO Automation for Radio UI.
