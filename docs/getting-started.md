## Getting started

### Prerequisites

In order to run Concierge, you need a Java JRE installed on your system.

### Launching Concierge

Download the latest version of the Concierge distribution from [the project website](http://www.eclipse.org/concierge)


Start the framework from the installation directory by launching the framework jar and provide an .xargs file:

```
java -jar framework/org.eclipse.concierge*.jar samples/default.xargs
```

This will launch the framework and provide you with a Concierge shell. Type the `bundles` command to see the active bundles:

```
Concierge> bundles
Bundles:
[ 0] (active) org.eclipse.concierge
[ 1] (active) Eclipse Concierge Shell

``` 

You can start an additional bundle from both a filesystem or a web URL. For example, to install the Apache Felix DS runtime, type:

```
Concierge> install http://www.us.apache.org/dist/felix/org.apache.felix.scr-1.8.0.jar
```

This will download and install the bundle:

```
Concierge> bundles
Bundles:
[ 0] (active) org.eclipse.concierge
[ 1] (active) Eclipse Concierge Shell
[ 2] (installed) Apache Felix Declarative Services
```

Start the bundle with the start command:

```
Concierge> start 2
[org.apache.felix.scr-1.8.0] started.
```

Congratulations, you have successfuly launched your Concierge runtime. For more info on the available
shell commands, type `help`. To shutdown the framework type `exit`.
