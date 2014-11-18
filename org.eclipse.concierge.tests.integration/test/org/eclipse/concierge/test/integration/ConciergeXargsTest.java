/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.test.integration;

import java.io.File;

import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.compat.service.XargsFileLauncher;
import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.LocalBundleStorage;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Test class to test different variants of init.xargs launcher.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConciergeXargsTest extends AbstractConciergeTestCase {

	// TODO create bundles on the fly, with SyntheticBundleBuilder as file
	@BeforeClass
	public static void preloadRequiredBundles() throws Exception {
		LocalBundleStorage storage = LocalBundleStorage.getInstance();
		// will lookup, and implicitly load into cache
		storage.getUrlForBundle("org.eclipse.osgi.services_3.4.0.v20140312-2051.jar");
		storage.getUrlForBundle("org.eclipse.equinox.util_1.0.500.v20130404-1337.jar");
		storage.getUrlForBundle("org.apache.felix.gogo.runtime_0.10.0.v201209301036.jar");
		storage.getUrlForBundle("org.eclipse.equinox.console_1.1.100.v20141023-1406.jar");
		storage.getUrlForBundle("org.eclipse.equinox.common_3.6.200.v20130402-1505.jar");
		storage.getUrlForBundle("org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar");
	}

	@Test
	public void test01XArgsVariantsOK() throws Exception {
		runOK("");
		runOK("# one line");
		runOK("# one line\n# second line");
		runOK("-Dprop=value");
		runOK("# one line\n-Dprop=value\n");
		runOK("  # one line\n# -Dprop=value\n");
		runOK("", 0);
		runOK("# one line", 0);
		runOK("# one line\n# second line", 0);
		runOK("-Dprop=value", 0);
		runOK("# one line\n-Dprop=value\n", 0);
		runOK("  # one line\n# -Dprop=value\n", 0);
	}

	@Test
	public void test02XArgsPropertiesMultiline() throws Exception {
		runOK("-Dprop=value");
		Assert.assertEquals("value", getFrameworkProperty("prop"));
		runOK("-Dprop=value1;\\\n" + "value2");
		Assert.assertEquals("value1;value2", getFrameworkProperty("prop"));
		runOK("-Dprop=value1; \\\n" + " value2\\\n" + " value3");
		Assert.assertEquals("value1;value2value3", getFrameworkProperty("prop"));
	}

	@Test
	public void test03XArgsPropertiesMultilineWithComment() throws Exception {
		runOK("-Dprop=value # comment");
		Assert.assertEquals("value", getFrameworkProperty("prop"));
		runOK("-Dprop=value1;\\ # comment \n" + "value2 # another comment");
		Assert.assertEquals("value1;value2", getFrameworkProperty("prop"));
		runOK("-Dprop=value1; \\#COMMENT\n" + " value2\\#COMMENT\n"
				+ " value3#COMMENT");
		Assert.assertEquals("value1;value2value3", getFrameworkProperty("prop"));
	}

	@Test
	public void test04XArgsMultiplePropertiesInOneLine() throws Exception {
		runOK("-Dprop1=value1\n" + "-Dprop2=value2");
		Assert.assertEquals("value1", getFrameworkProperty("prop1"));
		Assert.assertEquals("value2", getFrameworkProperty("prop2"));

		runOK("-Dprop1=value1\n" + "-Dprop2=value2\n"
				+ "-Dprop3=${prop1}${prop2}");
		Assert.assertEquals("value1", getFrameworkProperty("prop1"));
		Assert.assertEquals("value2", getFrameworkProperty("prop2"));
		Assert.assertEquals("value1value2", getFrameworkProperty("prop3"));

		runOK("-Dprop1=value1\n" + "-Dprop2=value2\n"
				+ "-Dprop3=${prop1}XXX${prop2}");
		Assert.assertEquals("value1", getFrameworkProperty("prop1"));
		Assert.assertEquals("value2", getFrameworkProperty("prop2"));
		Assert.assertEquals("value1XXXvalue2", getFrameworkProperty("prop3"));

		runOK("-Dprop1=value1\n" + "-Dprop2=value2\n"
				+ "-Dprop3=ABC${prop1}XXX${prop2}XYZ");
		Assert.assertEquals("value1", getFrameworkProperty("prop1"));
		Assert.assertEquals("value2", getFrameworkProperty("prop2"));
		Assert.assertEquals("ABCvalue1XXXvalue2XYZ",
				getFrameworkProperty("prop3"));
	}

	@Test
	public void test05XArgsPropertiesReplaceProperties() throws Exception {
		runOK("-Dprop1=value1\n" + "-Dprop2=${prop1}");
		Assert.assertEquals("value1", getFrameworkProperty("prop1"));
		Assert.assertEquals("value1", getFrameworkProperty("prop2"));
	}

	@Test
	public void test10XArgsInstallSomeBundles() throws Exception {
		runOK("-install ./target/plugins/concierge.test.version_1.0.0.jar", 1);
		runOK("-install ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-install ./target/plugins/concierge.test.version_1.1.0.jar",
				2);
		runOK("-install ./target/plugins/concierge.test.version_0.1.0.jar\n"
				+ "-install ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-install ./target/plugins/concierge.test.version_1.1.0.jar",
				3);
	}

	@Test
	public void test11XArgsInstallAndStartSomeBundles() throws Exception {
		runOK("-istart ./target/plugins/concierge.test.version_1.0.0.jar", 1,
				true);
		runOK("-istart ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1.0.jar",
				2, true);
		runOK("-istart ./target/plugins/concierge.test.version_0.1.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1.0.jar",
				3, true);
	}

	@Test
	public void test20XArgsInstallWithWildcard() throws Exception {
		runOK("-istart ./target/plugins/concierge.test.version*.jar", 1, true);
		runOK("-istart ./target/plugins/concierge.test.version_0*.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1*.jar", 2,
				true);
		runOK("-istart ./target/plugins/concierge.test.version_0*.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.0*.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1*.jar",
				3, true);
	}

	@Test
	public void test21XArgsInstallWithWildcard() throws Exception {
		runOK("-istart ./target/plugins/concierge.test.version*.jar", 1, true);
		runOK("-istart ./target/plugins/concierge.test.version*", 1, true);
		runOK("-istart ./target/plugins/concierge.test.*.jar", 1, true);
		runOK("-istart ./target/plugins/concierge.test.version_1.0.0.201407232153*.jar",
				1, true);
		runOK("-istart ./target/plugins/concierge.test.version_1.0.0.201407232153.jar*",
				1, true);
		runOK("-istart ./target/plugins/*concierge.test.version_1.0.0.201407232153.jar",
				1, true);
	}

	@Test
	public void test22XArgsInstallWithFailedWildcard() throws Exception {
		runFail("-istart ./target/plugins/concierge.test.versionABC*.jar");
		runFail("-istart ./target/plugins/concierge.test.version*ABC");
		runFail("-istart ./target/plugins/concierge.test.versionA*B.jar");
		runFail("-istart ./target/plugins/concierge.test.version_1.0.0.201407232154*.jar");
		runFail("-istart ./target/plugins/concierge.test.version_1.0.0.201407232153.jarAB*");
		runFail("-istart ./target/plugins/A*concierge.test.version_1.0.0.201407232153.jar");
		runFail("-istart ./target/plugins1/concierge.test.version*.jar");
	}

	/**
	 * Will check of the LogService will be used. Check console output, should
	 * be OSGi logger.
	 */
	@Test
	public void test23XArgsInstallAndStartWithLogServiceReference()
			throws Exception {
		// set to use Concierge builtin logger
		runOK("-Dorg.eclipse.concierge.log.enabled=true\n"
				+ "-Dorg.eclipse.concierge.log.level=4\n" // DEBUG
				+ "-istart ./target/plugins/concierge.test.version_1.0.0.jar",
				1, true);
	}

	/**
	 * TODO this test will fail at 8th check: will fail with assert, sometime
	 * with a JavaMV core dump. All other test will fail then too as bundles can
	 * not installed again. Seems to be related with shutdown in error case, and
	 * freeing ZIP bundle resources.
	 */
	@Test
	@Ignore
	public void test24XArgsInstallWithMultipleVersions() throws Exception {
		runWithCheck(
				"-istart ./target/plugins/concierge.test.version_1.0.0.jar",
				"file:././target/plugins/concierge.test.version_1.0.0.jar");
		runWithCheck("-istart ./target/plugins/concierge.test.version_1.*.jar",
				"file:././target/plugins/concierge.test.version_1.1.0.jar");
		runWithCheck("-istart ./target/plugins/concierge.test.version_*.jar",
				"file:././target/plugins/concierge.test.version_1.1.0.jar");
		runWithCheck("-istart ./target/plugins/concierge.test.version_0*.jar",
				"file:././target/plugins/concierge.test.version_0.2.0.jar");
		runWithCheck("-istart ./target/plugins/concierge.test.version_0.*.jar",
				"file:././target/plugins/concierge.test.version_0.2.0.jar");
		runWithCheck(
				"-istart ./target/plugins/concierge.test.version_0.1*.jar",
				"file:././target/plugins/concierge.test.version_0.1.0.jar");
		runWithCheck(
				"-istart ./target/plugins/concierge.test.version_1.0.0*.jar",
				"file:././target/plugins/concierge.test.version_1.0.0.jar");
		runWithCheck(
				"-istart ./target/plugins/concierge.test.version_1.0.0.*.jar",
				"file:././target/plugins/concierge.test.version_1.0.0.0.jar");
	}

	@Test
	public void test30XArgsInstallWithVariable() throws Exception {
		runOK("-Ddir=./target/plugins\n"
				+ "-install ${dir}/concierge.test.version*.jar", 1);
		runOK("-Ddir=./target/plugins\n"
				+ "-install ${dir}/concierge.test.version_0*.jar\n"
				+ "-install ${dir}/concierge.test.version_1.0*.jar\n"
				+ "-install ${dir}/concierge.test.version_1.1*.jar", 3);
	}

	@Test
	public void test31XArgsInstallAndStartWithVariable() throws Exception {
		runOK("-Ddir=./target/plugins\n"
				+ "-istart ${dir}/concierge.test.version*.jar", 1, true);
		runOK("-Ddir=./target/plugins\n"
				+ "-istart ${dir}/concierge.test.version_0*.jar\n"
				+ "-istart ${dir}/concierge.test.version_1.0*.jar\n"
				+ "-istart ${dir}/concierge.test.version_1.1*.jar", 3, true);
	}

	@Test
	public void test32XArgsInstallAndStartLaterWithVariable() throws Exception {
		runOK("-Ddir=./target/plugins\n"
				+ "-install ${dir}/concierge.test.version*.jar\n"
				+ "-start ${dir}/concierge.test.version*.jar", 1, true);
	}

	@Test
	public void test33XArgsSkipBetween() throws Exception {
		runOK("-istart ./target/plugins/concierge.test.version_0.1.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1.0.jar\n"
				+ "-skip", 3, true);
		runOK("-istart ./target/plugins/concierge.test.version_0.1.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-skip\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1.0.jar",
				2, true);
		runOK("-istart ./target/plugins/concierge.test.version_0.1.0.jar\n"
				+ "-skip\n"
				+ "-istart ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1.0.jar",
				1, true);
		runOK("-skip\n"
				+ "-istart ./target/plugins/concierge.test.version_0.1.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.0.0.jar\n"
				+ "-istart ./target/plugins/concierge.test.version_1.1.0.jar",
				0, true);
	}

	@Test
	public void test40XArgsInstallAndStartEquinoxRegistry() throws Exception {
		runOK("-Dcache.dir=./target/localCache\n"
				+ "-Dplugins.dir=./target/plugins\n"
				+ "-DbundlesFixed.dir=./target/bundles-fixed\n"
				+ "-Dpatched.dir=./target/patched\n"
				+ "-Dorg.osgi.framework.bootdelegation=sun.,com.sun.org.apache.xerces.internal.jaxp,\\\n"
				+ " javax.xml.parsers,org.xml.sax,org.xml.sax.helpers,javax.xml.transform,javax.script\n"
				+ "-Dorg.osgi.framework.system.packages.extra=javax.xml.parsers,org.xml.sax,org.xml.sax.helpers\n"
				+ "-install ${plugins.dir}/org.eclipse.concierge.extension.permission-1.0.0.alpha2.jar\n"
				+ "-istart ${plugins.dir}/org.eclipse.concierge.service.xmlparser-1.0.0.alpha2.jar\n"
				+ "-istart ${cache.dir}/org.eclipse.osgi.services_3.4.0.v20140312-2051.jar\n"
				+ "-istart ${bundlesFixed.dir}/org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar\n"
				+ "-istart ${cache.dir}/org.eclipse.equinox.util_1.0.500.v20130404-1337.jar\n"
				+ "-istart ${cache.dir}/org.apache.felix.gogo.runtime_0.10.0.v201209301036.jar\n"
				+ "-istart ${bundlesFixed.dir}/org.eclipse.equinox.console_1.1.100.v20141023-1406.jar\n"
				+ "-istart ${cache.dir}/org.eclipse.equinox.common_3.6.200.v20130402-1505.jar\n"
				+ "-istart ${cache.dir}/org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar\n",
				9, true);
	}

	// private methods

	private void runOK(String initXargs) throws Exception {
		runOK(initXargs, -1, false);
	}

	private void runOK(String initXargs, int noOfBundles) throws Exception {
		runOK(initXargs, noOfBundles, false);
	}

	private void runOK(String initXargs, int noOfBundles,
			boolean checkForResolved) throws Exception {
		final XargsFileLauncher xargsLauncher = new XargsFileLauncher();
		final File xargs = TestUtils.createFileFromString(initXargs, "xargs");
		try {
			final Concierge framework = xargsLauncher.processXargsFile(xargs);

			if (noOfBundles != -1) {
				int nInstalledBundles = framework.getBundle()
						.getBundleContext().getBundles().length;
				// framework bundle will be not included
				Assert.assertEquals(noOfBundles, nInstalledBundles - 1);
			}
			useFramework(framework);

			Bundle[] bundles = framework.getBundleContext().getBundles();
			if (checkForResolved) {
				assertBundlesResolved(bundles);
			}
		} finally {
			xargs.delete();
			stopFramework();
		}
	}

	private void runFail(String initXargs) throws Exception {
		final XargsFileLauncher xargsLauncher = new XargsFileLauncher();
		final File xargs = TestUtils.createFileFromString(initXargs, "xargs");
		try {
			xargsLauncher.processXargsFile(xargs);
			Assert.fail("Oops, this test should fail with bundle can not be installed exception");
		} catch (BundleException ex) {
			// OK, expected in failed tests
		} finally {
			xargs.delete();
			stopFramework();
		}
	}

	private void runWithCheck(String initXargs, String bundleLocation)
			throws Exception {
		final XargsFileLauncher xargsLauncher = new XargsFileLauncher();
		final File xargs = TestUtils.createFileFromString(initXargs, "xargs");
		try {
			final Concierge framework = xargsLauncher.processXargsFile(xargs);
			final Bundle b = framework.getBundleContext().getBundle(
					bundleLocation);
			Assert.assertNotNull(b);

			useFramework(framework);
		} finally {
			xargs.delete();
			stopFramework();
		}
	}

	private void createBundle(String name, String version) {
		SyntheticBundleBuilder builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName(name).bundleVersion(version)
				.addManifestHeader("Import-Package", "org.osgi.framework");
	}
}
