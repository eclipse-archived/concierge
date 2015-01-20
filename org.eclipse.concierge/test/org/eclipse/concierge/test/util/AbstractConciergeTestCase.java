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
package org.eclipse.concierge.test.util;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.Factory;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * @author Jochen Hiller
 */
public abstract class AbstractConciergeTestCase {

	protected Framework framework = null;
	protected BundleContext bundleContext = null;
	protected LocalBundleStorage localBundleStorage = LocalBundleStorage
			.getInstance();

	/** Start framework with default settings, which cleans storage first. */
	public void startFramework() throws Exception {
		final Map<String, String> launchArgs = new HashMap<String, String>();
		// start OSGi framework in clean mode as default
		startFrameworkClean(launchArgs);
	}

	/** Start framework with given settings but in clean mode. */
	public void startFrameworkClean(Map<String, String> launchArgs)
			throws Exception {
		launchArgs.put("org.eclipse.concierge.debug", "true");
		launchArgs.put("org.osgi.framework.storage.clean", "onFirstInit");
		startFramework(launchArgs);
	}

	/** Start framework with given settings. */
	public void startFramework(final Map<String, String> launchArgs)
			throws Exception {
		Framework frameworkToStart = new Factory().newFramework(launchArgs);
		frameworkToStart.init();
		frameworkToStart.start();
		useFramework(frameworkToStart);
	}

	/** Start framework for a given framework. */
	public void useFramework(final Framework frameworkToStart) throws Exception {
		// start OSGi framework
		this.framework = frameworkToStart;
		this.bundleContext = this.framework.getBundleContext();

		if (stayInShell()) {
			String shellJarName = "./test/resources/org.eclipse.concierge.shell-1.0.0.jar";
			if (!new File(shellJarName).exists()) {
				System.err.println("Oops, could not find shell bundle at "
						+ shellJarName);
			} else {
				// assume to get shell jar file in target folder
				installAndStartBundle(shellJarName);
			}
		}
	}

	public void stopFramework() throws Exception {
		// it may happen that framework has not been set
		if (this.framework != null) {
			if (stayInShell()) {
				this.framework.waitForStop(0);
			} else {
				this.framework.stop();
				FrameworkEvent event = framework.waitForStop(10000);
				Assert.assertEquals(FrameworkEvent.STOPPED, event.getType());
				
				// force a GC to allow cleanup of files
				// on Mac from time to time files from storage can not be deleted
				// until a GC has been run
				System.gc();
				
				// TODO we have from time to time problems when shutdown the
				// framework, that next tests are failing
				// for CI build we can define a timeout to wait here. A good
				// value is 100ms

				String propName = "org.eclipse.concierge.tests.waitAfterFrameworkShutdown";
				String propValue = System.getProperty(propName);
				// System.err.println(propName + "=" + propValue);

				int timeout = -1;
				if ((propValue != null) && (propValue.length() > 0)) {
					try {
						timeout = Integer.valueOf(propValue);
					} catch (NumberFormatException ex) {
						// ignore
					}
					if (timeout > 0) {
						Thread.sleep(timeout);
					}
				}

			}
		}
	}

	/**
	 * stop the framework, but wait until it will be stopped by someone. Use
	 * this if framework started, but will be closed interactively.
	 */
	public void stopFrameworkWaitForStop() throws Exception {
		if (this.framework != null) {
			this.framework.waitForStop(0);
		}
	}

	// Utilities

	/**
	 * Gets a framework property by casting into implementation.
	 */
	protected String getFrameworkProperty(String propertyName) {
		try {
			final Concierge c = (Concierge) this.framework;
			final Field f = c.getClass().getDeclaredField("properties");
			f.setAccessible(true);
			Object o = f.get(c);
			Properties p = (Properties) o;
			String s = (String) p.get(propertyName);
			return s;
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Override when a test case should use shell and wait for manual exit of
	 * framework.
	 */
	protected boolean stayInShell() {
		return false;
	}

	protected Bundle[] installBundles(final String[] bundleNames)
			throws BundleException {
		final Bundle[] bundles = new Bundle[bundleNames.length];
		for (int i = 0; i < bundleNames.length; i++) {
			final String url = this.localBundleStorage
					.getUrlForBundle(bundleNames[i]);
			bundles[i] = bundleContext.installBundle(url);
		}
		return bundles;
	}

	protected void startBundles(Bundle[] bundles) throws BundleException {
		for (int i = 0; i < bundles.length; i++) {
			if (!isFragmentBundle(bundles[i])) {
				bundles[i].start();
			}
		}
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
	 * Install a bundle for given name.
	 */
	protected Bundle installBundle(final String bundleName)
			throws BundleException {
		final String url = this.localBundleStorage.getUrlForBundle(bundleName);
		final Bundle bundle = bundleContext.installBundle(url);
		return bundle;
	}

	/**
	 * Install a bundle for given name. Will check whether bundle can be
	 * resolved.
	 */
	protected Bundle installAndStartBundle(final String bundleName)
			throws BundleException {
		final String url = this.localBundleStorage.getUrlForBundle(bundleName);
		// System.err.println("installAndStartBundle: " + bundleName);

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
		// initiate resolver
		framework.adapt(FrameworkWiring.class).resolveBundles(
				Collections.singleton(bundle));
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

	/** Checks about Bundle ACTIVE state for all bundles. */
	protected void assertBundlesActive(final Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			assertBundleActive(bundles[i]);
		}
	}

	/** Checks about Bundle RESOLVED or ACTIVE state. */
	protected void assertBundleResolved(final Bundle bundle) {
		if (isBundleResolved(bundle)) {
			// all fine
		} else {
			Assert.fail("Bundle " + bundle.getSymbolicName() + " needs to be "
					+ getBundleStateAsString(Bundle.RESOLVED) + " or "
					+ getBundleStateAsString(Bundle.ACTIVE) + " but was "
					+ getBundleStateAsString(bundle.getState()));
		}
	}

	/** Checks about Bundle RESOLVED or ACTIVE state. */
	protected boolean isBundleResolved(final Bundle bundle) {
		return ((bundle.getState() == Bundle.RESOLVED) || (bundle.getState() == Bundle.ACTIVE));
	}

	/** Checks about Bundle ACTIVE state. */
	protected void assertBundleActive(final Bundle bundle) {
		if (bundle.getState() == Bundle.ACTIVE) {
			// all fine
		} else {
			if (isFragmentBundle(bundle) && isBundleResolved(bundle)) {
				// all fine
			} else {
				Assert.fail("Bundle " + bundle.getSymbolicName()
						+ " needs to be "
						+ getBundleStateAsString(Bundle.ACTIVE) + " but was "
						+ getBundleStateAsString(bundle.getState()));
			}
		}
	}

	/** Checks about Bundle INSTALLED state. */
	protected void assertBundleInstalled(final Bundle bundle) {
		if (bundle.getState() == Bundle.INSTALLED) {
			// all fine
		} else {
			Assert.fail("Bundle " + bundle.getSymbolicName() + " needs to be "
					+ getBundleStateAsString(Bundle.INSTALLED) + " but was "
					+ getBundleStateAsString(bundle.getState()));
		}
	}

	/**
	 * This method will install a "pseudo" bundle into the framework.
	 */
	protected Bundle installBundle(SyntheticBundleBuilder builder)
			throws BundleException {
		Bundle b = this.installBundle(builder.getBundleSymbolicName(),
				builder.asInputStream());
		return b;
	}

	protected Bundle installBundle(String bundleName, InputStream is)
			throws BundleException {
		final Bundle b = bundleContext.installBundle(bundleName, is);
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

	protected void dumpStorage() throws Exception {
		Concierge concierge = (Concierge) framework;
		Field field = concierge.getClass().getDeclaredField("STORAGE_LOCATION");
		field.setAccessible(true);
		Object o = field.get(concierge);
		System.err.println("dumpStorage: STORAGE_LOCATION=" + o);
		File dir = new File ((String) o);
		dumpStorageDirectory(dir);
	}
	
	private static void dumpStorageDirectory(File path) {
		File[] files = path.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				dumpStorageDirectory(files[i]);
			} else {
				System.err.println("dumpStorage: " + files[i]);
			}
		}
		System.err.println("dumpStorage: " + path);
	}


	/**
	 * The <code>RunInClassLoader</code> class helps to run code in ClassLoader
	 * of the bundle using Java reflection. This allows to call testing code in
	 * context of a bundle without need to have classes and/or compile code for
	 * testing purposes.
	 */
	public static class RunInClassLoader {

		private final Bundle bundle;
		private boolean debug = false;

		public RunInClassLoader(Bundle b) {
			this.bundle = b;
		}

		public void debug(boolean debug) {
			this.debug = debug;
		}

		/**
		 * Get a class references based on bundle classloader.
		 */
		public Class<?> getClass(final String className) throws Exception {
			final Class<?> clazz = this.bundle.loadClass(className);
			return clazz;
		}

		public Object getService(final String className) {
			Object service = null;
			ServiceReference<?> sref = this.bundle.getBundleContext()
					.getServiceReference(className);
			service = this.bundle.getBundleContext().getService(sref);
			return service;
		}

		public Object getClassField(final String className,
				final String classFieldName) throws Exception {
			final Class<?> clazz = this.bundle.loadClass(className);
			final Field field = clazz.getField(classFieldName);
			if (!Modifier.isStatic(field.getModifiers())) {
				throw new RuntimeException("Oops, field " + field.toString()
						+ " is not static");
			}
			// get the value of field, as class field object == null
			final Object result = field.get(null);
			return result;
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

		public Object createInstance(String className, final Object[] args)
				throws Exception {
			final Class<?> clazz = this.bundle.loadClass(className);
			dumpDeclaredConstructors(clazz);
			// get parameter types from args
			final Class<?>[] parameterTypes = new Class[args.length];
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null) {
					parameterTypes[i] = Object.class;
				} else {
					parameterTypes[i] = args[i].getClass();
				}
			}
			final Constructor<?> constructor = clazz
					.getDeclaredConstructor(parameterTypes);
			// TODO Maybe set accessible if private?
			final Object result = constructor.newInstance(args);
			return result;
		}

		public Object createInstance(String className,
				final String[] parameterTypeNames, final Object[] args)
				throws Exception {
			final Class<?> clazz = this.bundle.loadClass(className);
			dumpDeclaredConstructors(clazz);
			// get parameter types from args
			final Class<?>[] parameterTypes = new Class[args.length];
			for (int i = 0; i < parameterTypeNames.length; i++) {
				parameterTypes[i] = bundle.getClass().getClassLoader()
						.loadClass(parameterTypeNames[i]);
			}
			final Constructor<?> constructor = clazz
					.getDeclaredConstructor(parameterTypes);
			// TODO Maybe set accessible if private?
			final Object result = constructor.newInstance(args);
			return result;
		}

		/**
		 * Call an instance method for given object. The method will be detected
		 * based on types of arguments.
		 */
		public Object callMethod(final Object obj, final String methodName,
				final Object[] args) throws Exception {
			final Class<?> clazz = obj.getClass();
			final Class<?>[] parameterTypes = new Class[args.length];
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null) {
					parameterTypes[i] = Object.class;
				} else {
					parameterTypes[i] = args[i].getClass();
				}
			}
			dumpMethods(clazz);
			dumpDeclaredMethods(clazz);
			final Method method = clazz.getMethod(methodName, parameterTypes);
			if (Modifier.isStatic(method.getModifiers())) {
				throw new RuntimeException("Oops, method " + method.toString()
						+ " is static");
			}
			final Object result = method.invoke(obj, args);
			return result;
		}

		/**
		 * Call an instance method for given object. The method will be detected
		 * based on give parameter types. Needed when args have to precise
		 * argument types, e.g. a method is of type Object, but the args is of
		 * type SomeClass.
		 */
		public Object callMethod(final Object obj, final String methodName,
				final Class<?>[] parameterTypes, final Object[] args)
				throws Exception {
			final Class<?> clazz = obj.getClass();
			dumpMethods(clazz);
			dumpDeclaredMethods(clazz);
			final Method method = clazz.getMethod(methodName, parameterTypes);
			if (Modifier.isStatic(method.getModifiers())) {
				throw new RuntimeException("Oops, method " + method.toString()
						+ " is static");
			}
			final Object result = method.invoke(obj, args);
			return result;
		}

		private void dumpMethods(Class<?> clazz) {
			if (!debug) {
				return;
			}
			System.out.println("dumpMethods: " + clazz.getName());
			final Method[] methods = clazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				System.out.println(methods[i]);
			}
			System.out.println("==================");
		}

		private void dumpDeclaredMethods(Class<?> clazz) {
			if (!debug) {
				return;
			}
			System.out.println("dumpDeclaredMethods: " + clazz.getName());
			final Method[] methods = clazz.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				System.out.println(methods[i]);
			}
			System.out.println("==================");
		}

		private void dumpDeclaredConstructors(Class<?> clazz) {
			if (!debug) {
				return;
			}
			System.out.println("dumpDeclaredConstructors: " + clazz.getName());
			final Constructor<?>[] constructors = clazz
					.getDeclaredConstructors();
			for (int i = 0; i < constructors.length; i++) {
				System.out.println(constructors[i]);
			}
			System.out.println("==================");
		}
	}
}
