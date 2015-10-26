#!/bin/bash
# start this script by a Hudson shell script:

# chmod u+x ./distribution/publish/publish.sh
# ./distribution/publish/publish.sh

# TODO: cleanup SNAPSHOT builds if more >50, older >30 days

# enable for "debugging" of script
set -x

version=`cat version.txt`
echo "VERSION=$version"
if [[ "$version" == *"SNAPSHOT"* ]] ; then
  BUILD_TYPE=snapshots
else
  BUILD_TYPE=releases
fi
echo "BUILD_TYPE=$BUILD_TYPE"
UPLOAD_BASE=/home/data/httpd/download.eclipse.org/concierge
UPLOAD_LOCATION=$UPLOAD_BASE/$BUILD_TYPE
PUBLISH_LOG=$UPLOAD_BASE/publish.log
echo "UPLOAD_LOCATION=$UPLOAD_LOCATION"
echo "PUBLISH_LOG=$PUBLISH_LOG"

# get the version of this build
buildVersion=`(cd ./distribution/build/distributions/ ; ls *.tar.gz) | sed -e 's/\.tar\.gz//g'`
echo "buildVersion=$buildVersion"

(
# current time in UTC with Timezone information
now=`date -u '+%Y-%m-%d %H:%M:%S %Z'`
echo "$now: publishing last successful build for $version"

# copy latest build artifacts (tar.gz, zip)
echo -n "$BUILD_TYPE/"
echo `(cd ./distribution/build/distributions/ ; ls *.tar.gz)`
cp ./distribution/build/distributions/*.tar.gz $UPLOAD_LOCATION
echo -n "$BUILD_TYPE/"
echo `(cd ./distribution/build/distributions/ ; ls *.zip)`
cp ./distribution/build/distributions/*.zip $UPLOAD_LOCATION

# now link latest snapshot to this build
if [ "$BUILD_TYPE" == "snapshots" ] ; then
  echo "Link latest snapshot to $buildVersion"
  (
    cd $UPLOAD_LOCATION
    for f in concierge-incubation-SNAPSHOT-latest.tar.gz concierge-incubation-SNAPSHOT-latest.zip ; do
      if [ -f $f ] ; then rm $f ; fi
    done
    echo ln -s "$buildVersion".tar.gz concierge-incubation-SNAPSHOT-latest.tar.gz
    ln -s "$buildVersion".tar.gz concierge-incubation-SNAPSHOT-latest.tar.gz
    echo ln -s "$buildVersion".zip concierge-incubation-SNAPSHOT-latest.zip
    ln -s "$buildVersion".zip concierge-incubation-SNAPSHOT-latest.zip
    
    ls -al
  )
fi

echo " "

) | tee >>$PUBLISH_LOG

echo "See http://download.eclipse.org/concierge/$BUILD_TYPE/?d for uploaded files..."
