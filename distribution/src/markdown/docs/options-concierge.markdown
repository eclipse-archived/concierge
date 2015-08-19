## Concierge Options

Besides the [options defined by the OSGi specification]((options-osgi.html)), Concierge also has some custom configuration options:

### Framework properties

These properties allow you to configure the framework instance to launch.

```
-Dorg.eclipse.concierge.profile=<profile-name>
```
Sets a custom profile name. This will persist the installed bundles to a separate storage directory, allowing you to restore this specific profile later on.

```
-Dorg.eclipse.concierge.basedir=<directory>
```
Set the base storage directory for the framework

```
-Dorg.eclipse.concierge.jars=<directory>
```
Base bundle location uri for saving installed bundle .jar files. Defaults to "file://<basedir>"

```
-Dorg.eclipse.concierge.classloader.buffersize=2048
```
Buffer size in bytes for reading in .class files in the bundle classloader. Defaults to 2048.

```
-Dorg.eclipse.concierge.alwaysDecompress=true
```
Set this property to always unpack the bundle .jar files in the storage directory 

### Debugging

These properties can be set to enable more verbose logging and debugging information:

```
-Dorg.eclipse.concierge.debug=true
-Dorg.eclipse.concierge.log.enabled=true
-Dorg.eclipse.concierge.log.quiet=false
-Dorg.eclipse.concierge.log.buffersize=100
-Dorg.eclipse.concierge.log.level=4 # DEBUG
-Dorg.eclipse.concierge.debug.bundles=true
-Dorg.eclipse.concierge.debug.packages=true
-Dorg.eclipse.concierge.debug.services=true
-Dorg.eclipse.concierge.debug.classloading=true
```