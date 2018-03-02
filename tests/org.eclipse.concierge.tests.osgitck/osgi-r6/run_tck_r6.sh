#!/bin/bash

usage() {
	echo "run_tck_r6.sh [help] [run] [clean] [src-prepare] [src-run]"
	echo "  help           this help"
	echo "  run            Download TCK, and run the TCK tests"
	echo "  clean          Clean all generated files, incl. cloned Git source repo at $OSGI_INSTALL_DIR"
	echo "  src-prepare    Build TCK from source, and prepare the TCK tests, incl. clone OSGi repo, build osgi.ct"
	echo "  src-run        Build TCK from source, and run the TCK tests"
	echo " "
	echo "Your have to set these environment variables, e.g.:"
	echo "  export OSGI_MEMBER_USER=email@domain.com"
	echo "  export OSGI_MEMEBR_PASSWORD=some-secret"
	echo "  export OSGI_REPO=.../build.git"
	echo "  export OSGI_INSTALL_DIR=~/osgi-r6"
}

if [ "$1" == "help" ] ; then
	usage
	exit 1
fi

if [ "$#" -ne 1 ]; then
	usage
	exit 1
fi

if [ ! "$1" == "run" -a ! "$1" == "clean" -a ! "$1" == "src-prepare" -a ! "$1" == "src-run" ] ; then
	echo "Error: Unknown command $1"
	usage
	exit 1
fi

if [ "$1" == "run" -a -z $OSGI_MEMBER_USER -o -z $OSGI_MEMBER_PASSWORD ] ; then
	echo "Error: OSGI_MEMBER_USER and OSGI_MEMBER_PASSWORD must be set"
	usage
	exit 2
fi

if [ "$1" == "src-prepare" -o "$1" == "src-run" ] ; then
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
fi


# all OK now

# create our output folder
DEST_DIR=./build
if [ ! -d $DEST_DIR ] ; then
	mkdir $DEST_DIR
fi

if [ "$1" == "run" ] ; then
	(
		echo "Preparing OSGi TCK Run environment..."
		# download the OSGi TCK for R5, see https://osgi.org/members/Release5/HomePage
		# download CT: https://osgi.org/.../470/artifact/osgi.ct/generated/osgi.ct.core.jar
		# copy the test binary to DEST_DIR
		wget --user "$OSGI_MEMBER_USER" --password "$OSGI_MEMBER_PASSWORD" -O $DEST_DIR/osgi.ct.core.jar https://osgi.org/hudson/job/build.core/470/artifact/osgi.ct/generated/osgi.ct.core.jar 2>/dev/null
		
		cd $DEST_DIR
		if [ ! -d ./run ] ; then mkdir -p ./run ; fi
		cd ./run
		jar xf ../osgi.ct.core.jar
		if [ ! -d ./concierge ] ; then mkdir ./concierge ; fi
		cd ..

		# copy framework under test to folder build/run/concierge
		cp ../_under-test/org.eclipse.concierge-6.0.0*.jar ./run/concierge/org.eclipse.concierge-6.0.0.jar
		cp ../_under-test/org.eclipse.concierge.service.permission-5.1.0*.jar ./run/concierge/org.eclipse.concierge.service.permission-5.1.0.jar

		cd ./run
		# patch some files to run Concierge
		# save all files to ORI
		for f in \
			org.osgi.test.cases.condpermadmin.bnd \
			org.osgi.test.cases.framework.launch.secure.bnd \
			org.osgi.test.cases.framework.secure.bnd \
			org.osgi.test.cases.permissionadmin.bnd \
			shared.inc ; do
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
		cat shared.inc.ORI | sed -e 's|jar/org.eclipse.osgi-3.10.0.jar|concierge/org.eclipse.concierge-6.0.0.jar|g' >shared.inc
		
		# show diff
		for f_ori in *.ORI ; do
			f=`echo $f_ori | sed -e 's|.ORI||g'`
			# diff $f $f_ori
		done

		echo "Running OSGi TCK tests now..."
		chmod u+x runtests
		./runtests
	) 2>&1 | tee $DEST_DIR/RUN.log
	exit 0
fi

if [ "$1" == "clean" ] ; then
	(
		rm -v -r -f $DEST_DIR/*.log
		rm -v -r -f $DEST_DIR/run
		rm -v -r -f $DEST_DIR/*.jar
		if [ -d $DEST_DIR ] ; then
			rmdir $DEST_DIR
		fi

		if [ ! -z $OSGI_INSTALL_DIR ] ; then
			cd $OSGI_INSTALL_DIR
			if [ -d ./build ] ; then
				rm -v -r -f ./build/.git
				rm -v -r -f ./build/.git*
				rm -v -r -f ./build/*
				rm -v -f ./.DS_Store
				rmdir ./build
			fi
		fi
	)
	exit 0
fi

if [ "$1" == "src-prepare" ] ; then
	(
		_current_dir=`pwd`
		cd $OSGI_INSTALL_DIR
		if [ -f ./build-ori.tar.gz ] ; then
			echo "Uncompressing TCK from $OSGI_REPO/build.git from file build-ori.tar.gz ..."
			tar xzf build-ori.tar.gz
		elif [ ! -d ./build ] ; then
			echo "Cloning TCK from $OSGI_REPO/build.git ..."
			# check out the tag of TCK for R6
			git clone -b r6-core-ri-ct-final $OSGI_REPO/build.git
			echo "Compressing TCK from $OSGI_REPO/build.git for later usage ..."
			tar czf build-ori.tar.gz ./build
		else
			echo "TCK yet installed at $OSGI_INSTALL_DIR/build ..."
		fi

		echo "Now patching TCK to get it running ..."
		cd ./build
		
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

		# fix problem with JavaDoc. See https://bugs.openjdk.java.net/browse/JDK-8041628
		if [ ! -f osgi.build/build.xml.ORI ] ; then
			cp osgi.build/build.xml osgi.build/build.xml.ORI
		fi
		# remove generating javadoc from build
		cat osgi.build/build.xml.ORI | \
			sed -e 's|master.deploy,javadoc,japitools|master.deploy,japitools|g' >osgi.build/build.xml

		# show patched files
		find . -name "*.ORI"

		# show java version
		java -version
		
		# now build the TCK
		ant clean
		ant build

	) | tee -a $DEST_DIR/SRC-PREPARE.log
	
	exit 0
fi

if [ "$1" == "src-run" ] ; then
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
		cp ./_under-test/org.eclipse.concierge-6.0.0*.jar $DEST_DIR/run/concierge/org.eclipse.concierge-6.0.0.jar
		cp ./_under-test/org.eclipse.concierge.service.permission-5.1.0*.jar $DEST_DIR/run/concierge/org.eclipse.concierge.service.permission-5.1.0.jar
	
		cd $DEST_DIR/run
		# patch some files to run Concierge
		# save all files to ORI
		for f in \
			org.osgi.test.cases.condpermadmin.bnd \
			org.osgi.test.cases.framework.launch.secure.bnd \
			org.osgi.test.cases.framework.secure.bnd \
			org.osgi.test.cases.permissionadmin.bnd \
			shared.inc ; do
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
		cat shared.inc.ORI | sed -e 's|jar/org.eclipse.osgi-3.8.0.jar|concierge/org.eclipse.concierge-6.0.0.jar|g' >shared.inc
		
		# show diff
		for f_ori in *.ORI ; do
			f=`echo $f_ori | sed -e 's|.ORI||g'`
			# diff $f $f_ori
		done
		
		chmod u+x runtests
		./runtests
	) 2>&1 | tee $DEST_DIR/SRC-RUN.log
	exit 0
fi
