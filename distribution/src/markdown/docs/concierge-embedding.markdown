## Embed Concierge in your Java application

Since OSGi v4.2, a standard Framework Launch API was specified. This mechanism can also be used
to embed and launch Concierge within your own Java application.

### Constructing the framework

The framework launching API uses the Java ServiceLoader mechanism to load a FrameworkFactory. When the Concierge .jar is available on the classpath, this will give you a reference to the Concierge framework factory. Create a new instance of the framework by calling `newFramework()`, which accepts a map of properties you optionally want to provide to the framework. Finally start the framework by calling `start()` on the newly created instance.

```
// Load a framework factory
FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

// Create a framework
Map<String, String> config = new HashMap<String, String>();
// TODO: add some config properties
Framework concierge = frameworkFactory.newFramework(config);

// Start the framework
concierge.start();
```

### Starting and stopping bundles

Once you have created and started the Framework instance, you can interact with the framework by getting its `BundleContext`:

```
BundleContext context = concierge.getBundleContext();
Bundle shell = context.installBundle("file:org.eclipse.concierge.shell-0.9.0.jar"));
shell.start();
```

### Stopping the framework

Most likely the framework should run during the whole application lifetime. Often, the signal to stop the application will come from an OSGi bundle. You can handle the framework shutdown in your code, for example:

```
try {
    concierge.waitForStop(0);
} finally {
    System.exit(0);
}
```
