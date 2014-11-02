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
package org.eclipse.concierge.test.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Tests for Concierge services, e.g. XML parser service.
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConciergeServicesTest extends AbstractConciergeTestCase {

	/**
	 * Tests whether a XML parser can be retrieved with a ServiceTracker.
	 */
	@Test
	public void test01JavaxXmlSAXFactoryServiceTracker() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			// add javax.xml.parsers to system extra packages
			// needed as bundle service.xmlparser will make an import package of
			// XML parsers
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers");
			startFrameworkClean(launchArgs);

			// install and start the Concierge service to register XML parsers
			final Bundle xmlParserServiceBundle = installAndStartBundle("org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar");
			assertBundleActive(xmlParserServiceBundle);

			// install pseudo bundle
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"concierge.test.test01JavaxXmlSAXFactoryServiceTracker")
					.bundleVersion("1.0.0")
					.addManifestHeader("Import-Package",
							"org.osgi.framework,org.osgi.util.tracker");
			final Bundle bundleUnderTest = installBundle(builder);
			bundleUnderTest.start();
			assertBundleActive(bundleUnderTest);

			/**
			 * this code will essentially do:
			 * 
			 * <pre>
			 * xmlTracker = new ServiceTracker(Activator.getContext(),
			 * 		SAXParserFactory.class.getName(), null);
			 * xmlTracker.open();
			 * return (SAXParserFactory) xmlTracker.getService();
			 * </pre>
			 */
			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
			// xmlTracker = new ServiceTracker(Activator.getContext(),
			// SAXParserFactory.class.getName(), null);
			Object xmlTracker = runner.createInstance(
					"org.osgi.util.tracker.ServiceTracker", new String[] {
							"org.osgi.framework.BundleContext",
							"java.lang.String",
							"org.osgi.util.tracker.ServiceTrackerCustomizer" },
					new Object[] { bundleUnderTest.getBundleContext(),
							"javax.xml.parsers.SAXParserFactory", null });
			Assert.assertEquals("org.osgi.util.tracker.ServiceTracker",
					xmlTracker.getClass().getName());

			// xmlTracker.open();
			Object res1 = runner
					.callMethod(xmlTracker, "open", new Object[] {});
			Assert.assertNull(res1);

			// return (SAXParserFactory) xmlTracker.getService();
			Object res2 = runner.callMethod(xmlTracker, "getService",
					new Object[] {});
			Assert.assertNotNull(res2);
			// instance must be of type JavaSE default factory impl
			Assert.assertEquals(
					"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
					res2.getClass().getName());
		} finally {
			stopFramework();
		}
	}

	/**
	 * Tests whether XML parser retrieval does work with test support bundle.
	 */
	@Test
	public void test02CheckSaxParserWithBootdelegation() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			// add some classes to take from boot classloader
			// otherwise the classes are taken from wrong classloaders
			launchArgs
					.put("org.osgi.framework.bootdelegation",
							"com.sun.org.apache.xerces.internal.jaxp,javax.xml.parsers,org.xml.sax");
			// add javax.xml.parsers to system extra packages
			// needed as bundle service.xmlparser will make an import package of
			// XML parsers
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar",
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.concierge.test.support_1.0.0.jar", });
			assertBundlesResolved(bundles);

			final Bundle bundleUnderTest = installAndStartBundle("org.eclipse.concierge.test.support_1.0.0.jar");
			assertBundleResolved(bundleUnderTest);

			/**
			 * this code will essentially do:
			 * 
			 * <pre>
			 * import org.eclipse.concierge.test.support;
			 * 
			 * Activator.checkSAXParserFactory();
			 * SAXParserFactory factory = Monitor.saxParserFactory;
			 * assertEquals("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
			 *     factory.getClass().getName());
			 * SAXParser parser = Monitor.saxParser;
			 * assertEquals("com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl",
			 *     parser.getClass().getName());
			 * </pre>
			 */
			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
			// check SAXParserFacory and SAXParser
			Object _void = runner.callClassMethod(
					"org.eclipse.concierge.test.support.Activator",
					"checkSAXParserFactory", new Object[] {});
			Assert.assertNull(_void);
			Object factory = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor",
					"saxParserFactory");
			Assert.assertEquals(
					"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
					factory.getClass().getName());
			Object parser = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor", "saxParser");
			Assert.assertEquals(
					"com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl",
					parser.getClass().getName());
		} finally {
			stopFramework();
		}
	}

	/**
	 * shell-1.0.0 is not a valid JAR file: MANIFEST.MF is at wrong place
	 */
	@Test
	public void testShell100JarManifest() throws IOException {
		// this is OK
		File f1 = new File("./target/plugins/shell-1.0.0.jar");
		JarFile jf1 = new JarFile(f1);
		Manifest mf1 = jf1.getManifest();
		Assert.assertNotNull(mf1);
		jf1.close();
		// this does not work when opened as stream
		// reasons is that META-INF/MANIFEST.MF is not first entry
		// see also
		// http://stackoverflow.com/questions/13814891/jarinputstream-getmanifest-is-null
		// see also http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4338238
		// see also http://bugs.java.com/bugdatabase/view_bug.do?bug_id=5046178
		File f2 = new File("./target/plugins/shell-1.0.0.jar");
		FileInputStream fis2 = new FileInputStream(f2);
		JarInputStream jis2 = new JarInputStream(fis2);
		Manifest mf2 = jis2.getManifest();
		Assert.assertNotNull(mf2);
		jis2.close();
		fis2.close();
	}
}
