/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Tests which install bundles which refers to native code.
 * 
 * TODO add more tests and combinations for Bundle-NativeCode, for more OS/ARCH
 * types
 * 
 * @author Jochen Hiller
 */
public class BundlesWithNativeCodeTest extends AbstractConciergeTestCase {

	@Before
	public void setUp() throws Exception {
		startFramework();
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	@Test
	public void testBundleNativeCodeMacOSX_X86_64() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeMacOSX_X86_64")
				.addManifestHeader("Bundle-Version", "1.0.0")
				.addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=MacOSX; processor=x86_64");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isMacOSX() && isX86_64(), resolved);
	}

	@Test
	public void testBundleNativeCodeMacOSX_X86_64_WithSelectionFilter()
			throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName(
				"testBundleNativeCodeMacOSX_X86_64_WithSelectionFilter")
				.addManifestHeader("Bundle-Version", "1.0.0")
				.addManifestHeader(
						"Bundle-NativeCode",
						"lib/native/someNative.so; osname=Linux; processor=ARM; selection-filter=\"(&(kura.arch=armv7_hf))\","
								+ "lib/native/otherNative.so; osname=MacOSX; processor=x86_64");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isMacOSX() && isX86_64(), resolved);
	}

	@Test
	public void testBundleNativeCodeMacOSX_X86() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeMacOSX_X86")
				.addManifestHeader("Bundle-Version", "1.0.0")
				.addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=MacOSX; processor=x86");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isMacOSX() && isX86(), resolved);
	}

	private boolean isMacOSX() {
		final String osname = System.getProperty("os.name");
		return "Mac OS X".equals(osname);
	}

	private boolean isX86_64() {
		final String osarch = System.getProperty("os.arch");
		return "x86_64".equals(osarch);
	}

	private boolean isX86() {
		final String osarch = System.getProperty("os.arch");
		return "x86".equals(osarch);
	}
}
