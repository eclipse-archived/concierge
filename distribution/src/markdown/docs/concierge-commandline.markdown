## Run Concierge from command line

When launching the Concierge .jar from command line, it takes an .xargs file as single argument. 
This file is used to configure the framework, set a couple of properties, and install or start
some bundles when first launched.

### .xargs file syntax

The .xargs file can contain both runtime properties for configuring the framework, as well as a set 
of framework commands. 

Properties are declared as `-Dkey=value`. Before launching, all `-D` properties are collected and
used to create a new framework instance. Within the .xargs file, one can also reference the declared property values by using `${key}`.

Next to properties, the .xargs file can also contain framework commands that are executed after the
framework is launched. These commands are executed in the order of occurence. Available commands are
`-install` to install a bundle, `-start` to start a bundle, `-istart` to install and start 
a bundle, etc.

Comments are preceded by a hash `#` sign.

### .xargs commands

`-install <bundle URL>` : installs a bundle from a given bundle URL  

`-start <bundle URL>` : starts a bundle that was previously installed from the given bundle URL

`-istart <bundle URL>` : install and starts a bundle from a given bundle URL  

`-all <file directory>` : install and start all .jar files in a given directory

`-initlevel <level>` : sets the startlevel that will be used for all next bundles to be installed 

`-skip` : skips the remainder of the .xargs file (handy for debugging)


### Sample .xargs files

The Concierge distribution download comes with sample .xargs files. For example, the following
.xargs file launches Concierge with some basic Apache Felix bundles providing declarative services,
an event admin, configuration admin and metatype:

```
 # xargs sample file to load some Felix bundles

 # uncomment to clean storage first
 # -Dorg.osgi.framework.storage.clean=onFirstInit

 # use our own profile
 -Dorg.eclipse.concierge.profile=felix

 # repos to load bundles from
 -Drepo=http://www.us.apache.org/dist/felix

 # load bundles
 -istart bundles/org.eclipse.concierge.shell-0.9.0.*.jar
 -istart ${repo}/org.apache.felix.log-1.0.1.jar
 -istart ${repo}/org.apache.felix.scr-1.8.0.jar
 -istart ${repo}/org.apache.felix.eventadmin-1.4.2.jar
 -istart ${repo}/org.apache.felix.metatype-1.0.12.jar
 -istart ${repo}/org.apache.felix.configadmin-1.8.4.jar
```

First, some properties are set using the `-Dkey=value` syntax, and next the bundles to 
start are declared.

The first two properties have to do with the Concierge storage directory. 
When Concierge launches, it automatically creates a storage directory to cache all the 
installed bundles. When launching again, it will first try to restore the previously cached
state. If you don't want this behavior, and want to start with a clean environment every
time, you can set the `org.osgi.framework.storage.clean` property to `onFirstInit`.
The `org.eclipse.concierge.profile` allows you to create a separate storage directory
for each profile.

The `repo` property is declared pointing to the Apache Felix repository, and is used later
in the commands as `${repo}`.

Finally all bundles are installed and started with the `-istart` command. This can take 
both a web URL or a file system URL.



