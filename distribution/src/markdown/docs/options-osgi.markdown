## OSGi Options supported by Concierge

Concierge allows you to set various framework launching properties as defined by the OSGi specification. The most important ones are described here.

### System Packages Extra

The `osgi.framework.system.packages.extra` property identifies extra packages which the system bundle must export. 

These packages will be added to the packages listed by the `osgi.framework.system.packages` property, which contains the main OSGi framework packages. Do not change the `osgi.framework.system.packages` property unless you know what you are doing. The default `osgi.framework.system.packages` provided by Concierge contains the following packages:
```
org.osgi.framework,
org.osgi.framework.hooks.bundle,
org.osgi.framework.hooks.resolver,
org.osgi.framework.hooks.service,
org.osgi.framework.hooks.weaving,
org.osgi.framework.launch,
org.osgi.framework.namespace,
org.osgi.framework.startlevel,
org.osgi.framework.wiring,
org.osgi.resource,
org.osgi.service.log,
org.osgi.service.packageadmin,
org.osgi.service.startlevel,
org.osgi.service.url,
org.osgi.service.resolver,
org.osgi.util.tracker
```

### OSGi Bootdelegation

The `org.osgi.framework.bootdelegation` launching property identifies packages for which the Framework must delegate class loading to the parent class loader of the bundle. This must be used for packages that are required by a bundle, although not stated as an Import-Package. By default Concierge only delegates `java.*` packages to the parent class loader. If you use `javax.*` and/or `sun.*` packages you should explicitly set the `org.osgi.framework.bootdelegation` property to `javax.*,sun.*`. If you need to add other packages as well, double check whether you shouldn't have to add the package as an Import-Package of your bundle and use `osgi.framework.system.packages.extra` to have this package exported by the Framework.


### Framework Parent ClassLoader

The org.osgi.framework.bundle.parent launching property specifies the parent class loader type for all bundle class loaders. This defaults to the boot classloader. Available options are:
* boot - use the boot classloader (default)
* ext - use the extension classloader
* app - use the application classloader
* framework - use the framework bundle classloader



 
