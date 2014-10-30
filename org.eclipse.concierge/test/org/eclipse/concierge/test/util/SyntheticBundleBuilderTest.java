package org.eclipse.concierge.test.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.Assert;
import org.junit.Test;

public class SyntheticBundleBuilderTest {

	@Test
	public void test01Constructor() {
		Assert.assertNotNull(SyntheticBundleBuilder.newBuilder());
	}

	@Test
	public void test02AsInputStream() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("xxx").addManifestHeader("Import-Package",
				"org.osgi.framework");
		InputStream is = builder.asInputStream();
		Assert.assertNotNull(is);
		is.close();
	}

	@Test
	public void test03AsFileWithoutVersion() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("xxx").addManifestHeader("Import-Package",
				"org.osgi.framework");
		File f = builder.asFile();
		f.deleteOnExit();
		Assert.assertNotNull(f);
		Assert.assertEquals("concierge-xxx-0.0.0.jar", f.getName());
	}

	@Test
	public void test04AsFileWithVersion() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("xxx").bundleVersion("1.0.0")
				.addManifestHeader("Import-Package", "org.osgi.framework");
		File f = builder.asFile();
		f.deleteOnExit();
		Assert.assertNotNull(f);
		Assert.assertEquals("concierge-xxx-1.0.0.jar", f.getName());
	}

	@Test
	public void test10FluentInterface() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("xxx").singleton()
				.addManifestHeader("Import-Package", "org.osgi.framework");
		Assert.assertEquals("xxx;singleton:=true", builder.getBundleSymbolicName());
		InputStream is = builder.asInputStream();
		Assert.assertNotNull(is);
		is.close();
	}

	@Test
	public void test20AddFiles() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		File f = TestUtils.createFileFromString("<xml>", "xml");
		builder.bundleSymbolicName("xxx")
				.addManifestHeader("Bundle-Version", "1.0.0")
				.addManifestHeader("Import-Package", "org.osgi.framework")
				.addFile("plugin.xml", f);
		InputStream is = builder.asInputStream();
		JarInputStream jis = new JarInputStream(is);
		Manifest mf = jis.getManifest();
		Assert.assertEquals("1.0.0",
				mf.getMainAttributes().getValue("Bundle-Version"));
		Assert.assertEquals("org.osgi.framework", mf.getMainAttributes()
				.getValue("Import-Package"));
		JarEntry je = jis.getNextJarEntry();
		Assert.assertEquals("plugin.xml", je.getName());
		jis.close();
		is.close();
	}

}
