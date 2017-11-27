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
package org.eclipse.concierge;

import java.util.Iterator;
import java.util.List;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Tests how java.specification.{name|version} properties will be parsed and
 * processed into OSGi Execution Environments and its capabilities in osgi.ee
 * namespace.
 */
public class ConciergeJvmOptions extends AbstractConciergeTestCase {

	/**
	 * Test if startup of framework does work when java.specification.name is
	 * NOT null. This is the standard case for JavaSE 7/8 etc JVMs.
	 */
	@Test
	public void testJavaSpecificationNameNotNull() throws Exception {
		String propName = "java.specification.name";
		String propValue = System.getProperty(propName);
		Assert.assertNotNull(propValue);
		try {
			startFramework();

			Assert.assertEquals(0, framework.getBundleId());
			BundleWiring w = framework.adapt(BundleWiring.class);
			List<BundleCapability> caps = w.getCapabilities("osgi.ee");
			checkEECaps(caps);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Test if startup of framework does work when java.specification.name IS
	 * null. This will for example happen when running on CEE-J.
	 */
	@Test
	public void testJavaSpecificationNameNull() throws Exception {
		String propName = "java.specification.name";
		String savedPropValue = System.getProperty(propName);
		try {
			System.clearProperty(propName);
			String propValue = System.getProperty(propName);
			Assert.assertNull(propValue);
			startFramework();

			Assert.assertEquals(0, framework.getBundleId());
			BundleWiring w = framework.adapt(BundleWiring.class);
			List<BundleCapability> caps = w.getCapabilities("osgi.ee");
			checkEECaps(caps);
		} finally {
			stopFramework();
			System.setProperty(propName, savedPropValue);
		}
	}

	/**
	 * Test if startup of framework does work when java.specification.name is
	 * "J2ME Foundation Specification". This will run into a case where only
	 * CDC-Profiles does exist in JVM.
	 */
	@Test
	public void testJavaSpecificationNameJ2ME() throws Exception {
		String propName = "java.specification.name";
		String savedPropNameValue = System.getProperty(propName);
		String propVersion = "java.specification.version";
		String savedPropVersionValue = System.getProperty(propVersion);
		try {
			// fixed value, see https://jcp.org/en/jsr/detail?id=36
			String j2mePropValue = "J2ME Foundation Specification";
			System.setProperty(propName, j2mePropValue);
			String propValue = System.getProperty(propName);
			Assert.assertEquals(j2mePropValue, propValue);

			// we have to fake the spec version too, use here 1.1
			System.setProperty(propVersion, "1.1");

			startFramework();

			Assert.assertEquals(0, framework.getBundleId());
			BundleWiring w = framework.adapt(BundleWiring.class);
			List<BundleCapability> caps = w.getCapabilities("osgi.ee");

			// in case of J2ME, there is only CDC-profiles, no OSGi EE
			// capabilities
			// TODO Jan is that correct?
			Assert.assertEquals(0, caps.size());
		} finally {
			stopFramework();
			System.setProperty(propName, savedPropNameValue);
			System.setProperty(propVersion, savedPropVersionValue);
		}
	}

	// helper methods to check results

	private void checkEECaps(List<BundleCapability> caps) throws Exception {
		String propVersion = "java.specification.version";
		String propVersionValue = System.getProperty(propVersion);
		String propName = "java.specification.name";
		String propNameValue = System.getProperty(propName);
		// now check if we see the expected capabilities for "osgi.ee"
		// this test does only work on for JavaSE 7/8 Full-JRE yet
		// for other JVM to run test on we have to add expected values
		if ("1.8".equals(propVersionValue)) {
			checkEECapsForJava8(caps);
		} else if ("1.7".equals(propVersionValue)) {
			checkEECapsForJava7(caps);
		} else {
			Assert.fail("Test not yet supported on JavaVM " + propNameValue
					+ " " + propVersionValue);
		}
	}

	private void checkEECapsForJava8(List<BundleCapability> caps)
			throws Exception {
		// we expect for "JavaSE 8" 5 capabilities:
		// JavaSE,JavaSE/compact1,JavaSE/compact3,JavaSE/compact3,
		// OSGi/Minimum
		Assert.assertEquals(5, caps.size());
		Iterator<BundleCapability> iter = caps.iterator();
		while (iter.hasNext()) {
			BundleCapability cap = iter.next();

			// get the details for a capability
			String ee = (String) cap.getAttributes().get("osgi.ee");
			@SuppressWarnings("unchecked")
			List<String> version = (List<String>) cap.getAttributes()
					.get("version");

			if (ee.equals("JavaSE")) {
				// version=[1.8.0, 1.7.0, 1.6.0, 1.5.0, 1.4.0, 1.3.0,
				// 1.2.0, 1.1.0]
				Assert.assertEquals(8, version.size());
			} else if (ee.equals("JavaSE/compact1")) {
				// version=[1.8.0]
				Assert.assertEquals(1, version.size());
			} else if (ee.equals("JavaSE/compact2")) {
				// version=[1.8.0]
				Assert.assertEquals(1, version.size());
			} else if (ee.equals("JavaSE/compact3")) {
				// version=[1.8.0]
				Assert.assertEquals(1, version.size());
			} else if (ee.equals("OSGi/Minimum")) {
				// version=[1.2.0, 1.1.0, 1.0.0]
				Assert.assertEquals(3, version.size());
			} else {
				Assert.fail("Unknown capability namespace " + ee + " found");
			}
		}
	}

	private void checkEECapsForJava7(List<BundleCapability> caps)
			throws Exception {
		// we expect for "JavaSE 7" 2 capabilities:
		// JavaSE, OSGi/Minimum
		Assert.assertEquals(2, caps.size());
		Iterator<BundleCapability> iter = caps.iterator();
		while (iter.hasNext()) {
			BundleCapability cap = iter.next();

			// get the details for a capability
			String ee = (String) cap.getAttributes().get("osgi.ee");
			@SuppressWarnings("unchecked")
			List<String> version = (List<String>) cap.getAttributes()
					.get("version");

			if (ee.equals("JavaSE")) {
				// version=[1.7.0, 1.6.0, 1.5.0, 1.4.0, 1.3.0,
				// 1.2.0, 1.1.0]
				Assert.assertEquals(7, version.size());
			} else if (ee.equals("OSGi/Minimum")) {
				// version=[1.2.0, 1.1.0, 1.0.0]
				Assert.assertEquals(3, version.size());
			} else {
				Assert.fail("Unknown capability namespace " + ee + " found");
			}
		}
	}

}
