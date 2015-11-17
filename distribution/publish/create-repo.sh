#!/bin/bash
# create p2/OSGi-R5 repos from a ZIP file
# TODO helper file, needs to be integrated into regular build

# chmod u+x ./distribution/publish/create-repo.sh
# ./distribution/publish/create-repo.sh

# enable for "debugging" of script
set -x

if [[ "$1" == "snapshots" ]] ; then
  TARGET_BUILD_TYPE=snapshots
elif [[ "$1" == "release" ]] ; then
  TARGET_BUILD_TYPE=releases
else
  echo "Usage: Wrong build type: use ./publish.s {snapshots|release} ..."
  exit 1
fi
echo "TARGET_BUILD_TYPE=$TARGET_BUILD_TYPE"


# get tools for creating repos
if [ ! -d tools ] ; then
  mkdir tools
  cd tools
  if [ ! -f eclipse-java-mars-1-linux-gtk-x86_64.tar.gz ] ; then
    echo "Downloading Eclipse ..."
    curl -q -L -o eclipse-java-mars-1-linux-gtk-x86_64.tar.gz \
      "https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/mars/1/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz&r=1"
  fi
  if [ ! -f org.osgi.impl.bundle.repoindex.cli-3.1.0.jar ] ; then
    echo "Downloading bnd repoindex cli ..."
    curl -L -o org.osgi.impl.bundle.repoindex.cli-3.1.0.jar \
      "https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/artifact/org.osgi.impl.bundle.repoindex.cli/generated/org.osgi.impl.bundle.repoindex.cli-3.1.0.jar"
  fi
  cd ..
fi

ECLIPSE_TAR_GZ=./tools/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz
BND_REPOINDEX_CLI=./tools/org.osgi.impl.bundle.repoindex.cli-3.1.0.jar

DIST_ZIP=`cd ./distribution/build/distributions ; ls concierge-incubation-*.zip`

TMP_DIR=./distribution/build/tmp
if [ -d $TMP_DIR ] ; then
  rm -rf $TMP_DIR
fi
mkdir $TMP_DIR
cd $TMP_DIR
unzip -q ../distributions/$DIST_ZIP
tar xzf ../../../$ECLIPSE_TAR_GZ

mkdir -p concierge/features
mkdir -p concierge/plugins
cp con*/framework/* concierge/plugins
cp con*/bundles/* concierge/plugins
rm concierge/plugins/*nodebug*
find concierge

if [ -d ../repo/$TARGET_BUILD_TYPE ] ; then
  rm -rf ../repo/$TARGET_BUILD_TYPE
fi
mkdir -p ../repo/p2/$TARGET_BUILD_TYPE

CWD=`pwd`
java -jar ./eclipse/plugins/org.eclipse.equinox.launcher_*.jar \
  -console -consoleLog \
  -nosplash \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository file://$CWD/../repo/p2/$TARGET_BUILD_TYPE \
  -artifactRepository file://$CWD/../repo/p2/$TARGET_BUILD_TYPE \
  -source $CWD/concierge \
  -publishArtifacts

# more args for development
#  -console -consoleLog \
#  -compress \

echo "Generated p2 repo at $CWD/../repo/p2/$TARGET_BUILD_TYPE ..."

mkdir -p ../repo/osgi-r5/$TARGET_BUILD_TYPE/bundles
cp ./concierge/plugins/* ../repo/osgi-r5/$TARGET_BUILD_TYPE/bundles
# java -jar ../../../$BND_REPOINDEX_CLI -h
cd ../repo/osgi-r5/$TARGET_BUILD_TYPE
java -jar ../../../../../$BND_REPOINDEX_CLI -v -n Concierge -l file:../../src/main/dist/epl-v10.html --pretty bundles
cd ../..

echo "Generated OSGi-R5 repo at $CWD/../repo/osgi-r5/$TARGET_BUILD_TYPE ..."

echo "Note: needs to be updated to repo manually at the moment"
