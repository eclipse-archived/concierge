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
package org.eclipse.concierge.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.Factory;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;

/**
 * @author Jochen Hiller
 */
public abstract class AbstractConciergeTestCase {

	private static final String LOCAL_BASE_URL = "test/plugins/";
	private static final String ORBIT_BASE_URL = "http://download.eclipse.org/tools/orbit/downloads/drops/R20140114142710/repository/plugins/";
	private static boolean showLocalFilesMissingWarning = false;

	protected Framework framework = null;
	protected BundleContext bundleContext = null;

	public void startFramework() throws Exception {
		// start OSGi framework
		final Map<String, String> launchArgs = new HashMap<String, String>();
		launchArgs.put("org.eclipse.concierge.debug", "true");
		launchArgs.put("org.osgi.framework.storage.clean", "onFirstInit");
		startFramework(launchArgs);
	}

	public void startFramework(Map<String, String> launchArgs) throws Exception {
		// start OSGi framework
		framework = new Factory().newFramework(launchArgs);
		framework.init();
		framework.start();
		bundleContext = framework.getBundleContext();

		if (stayInShell()) {
			installAndStartBundle("./lib/shell-1.0.0.jar");
		}
	}

	public void stopFramework() throws Exception {
		if (stayInShell()) {
			framework.waitForStop(0);
		} else {
			framework.stop();
			FrameworkEvent event = framework.waitForStop(10000);
			Assert.assertEquals(FrameworkEvent.STOPPED, event.getType());
		}
	}

	// abstract methods

	protected String getURLForBundle(final String bundleName) {
		if (new File(bundleName).exists()) {
			return bundleName;
		} else {
			final String localFilename = LOCAL_BASE_URL + bundleName;
			final File localFile = new File(localFilename);
			System.err.println("CHECKING " + localFile.getAbsolutePath());
			if (localFile.exists()) {
				System.err.println("FOUND");
				return localFilename;
			} else {
				if (!showLocalFilesMissingWarning) {
					System.err
							.println("Warning: local files are missing. Consider to put all bundles into "
									+ LOCAL_BASE_URL
									+ " folder for using them in local tests.");
					showLocalFilesMissingWarning = true;
				}
				return ORBIT_BASE_URL + bundleName;
			}
		}
	}

	// Utilities

	/**
	 * Override when a test case should use shell and wait for manual exit of
	 * framework.
	 */
	protected boolean stayInShell() {
		return false;
	}

	protected Bundle[] installAndStartBundles(final String[] bundleNames)
			throws BundleException {
		final Bundle[] bundles = new Bundle[bundleNames.length];
		for (int i = 0; i < bundleNames.length; i++) {
			bundles[i] = installAndStartBundle(bundleNames[i]);
		}
		return bundles;
	}

	/**
	 * Install a bundle for given name. Will check whether bundle can be
	 * resolved.
	 */
	protected Bundle installAndStartBundle(final String bundleName)
			throws BundleException {
		final String url = getURLForBundle(bundleName);
		System.err.println("installAndStartBundle: " + bundleName);

		final Bundle bundle = bundleContext.installBundle(url);

		if (!isFragmentBundle(bundle)) {
			bundle.start();
		}
		return bundle;
	}

	/**
	 * Enforce to call resolve bundle in Concierge framework for the specified
	 * bundle.
	 */
	protected void enforceResolveBundle(final Bundle bundle) {
		final Concierge concierge = (Concierge) framework;
		final Collection<Bundle> bundlesToResolve = new ArrayList<Bundle>();
		bundlesToResolve.add(bundle);
		concierge.resolveBundles(bundlesToResolve);
	}

	/** Returns true when the specified bundle is a fragment. */
	protected boolean isFragmentBundle(final Bundle bundle) {
		return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	/** Checks about Bundle RESOLVED state for all bundles. */
	protected void assertBundlesResolved(final Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			assertBundleResolved(bundles[i]);
		}
	}

	/** Checks about Bundle RESOLVED or ACTIVE state. */
	protected void assertBundleResolved(final Bundle bundle) {
		if ((bundle.getState() == Bundle.RESOLVED)
				|| (bundle.getState() == Bundle.ACTIVE)) {
			// all fine
		} else {
			Assert.fail("Bundle " + bundle.getSymbolicName() + " needs to be "
					+ getBundleStateAsString(Bundle.RESOLVED) + " or "
					+ getBundleStateAsString(Bundle.ACTIVE) + " but was "
					+ getBundleStateAsString(bundle.getState()));
		}
	}

	/**
	 * This method will install a "pseudo" bundle into the framework. The bundle
	 * will get its <code>META-INF/MANIFEST.MF</code> from given headers. The
	 * bundle will be generared as JarOutputStream and installed from
	 * corresponding InpuStream.
	 */
	protected Bundle installBundle(final String bundleName,
			final Map<String, String> headers) throws IOException,
			BundleException {
		// copy MANIFEST to a jar file in memory
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION,
				"1.0");
		manifest.getMainAttributes().put(
				new Attributes.Name(Constants.BUNDLE_MANIFESTVERSION), "2");
		manifest.getMainAttributes().put(
				new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME), bundleName);

		for (Iterator<Map.Entry<String, String>> iter = headers.entrySet()
				.iterator(); iter.hasNext();) {
			final Map.Entry<String, String> entry = iter.next();
			manifest.getMainAttributes().put(
					new Attributes.Name(entry.getKey()), entry.getValue());
		}
		final JarOutputStream jarStream = new JarOutputStream(out, manifest);
		jarStream.close();
		final InputStream is = new ByteArrayInputStream(out.toByteArray());
		final Bundle b = bundleContext.installBundle(bundleName, is);
		out.close();
		return b;
	}

	/** Returns bundle state as readable string. */
	protected String getBundleStateAsString(final int state) {
		switch (state) {
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		default:
			return "UNKNOWN state: " + state;
		}
	}

	/**
	 * The <code>RunInClassLoader</code> class helps to run code in ClassLoader
	 * of the bundle using Java reflection. This allows to call testing code in
	 * context of a bundle without need to have classes and/or compile code for
	 * testing purposes.
	 */
	static class RunInClassLoader {

		private final Bundle bundle;

		public RunInClassLoader(Bundle b) {
			this.bundle = b;
		}

		/**
		 * Call a class method for given class name. The method will be detected
		 * based on arguments.
		 */
		public Object callClassMethod(final String className,
				final String classMethodName, final Object[] args)
				throws Exception {
			final Class<?> clazz = this.bundle.loadClass(className);
			// get parameter types from args
			final Class<?>[] parameterTypes = new Class[args.length];
			for (int i = 0; i < args.length; i++) {
				parameterTypes[i] = args[i].getClass();
			}
			final Method method = clazz.getDeclaredMethod(classMethodName,
					parameterTypes);
			if (!Modifier.isStatic(method.getModifiers())) {
				throw new RuntimeException("Oops, method " + method.toString()
						+ " is not static");
			}
			// TODO Maybe set accessible if private?
			final Object result = method.invoke(null, args);
			return result;
		}

		/**
		 * Call an instance method for given object. The method will be detected
		 * based on arguments.
		 */
		public Object callMethod(final Object obj, final String methodName,
				final Object[] args) throws Exception {
			final Class<?> clazz = obj.getClass();
			final Class<?>[] parameterTypes = new Class[args.length];
			for (int i = 0; i < args.length; i++) {
				parameterTypes[i] = args[i].getClass();
			}
			final Method method = clazz.getDeclaredMethod(methodName,
					parameterTypes);
			if (Modifier.isStatic(method.getModifiers())) {
				throw new RuntimeException("Oops, method " + method.toString()
						+ " is static");
			}
			final Object result = method.invoke(obj, args);
			return result;
		}
	}
}
