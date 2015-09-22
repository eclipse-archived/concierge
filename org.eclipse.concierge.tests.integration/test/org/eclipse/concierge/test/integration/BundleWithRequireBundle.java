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
package org.eclipse.concierge.test.integration;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Test which tries to install some bundles with basic assumptions.
 * 
 * TODO add more tests and combinations for Bundle-NativeCode, for more OS/ARCH
 * types
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BundleWithRequireBundle extends AbstractConciergeTestCase {

	@Test
	public void test10InstallAndStartJavaXmlJarFileWithRequireBundleSystemBundle()
			throws Exception {
		startFramework();
		// See OSGi core spec, 10.2.3.7: getSymbolicName()
		// Returns the symbolic name of this Framework. The symbolic name is
		// unique for the implementation of the framework. However, the symbolic
		// name "system.bundle" must be recognized as an alias to the
		// implementation-defined symbolic name since this Framework is also a
		// System Bundle.

		// We get a javax.xml jar file from Orbit which has such a directive
		String url = "http://archive.eclipse.org/tools/orbit/downloads/drops/R20140114142710/repository/plugins/javax.xml_1.3.4.v201005080400.jar";
		final Bundle bundle = bundleContext.installBundle(url);
		// resolve, check if resolved
		enforceResolveBundle(bundle);
		assertBundleResolved(bundle);
		// start, check if active
		bundle.start();
		assertBundleActive(bundle);
		stopFramework();
	}

}
