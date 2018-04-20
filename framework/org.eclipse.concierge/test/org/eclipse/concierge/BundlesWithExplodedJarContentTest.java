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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Tests which install bundles which refers to native code.
 * 
 * TODO jhi add more tests and combinations for Bundle-NativeCode, for more OS/ARCH
 * types
 * 
 * @author Jochen Hiller
 */
public class BundlesWithExplodedJarContentTest extends
		AbstractConciergeTestCase {

	private Bundle bundleUnderTest;
	private String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private String propsContent = "pluginName = TestBundleWithExplodedJarContent";

	@Before
	public void setUp() throws Exception {
		startFramework();
	}

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	private void setupDefaultBundle() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testBundleWithExplodedJarContent")
				.bundleVersion("1.0.0");
		// enforce exploding jar
		File someJar = TestUtils.createFileFromString("some.jar Content");
		builder.addManifestHeader("Bundle-Classpath", ".,lib/some.jar");
		builder.addFile("lib/some.jar", someJar);
		// add resources
		builder.addFile("plugin.xml", xmlContent);
		builder.addFile("plugin.properties", propsContent);

		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	@Test
	public void testIsExploded0ClasspathEntries() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testIsExploded0ClasspathEntries");
		// do NOT set Bundle-Classpath
		// builder.addManifestHeader("Bundle-Classpath", "");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		// the files must NOT be in file system
		String explodedFolder = "./storage/default/1/content0";
		Assert.assertFalse(new File(explodedFolder).isDirectory());
	}

	@Test
	public void testIsExploded1ClasspathEntries() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testIsExploded1ClasspathEntries");
		// enforce exploding jar
		File someJar = TestUtils.createFileFromString("some.jar Content");
		builder.addFile("lib/some.jar", someJar);
		builder.addManifestHeader("Bundle-Classpath", ".,lib/some.jar");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		// the files must be in file system
		String explodedFolder = "./storage/default/1/content0";
		Assert.assertTrue(new File(explodedFolder).isDirectory());
		Assert.assertTrue(new File(explodedFolder + "/lib").isDirectory());
		Assert.assertTrue(new File(explodedFolder + "/lib/some.jar").exists());
		Assert.assertTrue(new File(explodedFolder + "/META-INF").isDirectory());
		Assert.assertTrue(new File(explodedFolder + "/META-INF/MANIFEST.MF")
				.exists());
	}

	@Test
	public void testIsExploded2ClasspathEntries() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testIsExploded2ClasspathEntries");
		// enforce exploding jar
		File someJar = TestUtils.createFileFromString("some.jar Content");
		File otherJar = TestUtils.createFileFromString("other.jar Content");
		builder.addFile("lib/some.jar", someJar);
		builder.addFile("lib/other.jar", otherJar);
		builder.addManifestHeader("Bundle-Classpath",
				".,lib/some.jar;lib/other.jar");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		// the files must be in file system
		String explodedFolder = "./storage/default/1/content0";
		Assert.assertTrue(new File(explodedFolder).isDirectory());
		Assert.assertTrue(new File(explodedFolder + "/lib").isDirectory());
		Assert.assertTrue(new File(explodedFolder + "/lib/some.jar").exists());
		Assert.assertTrue(new File(explodedFolder + "/lib/other.jar").exists());
		Assert.assertTrue(new File(explodedFolder + "/META-INF").isDirectory());
		Assert.assertTrue(new File(explodedFolder + "/META-INF/MANIFEST.MF")
				.exists());
	}

	@Test
	public void testBundleWithExplodedJarContentGetEntry() throws Exception {
		setupDefaultBundle();
		// the files MUST be in file system
		String explodedFolder = "./storage/default/1/content0";
		Assert.assertTrue(new File(explodedFolder).isDirectory());
		// Now test whether resource can be retrieved
		URL url1 = bundleUnderTest.getEntry("plugin.xml");
		Assert.assertNotNull(url1);
		String content1 = TestUtils.getContentFromUrl(url1);
		Assert.assertEquals(xmlContent, content1);

		URL url2 = bundleUnderTest.getEntry("plugin.properties");
		Assert.assertNotNull(url2);
		String content2 = TestUtils.getContentFromUrl(url2);
		Assert.assertEquals(propsContent, content2);

		URL url3 = bundleUnderTest.getEntry("unknown.txt");
		Assert.assertNull(url3);
	}

	@Test
	public void testBundleWithExplodedJarContentGetEntryPaths()
			throws Exception {
		setupDefaultBundle();
		// the files MUST be in file system
		String explodedFolder = "./storage/default/1/content0";
		Assert.assertTrue(new File(explodedFolder).isDirectory());
		// getEntryPaths
		Enumeration<String> urls = bundleUnderTest.getEntryPaths("/");
		checkEntryPaths(urls);
		urls = bundleUnderTest.getEntryPaths("/");
		List<String> urlsAsList = asList(urls);
		Assert.assertEquals(4, urlsAsList.size());
		Assert.assertTrue(urlsAsList.contains("plugin.xml"));
		Assert.assertTrue(urlsAsList.contains("plugin.properties"));
		Assert.assertTrue(urlsAsList.contains("lib/"));
		Assert.assertTrue(urlsAsList.contains("META-INF/"));
		Enumeration<String> urlsLib = bundleUnderTest.getEntryPaths("/lib/");
		List<String> urlsLibAsList = asList(urlsLib);
		Assert.assertEquals(1, urlsLibAsList.size());
		Assert.assertTrue(urlsLibAsList.contains("lib/some.jar"));
		Enumeration<String> urlsMetaInf = bundleUnderTest.getEntryPaths("/META-INF/");
		List<String> urlsMetaInfAsList = asList(urlsMetaInf);
		Assert.assertEquals(1, urlsMetaInfAsList.size());
		Assert.assertTrue(urlsMetaInfAsList.contains("META-INF/MANIFEST.MF"));
	}
	
	/**
	 * This test creates a bundle with an inner jar file which is on bundle
	 * classpath. The inner jar file contains some resources which should be
	 * visible via getResources() on a folder and should also be retrieved.
	 * 
	 * @author Simon Kaufmann
	 */
	@Test
	public void testBundleWithExplodedJarContentListResources()
			throws Exception {
		SyntheticBundleBuilder innerJar = SyntheticBundleBuilder.newBuilder();
		File someFile = TestUtils.createFileFromString("some content");
		innerJar.addFile("res/somefile.txt", someFile);
		File someJar = innerJar.asFile();

		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("testBundleWithEmbeddedJar")
				.bundleVersion("1.0.0");
		builder.addManifestHeader("Bundle-Classpath", "lib/some.jar,.");
		builder.addFile("lib/some.jar", someJar);
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);

		ClassLoader classLoader = bundleUnderTest.adapt(BundleWiring.class)
				.getClassLoader();
		Enumeration<URL> resources = classLoader.getResources("res");
		ArrayList<URL> urls = Collections.list(resources);
		assertNotNull(urls);
		assertEquals(1, urls.size());

		URI uri = urls.get(0).toURI();
		assertEquals("bundle", uri.getScheme());
		assertEquals("/lib/some.jar", uri.getPath());
		assertEquals("res", uri.getFragment());

		Collection<String> resourceList = bundleUnderTest
				.adapt(BundleWiring.class)
				.listResources("/" + uri.getFragment() + "/", "*.txt", BundleWiring.LISTRESOURCES_LOCAL);
		assertNotNull(resourceList);
		assertEquals(1, resourceList.size());
		String resourceName = resourceList.iterator().next();
		assertEquals("res/somefile.txt", resourceName);
		
		// now check if resource can be read
		URL contentUrl = classLoader.getResource(resourceName);
		assertNotNull (contentUrl);
		String content = TestUtils.getContentFromUrl(contentUrl);
		Assert.assertEquals("some content", content);
	}



	private void checkEntryPaths(Enumeration<String> urls) {
		Assert.assertNotNull(urls);
		while (urls.hasMoreElements()) {
			String path = urls.nextElement();
			Assert.assertNotNull(path);
			URL url = bundleUnderTest.getEntry(path);
			Assert.assertNotNull(url);
			if (path.endsWith("/")) {
				Enumeration<String> childUrls = bundleUnderTest
						.getEntryPaths(path);
				checkEntryPaths(childUrls);
			} else {
				String s = TestUtils.getContentFromUrl(url);
				if ("plugin.xml".equals(path)) {
					Assert.assertEquals(xmlContent, s);
				} else if ("plugin.properties".equals(path)) {
					Assert.assertEquals(propsContent, s);
				} else {
					Assert.assertNotNull(s);
				}
			}
		}
	}

	private List<String> asList(Enumeration<String> e) {
		List<String> l = new ArrayList<String>();
		while (e.hasMoreElements()) {
			String s = e.nextElement();
			l.add(s);
		}
		return l;
	}

}
