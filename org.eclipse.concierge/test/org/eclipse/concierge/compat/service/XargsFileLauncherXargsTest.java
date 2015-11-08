/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.compat.service;

import static org.hamcrest.CoreMatchers.is;

import java.io.File;

import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 * Tests the XargsFileLauncher with different xargs files.
 */
public class XargsFileLauncherXargsTest extends AbstractConciergeTestCase {

	private String dir;
	private File fileA;
	private File fileB;
	private File fileC;

	@Before
	public void setUp() throws Exception {
		dir = "build/tests/testXargsFileLauncher";
		new File(dir).mkdirs();
		SyntheticBundleBuilder builder;

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleA");
		fileA = builder.asFile(dir + "/bundleA.jar");

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleB")
				.addManifestHeader("Require-Bundle", "bundleA");
		fileB = builder.asFile(dir + "/bundleB.jar");

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleC")
				.addManifestHeader("Require-Bundle", "bundleB");
		fileC = builder.asFile(dir + "/bundleC.jar");
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
		fileC.delete();
		fileB.delete();
		fileA.delete();
		fileA = fileB = fileC = null;
		new File(dir).delete();
		dir = null;
	}

	/** Test is these bundles can be started with default start level 1. */
	@Test
	public void testDefaultStartLevel() throws Exception {
		File f = TestUtils.createFileFromString(
				"-Dorg.osgi.framework.storage.clean=onFirstInit             \n"
						+ "-istart " + fileA.getPath() + "# Comment to align\n"
						+ "-istart " + fileB.getPath() + "# Comment to align\n"
						+ "-istart " + fileC.getPath() + "# Comment to align\n",
				"xargs");
		framework = Concierge.doMain(new String[] { f.toString() });
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles.length, is(4));
		assertBundlesActive(bundles);
		Assert.assertThat(asBSL(bundles[1]).getStartLevel(), is(1));
		Assert.assertThat(asBSL(bundles[2]).getStartLevel(), is(1));
		Assert.assertThat(asBSL(bundles[3]).getStartLevel(), is(1));
	}

	/** Test if start level will be applied correct. */
	@Test
	public void testStartLevelIStart123() throws Exception {
		File f = TestUtils.createFileFromString(
				"-Dorg.osgi.framework.storage.clean=onFirstInit             \n"
						+ "-initlevel 1                   # Comment to align\n"
						+ "-istart " + fileA.getPath() + "# Comment to align\n"
						+ "-initlevel 2                   # Comment to align\n"
						+ "-istart " + fileB.getPath() + "# Comment to align\n"
						+ "-initlevel 3                   # Comment to align\n"
						+ "-istart " + fileC.getPath() + "# Comment to align\n",
				"xargs");
		framework = Concierge.doMain(new String[] { f.toString() });
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles.length, is(4));
		assertBundlesActive(bundles);
		Assert.assertThat(asBSL(bundles[1]).getStartLevel(), is(1));
		Assert.assertThat(asBSL(bundles[2]).getStartLevel(), is(2));
		Assert.assertThat(asBSL(bundles[3]).getStartLevel(), is(3));
	}

	/** Test if start level will be applied correct. */
	@Test
	public void testStartLevelIStart321() throws Exception {
		File f = TestUtils.createFileFromString(
				"-Dorg.osgi.framework.storage.clean=onFirstInit             \n"
						+ "-initlevel 3                   # Comment to align\n"
						+ "-istart " + fileA.getPath() + "# Comment to align\n"
						+ "-initlevel 2                   # Comment to align\n"
						+ "-istart " + fileB.getPath() + "# Comment to align\n"
						+ "-initlevel 1                   # Comment to align\n"
						+ "-istart " + fileC.getPath() + "# Comment to align\n",
				"xargs");
		framework = Concierge.doMain(new String[] { f.toString() });
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles.length, is(4));
		assertBundlesActive(bundles);
		Assert.assertThat(asBSL(bundles[1]).getStartLevel(), is(3));
		Assert.assertThat(asBSL(bundles[2]).getStartLevel(), is(2));
		Assert.assertThat(asBSL(bundles[3]).getStartLevel(), is(1));
	}

	/** Test if start level of 4 will be applied correct. */
	@Test
	@Ignore("TODO does not work: bundles with startlevel > 3 will NOT be started")
	public void testStartLevelIStart4() throws Exception {
		File f = TestUtils.createFileFromString(
				"-Dorg.osgi.framework.storage.clean=onFirstInit             \n"
						+ "-initlevel 4                   # Comment to align\n"
						+ "-istart " + fileA.getPath() + "# Comment to align\n",
				"xargs");
		framework = Concierge.doMain(new String[] { f.toString() });
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles.length, is(2));
		assertBundlesActive(bundles);
		Assert.assertThat(asBSL(bundles[1]).getStartLevel(), is(4));
	}

	/**
	 * Test if start level will be applied correct, when start levels are bigger
	 * than 3.
	 */
	@Test
	@Ignore("TODO does not work: bundles with startlevel > 3 will NOT be started")
	public void testStartLevelIStart102030() throws Exception {
		File f = TestUtils.createFileFromString(
				"-Dorg.osgi.framework.storage.clean=onFirstInit             \n"
						+ "-initlevel 10                  # Comment to align\n"
						+ "-istart " + fileA.getPath() + "# Comment to align\n"
						+ "-initlevel 20                  # Comment to align\n"
						+ "-istart " + fileB.getPath() + "# Comment to align\n"
						+ "-initlevel 30                  # Comment to align\n"
						+ "-istart " + fileC.getPath() + "# Comment to align\n",
				"xargs");
		framework = Concierge.doMain(new String[] { f.toString() });
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles.length, is(4));
		assertBundlesActive(bundles);
		Assert.assertThat(asBSL(bundles[1]).getStartLevel(), is(10));
		Assert.assertThat(asBSL(bundles[2]).getStartLevel(), is(20));
		Assert.assertThat(asBSL(bundles[3]).getStartLevel(), is(30));
	}

	/**
	 * Test if start level will be applied correct when using install and start
	 * separately.
	 */
	@Test
	public void testStartLevelInstallStart123() throws Exception {
		File f = TestUtils.createFileFromString(
				"-Dorg.osgi.framework.storage.clean=onFirstInit             \n"
						+ "-initlevel 1                   # Comment to align\n"
						+ "-install " + fileA.getPath() + "#Comment to align\n"
						+ "-initlevel 2                   # Comment to align\n"
						+ "-install " + fileB.getPath() + "#Comment to align\n"
						+ "-initlevel 3                   # Comment to align\n"
						+ "-install " + fileC.getPath() + "#Comment to align\n"
						+ "-initlevel 1                   # Comment to align\n"
						+ "-start  " + fileA.getPath() + "# Comment to align\n"
						+ "-initlevel 2                   # Comment to align\n"
						+ "-start  " + fileB.getPath() + "# Comment to align\n"
						+ "-initlevel 3                   # Comment to align\n"
						+ "-start  " + fileC.getPath() + "# Comment to align\n",
				"xargs");
		framework = Concierge.doMain(new String[] { f.toString() });
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertThat(bundles.length, is(4));
		assertBundlesActive(bundles);
		Assert.assertThat(asBSL(bundles[1]).getStartLevel(), is(1));
		Assert.assertThat(asBSL(bundles[2]).getStartLevel(), is(2));
		Assert.assertThat(asBSL(bundles[3]).getStartLevel(), is(3));
	}

	private BundleStartLevel asBSL(Bundle b) {
		return b.adapt(BundleStartLevel.class);
	}

}
