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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EclipseJettyTest extends AbstractConciergeTestCase {

	@Test
	public void test01Jetty() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.naming,javax.net.ssl,"
							+ "javax.security.auth,javax.security.cert,"
							+ "javax.sql,"
							+ "javax.imageio,javax.imageio.metadata");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"javax.xml_1.3.4.v201005080400.jar",
					"javax.activation_1.1.0.v201211130549.jar",
					"javax.xml.stream_1.0.1.v201004272200.jar",
					"javax.xml.bind_2.2.0.v201105210648.jar",
					"javax.servlet_3.0.0.v201112011016.jar",
					"org.eclipse.jetty.util_8.1.14.v20131031.jar",
					"org.eclipse.jetty.io_8.1.14.v20131031.jar",
					"org.eclipse.jetty.http_8.1.14.v20131031.jar",
					"org.eclipse.jetty.continuation_8.1.14.v20131031.jar",
					"org.eclipse.jetty.server_8.1.14.v20131031.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}
}
