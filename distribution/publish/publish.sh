#!/bin/bash
# Copy this script to Hudson job "ConciergeDistributionPublishSnapshot"

set -x

BUILD_LOC=/home/data/httpd/download.eclipse.org/concierge
logFile=publish.log

version=`cat version.txt`
echo "VERSION=$version"
if [ ]
if [[ "$version" == *-SNAPSHOT ]] ; then
  BUILD_LOC_TYPE=snapshots
else
  BUILD_LOC_TYPE=release
fi
echo "BUILD_LOC_TYPE=$BUILD_LOC_TYPE"


if [ -d $BUILD_LOC/tmp ] ; then rm -rf $BUILD_LOC/tmp/* ; fi
if [ ! -d $BUILD_LOC/tmp ] ; then mkdir -p $BUILD_LOC/tmp ; fi
cd $BUILD_LOC/tmp

rm -f $logFile

now=`date '+%Y/%m/%d %H:%M:%S'`
echo "$now: getting last successful build" >>$logFile

wget --no-check-certificate https://hudson.eclipse.org/concierge/job/ConciergeDistribution/lastSuccessfulBuild/artifact/distribution/build/distributions/concierge-$version.zip -a $logFile
wget --no-check-certificate https://hudson.eclipse.org/concierge/job/ConciergeDistribution/lastSuccessfulBuild/artifact/distribution/build/distributions/concierge-$version.tar -a $logFile

now=`date '+%Y/%m/%d %H:%M:%S'`
echo "$now: publishing last successful build" >>$logFile

mv -fv *.zip *.tar ../$BUILD_LOC_TYPE/ >>$logFile

now=`date '+%Y/%m/%d %H:%M:%S'`
echo "$now: finished publishing last successful build" >>$logFile

mv -f $logFile ../snapshots/

cd ..
if [ -d $BUILD_LOC/tmp ] ; then rm -rf $BUILD_LOC/tmp/* ; rmdir $BUILD_LOC/tmp ; fi
