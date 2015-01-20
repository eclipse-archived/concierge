package org.eclipse.concierge;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Tests the main option "-all" with dependencies.
 */
public class ConciergeMainOptionAllTest extends AbstractConciergeTestCase {

	private String dir;
	private File fileA;
	private File fileB;
	private File fileC;

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	/**
	 * Setup Bundles: A, B requires A, C requires B. So natural order can be
	 * used to start.
	 */
	private void setupSortedBundles() throws Exception {
		dir = "build/tests/testAllWithSortedBundles";
		new File(dir).mkdirs();
		SyntheticBundleBuilder builder;

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleA");
		fileA = builder.asFile(dir + "/bundleA.jar");
		fileA.deleteOnExit();

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleB").addManifestHeader(
				"Require-Bundle", "bundleA");
		fileB = builder.asFile(dir + "/bundleB.jar");
		fileB.deleteOnExit();

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleC").addManifestHeader(
				"Require-Bundle", "bundleB");
		fileC = builder.asFile(dir + "/bundleC.jar");
		fileC.deleteOnExit();
	}

	/**
	 * Setup Bundles: A requires B, B requires C, C. So bundles have to be
	 * installed first, and can be started afterwards.
	 */
	private void setupUnsortedBundles() throws Exception {
		dir = "build/tests/testAllWithUnsortedBundles";
		new File(dir).mkdirs();
		SyntheticBundleBuilder builder;

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleA").addManifestHeader(
				"Require-Bundle", "bundleB");
		fileA = builder.asFile(dir + "/bundleA.jar");
		fileA.deleteOnExit();

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleB").addManifestHeader(
				"Require-Bundle", "bundleC");
		fileB = builder.asFile(dir + "/bundleB.jar");
		fileB.deleteOnExit();

		builder = new SyntheticBundleBuilder();
		builder.bundleSymbolicName("bundleC");
		fileC = builder.asFile(dir + "/bundleC.jar");
		fileC.deleteOnExit();
	}

	/** Test is these bundles can be started at all. */
	@Test
	public void testSortedBundles() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		startFrameworkClean(launchArgs);
		setupSortedBundles();
		Bundle bundleA = installBundle(fileA.getPath());
		Bundle bundleB = installBundle(fileB.getPath());
		Bundle bundleC = installBundle(fileC.getPath());
		bundleA.start();
		bundleB.start();
		bundleC.start();
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertEquals(4, bundles.length);
		assertBundlesActive(bundles);
	}

	/** Test now if they can be started with -all option. */
	@Test
	public void testAllWithSortedBundles() throws Exception {
		setupSortedBundles();
		framework = Concierge
				.doMain(new String[] { "-Dorg.eclipse.concierge.debug=true",
						"-Dorg.osgi.framework.storage.clean=onFirstInit",
						"-all", dir });
		Assert.assertNotNull(framework);
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertEquals(4, bundles.length);
		assertBundlesActive(bundles);
	}

	/** Test is these bundles can be started at all. */
	@Test
	public void testUnsortedBundles() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		startFrameworkClean(launchArgs);
		setupUnsortedBundles();
		Bundle bundleA = installBundle(fileA.getPath());
		Bundle bundleB = installBundle(fileB.getPath());
		Bundle bundleC = installBundle(fileC.getPath());
		bundleA.start();
		bundleB.start();
		bundleC.start();
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertEquals(4, bundles.length);
		assertBundlesActive(bundles);
	}

	/** Test now if they can be started with -all option. */
	@Test
	public void testAllWithUnortedBundles() throws Exception {
		setupUnsortedBundles();
		framework = Concierge
				.doMain(new String[] { "-Dorg.eclipse.concierge.debug=true",
						"-Dorg.osgi.framework.storage.clean=onFirstInit",
						"-all", dir });
		Assert.assertNotNull(framework);
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertEquals(4, bundles.length);
	}

}
