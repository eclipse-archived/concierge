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
	public void testConstructor() {
		Assert.assertNotNull(SyntheticBundleBuilder.newBuilder());
	}

	@Test
	public void testAsInputStream() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testAsInputStream").addManifestHeader(
				"Import-Package", "org.osgi.framework");
		InputStream is = builder.asInputStream();
		Assert.assertNotNull(is);
		is.close();
	}

	@Test
	public void testAsFileWithoutVersion() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testAsFileWithoutVersion")
				.addManifestHeader("Import-Package", "org.osgi.framework");
		File f = builder.asFile();
		f.deleteOnExit();
		Assert.assertNotNull(f);
		Assert.assertEquals("concierge-testAsFileWithoutVersion-0.0.0.jar",
				f.getName());
	}

	@Test
	public void testAsFileWithVersion() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testAsFileWithVersion")
				.bundleVersion("1.0.0")
				.addManifestHeader("Import-Package", "org.osgi.framework");
		File f = builder.asFile();
		f.deleteOnExit();
		Assert.assertNotNull(f);
		Assert.assertEquals("concierge-testAsFileWithVersion-1.0.0.jar",
				f.getName());
	}

	@Test
	public void testAsFileWithDestFileName() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testAsFileWithDestFileName")
				.bundleVersion("1.0.0")
				.addManifestHeader("Import-Package", "org.osgi.framework");
		File f = builder
				.asFile("build/tests/testAsFileWithDestFileName-1.0.0.jar");
		f.deleteOnExit();
		Assert.assertNotNull(f);
		Assert.assertTrue(f.exists());
	}

	@Test
	public void testFluentInterface() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testFluentInterface").singleton()
				.addManifestHeader("Import-Package", "org.osgi.framework");
		Assert.assertEquals("testFluentInterface;singleton:=true",
				builder.getBundleSymbolicName());
		InputStream is = builder.asInputStream();
		Assert.assertNotNull(is);
		is.close();
	}

	@Test
	public void testAddFiles() throws IOException {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		File f = TestUtils.createFileFromString("<xml>", "xml");
		builder.bundleSymbolicName("testAddFiles").bundleVersion("1.0.0")
				.addManifestHeader("Import-Package", "org.osgi.framework")
				.addFile("plugin.xml", f)
				.addFile("plugin.properties", "name=value");
		InputStream is = builder.asInputStream();
		JarInputStream jis = new JarInputStream(is);
		Manifest mf = jis.getManifest();
		Assert.assertEquals("1.0.0",
				mf.getMainAttributes().getValue("Bundle-Version"));
		Assert.assertEquals("org.osgi.framework", mf.getMainAttributes()
				.getValue("Import-Package"));
		JarEntry je1 = jis.getNextJarEntry();
		Assert.assertEquals("plugin.xml", je1.getName());
		JarEntry je2 = jis.getNextJarEntry();
		Assert.assertEquals("plugin.properties", je2.getName());
		jis.close();
		is.close();
	}

}
