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
package org.eclipse.concierge.test.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test of the FilteredClassLoader, if filtered classes will really be denied.
 */
public class FilteredClassLoaderTest extends AbstractConciergeTestCase {

	@Test
	public void testFilterFullClassName() throws Exception {
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader(), "org.w3c.dom.Document");
		// this class needs to be found
		Class<?> clsFound = Class.forName("java.lang.Object", false, cl);
		Assert.assertNotNull(clsFound);
		try {
			// this class should throw a CNF Exception, due to filtering
			Class.forName("org.w3c.dom.Document", false, cl);
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException expected) {
			// CNF exception expected
		}
	}

	@Test
	public void testFilterPackageNameWithStarAtEnd() throws Exception {
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader(), "org.w3c.dom.*");
		// this class needs to be found
		Class<?> clsFound = Class.forName("java.lang.Object", false, cl);
		Assert.assertNotNull(clsFound);
		try {
			// this class should throw a CNF Exception, due to filtering
			Class.forName("org.w3c.dom.Document", false, cl);
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException expected) {
			// CNF exception expected
		}
	}

	@Test
	public void testFilterPackageNameWithStarTopLevel() throws Exception {
		ClassLoader cl = new FilteredClassLoader(
				this.getClass().getClassLoader(), "org.*");
		// this class needs to be found
		Class<?> clsFound = Class.forName("java.lang.Object", false, cl);
		Assert.assertNotNull(clsFound);
		try {
			// this class should throw a CNF Exception, due to filtering
			Class.forName("org.w3c.dom.Document", false, cl);
			Assert.fail("Uups, ClassNotFoundException expected");
		} catch (ClassNotFoundException expected) {
			// CNF exception expected
		}
	}
}
