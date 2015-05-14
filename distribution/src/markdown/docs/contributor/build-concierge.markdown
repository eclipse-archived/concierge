# How to build Concierge

## Gradle based build system

Concierge will be build using [Gradle](http://gradle.org/). The gradle wrapper scripts will be used, to simplify installation. The wrapper scripts are checked in into repository as `./gradlew` and `./gradlew.bat`. The required files to start and download gradle are in folder `.gradle-wrapper`, do not touch. 

```gradle
// task to create a gradle wrapper with used gradle version
task wrapper(type: Wrapper) {
    gradleVersion = '2.2.1'
    jarFile = '.gradle-wrapper/gradle-wrapper.jar'
}
```

Updates of gradle version can be done by changing the wrapper task in `./build.gradle` to an updated version and running the task `./gradlew wrapper` again.

```
$ ./gradlew wrapper
```

## Build Concierge using Gradle

A complete build can be done using

```
$ ./gradlew clean build distZip distTar installDist
```

This will clean workspace, build and run tests, and will create a distribution file named `distribution/build/distributions/concierge-<version>.zip` and `...tar.gz`. By `installDist` it will be unpacked into `distribution/build/install` to easily check artifact results.

## Versioning

The framework and the bundles jars will be named to Bundle-SymbolicName and Bundle-Version, e.g. `org.eclipse.concierge-0.9.0.20141230122000.jar`. The Manifest.MF will specify the `Bundle-Version: 0.9.0.qualifier`, qualifier will be replaced by the current build date/time (MANIFEST driven approach). The version maintenance is up to the individual bundles/framework dependent on compatibility of changes.

The version for the distribution will be defined in `version.txt` in root directory. The used release plugin will automatically read in this file and use it as `project.version` property. When this file will have a `-SNAPSHOT` ending, it will be considered as a SNAPSHOT version. When doing a release (by calling `./gradlew release`) it will remove SNAPSHOT, make a release build, create a tag in git, and increase the version to next number.

## Distribution files

Files added to the distribution can be added to the folder `distribution/src/main/dist`. These files will automatically added to distribution folder.

In folder `distribution/src/markdown` are all documentation files in markdown format. They will be transformed to HTML using gradle tasks. For displaying markdown files [Strapdown](http://strapdownjs.com/) will be used.

Documentation for Concierge developers/committers is included in folder `distribution/src/markdown/docs/contributor`.

## Hudson based Continuous Integration build

There is a Hudson instance (HIPP) hosted at [Eclipse Hudson Infrastructure](http://hudson.eclipse.org/concierge/). All committer have access to this instance, please login with your Eclipse committer account.

There is a job [ConciergeDistribution](https://hudson.eclipse.org/concierge/job/ConciergeDistribution/) doing a build by checking git master branch for changes. This script will start the gradle-wrapper by a shell build task using

```
./gradlew -Dhttps.proxyHost=proxy.eclipse.org -Dhttps.proxyPort=9898 -Dhttps.nonProxyHosts=*.eclipse.org clean build distZip distTar installDist
```

The build job has to set the proxy settings to get gradle-version downloaded from Internet.
The Files to archive will be set to `distribution/build/distributions/*`.

When distribution has been made, a SNAPSHOT build will be published to Eclipse Download section by the job [ConciergeDistributionPublishSnapshot](https://hudson.eclipse.org/concierge/job/ConciergeDistributionPublishSnapshot/). It will upload these file to download area and will provide a [publish.log] file for checking the publishing process:

* Concierge Downloads: [http://download.eclipse.org/concierge/](http://download.eclipse.org/concierge/?d)
* Concierge Snapshot builds: [http://download.eclipse.org/concierge/snapshots](http://download.eclipse.org/concierge/snapshots/?d)
* Concierge Release builds: [http://download.eclipse.org/concierge/releases](http://download.eclipse.org/concierge/releases/?d)


## Open Issues

* Create maven artifacts
  * Which group id, which artifact ids?
    * Proposal: groupId: "org.eclise.concierge"
    * Artifact-ids: full qualified name, e.g. "org.eclipse.concierge",
      "org.eclipse.concierge.service.startlevel"
    * or: "framework", "service-startlevel"
  * Install into local maven repo
  * Install SNAPSHOT builds into Eclipse Maven repo
  * Install RELEASE builds into Eclipse Maven repo
* SNAPHSHOT vs. RELEASE builds
  * make a release build
* Hudson: get the gradle Hudson/Jenkins plugin installed
  * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=449992
* Add task for updating Copyright notice in all .java files
  * put template to distribution/src/config/copyright.txt file
* Restructure all projects
  * ./framework/org.eclipse.concierge
  * ./bundles/org.eclipse.concierge.*
  * ./docs/
  * ./distribution/
  * within Java project: src/main/java, src/test/java, ...
    * let META-INF/MANIFEST as it is (Manifest first approach)
* Align documentation with portal
  * shall HTML files be instrumented with Google Analytics or similar?
  * What is standard Eclipse approach?
* Remove build.xml files for legacy ANT build
* Extend build jobs (wait for Gradle Jenkins plugin)
  * overall test results
  * include findbugs/PMD/checkstyle
