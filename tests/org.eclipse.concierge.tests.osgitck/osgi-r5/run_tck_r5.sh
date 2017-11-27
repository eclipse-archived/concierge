#!/bin/bash

usage() {
	echo "run_tck_r5.sh [--help] [--clean] [--prepare] [--run]"
	echo "  --help       this help"
	echo "  --clean      Clean the generated directories, incl. $OSGI_INSTALL_DIR"
	echo "  --prepare    Prepare the TCK tests, incl. clone OSGi repo, build osgi.ct"
	echo "  --run        Run the TCK tests"
	echo " "
	echo "Your have to set these environment variables, e.g.:"
	echo "  export OSGI_REPO=.../build.git"
	echo "  export OSGI_INSTALL_DIR=~/osgi-r5"
}

if [ "$1" == "--help" ] ; then
	usage
	exit 1
fi

if [ "$#" -ne 1 ]; then
	usage
	exit 1
fi

if [ ! "$1" == "--clean" -a ! "$1" == "--prepare" -a ! "$1" == "--run" ] ; then
	usage
	exit 1
fi

if [ -z $OSGI_REPO -o -z $OSGI_INSTALL_DIR ] ; then
	echo "Error: OSGI_REPO and OSGI_INSTALL_DIR must be set"
	usage
	exit 2
fi

if [ ! -d $OSGI_INSTALL_DIR ] ; then
	echo "Error: OSGI_INSTALL_DIR=$OSGI_INSTALL_DIR does not exist"
	usage
	exit 2
fi


# all OK now

# create our output folder
DEST_DIR=./build
if [ ! -d $DEST_DIR ] ; then
	mkdir $DEST_DIR
fi

if [ "$1" == "--clean" ] ; then
	(
		rm -v -r -f $DEST_DIR/*.log
		if [ -d $DEST_DIR ] ; then
			rmdir $DEST_DIR
		fi

		cd $OSGI_INSTALL_DIR
		if [ -d ./build ] ; then
			rm -v -r -f ./build/.git
			rm -v -r -f ./build/.git*
			rm -v -r -f ./build/*
			rm -v -f ./.DS_Store
			rmdir ./build
		fi
	)
	exit 0
fi

if [ "$1" == "--prepare" ] ; then
	(
		_current_dir=`pwd`
		cd $OSGI_INSTALL_DIR
		if [ -f ./build-ori.tar.gz ] ; then
			echo "Uncompressing TCK from $OSGI_REPO/build.git from file build-ori.tar.gz ..."
			tar xzf build-ori.tar.gz
		elif [ ! -d ./build ] ; then
			echo "Cloning TCK from $OSGI_REPO/build.git ..."
			# check out the tag of TCK for R5
			git clone -b r5-core-ri-ct-final $OSGI_REPO/build.git
			echo "Compressing TCK from $OSGI_REPO/build.git for later usage ..."
			tar czf build-ori.tar.gz ./build
		else
			echo "TCK yet installed at $OSGI_INSTALL_DIR/build ..."
		fi

		echo "Now patching TCK to get it running ..."
		cd ./build
		
		# replace osgi.core-4.3.0.jar against osgi.core-4.3.1.jar
		# as pointed out with Peter the 4.3.0 does not support generics
		if [ ! -f ./cnf/repo/osgi.core/osgi.core-4.3.1.jar ] ; then
			wget -o osgi.core-4.3.1.jar \
				https://search.maven.org/remotecontent?filepath=org/osgi/osgi.core/4.3.1/osgi.core-4.3.1.jar
			mv osgi.core-4.3.1.jar ./cnf/repo/osgi.core/osgi.core-4.3.1.jar
		fi
		if [ -f ./cnf/repo/osgi.core/osgi.core-4.3.0.jar ] ; then
			rm ./cnf/repo/osgi.core/osgi.core-4.3.0.jar
		fi

		# special changes, as org.osgi.service.event is missing on buildpath
		if [ ! -f org.osgi.test.cases.blueprint.java5/bnd.bnd.ORI ] ; then
			cp org.osgi.test.cases.blueprint.java5/bnd.bnd org.osgi.test.cases.blueprint.java5/bnd.bnd.ORI
		fi
		cat org.osgi.test.cases.blueprint.java5/bnd.bnd.ORI | \
			sed -e 's/4\.1/4\.1, org\.osgi\.service\.event\; version\=latest /g' >org.osgi.test.cases.blueprint.java5/bnd.bnd
		if [ ! -f org.osgi.test.cases.blueprint.secure/bnd.bnd.ORI ] ; then
			cp org.osgi.test.cases.blueprint.secure/bnd.bnd org.osgi.test.cases.blueprint.secure/bnd.bnd.ORI
		fi
		cat org.osgi.test.cases.blueprint.secure/bnd.bnd.ORI | \
			sed -e 's/4\.1/4\.1, org\.osgi\.service\.event\; version\=latest /g' >org.osgi.test.cases.blueprint.secure/bnd.bnd


		# show patched files
		find . -name "osgi.core-4.3.1.jar"
		find . -name "*.ORI"

		# show java version
		java -version
		
		# now build the TCK
		ant clean
		ant build

	) | tee -a $DEST_DIR/PREPARE.log
	
	exit 0
fi

if [ "$1" == "--run" ] ; then
	(
		# now copy the test binaries to DEST_DIR
		cp $OSGI_INSTALL_DIR/build/osgi.ct/generated/osgi.ct.core.jar $DEST_DIR
		cp $OSGI_INSTALL_DIR/build/osgi.ri/generated/osgi.ri.core.jar $DEST_DIR
		if [ ! -d $DEST_DIR/run ] ; then mkdir -p $DEST_DIR/run; fi
		cd $DEST_DIR/run
		jar xf ../osgi.ct.core.jar
		jar xf ../osgi.ri.core.jar
		if [ ! -d ./concierge ] ; then mkdir ./concierge ; fi
		cd ../..
		cp ./_under-test/org.eclipse.concierge-5.1.0*.jar $DEST_DIR/run/concierge/org.eclipse.concierge-5.1.0.jar
		cp ./_under-test/org.eclipse.concierge.service.permission-5.1.0*.jar $DEST_DIR/run/concierge/org.eclipse.concierge.service.permission-5.1.0.jar
	
		cd $DEST_DIR/run
		# patch some files to run Concierge
		# save all files to ORI
		for f in \
			org.osgi.test.cases.condpermadmin.bnd \
			org.osgi.test.cases.framework.launch.secure.bnd \
			org.osgi.test.cases.framework.secure.bnd \
			org.osgi.test.cases.permissionadmin.bnd \
			shared.inc \
			runtests ; do
			mv $f $f.ORI
		done
		cat org.osgi.test.cases.condpermadmin.bnd.ORI | \
			sed -e 's|runbundles = |runbundles = concierge/org.eclipse.concierge.service.permission-5.1.0.jar;version=file,|g' \
			>org.osgi.test.cases.condpermadmin.bnd
		cat org.osgi.test.cases.framework.launch.secure.bnd.ORI | \
			sed -e 's|runbundles = |runbundles = concierge/org.eclipse.concierge.service.permission-5.1.0.jar;version=file,|g' \
		 	>org.osgi.test.cases.framework.launch.secure.bnd
		cat org.osgi.test.cases.framework.secure.bnd.ORI | \
			sed -e 's|runbundles = |runbundles = concierge/org.eclipse.concierge.service.permission-5.1.0.jar;version=file,|g' \
			>org.osgi.test.cases.framework.secure.bnd
		cat org.osgi.test.cases.permissionadmin.bnd.ORI | \
			sed -e 's|runbundles = |runbundles = concierge/org.eclipse.concierge.service.permission-5.1.0.jar;version=file,|g' \
			>org.osgi.test.cases.permissionadmin.bnd
		cat shared.inc.ORI | sed -e 's|jar/org.eclipse.osgi-3.8.0.jar|concierge/org.eclipse.concierge-5.1.0.jar|g' >shared.inc
		cat runtests.ORI | sed -e 's|-title|--title|' >runtests
		
		# show diff
		for f_ori in *.ORI ; do
			f=`echo $f_ori | sed -e 's|.ORI||g'`
			# diff $f $f_ori
		done
		
		chmod u+x runtests
		./runtests
	) 2>&1 | tee $DEST_DIR/RUN.log
	exit 0
fi
