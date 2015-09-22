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
package org.eclipse.concierge.test.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Tests for using Equinox service implementations within Concierge framework.
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EclipseEquinoxTest extends AbstractConciergeTestCase {

	@Override
	protected boolean stayInShell() {
		return false;
	}

	/**
	 * Just load OSGi services compendium.
	 */
	@Test
	public void test01EquinoxOSGiServices() throws Exception {
		try {
			startFramework();

			final String bundleName = "org.eclipse.osgi.services_3.4.0.v20140312-2051.jar";
			final Bundle bundle = installAndStartBundle(bundleName);
			assertBundleResolved(bundle);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Just load OSGi services compendium but system packages are defined.
	 */
	@Test
	public void test02EquinoxOSGiServicesWithSystemPackages() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			// the value of this property does not matter. If the property is
			// NOT set this test will work
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"com.example.some.package");
			startFrameworkClean(launchArgs);

			final String bundleName = "org.eclipse.osgi.services_3.4.0.v20140312-2051.jar";
			final Bundle bundle = installAndStartBundle(bundleName);
			assertBundleResolved(bundle);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox DS requires bundles Equinox-Util, and org.osgi.service.cm (from
	 * org.eclipse.osgi.services).
	 * 
	 * But it fails with missing requirements:
	 * 
	 * <pre>
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.framework.log;version="1.0.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.service.debug;version="1.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.service.environment;version="1.2.0"} CANDIDATES WERE []
	 * COULD NOT RESOLVE REQUIREMENT BundleRequirement{Import-Package org.eclipse.osgi.util} CANDIDATES WERE []
	 * </pre>
	 */
	@Test
	public void test03EquinoxSupplement() throws Exception {
		try {
			startFramework();
			final String bundleName = "org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar";
			final Bundle bundle = installAndStartBundle(bundleName);
			assertBundleResolved(bundle);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test04EquinoxCommon() throws Exception {
		try {
			startFramework();
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox Console requires bundles some bundles.
	 * 
	 * <pre>
	 * org.osgi.framework.BundleException: Resolution failed [
	 * BundleRequirement{Import-Package org.eclipse.osgi.framework.console}, 
	 * BundleRequirement{Import-Package org.eclipse.osgi.report.resolution; version="[1.0,2.0)"}, 
	 * BundleRequirement{Import-Package org.osgi.framework.namespace;version="1.1.0"}]
	 * </pre>
	 */
	@Test
	public void test05EquinoxConsole() throws Exception {
		try {
			startFramework();

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.apache.felix.gogo.runtime_0.10.0.v201209301036.jar",
					// required by Equinox Console, is not optional
					"org.eclipse.concierge.extension.permission_1.0.0.201408052201.jar",
					"org.eclipse.equinox.console_1.1.100.v20141023-1406.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * This test will work but will produce an error message in console. This
	 * indicates that resource can not be looked up the correct way. See
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=438781
	 * 
	 * TODO add here test verification by listening to log messages
	 * 
	 * <pre>
	 * [Mon Jul 21 11:56:30 CEST 2014] [INFO] 	RETURNED []
	 * Error:  Could not parse XML contribution for "org.eclipse.equinox.registry//plugin.xml". Any contributed extensions and extension points will be ignored.
	 * </pre>
	 */
	@Test
	public void test06InstallAndStartEquinoxRegistry() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers,org.xml.sax,org.xml.sax.helpers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar",
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.apache.felix.gogo.runtime_0.10.0.v201209301036.jar",
					// required by Equinox Console, is not optional
					"org.eclipse.concierge.extension.permission_1.0.0.201408052201.jar",
					"org.eclipse.equinox.console_1.1.100.v20141023-1406.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
					"org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test06EquinoxRegistryGetAdapterExtensionPoint()
			throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers,org.xml.sax,org.xml.sax.helpers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar",
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
					"org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar" });
			assertBundlesResolved(bundles);

			Bundle bundleUnderTest = bundles[4];

			// check if adapter extension point is visible
			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
			Object extensionRegistry = runner
					.getService("org.eclipse.core.runtime.IExtensionRegistry");
			Assert.assertNotNull(extensionRegistry);
			Assert.assertEquals(
					"org.eclipse.core.internal.registry.ExtensionRegistry",
					extensionRegistry.getClass().getName());

			Object extensionPoint = runner.callMethod(extensionRegistry,
					"getExtensionPoint",
					new Object[] { "org.eclipse.core.runtime.adapters" });
			Assert.assertNotNull(extensionPoint);

			Object extensionPointIdentifier = runner.callMethod(extensionPoint,
					"getUniqueIdentifier", new Object[] {});
			Assert.assertNotNull(extensionPointIdentifier);
			Assert.assertEquals("org.eclipse.core.runtime.adapters",
					(String) extensionPointIdentifier);

		} finally {
			stopFramework();
		}
	}

	// TODO update again
	@Test
	@Ignore
	public void test07EquinoxRegistryInstallPluginXml() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers,org.xml.sax,org.xml.sax.helpers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar",
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
					"org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar" });
			assertBundlesResolved(bundles);

			// install pseudo bundle
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"concierge.test.test07EquinoxRegistryInstallPluginXml")
					.singleton()
					.bundleVersion("1.0.0")
					.addManifestHeader("Import-Package",
							"org.eclipse.core.runtime;registry=split, org.eclipse.core.runtime.spi")
					.addFile("plugin.xml",
							new File("./test/resources/plugin-01.xml"));
			// .addFile("schema/content_handler.exsd",
			// new File("./test/resources/content_handler.exsd"));

			Bundle bundleUnderTest = installBundle(builder);
			bundleUnderTest.start();
			assertBundleResolved(bundleUnderTest);

			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
			Object extensionRegistry = runner
					.getService("org.eclipse.core.runtime.IExtensionRegistry");
			Assert.assertNotNull(extensionRegistry);
			Assert.assertEquals(
					"org.eclipse.core.internal.registry.ExtensionRegistry",
					extensionRegistry.getClass().getName());

			Object extP = runner.callMethod(extensionRegistry,
					"getExtensionPoint",
					new Object[] { "org.eclipse.core.runtime.adapters" });
			Assert.assertNotNull(extP);

			// TODO check if extension has been registered
			// Object ext = runner.callMethod(extensionRegistry, "getExtension",
			// new Object[] { "org.eclipse.core.runtime.adapters" });
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox Event requires bundles Equinox-Util and Equinox-Supplement.
	 * 
	 * TODO test does work alone, does fail in test suite
	 */
	@Test
	public void test10InstallAndStartEquinoxEvent() throws Exception {
		try {
			startFramework();
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.eclipse.equinox.event_1.3.100.v20140115-1647.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox DS requires Equinox console, will fail due to missing
	 * resolvements.
	 */
	@Test
	public void test11InstallAndStartEquinoxDS() throws Exception {
		try {
			startFramework();

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.eclipse.equinox.ds_1.4.200.v20131126-2331.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox DS requires Equinox console, will fail due to missing
	 * resolvements.
	 */
	@Test
	public void test12TestOrderOfActivationOfEquinoxDS() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.bootdelegation",
					"javax.xml.parsers,org.xml.sax");
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.eclipse.equinox.ds_1.4.200.v20131126-2331.jar" });
			assertBundlesResolved(bundles);

			final Bundle bundleUnderTest = installBundle("org.eclipse.concierge.test.support_1.0.0.jar");
			bundleUnderTest.start();
			assertBundleResolved(bundleUnderTest);

			// check the order of Activator and DS activate/deactive calls
			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);

			/**
			 * this code will essentially do:
			 * 
			 * <pre>
			 * serviceTracker = new ServiceTracker(Activator.getContext(),
			 * 		org.eclipse.concierge.test.support.Service.class.getName(), null);
			 * serviceTracker.open();
			 * return (Service) serviceTracker.getService();
			 * </pre>
			 */
			// first request the service, which should activate component
			// serviceTracker = new
			// ServiceTracker(Activator.getContext(),Service.class.getName(),
			// null);
			Object serviceTracker = runner
					.createInstance(
							"org.osgi.util.tracker.ServiceTracker",
							new String[] { "org.osgi.framework.BundleContext",
									"java.lang.String",
									"org.osgi.util.tracker.ServiceTrackerCustomizer" },
							new Object[] {
									bundleUnderTest.getBundleContext(),
									"org.eclipse.concierge.test.support.Service",
									null });
			Assert.assertEquals("org.osgi.util.tracker.ServiceTracker",
					serviceTracker.getClass().getName());
			// serviceTracker.open();
			Object res1 = runner.callMethod(serviceTracker, "open",
					new Object[] {});
			Assert.assertNull(res1);

			// return (Service) serviceTracker.getService();
			Object res2 = runner.callMethod(serviceTracker, "getService",
					new Object[] {});
			Assert.assertNotNull(res2);
			// instance must be ComponentImpl
			Assert.assertEquals(
					"org.eclipse.concierge.test.support.ComponentImpl", res2
							.getClass().getName());

			// stop bundle again, should call deactivate
			bundleUnderTest.stop();

			// now check the calls
			Object synchronizeList = runner.getClassField(
					"org.eclipse.concierge.test.support.Monitor",
					"callSequence");
			@SuppressWarnings("unchecked")
			List<String> list = new ArrayList<String>(
					(Collection<String>) synchronizeList);
			// 4 entries expected
			Assert.assertEquals(4, list.size());
			for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
				String s = iter.next();
				System.out.println(s);
			}
			Assert.assertEquals("Activator.start", list.get(0));
			Assert.assertEquals("ComponentImpl.activate", list.get(1));
			Assert.assertEquals("ComponentImpl.deactivate", list.get(2));
			Assert.assertEquals("Activator.stop", list.get(3));
		} finally {
			stopFramework();
		}
	}
}
