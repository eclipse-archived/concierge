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

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
public class FragmentBundleWithImportsTest extends AbstractConciergeTestCase {

	@Before
	public void setUp() throws Exception {
		startFramework();

		// provider bundle for hosting a package
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("FragmentBundleWithImportsTest.provider")
				.addManifestHeader("Export-Package",
						"package1;version=\"1.0.0\"");
		Bundle providerBundle = installBundle(builder);
		providerBundle.start();
		assertBundleActive(providerBundle);
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	/**
	 * This test will install a fragment bundle to a host bundle with NO
	 * conflicting import package directives (no version specified).
	 */
	@Test
	public void testFrameworkBundleNoConflictingImportPackages()
			throws Exception {
		Bundle hostBundle = installHostBundle("package1");
		Bundle fragmentBundle = installFragmentBundle("package1");
		// now start host bundle. Fragment bundle should be started too
		hostBundle.start();
		assertBundleActive(hostBundle);
		assertBundleResolved(fragmentBundle);
	}

	/**
	 * This test will install a fragment bundle to a host bundle with
	 * conflicting import package directives (fragment newer that host version).
	 */
	@Test
	public void testFrameworkBundleWithConflictingImportPackagesTooNew()
			throws Exception {
		Bundle hostBundle = installHostBundle("package1;version=\"1.0.0\"");
		Bundle fragmentBundle = installFragmentBundle("package1;version=\"2.0.0\"");
		// now start host bundle. Fragment bundle should NOT be started due to
		// package version conflicts
		hostBundle.start();
		assertBundleActive(hostBundle);
		assertBundleInstalled(fragmentBundle);
	}

	/**
	 * This test will install a fragment bundle to a host bundle with
	 * conflicting import package directives (host version newer than fragment
	 * version).
	 */
	@Test
	public void testFrameworkBundleWithConflictingImportPackagesTooOld()
			throws Exception {
		Bundle hostBundle = installHostBundle("package1;version=\"1.0.0\"");
		Bundle fragmentBundle = installFragmentBundle("package1;version=\"0.0.1\"");
		// now start host bundle. Fragment bundle should NOT be started due to
		// package version conflicts
		hostBundle.start();
		assertBundleActive(hostBundle);
		assertBundleInstalled(fragmentBundle);
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
