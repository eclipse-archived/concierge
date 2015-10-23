#!/bin/bash
# start this script by a Hudson shell script:

# chmod u+x ./distribution/publish/publish.sh
# ./distribution/publish/publish.sh

set -x

version=`cat version.txt`
echo "VERSION=$version"
if [[ "$version" == *"SNAPSHOT"* ]] ; then
  BUILD_TYPE=snapshots
else
  BUILD_TYPE=releases
fi
echo "BUILD_TYPE=$BUILD_TYPE"
UPLOAD_LOCATION=/home/data/httpd/download.eclipse.org/concierge/$BUILD_TYPE
PUBLISH_LOG=$UPLOAD_LOCATION/publish.log
echo "UPLOAD_LOCATION=$UPLOAD_LOCATION"
echo "PUBLISH_LOG=$PUBLISH_LOG"

now=`date '+%Y/%m/%d %H:%M:%S'`
echo "$now: publishing last successful build for $version" >>$PUBLISH_LOG

echo `ls ./distribution/build/distributions/*.zip` >>$PUBLISH_LOG
cp ./distribution/build/distributions/*.zip $UPLOAD_LOCATION
echo `ls ./distribution/build/distributions/*.tar.gz` >>$PUBLISH_LOG
cp ./distribution/build/distributions/*.tar.gz $UPLOAD_LOCATION

# cleanup
rm /home/data/httpd/download.eclipse.org/concierge/snapshots/concierge-0.8.0*
rm /home/data/httpd/download.eclipse.org/concierge/snapshots/concierge-0.9.0*
rm /home/data/httpd/download.eclipse.org/concierge/snapshots/concierge-1.0.0*
rm /home/data/httpd/download.eclipse.org/concierge/snapshots/publish.log
