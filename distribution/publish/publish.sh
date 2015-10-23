#!/bin/bash
# start this script by a Hudson shell script:

# chmod u+x ./distribution/publish/publish.sh
# ./distribution/publish/publish.sh

set -x

BUILD_LOC=/home/data/httpd/download.eclipse.org/concierge
logFile=publish.log

version=`cat version.txt`
echo "VERSION=$version"
if [[ "$version" == *"SNAPSHOT"* ]] ; then
  BUILD_LOC_TYPE=snapshots
else
  BUILD_LOC_TYPE=releases
fi
echo "BUILD_LOC_TYPE=$BUILD_LOC_TYPE"

# pwd ; find .
# find /home/hudson/genie.concierge/.hudson/jobs/ConciergeDistribution/lastSuccessfulBuild

# if [ -d $BUILD_LOC/tmp ] ; then rm -rf $BUILD_LOC/tmp/* ; fi
# if [ ! -d $BUILD_LOC/tmp ] ; then mkdir -p $BUILD_LOC/tmp ; fi

now=`date '+%Y/%m/%d %H:%M:%S'`
echo "$now: publishing last successful build" >>$BUILD_LOC/$logFile

cp ./distribution/build/distributions/*.zip $BUILD_LOC
cp ./distribution/build/distributions/*.tar.gz $BUILD_LOC

# mv -fv *.zip *.tar.gz ../$BUILD_LOC_TYPE/ >>$logFile

now=`date '+%Y/%m/%d %H:%M:%S'`
echo "$now: finished publishing last successful build" >>$BUILD_LOC/$logFile

# mv -f $logFile ../$BUILD_LOC_TYPE/

# cd ..
if [ -d $BUILD_LOC/tmp ] ; then rm -rf $BUILD_LOC/tmp/* ; rmdir $BUILD_LOC/tmp ; fi
