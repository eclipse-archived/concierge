# Eclipse Concierge Release 5.1.0

This document is intended for committers making a release of Eclipse Concierge. It described current approach for 5.1.0 and later. It might change for next versions when IDE/CI will be enhanced further on.

## How to publish 5.1.0 release

The Eclipse Concierge release 5.1.0 will be published first time as Maven artifacts. Therefore the CI build has been improved to support publishing the release 5.1.0 to all distribution channels:

* Eclipse download server
* Eclipse Nexus repository
* Maven Central repository

This page describes all steps which needed to be done to publish this version.

### Preparation

Prepare JIPP instance for publishing (needed only once):

* JIPP instance at https://ci.eclipse.org/concierge/
* Prepare JIPP instance to upload to Eclipse Nexus
  * ask Webmaster to add credentials for upload to Eclipse Nexus to `~/.gradle/gradle.properties` (eclipseRepoUsername, eclipseRepoPassword)
* Prepare JIPP instance to sign the Maven artifacts
  * ask Webmaster to generate GPG key signed by Eclipse Foundation
    * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=540505
  * ask Webmaster to add GPG key information to `~/.gradle/gradle.properties` (signing.gnupg.keyName, signing.gnupg.passphrase)
* Prepare JIPP instance to upload to Maven central
  * ask Webmaster to add credentials for upload to Maven Central to `~/.gradle/gradle.properties` (mavenCentralUsername, mavenCentralPassword)

The file `~/.gradle/gradle.propeties` should look like:
  
```
// publishing to Eclipse Nexus
eclipseRepoUsername ≤the-eclipse-nexus-username>
eclipseRepoPassword ≤the-eclipse-nexus-password>

// signing artifacts with that key
signing.gnupg.keyName=<the-key-id>
signing.gnupg.passphrase=<the-key-passphrase>

// publishing to Maven central
mavenCentralUsername ≤the-maven-central-username>
mavenCentralPassword ≤the-maven-central-password>
```

* apply latest changes to build scripts to support release build and maven upload. This includes signing of all artifacts.
* Set `version.txt` to final version (e.g. `5.1.0`)
* Commit all changes


### CI Jobs

We have 2 jobs running:
* Build-5.1.0: this job does only make the build, but does NOT publish yet
* Publish-5.1.0: this job does only publish the artifacts
  # Download Server
  # Maven Central
  # Eclipse Nexus

The build will essentially do:

```
./gradlew \
 -Dhttp.proxyHost=proxy.eclipse.org \
 -Dhttp.proxyPort=9898 \
 -Dhttp.nonProxyHosts=*.eclipse.org \
 -Dhttps.proxyHost=proxy.eclipse.org \
 -Dhttps.proxyPort=9898 \
 -Dhttps.nonProxyHosts=*.eclipse.org \
 -Dorg.eclipse.concierge.tests.waitAfterFrameworkShutdown=1000 \
 clean signMavenJavaPublication test distZip distTar assembleDist installDist
```

### Publish to downloads

The script at `./distribution/publish/publish.sh` will upload/copy the built distribution archives to the Eclipse download server.

It will check the version built (taken from version.txt):
* If it is a snapshot version, it will be uploaded to http://download.eclipse.org/concierge/download/snapshots/?d
* Otherwise if it is a release version (Milestone, ReleaseCandidate, Final version), it will be uploaded to http://download.eclipse.org/concierge/download/releases/?d
* it will upload both a `concierge-incubation-<version>.zip` and a `concierge-incubation-<version>.tar.gz` archive of all artifacts

```
chmod u+x ./distribution/publish/publish.sh
./distribution/publish/publish.sh release
```



References:
* Snapshot builds: http://download.eclipse.org/concierge/download/snapshots/?d
* Release builds:  http://download.eclipse.org/concierge/download/releaes/?d

### Public to Eclipse Nexus (repo.eclipse.org)

The gradle build is able to publish generated signed artifacts to a Maven repository. We configured the Eclipse Nexus repository as target for publishing.

The publishing to Eclipse Nexus can be done that way:

```
./gradlew publishMavenJavaPublicationToEclipseRepoRepository
```

References:
* Snapshot artifacts: https://repo.eclipse.org/content/repositories/concierge-snapshots/
* Release artifacts:  https://repo.eclipse.org/content/repositories/concierge-releases/

### Publish to Maven Central (

* Create an account at Sonatype and ask to publish to group "org.eclipse.concierge"
  * see https://issues.sonatype.org/browse/OSSRH-43650
* Use the credentials to upload to Maven central into a staging repository

The publishing to Maven central can be done that way:
* make sure there is no stageing repository yet open/closed. If so drop the staging repo first
* Then publish to staging repository

```
# Proxy needed to connect to Internet
./gradlew \
 -Dhttp.proxyHost=proxy.eclipse.org \
 -Dhttp.proxyPort=9898 \
 -Dhttp.nonProxyHosts=*.eclipse.org \
 -Dhttps.proxyHost=proxy.eclipse.org \
 -Dhttps.proxyPort=9898 \
 -Dhttps.nonProxyHosts=*.eclipse.org \
 publishMavenJavaPublicationToMavenCentralRepository
```

After upload to staging repository:
* Go to staging repository at https://oss.sonatype.org/#stagingRepositories (login first)
* If staging repository upload is complete, first "Close" it
* If staging repository verifiation is fine, you can "Release" it


References
* Artifacts during staging: https://oss.sonatype.org/content/groups/staging/org/eclipse/concierge/
* Release artifacts: https://oss.sonatype.org/content/groups/public/org/eclipse/concierge/
* Good article how to publish to Maven Central
  * https://medium.com/@nmauti/publishing-a-project-on-maven-central-8106393db2c3 (Part 1)
  * https://medium.com/@nmauti/sign-and-publish-on-maven-central-a-project-with-the-new-maven-publish-gradle-plugin-22a72a4bfd4b (Part 2)


### Post work after release is done

As last step, update the project download page to refer to latest release as well.
Goto https://projects.eclipse.org/projects/iot.concierge and edit download section.

When release build has been done, tag the commit to `v5.1.0`.
