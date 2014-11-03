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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Tests for using Equinox service implementations within Concierge framework.
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EclipsePlatformTest extends AbstractConciergeTestCase {

	@Test
	public void test01EclipseCoreRuntime() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.xml.parsers,org.xml.sax,org.xml.sax.helpers,org.xml.sax.ext");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.equinox.supplement_1.5.100.v20140428-1446.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
					"org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar",
					"org.eclipse.equinox.preferences_3.5.200.v20140224-1527.jar",
					"org.eclipse.equinox.app_1.3.200.v20130910-1609.jar",
					"org.eclipse.core.contenttype_3.4.200.v20140207-1251.jar",
					"org.eclipse.core.jobs_3.6.0.v20140424-0053.jar",
					"org.eclipse.core.runtime_3.10.0.201408192241.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

}
