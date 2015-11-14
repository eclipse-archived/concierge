## Concierge Additional Bundles

In order to keep the Framework as minimal as possible, Concierge also provides some basic services that are often bundled inside an OSGi Framework implementation as separate bundles.

### org.eclipse.concierge.shell

Lightweight shell implementation for interacting with the Concierge Framework.

### org.eclipse.concierge.extension.permission

Provides the `org.osgi.service.condpermadmin` and `org.osgi.service.permissionadmin` interfaces as framework extension
Note: there is also a bundle org.eclipse.concierge.service.permission which contains same classes but not as framework extension but as separate bundle.

### org.eclipse.concierge.service.packageadmin

Provides an implementation of the `PackageAdmin` service.

### org.eclipse.concierge.service.startlevel

Provides an implementation of the `StartLevel` service.

### org.eclipse.concierge.service.xmlparser

Allows any JAXP compliant XML Parser to register itself as an OSGi parser service.

### org.eclipse.concierge.service.eventadmin

Provides a very lightweight implementation of EventAdmin (just 3 clasees).
