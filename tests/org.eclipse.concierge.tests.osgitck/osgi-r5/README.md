# Eclipse Concierge - OSGi TCK

## Overview

Eclipse Concierge is an OSGi Framework R5 implementation.
To assure that, the OSGi TCK (Testing Compatibility Kit) will be executed on every release version.

Note: As the OSGi TCK is only available to OSGi Members, you need access to Git repos from OSGi Alliance to run the test suite. For more details see https://www.osgi.org/osgi-compliance/osgi-certification/

The process overall is like that:

* check out the tag `r5-core-ri-ct-final` from OSGi Git Build repository
* build the OSGi TCK
* extract the test suite generated, adapt it to your OSGi framework
* run the test suite
* check the reports

## How to run the TCK tests

To simplify the overall process we created some helper scripts which runs the TCK in an easy way.

First you have to set environment variables to specify the OSGI_REPO and the installation directory where the TCK will be cloned.

Sample:

```
export OSGI_REPO=https://.... # without build.git at the end
export OSGI_INSTALL_DIR=/home/myuser/osgi-r5
```

Then you can run the helper scripts that way:

```
# clean all previous cloned and build files
./run_tck_r5.sh --clean
# clone and build the OSGi TCK, may take 10-15 min
./run_tck_r5.sh --prepare
# run the test suite
./run_tck_r5.sh --run
```

The test results are then available at `./build/run/reports`.

