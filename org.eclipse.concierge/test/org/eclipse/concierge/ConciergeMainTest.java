package org.eclipse.concierge;

import java.io.File;

import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the XargsLauncher with its pattern replacement and wildcard support.
 */
public class ConciergeMainTest {

	@Test
	public void testDoMainNull() throws Exception {
		Concierge framework = Concierge.doMain(null);
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainEmptyArray() throws Exception {
		Concierge framework = Concierge.doMain(new String[] {});
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainNotValidXargsFile() throws Exception {
		Concierge framework = Concierge
				.doMain(new String[] { "someunknownfile.xargs" });
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainXargsFileViaProperty() throws Exception {
		System.setProperty("org.eclipse.concierge.init.xargs",
				"someunknownfileviaproperty.xargs");
		Concierge framework = Concierge.doMain(new String[] {});
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainValidXargsFile() throws Exception {
		File f = TestUtils.createFileFromString("# ", "xargs");
		Concierge framework = Concierge.doMain(new String[] { f.toString() });
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainHelp() throws Exception {
		Concierge framework = Concierge.doMain(new String[] { "-help" });
		// when usage message, NO framework will be created
		Assert.assertNull(framework);
	}

	/**
	 * TODO: does not work. The file created is full path, e.g /data/...jar. As
	 * in installNewBundle there should be a check about filename with leading
	 * "/". In this case the bundle location should be file:///some/path...
	 * instead of file:.//some/path
	 */
	@Test
	@Ignore("Fails due to missing check for bundles with absolute path")
	public void testDoMainInstallBundleWithLeadingSlash() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testDoMainInstallBundleWithLeadingSlash");
		File f = builder.asFile();
		f.deleteOnExit();
		Concierge framework = Concierge.doMain(new String[] {
				"-Dorg.osgi.framework.storage.clean=onFirstInit", "-install",
				f.toString() });
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainInstallBundleWithFileAndLeadingSlash()
			throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testDoMainInstallBundleWithFileAndLeadingSlash");
		File f = builder.asFile();
		f.deleteOnExit();
		Concierge framework = Concierge.doMain(new String[] { "-install",
				"file://" + f.toString() });
		Assert.assertNotNull(framework);
		framework.stop();
	}

	@Test
	public void testDoMainInstallAndStartBundleWithFileAndLeadingSlash()
			throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testDoMainInstallAndStartBundleWithFileAndLeadingSlash");
		File f = builder
				.asFile("build/tests/testDoMainInstallAndStartBundleWithFileAndLeadingSlash-0.0.0.jar");
		f.deleteOnExit();
		// mix props and install directives
		Concierge framework = Concierge.doMain(new String[] {
				"-Dorg.eclipse.concierge.debug=true",
				"-Dorg.eclipse.concierge.debug.bundles=true",
				"-Dorg.eclipse.concierge.debug.packages=true",
				"-Dorg.eclipse.concierge.debug.services=true",
				// relative path to .
				"-istart", "file:./" + f.toString(),
				"-Dorg.eclipse.concierge.log.enabled=true",
				"-Dorg.eclipse.concierge.log.quiet=false",
				"-Dorg.eclipse.concierge.log.buffersize=100",
				"-Dorg.eclipse.concierge.log.level=4", });
		Assert.assertNotNull(framework);
		framework.stop();
	}
}
