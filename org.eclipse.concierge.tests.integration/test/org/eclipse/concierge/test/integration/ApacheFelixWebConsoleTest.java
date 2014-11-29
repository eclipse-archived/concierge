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

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test Apache WebConsole running in Concierge.
 */
public class ApacheFelixWebConsoleTest extends AbstractConciergeTestCase {

	@After
	public void tearDown() throws Exception {
		stopFramework();
	}

	@Test
	public void testInstallAndStartWebConsole() throws Exception {
		startFramework();
		Bundle[] bundles = installAndStartBundles(new String[] {
				"org.eclipse.concierge.service.packageadmin-1.0.0.alpha2.jar",
				"org.eclipse.concierge.service.startlevel-1.0.0.alpha2.jar",
				"org.apache.felix.httplite.complete-0.1.4.jar",
				"org.apache.felix.webconsole-4.2.2-all.jar", });
		assertBundlesActive(bundles);

		// check if server is running OK
		String htmlContent = TestUtils
				.getContentFromHttpGetBasicAuth(
						"http://localhost:8080/system/console/vmstat", "admin",
						"admin");
		Assert.assertTrue(htmlContent.contains("/system/console/vmstat"));
		Assert.assertTrue(htmlContent.contains("System is up and running!"));
	}

}
