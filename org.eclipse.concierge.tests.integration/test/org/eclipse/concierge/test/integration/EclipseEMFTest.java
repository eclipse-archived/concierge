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
import org.eclipse.concierge.test.util.SyntheticBundleBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;

/**
 * Tests for using EMF and Xtext with Concierge framework.
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EclipseEMFTest extends AbstractConciergeTestCase {

	private static final String asEmfBuild(String bundleName) {
		return bundleName + "_2.10.0.v20140514-1158" + ".jar";
	}

	private static final String asXTextBuild(String bundleName) {
		return bundleName + "_2.6.1.v201406120726" + ".jar";
	}

	@Override
	protected boolean stayInShell() {
		// set to true to stay in shell for interactive testing
		return false;
	}

	/**
	 * Install and start EMF common into Concierge.
	 */
	@Test
	public void test01EclipseEMFCommonInstallAndStart() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			startFrameworkClean(launchArgs);
			final Bundle[] bundles = installAndStartBundles(new String[] { asEmfBuild("org.eclipse.emf.common"), });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Install and start EMF common and EMF core into Concierge. Note: EMF core
	 * needs some javax.* imports which have to be added to system packages
	 * extra.
	 */
	@Test
	public void test02EclipseEMFCoreInstallAndStart() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.crypto,javax.crypto.spec,"
									+ "javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,"
									+ "org.xml.sax,org.xml.sax.helpers");
			startFrameworkClean(launchArgs);
			final Bundle[] bundles = installAndStartBundles(new String[] {
					asEmfBuild("org.eclipse.emf.common"),
					asEmfBuild("org.eclipse.emf.ecore"), });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Install and start EMF common, core, core.xmi. Note: core.xmi needs more
	 * packages to be added to system packages extra (org.xml.sax.ext,
	 * org.w3c.dom). When trying install org.eclipse.emf.codegen,
	 * org.eclipse.emf.codegen.ecore they need Eclipse Core Runtime.
	 */
	@Test
	public void test03EclipseEMFInstallAndStart() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.crypto,javax.crypto.spec,"
									+ "javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,"
									+ "org.xml.sax,org.xml.sax.helpers,org.xml.sax.ext,"
									+ "org.w3c.dom");
			startFrameworkClean(launchArgs);
			// TODO add codegen here...
			final Bundle[] bundles = installAndStartBundles(new String[] {
					asEmfBuild("org.eclipse.emf.common"),
					asEmfBuild("org.eclipse.emf.ecore"),
					asEmfBuild("org.eclipse.emf.ecore.xmi"), });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Install and start EMF core. Then install EMX examples into Concierge, and
	 * run a basic test.
	 * 
	 * The EMF examples bundles is taken from EMF.on.Felix as the examples
	 * bundle from EMF has even Require-Bundle directives to
	 * org.eclipse.core.runtime, and the variante from MBarbero NOT.
	 * 
	 * The basic test will run in the classloader of an additional bundle
	 * installed into the framework. This installed bundle needs Import-Package
	 * directives to get access to needed classes.
	 * 
	 * This is done this way by reflection to avoid direct references within
	 * IDE.
	 * 
	 * @see <a
	 *      href="https://github.com/mbarbero/emf.on.felix/">https://github.com/mbarbero/emf.on.felix/</a>
	 * 
	 */
	@Test
	public void test10EclipseEMFCommonInstallAndWork() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.crypto,javax.crypto.spec,"
									+ "javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,"
									+ "org.xml.sax,org.xml.sax.helpers,org.xml.sax.ext,"
									+ "org.w3c.dom");
			startFrameworkClean(launchArgs);
			final Bundle[] bundles = installAndStartBundles(new String[] {
					asEmfBuild("org.eclipse.emf.common"),
					asEmfBuild("org.eclipse.emf.ecore"),
					asEmfBuild("org.eclipse.emf.ecore.xmi"),
					"org.eclipse.emf.examples.library-2.5.0.jar", });
			assertBundlesResolved(bundles);

			// Install a test bundle which is using the EMF examples bundle
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"concierge.test.testEclipseEMFCommonInstallAndWork")
					.bundleVersion("1.0.0")
					.addManifestHeader("Import-Package",
					// "org.eclipse.emf.common.util, "
					// "org.eclipse.emf.ecore.resource.impl, "
							"org.eclipse.emf.ecore.xmi.impl, "
									+ "org.eclipse.emf.examples.extlibrary");
			final Bundle bundleUnderTest = installBundle(builder);
			enforceResolveBundle(bundleUnderTest);
			assertBundleResolved(bundleUnderTest);

			// run in class loader of examples bundle
			// checkIfEMFIsWorking(bundles[3]);
			// TODO should run in test bundle too later
			checkIfEMFIsWorking(bundleUnderTest);
		} finally {
			stopFramework();
		}
	}

	/**
	 * Runs tests according to EMFExamples. Does same code but using reflection.
	 * 
	 * @see <a
	 *      href="hhttps://github.com/mbarbero/emf.on.felix/blob/master/emf.on.felix/src/org/example/ExampleComponent.java#L24">https://github.com/mbarbero/emf.on.felix/blob/master/emf.on.felix/src/org/example/ExampleComponent.java#L24</a>
	 */
	private void checkIfEMFIsWorking(Bundle bundle) throws Exception {
		RunInClassLoader runner = new RunInClassLoader(bundle);
		// System.out.println(EXTLibraryPackage.eINSTANCE);
		Object libraryPackageEInstance = runner.getClassField(
				"org.eclipse.emf.examples.extlibrary.EXTLibraryPackage",
				"eINSTANCE");
		Assert.assertEquals(
				"org.eclipse.emf.examples.extlibrary.impl.EXTLibraryPackageImpl",
				libraryPackageEInstance.getClass().getName());

		// Library l = EXTLibraryFactory.eINSTANCE.createLibrary();
		Object libraryFactoryEInstance = runner.getClassField(
				"org.eclipse.emf.examples.extlibrary.EXTLibraryFactory",
				"eINSTANCE");
		Object l = runner.callMethod(libraryFactoryEInstance, "createLibrary",
				new Object[] {});
		Assert.assertEquals(
				"org.eclipse.emf.examples.extlibrary.impl.LibraryImpl", l
						.getClass().getName());

		// l.setName("Alexandria");
		Object aVoid = runner.callMethod(l, "setName",
				new Object[] { "Alexandria" });
		Assert.assertNull(aVoid); // void method
		// name is now set correct
		Assert.assertEquals("Alexandria",
				runner.callMethod(l, "getName", new Object[] {}));

		// XMIResource resource = new XMIResourceImpl();
		// resource.getContents().add(l);
		Object resource = runner.createInstance(
				"org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl",
				new Object[] {});
		Object contents = runner.callMethod(resource, "getContents",
				new Object[] {});
		Assert.assertNotNull(contents);
		Boolean addResult = (Boolean) runner.callMethod(contents, "add",
				new Class[] { Object.class }, new Object[] { l });
		Assert.assertTrue(addResult);

		// try {
		// resource.save(System.out, null);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		Object saveResult = runner.callMethod(resource, "save", new Class[] {
				java.io.OutputStream.class, java.util.Map.class },
				new Object[] { System.out, null });
		System.out.println(saveResult);
		Assert.assertNull(saveResult); // void
		// TODO take result from System.out and check for valid result

		// TODO add the other stuff as testing by reflection
		// ComposedAdapterFactory adapterFactory = new
		// ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		// adapterFactory.addAdapterFactory(new
		// EcoreItemProviderAdapterFactory());
		// adapterFactory.addAdapterFactory(new
		// EXTLibraryItemProviderAdapterFactory());
		//
		// LibraryItemProvider libraryItemProvider = new
		// LibraryItemProvider(null);
		// System.out.println("ItemProvider.getText:" +
		// libraryItemProvider.getText(l));
		// AdapterFactoryItemDelegator delegator = new
		// AdapterFactoryItemDelegator(adapterFactory);
		// System.out.println("ItemDelegator.getText:" + delegator.getText(l));
		//
		// System.out.println("ItemDelegator.getText:" +
		// delegator.getText(EcorePackage.eINSTANCE));
	}

	@Test
	public void test20EclipseEMFCoreCheckStartup() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.crypto,javax.crypto.spec,"
									+ "javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,"
									+ "org.xml.sax,org.xml.sax.helpers");
			startFrameworkClean(launchArgs);

			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar",
					"org.eclipse.osgi.services_3.4.0.v20140312-2051.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					"org.apache.felix.gogo.runtime_0.10.0.v201209301036.jar",
					// required by Equinox Console, is not optional
					"org.eclipse.concierge.extension.permission_1.0.0.201408052201.jar",
					"org.eclipse.equinox.console_1.1.100.v20141023-1406.jar",
					"org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
					"org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar",
					asEmfBuild("org.eclipse.emf.common"), });
			assertBundlesResolved(bundles);

			Bundle emfBundle = installAndStartBundle("org.eclipse.emf.ecore_2.10.0.v20140514-1158.jar");
			assertBundleResolved(emfBundle);

			// Install a test bundle which is using the EMF examples bundle
			SyntheticBundleBuilder builder = SyntheticBundleBuilder
					.newBuilder();
			builder.bundleSymbolicName(
					"concierge.test.test02EclipseEMFCoreCheckStartup")
					.bundleVersion("1.0.0")
					.addManifestHeader("Import-Package",
							"org.eclipse.core.runtime;registry=split;");
			final Bundle bundleUnderTest = installBundle(builder);
			bundleUnderTest.start();
			assertBundleResolved(bundleUnderTest);

			RunInClassLoader runner = new RunInClassLoader(bundleUnderTest);
			Object o = runner
					.getService("org.eclipse.core.runtime.IExtensionRegistry");
			Assert.assertNotNull(o);
			Object extensions = runner.callMethod(o, "getExtensions",
					new Object[] { "org.eclipse.core.resources" });
			Assert.assertNotNull(extensions);
			System.out.println(extensions);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test30EclipseXtext() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.crypto,javax.crypto.spec,"
									+ "javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,"
									+ "org.xml.sax,org.xml.sax.helpers,org.xml.sax.ext,"
									+ "org.w3c.dom");
			startFrameworkClean(launchArgs);
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"org.slf4j.api_1.7.2.v20121108-1250.jar",
					"org.slf4j.log4j_1.7.2.v20130115-1340.jar",
					"org.antlr.runtime_3.2.0.v201101311130.jar",
					asEmfBuild("org.eclipse.emf.common"),
					asEmfBuild("org.eclipse.emf.ecore"),
					asEmfBuild("org.eclipse.emf.ecore.xmi"),
					asXTextBuild("org.eclipse.xtext.util"),
					"javax.inject_1.0.0.v20091030.jar",
					"com.google.inject_3.0.0.v201312141243.jar",
					asXTextBuild("org.eclipse.xtext"), });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}
}
