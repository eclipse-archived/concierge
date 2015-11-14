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
package org.eclipse.concierge;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;

/**
 * These tests will check whether fragment bundles will be resolved when using
 * import package statements. These import package statements can match the host
 * one's or will have a conflict with host based on version or other directives.
 * 
 * The test is setup using 3 types of bundles:
 * 
 * <pre>
 * Bundle "Provider": Export-Package = p1;version="1.2.3"
 * Bundle "Host":     Import-Package = p1
 * Bundle "Fragment": Import-Package = p1
 * </pre>
 *
 * The Import-Package statements will be specified with different versions, to
 * check whether this resolvement from Fragment to Host to Provider bundle is
 * possible or not.
 * 
 * See OSGi Spec R5, chapter 3.14
 * 
 * <pre>
 * When attaching a fragment bundle to a host bundle the Framework must perform the following
 * ...
 * 1. Append the import definitions for the Fragment bundle that do not conflict with an import 
 * definition of the host to the import definitions of the host bundle. A Fragment can provide an
 * import statement for a private package of the host. The private package in the host is 
 * hidden in that case.
 * ...
 * </pre>
 * 
 * TODO This tests does fail due to temporary change in
 * BundleImpl.checkConflicts, Line 2348. TODO add more tests for directives
 * 
 * @author Jochen Hiller - Initial Contribution
 */
@RunWith(Parameterized.class)
@Ignore("TODO Does not work due to temporary change in BundleImpl.Revision.checkConflicts")
public class FragmentBundleWithImportsTest extends AbstractConciergeTestCase {

	// Parameterized tests:
	// {0} description of test
	// {1} import package statement of host
	// {2} import package statement of fragment
	// {3} expected result: "has to be resolved": true/false
	@Parameters(name = "{index}: {0} for host={1} and fragment={2} resolved to: {3}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				// note: provided version is package;version="1.2.3"
				{ "Import packages are equals", "p1", "p1", true },
				{ "Import of fragment is newer than host",
						"p1;version=\"1.0.0\"", "p1;version=\"2.0.0\"", false },
				{ "Import of fragment is older than host, and host is less than provider",
						"p1;version=\"1.0.0\"", "p1;version=\"0.0.1\"", false },
				{ "Import of fragment is exact the host",
						"p1;version=\"1.2.3\"", "p1;version=\"1.2.3\"", true },
				// TODO is this correct?
				{ "Import of host is exact the provider, fragment is less than",
						"p1;version=\"1.2.3\"", "p1;version=\"1.0.0\"",
						true }, });
	}

	private String hostImportPackage;
	private String fragmentImportPackage;
	private boolean fragmentShouldBeResolved;

	public FragmentBundleWithImportsTest(String name, String host,
			String fragment, boolean shouldBeResolved) {
		this.hostImportPackage = host;
		this.fragmentImportPackage = fragment;
		this.fragmentShouldBeResolved = shouldBeResolved;
	}

	@Before
	public void setUp() throws Exception {
		startFramework();

		// provider bundle for hosting a package
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("Provider")
				.addManifestHeader("Export-Package", "p1;version=\"1.2.3\"");
		Bundle providerBundle = installBundle(builder);
		providerBundle.start();
		assertBundleActive(providerBundle);
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	@Test
	public void test() throws Exception {
		Bundle hostBundle = installHostBundle(this.hostImportPackage);
		Bundle fragmentBundle = installFragmentBundle(
				this.fragmentImportPackage);
		// now start host bundle. Check for fragment state based on expected
		// result
		hostBundle.start();
		assertBundleActive(hostBundle);
		if (this.fragmentShouldBeResolved) {
			assertBundleResolved(fragmentBundle);
		} else {
			assertBundleInstalled(fragmentBundle);
		}
	}

	/**
	 * Installs the host bundle with given import package statement.
	 */
	private Bundle installHostBundle(String importPackageHeader)
			throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("Host").addManifestHeader("Import-Package",
				importPackageHeader);
		Bundle hostBundle = installBundle(builder);
		return hostBundle;
	}

	/**
	 * Installs the fragment bundle with given import package statement.
	 */
	private Bundle installFragmentBundle(String importPackageHeader)
			throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("Fragment")
				.addManifestHeader("Fragment-Host", "Host")
				.addManifestHeader("Import-Package", importPackageHeader);
		Bundle fragmentBundle = installBundle(builder);
		return fragmentBundle;
	}
}
