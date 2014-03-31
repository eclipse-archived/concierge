/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.Factory;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * Test which tries to install a bundle which has a
 * 
 * <pre>
 * Require-Bundle: system.bundle
 * </pre>
 * 
 * directive in MANIFEST.MF. This bundle start can not be resolved due to the
 * reference to system bundle.
 * 
 * @author Jochen Hiller
 */
public class RequireBundleSystemBundleTest {

	@Test
	public void testInstallAndStartJavaXMLJarFile()
			throws InterruptedException, BundleException {
		// start OSGi framework
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.eclipse.concierge.debug", "true");
		final Framework framework = new Factory().newFramework(launchArgs);
		framework.init();
		framework.start();
		final BundleContext bundleContext = framework.getBundleContext();

		// See OSGi core spec, 10.2.3.7: getSymbolicName()
		// Returns the symbolic name of this Framework. The symbolic name is
		// unique
		// for the implementation of the framework. However, the symbolic name
		// “system.bundle“
		// must be recognized as an alias to the implementation-defined symbolic
		// name since
		// this Framework is also a System Bundle.

		// We get a javax.xml jar file from Orbit which has such a directive
		String url = "http://archive.eclipse.org/tools/orbit/downloads/drops/R20140114142710/repository/plugins/javax.xml_1.3.4.v201005080400.jar";
		final Bundle bundle = bundleContext.installBundle(url);

		// does fail
		Assert.assertEquals("Bundle needs to be resolved", Bundle.RESOLVED,
				bundle.getState());
		// start now would return
		// COULD NOT RESOLVE REQUIREMENT BundleRequirement{Require-Bundle
		// system.bundle} CANDIDATES WERE []
		bundle.start();
		framework.stop();
	}
}
