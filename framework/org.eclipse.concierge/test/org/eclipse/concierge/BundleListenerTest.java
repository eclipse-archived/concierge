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

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.concierge.test.util.AbstractConciergeTestCase;
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class BundleListenerTest extends AbstractConciergeTestCase {

	private Bundle bundleUnderTest;

	// do NOT mark as JUnit Before, will be called explicitly
	public void setUp() throws Exception {
		startFramework();
		SyntheticBundleBuilder builder = SyntheticBundleBuilder.newBuilder();
		builder.bundleSymbolicName("bundle");
		bundleUnderTest = installBundle(builder);
		bundleUnderTest.start();
		assertBundleActive(bundleUnderTest);
	}

	// do NOT mark as JUnit After, will be called explicitly
	public void tearDown() throws Exception {
		bundleUnderTest.uninstall();
		stopFramework();
	}

	@Test
	public void testBundleListenerOnInstalledBundle() throws Exception {
		setUp();
		MyBundleListener listener1 = new MyBundleListener();
		bundleUnderTest.getBundleContext().addBundleListener(listener1);
		bundleUnderTest.getBundleContext().removeBundleListener(listener1);
		bundleUnderTest.stop();
		bundleUnderTest.start();
		Assert.assertEquals(0, listener1.events.size());
		// now stop/start bundle in between, we expect 2 events
		MyBundleListener listener2 = new MyBundleListener();
		bundleUnderTest.getBundleContext().addBundleListener(listener2);
		bundleUnderTest.stop();
		bundleUnderTest.start();
		bundleUnderTest.getBundleContext().removeBundleListener(listener2);
		Assert.assertEquals(2, listener2.events.size());
		tearDown();
		// check for more events happening which should NOT happen
		Assert.assertEquals(0, listener1.events.size());
		Assert.assertEquals(2, listener2.events.size());
	}

	@Test
	@Ignore("See https://github.com/eclipse/concierge/issues/9")
	public void testBundleListenerOnFrameworkDuringFrameworkStop()
			throws Exception {
		setUp();
		MyBundleListener listener1 = new MyBundleListener();
		framework.getBundleContext().addBundleListener(listener1);
		framework.getBundleContext().removeBundleListener(listener1);
		Assert.assertEquals(0, listener1.events.size());
		tearDown();
		// check for more events
		// TODO the test here has 3 events happened. Why ?
		// see https://github.com/eclipse/concierge/issues/9, listener will NOT
		// be removed
		Assert.assertEquals(0, listener1.events.size());
	}

	@Test
	@Ignore("See https://github.com/eclipse/concierge/issues/9")
	public void testBundleListenerOnFrameworkDuringBundleStopStart()
			throws Exception {
		setUp();
		MyBundleListener listener1 = new MyBundleListener();
		framework.getBundleContext().addBundleListener(listener1);
		bundleUnderTest.stop();
		bundleUnderTest.start();
		framework.getBundleContext().removeBundleListener(listener1);
		Assert.assertEquals(2, listener1.events.size());
		tearDown();
		// check for more events
		// TODO the test here has 5 events happened. Why ?
		// see https://github.com/eclipse/concierge/issues/9, listener will NOT
		// be removed
		Assert.assertEquals(2, listener1.events.size());
	}

	public static class MyBundleListener implements BundleListener {
		public Queue<BundleEvent> events = new LinkedList<BundleEvent>();

		public void bundleChanged(BundleEvent event) {
			// nothing to do
			System.out.println("XXX: " + event);
			events.add(event);
		}
	}

}
