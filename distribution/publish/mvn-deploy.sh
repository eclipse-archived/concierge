#!/bin/bash
# start this script by a Hudson shell script:

# chmod u+x ./distribution/publish/mvn-deploy.sh
# ./distribution/publish/mvn-deploy.sh

# for more info see 
#   https://wiki.eclipse.org/Services/Nexus#Deploying_artifacts_to_repo.eclipse.org
#   https://maven.apache.org/plugins/maven-deploy-plugin/deploy-file-mojo.html

set -x

MAVEN_HOME=/shared/common/apache-maven-latest
MAVEN_BIN=$MAVEN_HOME/bin/mvn

WSP_LOC=.
logFile=mvn-deploy.log

version=`cat version.txt`
echo "VERSION=$version"
if [[ "$version" == *-SNAPSHOT ]] ; then
  BUILD_TYPE=snapshots
else
  BUILD_TYPE=releases
fi
echo "BUILD_TYPE=$BUILD_TYPE"


if [ -d $WSP_LOC/tmp ] ; then rm -rf $WSP_LOC/tmp/* ; fi
if [ ! -d $WSP_LOC/tmp ] ; then mkdir -p $WSP_LOC/tmp ; fi
if [ -f $WSP_LOC/tmp/$logFile ] ; then rm $WSP_LOC/tmp/$logFile ; fi


echo "these files have to be uploaded to repo..."
find ./distribution/build/repo/$BUILD_TYPE -name "*.jar"

if [ "$BUILD_TYPE" == "snapshots" ] ; then

$MAVEN_BIN \
  -DgroupId=org.eclipse.concierge						\
  -DartifactId=org.eclipse.concierge					\
  -Dversion=1.0.0-SNAPSHOT								\
  -Dpackaging=jar										\
  -Dfile=./distribution/build/repo/$BUILD_TYPE/org/eclipse/concierge/org.eclipse.concierge/$VERSION/org.eclipse.concierge-$VERSION.jar	\
  -DrepositoryId=repo.eclipse.org						\
  -Durl=https://repo.eclipse.org/content/repositories/concierge-snapshots/	\
  deploy:deploy-file 

else 

  echo "NOT YET IMPLEMENTED: Upload releases to repo"

fi


# cleanup
# if [ -d $WSP_LOC/tmp ] ; then rm -rf $WSP_LOC/tmp/* ; rmdir $WSP_LOC/tmp ; fi
