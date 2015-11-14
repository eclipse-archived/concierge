#!/bin/bash
# create p2/OSGi-R5 repos from a ZIP file
# TODO helper file, needs to be integrated into regular build

# adapt to your installation
ECLIPSE_TAR_GZ=../../tmp/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz
BND_REPOINDEX_CLI=../../tmp/org.osgi.impl.bundle.repoindex.cli-3.1.0.jar


DIST_ZIP=`ls ../build/distributions/concierge-incubation-*.zip`

if [ -d tmp ] ; then
  rm -rf ./tmp
fi
mkdir tmp
cd tmp
unzip -q ../$DIST_ZIP
tar xzf ../$ECLIPSE_TAR_GZ

mkdir -p concierge/features
mkdir -p concierge/plugins
cp con*/framework/* concierge/plugins
cp con*/bundles/* concierge/plugins
rm concierge/plugins/*nodebug*
find concierge
cd ..

if [ -d repository ] ; then
  rm -rf ./repository
fi
mkdir -p repository/p2

CWD=`pwd`
java -jar tmp/eclipse/plugins/org.eclipse.equinox.launcher_*.jar \
  -nosplash \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository file://$CWD/repository/p2 \
  -artifactRepository file://$CWD/repository/p2 \
  -source $CWD/tmp/concierge \
  -compress \
  -publishArtifacts

echo "Generated p2 repo at $CWD/repository/p2 ..."

mkdir -p repository/osgi-r5
mkdir -p repository/osgi-r5/bundles
cp tmp/concierge/plugins/* repository/osgi-r5/bundles
# java -jar $BND_REPOINDEX_CLI -h
cd repository/osgi-r5
java -jar ../../$BND_REPOINDEX_CLI -v -n Concierge -l file:../src/main/dist/epl-v10.html --pretty bundles
cd ../..

echo "Generated OSGi-R5 repo at $CWD/repository/osgi-r5 ..."

echo "Note: needs to be updated to repo manually at the moment"
