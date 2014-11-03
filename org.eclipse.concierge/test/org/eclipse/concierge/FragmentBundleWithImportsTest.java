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
package org.eclipse.concierge;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;

/**
 * This tests will check whether fragment bundles will be resolved when using
 * import package statements. These import package statements can match the host
 * one's or will have a conflict with host based on version or other directives.
 *
 * TODO add more tests for directives
 * 
 * @author Jochen Hiller - Initial Contribution
 */
@RunWith(Parameterized.class)
public class FragmentBundleWithImportsTest extends AbstractConciergeTestCase {

	@Parameters(name = "{index}: {0} should be resolved: {3}")
	public static Collection<Object[]> data() {
		return Arrays
				.asList(new Object[][] {
						// note: provided version is 1.2.3
						{ "Import packages are equals", "package1", "package1",
								true },
						{ "Import of fragment is newer than host",
								"package1;version=\"1.0.0\"",
								"package1;version=\"2.0.0\"", false },
						{
								"Import of fragment is older than host, and host is less than provider",
								"package1;version=\"1.0.0\"",
								"package1;version=\"0.0.1\"", false },
						{ "Import of fragment is exact the host",
								"package1;version=\"1.2.3\"",
								"package1;version=\"1.2.3\"", true },
				// TODO this should be resolved
				// {
				// "Import of host is exact the provider, fragment is less than",
				// "package1;version=\"1.2.3\"",
				// "package1;version=\"1.0.0\"", true },

				});
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
		builder.bundleSymbolicName("FragmentBundleWithImportsTest.provider")
				.addManifestHeader("Export-Package",
						"package1;version=\"1.2.3\"");
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
		Bundle fragmentBundle = installFragmentBundle(this.fragmentImportPackage);
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
		builder.bundleSymbolicName("FragmentBundleWithImportsTest.host")
				.addManifestHeader("Import-Package", importPackageHeader);
		Bundle hostBundle = installBundle(builder);
		return hostBundle;
	}

	/**
	 * Installs the fragment bundle with given import package statement.
	 */
	private Bundle installFragmentBundle(String importPackageHeader)
			throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("FragmentBundleWithImportsTest.fragment")
				.addManifestHeader("Fragment-Host",
						"FragmentBundleWithImportsTest.host")
				.addManifestHeader("Import-Package", importPackageHeader);
		Bundle fragmentBundle = installBundle(builder);
		return fragmentBundle;
	}
}
