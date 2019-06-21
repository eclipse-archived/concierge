/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 * TODO jhi add more tests and combinations for Bundle-NativeCode, for more
 * OS/ARCH types
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
	public void testBundleNativeCodeMacOSX_X86() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeMacOSX_X86")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=MacOSX; processor=x86");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isMacOSX() && isX86(), resolved);
	}

	@Test
	public void testBundleNativeCodeLinux_X86() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeLinux_X86")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=Linux; processor=x86");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(!isWindows() && !isMacOSX() && isX86(), resolved);
	}

	@Test
	public void testBundleNativeCodeWindows_X86() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeWindows_X86")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative32.dll; osname=Windows; processor=x86");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isWindows() && isX86(), resolved);
	}

	@Test
	public void testBundleNativeCodeMacOSX_X86_64() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeMacOSX_X86_64")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=MacOSX; processor=x86_64");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isMacOSX() && isX86_64(), resolved);
	}

	@Test
	public void testBundleNativeCodeLinux_X86_64() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeLinux_X86_64")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=Linux; processor=x86_64");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(!isWindows() && !isMacOSX() && isX86_64(), resolved);
	}

	@Test
	public void testBundleNativeCodeWindows_X86_64() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeWindows_X86_64")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative64.dll; osname=Windows; processor=x86_64");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isWindows() && isX86_64(), resolved);
	}

	@Test
	public void testBundleNativeCodeMacOSX_X86_64_WithSelectionFilter()
			throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName(
				"testBundleNativeCodeMacOSX_X86_64_WithSelectionFilter")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=Linux; processor=ARM; selection-filter=\"(&(kura.arch=armv7_hf))\","
								+ "lib/native/otherNative.so; osname=MacOSX; processor=x86_64");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		Assert.assertEquals(isMacOSX() && isX86_64(), resolved);
	}

	@Test
	public void testBundleNatvieCodeOSDefaults() throws Exception {
		String osname = System.getProperty("os.name");
		// Windows fixed ... 
		if( isWindows()) osname = "Windows";
		String osarch = System.getProperty("os.arch");
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();

		builder.bundleSymbolicName("testBundleNatvieCodeOSDefaults")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						(isWindows()?"lib/someNative.dll":"lib/someNative.so") 
							+ "; osname=" + osname + "; processor=" + osarch 
						+ "");
		final Bundle bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	@Test
	public void testBundleNativeCodeWithWildcard() throws Exception {
		final SyntheticBundleBuilder builder = SyntheticBundleBuilder
				.newBuilder();
		builder.bundleSymbolicName("testBundleNativeCodeWithWildcard")
				.bundleVersion("1.0.0").addManifestHeader("Bundle-NativeCode",
						"lib/native/someNative.so; osname=Unknown; processor=x86, *");
		final Bundle bundleUnderTest = installBundle(builder);
		enforceResolveBundle(bundleUnderTest);
		final boolean resolved = isBundleResolved(bundleUnderTest);
		// due to wildcard has to be resolved ALWAYS
		Assert.assertEquals("Wildcard should always resolve", true, resolved);
	}

	private boolean isMacOSX() {
		final String osname = System.getProperty("os.name");
		return "Mac OS X".equals(osname);
	}

	private boolean isWindows() {
		final String osname = System.getProperty("os.name");
		return osname != null && osname.startsWith( "Windows");
	}

	private boolean isX86_64() {
		final String osarch = System.getProperty("os.arch");
		return "x86_64".equals(osarch) || "amd64".equals(osarch);
	}

	private boolean isX86() {
		final String osarch = System.getProperty("os.arch");
		return "x86".equals(osarch);
	}
}
