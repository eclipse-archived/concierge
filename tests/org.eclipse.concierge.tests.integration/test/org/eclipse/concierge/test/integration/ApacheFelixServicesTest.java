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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Test Apache services running in Concierge.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApacheFelixServicesTest extends AbstractConciergeTestCase {

	/**
	 * Apache EventAdmin needs ConfigAdmin and Metatype service.
	 */
	@Test
	public void test01ApacheFelixEventAdminService() throws Exception {
		try {
			startFramework();
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.apache.felix.metatype-1.0.10.jar",
					"org.apache.felix.configadmin-1.8.0.jar",
					"org.apache.felix.eventadmin-1.4.2.jar", });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test02ApacheFelixDSService() throws Exception {
		try {
			startFramework();
			final Bundle bundle = installAndStartBundle("org.apache.felix.scr-1.8.2.jar");
			assertBundleResolved(bundle);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test03ApacheFelixGogo() throws Exception {
		try {
			startFramework();
			final Bundle[] bundles = installAndStartBundles(new String[] { "org.apache.felix.gogo.runtime_0.10.0.v201209301036.jar", });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Tests whether Apache Felix FileInstall can be installed. 3.2.6 does NOT
	 * work, as it requires a StartLevel service, which is NOT supported by
	 * Concierge. TODO fix the test case, this seems to work but does NOT work
	 * in Eclipse SmartHome
	 */
	@Test
	public void test04ApacheFelixFileInstall326() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("felix.fileinstall.poll", "5");
			launchArgs.put("felix.fileinstall.dir", "./test/plugins");
			startFrameworkClean(launchArgs);
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.apache.felix.configadmin-1.8.0.jar",
					"org.apache.felix.fileinstall_3.2.6.jar", });
			assertBundlesResolved(bundles);
			// wait 10 s to get poll done 1-2x
			Thread.sleep(10000);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Tests whether Apache Felix FileInstall can be installed. FileInstall
	 * 3.4.0 does work well.
	 * 
	 * TODO JOCHEN create a bug for
	 * 
	 * <pre>
	 * [Sat Nov 01 11:42:04 CET 2014] [DEBUG] REFRESHING PACKAGES FROM BUNDLES [[org.eclipse.concierge.extension.permission-1.0.0.201408052212], [org.eclipse.equinox.supplement-1.5.100.v20140428-1446]]
	 * [Sat Nov 01 11:42:04 CET 2014] [DEBUG] UPDATE GRAPH IS [[org.eclipse.concierge.extension.permission-1.0.0.201408052212], [org.eclipse.equinox.supplement-1.5.100.v20140428-1446], Concierge System Bundle]
	 * java.lang.ClassCastException: org.eclipse.concierge.Concierge cannot be cast to org.eclipse.concierge.BundleImpl
	 * 	at org.eclipse.concierge.Concierge$9.run(Concierge.java:1911)
	 * </pre>
	 */
	@Test
	public void test05ApacheFelixFileInstall340() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("felix.fileinstall.poll", "5");
			launchArgs.put("felix.fileinstall.dir", "./target/plugins");

			startFrameworkClean(launchArgs);
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.apache.felix.configadmin-1.8.0.jar",
					"org.apache.felix.fileinstall-3.4.0.jar", });
			assertBundlesResolved(bundles);
			// wait 10 s to get poll done 1-2x
			Thread.sleep(10000);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Equinox DS requires Equinox console, will fail due to missing
	 * resolvements.
	 */
	@Test
	public void test10TestOrderOfActivationOfFelixDS() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.bootdelegation",
					"javax.xml.parsers,org.xml.sax");
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.xml.parsers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.apache.felix.scr-1.8.2.jar" });
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
