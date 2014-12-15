/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tim Verbelen
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.test.integration;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * This tests checks about wrong wiring of implied dependencies.
 * 
 * The "test-tim-verbelen" just registers a simple servlet using HttpService.
 * The OSGi HttpService can not be loaded correctly as it has been wired once to
 * osgi-enterprise, and to org.apache.felix.http.api, which is wrong.
 */
public class ApacheFelixJettyHttpServerImpliedCapabilitiesTest extends
		AbstractConciergeTestCase {

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	@Test
	public void testImpliedOsgiHttpService() throws Exception {
		Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs
				.put("org.osgi.framework.system.packages.extra",
						"javax.annotation,"
								+ "javax.management,javax.management.modelmbean,javax.management.remote,"
								+ "javax.naming,javax.net.ssl,javax.security.auth,javax.security.cert,"
								+ "javax.xml.parsers,org.xml.sax,org.xml.sax.helpers");
		startFrameworkClean(launchArgs);

		Bundle[] bundles = installAndStartBundles(new String[] {
				"osgi.enterprise-5.0.0.jar",
				"org.apache.felix.http.servlet-api-1.0.1.jar",
				"org.apache.felix.http.api-2.3.2.jar",
				"org.apache.felix.http.jetty-2.3.2.jar",
				"test-tim-verbelen.jar" });
		assertBundlesActive(bundles);
	}
}
