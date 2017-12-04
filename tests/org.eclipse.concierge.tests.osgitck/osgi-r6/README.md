# Eclipse Concierge - OSGi TCK

## Overview

Eclipse Concierge is an OSGi Framework R6 implementation.
To assure compatibility, the OSGi TCK (Testing Compatibility Kit) will be executed on every release version.

Note: As the OSGi TCK is only available to OSGi Members, you need access to build infrastructure and/or Git repos from OSGi Alliance to run the test suite. For more details see https://www.osgi.org/osgi-compliance/osgi-certification/

The overall process how to run the TCK is like that:

* get the OSGi TCK (named osgi.ct) from OSGi R6 release build
* extract the test suite, adapt it to your OSGi framework
* run the test suite
* check the reports

## How to run the TCK tests

To simplify the overall process we created some helper scripts which runs the TCK in an easy way.

First you have to set environment variables to specify the OSGI_MEMBER_USER and OSGI_MEMBER_PASSWORD to download the OSGi TCK from the website.

Sample:

```
export OSGI_MEMBER_USER="myuser@myomain.com"
export OSGI_MEMBER_USER="mysecret"
```

Then you can run the helper scripts that way:

```
# clean all previous generated files
./run_tck_r6.sh clean
# run the test suite
./run_tck_r6.sh run
```

The test results are then available at `./build/run/reports`.

## How to run the TCK tests from Sources

To simplify the overall process we created some helper scripts which build and runs the TCK in an easy way.

First you have to set environment variables to specify the OSGI_REPO and the installation directory where the TCK will be cloned.

Sample:

```
export OSGI_REPO=https://.... # without build.git at the end
export OSGI_INSTALL_DIR=/home/myuser/osgi-r6
```

Then you can run the helper scripts that way:

```
# clean all previous cloned and generated files
./run_tck_r6.sh clean
# clone and build the OSGi TCK, may take 20-30 min
./run_tck_r6.sh src-prepare
# run the test suite
./run_tck_r6.sh src-run
```

The test results are then available at `./build/run/reports`.

