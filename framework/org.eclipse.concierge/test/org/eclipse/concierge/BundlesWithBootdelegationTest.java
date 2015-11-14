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

import java.util.HashMap;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Tests bootdelegation property.
 * 
 * @author Jochen Hiller
 */
public class BundlesWithBootdelegationTest extends AbstractConciergeTestCase {

	private Bundle bundleUnderTest;

	private void setupDefaultBundles() throws Exception {
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		bundleUnderTest = installBundle(builder);
	}

	@Test
	public void testGoodProperties() throws Exception {
		checkOK("javax.xml.parsers");
		checkOK("javax.*");
		checkOK("javax.*,sun.*,com.sun.*");
		checkOK("javax.*, sun.*, com.sun.*");
		checkOK("bla.*, bla.blub.*, javax.*");
	}

	@Test
	public void testBadProperties() throws Exception {
		checkFail("bla.blub");
		checkFail("bla.*");
	}

	/** A class can be loaded via bootdelegation. */
	private void checkOK(String bootDelegation) throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.bootdelegation", bootDelegation);
		startFrameworkClean(launchArgs);
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		Object o = runner.getClass("javax.xml.parsers.DocumentBuilder");
		Assert.assertNotNull(o);
		stopFramework();
	}

	/** A class can NOT be loaded via bootdelegation. */
	private void checkFail(String bootDelegation) throws Exception {
		HashMap<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.osgi.framework.bootdelegation", bootDelegation);
		startFrameworkClean(launchArgs);
		setupDefaultBundles();
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
		RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
		try {
			runner.getClass("javax.xml.parsers.DocumentBuilder");
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException ex) {
			// ignore
		}
		stopFramework();
	}
}
