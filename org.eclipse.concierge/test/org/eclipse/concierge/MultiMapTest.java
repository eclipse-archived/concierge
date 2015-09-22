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
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.concierge.ConciergeCollections.MultiMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MultiMapTest {

	private MultiMap<String, String> map;

	@Before
	public void setUp() throws Exception {
		map = new MultiMap<String, String>();
	}

	@After
	public void tearDown() throws Exception {
		map.clear();
		map = null;
	}

	@Test
	public void testOrder() {
		map.insert("2", "one");
		map.insert("1", "one");
		map.insert("1", "two");
		map.insert("2", "two");
		map.insert("3", "one");
		map.insert("1", "three");

		final List<String> values = map.getAllValues();

		assertNotNull(values);
		assertEquals(3, values.size());
		assertEquals("one", values.get(0));
		assertEquals("two", values.get(1));
		assertEquals("three", values.get(2));

		final List<String> val1 = map.lookup("1");

		assertNotNull(val1);
		assertEquals(3, val1.size());
		assertEquals("one", val1.get(0));
		assertEquals("two", val1.get(1));
		assertEquals("three", val1.get(2));

		final List<String> val2 = map.lookup("2");

		assertNotNull(val2);
		assertEquals(2, val2.size());
		assertEquals("one", val2.get(0));
		assertEquals("two", val2.get(1));

		final List<String> val3 = map.lookup("3");

		assertNotNull(val3);
		assertEquals(1, val3.size());
		assertEquals("one", val3.get(0));
	}

}
