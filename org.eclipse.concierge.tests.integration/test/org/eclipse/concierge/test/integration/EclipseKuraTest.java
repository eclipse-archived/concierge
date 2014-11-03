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
 * Tests whether Eclipse Kura can be installed and started in Concierge. There
 * are no tests which check whether it is really working in Concierge.
 * 
 * These bundles will be tested:
 * <ul>
 * <li>all bundles from kura/plugins/org.eclipse.kura.*</li>
 * </ul>
 *
 * These bundles needs to be tested, whether they can be installed. Actually
 * there is no dependency from Kura to these bundles.
 * 
 * <pre>
 * com.codeminders.hidapi_1.1.0.jar
 * org.apache.felix.dependencymanager_3.0.0.jar
 * org.apache.felix.deploymentadmin_0.9.5.jar
 * org.json_1.0.0.v201011060100.jar
 * org.knowhowlab.osgi.monitoradmin_1.0.2.jar
 * org.tigris.mtoolkit.iagent.rpc_3.0.0.20110411-0918.jar
 * </pre>
 * 
 * These bundles are NOT needed for Concierge:
 * 
 * <pre>
 * Equinox OSGi framework: org.eclipse.core.runtime_3.8.0.v20120521-2346.jar
 * Equinox OSGi framework: org.eclipse.equinox.launcher_1.3.0.v20120522-1813.jar
 * Equinox OSGi Services: org.eclipse.osgi_3.8.1.v20120830-144521.jar
 * JUnit 4 Testing: org.hamcrest.core_1.1.0.v20090501071000.jar
 * JUnit 4 Testing:  org.junit_4.10.0.v4_10_0_v20120426-0900.jar
 * </pre>
 * 
 * @author Jochen Hiller
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EclipseKuraTest extends AbstractConciergeTestCase {

	private static final String B_KURA(String bundleName) {
		return bundleName + "_0.2.0-SNAPSHOT.v201407201124" + ".jar";
	}

	@Override
	protected boolean stayInShell() {
		return false;
	}

	/**
	 * This test checks whether the included log4j bundles can be installed and
	 * started.
	 * 
	 * The log4j bundles will be installed first. The log4j bundle will be
	 * started, the fragment log4j.extras will be resolved implicitly.
	 * 
	 * There was a bug identified in Concierge about conflicts checking in
	 * fragments, for more details see <a
	 * href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=436724"
	 * >https://bugs.eclipse.org/bugs/show_bug.cgi?id=436724</a>.
	 * 
	 * Note: log4j and log4j.extra bundles requires a lot of
	 * <code>javax.*</code> packages, which needs to be added to system extra
	 * packages.
	 */
	@Test
	public void test01InstallAndStartLog4j() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							"javax.management,javax.naming,javax.xml.parsers,"
									+ "org.w3c.dom,org.xml.sax,org.xml.sax.helpers,"
									+ "javax.jmdns,"
									+ "javax.xml.transform,javax.xml.transform.dom,"
									+ "javax.xml.transform.sax,javax.xml.transform.stream");
			/*
			 * MultiMap {osgi.wiring.package=[BundleRequirement{ Import-Package
			 * javax.xml.parsers}, BundleRequirement{ Import-Package
			 * javax.xml.transform}, BundleRequirement{ Import-Package
			 * javax.xml.transform.dom}, BundleRequirement{ Import-Package
			 * javax.xml.transform.sax}, BundleRequirement{ Import-Package
			 * javax.xml.transform.stream}, BundleRequirement{ Import-Package
			 * org.apache.log4j.config}, BundleRequirement{ Import-Package
			 * org.apache.log4j.extras;version="1.1"},
			 * BundleRequirement{Import-Package
			 * org.apache.log4j.filter;version="1.1"},
			 * BundleRequirement{Import-Package org.apache.log4j.or},
			 * BundleRequirement{Import-Package
			 * org.apache.log4j.rolling;version="1.1"},
			 * BundleRequirement{Import-Package
			 * org.apache.log4j.rule;version="1.1"},
			 * BundleRequirement{Import-Package org.w3c.dom},
			 * BundleRequirement{Import-Package org.xml.sax},
			 * BundleRequirement{Import-Package org.xml.sax.helpers}],
			 * osgi.wiring.host=[BundleRequirement{Fragment-Host log4j }]}
			 */

			startFrameworkClean(launchArgs);

			// check log4j
			final Bundle[] bundles = installBundles(new String[] {
					"log4j_1.2.17.jar", "log4j.apache-log4j-extras_1.1.0.jar" });
			bundles[0].start();
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * This test checks whether the included log4j bundles AND slf4j
	 * implementation can be installed and started.
	 * 
	 * All bundles will be installed first. The log4j bundle and slf4j.api
	 * bundle will be started. By starting the slf4j.api bundle, the fragment
	 * slf4j.log4j bundle will resolved implicitly.
	 * 
	 * There was a bug identified in Concierge about conflicts checking in
	 * fragments, for more details see <a
	 * href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=436724"
	 * >https://bugs.eclipse.org/bugs/show_bug.cgi?id=436724</a>.
	 * 
	 * Note: log4j/slf4j requires some <code>javax.*</code> packages, which
	 * needs to be added to system extra packages.
	 */
	@Test
	public void test02InstallAndStartSlf4j() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra",
					"javax.management,javax.naming,javax.xml.parsers,"
							+ "org.w3c.dom,org.xml.sax,org.xml.sax.helpers");
			startFrameworkClean(launchArgs);

			// check log4j and slf4j
			final Bundle[] bundles = installBundles(new String[] {
					"log4j_1.2.17.jar", "slf4j.api_1.6.4.jar",
					"slf4j.log4j12_1.6.0.jar" });
			bundles[0].start();
			bundles[1].start();
			// do not start the fragment, must be resolved implicitly
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	/**
	 * This test checks whether soda.comm bundle can be installed and started.
	 * 
	 * There was a bug that bundle does fail on startup with a
	 * NullPointerException because some Java system properties will be used,
	 * which are only set oin Equinox and NOT in Concierge. See <a
	 * href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=436725"
	 * >https://bugs.eclipse.org/bugs/show_bug.cgi?id=436725</a>
	 */
	@Test
	public void test03SodaComm() throws Exception {
		try {
			startFramework();

			// check Soda Comm
			final Bundle[] bundles = installAndStartBundles(new String[] { "org.eclipse.soda.dk.comm_1.2.0.jar" });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test04USB() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
					// for javax.usb.common
							"javax.swing,javax.swing.tree,javax.swing.border,javax.swing.event");
			startFrameworkClean(launchArgs);

			// check USB bundles
			final Bundle[] bundles = installAndStartBundles(new String[] {
					"javax.usb.api_1.0.2.jar", "javax.usb.common_1.0.2.jar", });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test05CodeMinders() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra", "");
			startFrameworkClean(launchArgs);

			// check USB bundles
			final Bundle[] bundles = installAndStartBundles(new String[] { "com.codeminders.hidapi_1.1.0.jar", });
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}

	@Test
	public void test06MToookitIAgent() throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs.put("org.osgi.framework.system.packages.extra", "");
			startFrameworkClean(launchArgs);

			// check USB bundles
			final Bundle bundleUnderTest = installAndStartBundle("org.tigris.mtoolkit.iagent.rpc_3.0.0.20110411-0918.jar");
			assertBundleResolved(bundleUnderTest);

			// TODO this will raise an Exception in Concierge
			bundleUnderTest.stop();

		} finally {
			stopFramework();
		}
	}

	/**
	 * This test checks install and start of all Kura bundles.
	 * 
	 * There is actually one problem reported:
	 * 
	 * <pre>
	 * bundle://14.0/gosh_profile:40.1: CommandNotFoundException: Command not found:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              
	 * 	MATCH MATCH
	 * 	org.apache.felix.gogo.runtime.CommandNotFoundException: Command not found:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              
	 * 	at org.apache.felix.gogo.runtime.Closure.executeCmd(Closure.java:466)
	 * 	at org.apache.felix.gogo.runtime.Closure.executeStatement(Closure.java:395)
	 * 	at org.apache.felix.gogo.runtime.Pipe.run(Pipe.java:108)
	 * 	at org.apache.felix.gogo.runtime.Closure.execute(Closure.java:183)
	 * 	at org.apache.felix.gogo.runtime.Closure.execute(Closure.java:120)
	 * 	at org.apache.felix.gogo.runtime.CommandSessionImpl.execute(CommandSessionImpl.java:89)
	 * 	at org.apache.felix.gogo.shell.Shell.source(Shell.java:192)
	 * 	at org.apache.felix.gogo.shell.Shell.gosh(Shell.java:109)
	 * 	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	 * 	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	 * 	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	 * 	at java.lang.reflect.Method.invoke(Method.java:606)
	 * 	at org.apache.felix.gogo.runtime.Reflective.method(Reflective.java:136)
	 * 	at org.apache.felix.gogo.runtime.CommandProxy.execute(CommandProxy.java:82)
	 * 	at org.apache.felix.gogo.runtime.Closure.executeCmd(Closure.java:469)
	 * 	at org.apache.felix.gogo.runtime.Closure.executeStatement(Closure.java:395)
	 * 	at org.apache.felix.gogo.runtime.Pipe.run(Pipe.java:108)
	 * 	at org.apache.felix.gogo.runtime.Closure.execute(Closure.java:183)
	 * 	at org.apache.felix.gogo.runtime.Closure.execute(Closure.java:120)
	 * 	at org.apache.felix.gogo.runtime.CommandSessionImpl.execute(CommandSessionImpl.java:89)
	 * 	at org.apache.felix.gogo.shell.Activator.run(Activator.java:75)
	 * 	at java.lang.Thread.run(Thread.java:744)
	 * </pre>
	 */
	@Test
	public void test10InstallAndStartEclipseKuraWithDependencies()
			throws Exception {
		try {
			final Map<String, String> launchArgs = new HashMap<String, String>();
			launchArgs
					.put("org.osgi.framework.system.packages.extra",
							// for log4j and slf4j
							"javax.management,javax.naming,javax.xml.parsers,"
									+ "org.w3c.dom,org.xml.sax,org.xml.sax.helpers,"
									// for jetty
									+ "javax.net,javax.net.ssl,"
									+ "javax.security,javax.security.auth,javax.security.cert,"
									+ "javax.naming,javax.sql,"
									+ "org.ietf.jgss,"
									// for core.configuration
									+ "javax.xml.bind,javax.xml.bind.annotation,"
									+ "javax.xml.bind.annotation.adapters,javax.xml.bind.util,"
									+ "javax.xml.namespace,javax.xml.stream,"
									+ "org.w3c.dom,"
									// for kura.core
									+ "javax.crypto,javax.crypto.spec,"
									// for javax.usb.common
									+ "javax.swing,javax.swing.tree,javax.swing.border,javax.swing.event,"
									// for eclipse core jobs/contenttype
									+ "org.xml.sax.ext,");
			// use port 8080 otherwise default port 80 needs root permission
			launchArgs.put("org.osgi.service.http.port", "8080");
			startFrameworkClean(launchArgs);

			final Bundle xmlParserBundle = installAndStartBundle("org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar");
			assertBundleResolved(xmlParserBundle);

			// start log4j and slf4j first
			final Bundle[] log4jBundles = installBundles(new String[] {
					"log4j_1.2.17.jar", "slf4j.api_1.6.4.jar",
					"slf4j.log4j12_1.6.0.jar", });
			log4jBundles[0].start();
			log4jBundles[1].start();
			assertBundlesResolved(log4jBundles);

			final Bundle[] gogoBundles = installAndStartBundles(new String[] {
					"org.apache.felix.gogo.runtime_0.8.0.v201108120515.jar",
					"org.apache.felix.gogo.command_0.8.0.v201108120515.jar",
					"org.apache.felix.gogo.shell_0.8.0.v201110170705.jar", });
			assertBundlesResolved(gogoBundles);

			final Bundle[] equinoxRegistry = installAndStartBundles(new String[] {
					// registry and its deps
					"osgi.cmpn_4.3.0.201111022214.jar",
					"org.eclipse.equinox.supplement_1.5.100.v20140428-1446.jar",
					"org.eclipse.equinox.util_1.0.500.v20130404-1337.jar",
					// required by Equinox Console, is not optional
					"org.eclipse.concierge.extension.permission_1.0.0.201408052201.jar",
					"org.eclipse.equinox.console_1.1.0.v20140131-1639.jar",
					"org.eclipse.equinox.supplement_1.5.100.v20140428-1446.jar",
					"org.eclipse.equinox.common_3.6.200.v20130402-1505.jar",
					"org.eclipse.equinox.registry_3.5.400.v20140428-1507.jar", });
			assertBundlesResolved(equinoxRegistry);

			final Bundle[] jettyBundles = installAndStartBundles(new String[] {
					"javax.servlet_3.0.0.v201112011016.jar",
					"org.eclipse.jetty.util_8.1.3.v20120522.jar",
					"org.eclipse.jetty.io_8.1.3.v20120522.jar",
					"org.eclipse.jetty.http_8.1.3.v20120522.jar",
					"org.eclipse.jetty.continuation_8.1.3.v20120522.jar",
					"org.eclipse.jetty.server_8.1.3.v20120522.jar",
					"org.eclipse.jetty.security_8.1.3.v20120522.jar",
					"org.eclipse.jetty.servlet_8.1.3.v20120522.jar", });
			assertBundlesResolved(jettyBundles);
			final Bundle[] equinoxJettyBundles = installAndStartBundles(new String[] {
					"org.eclipse.equinox.http.servlet_1.1.300.v20120522-1841.jar",
					"org.eclipse.equinox.http.jetty_3.0.0.v20120522-1841.jar",
					"org.eclipse.equinox.http.registry_1.1.200.v20120522-2049.jar", });
			assertBundlesResolved(equinoxJettyBundles);

			final Bundle[] equinoxBundles = installAndStartBundles(new String[] {
					"org.eclipse.equinox.cm_1.0.400.v20120522-1841.jar",
					"org.eclipse.equinox.event_1.2.200.v20120522-2049.jar",
					"org.eclipse.equinox.metatype_1.2.0.v20120522-1841.jar",
					"org.eclipse.equinox.preferences_3.5.0.v20120522-1841.jar",
					"org.eclipse.equinox.app_1.3.100.v20120522-1841.jar",
					"org.eclipse.equinox.ds_1.4.200.v20131126-2331.jar" });
			assertBundlesResolved(equinoxBundles);

			final Bundle[] eclipseCore = installAndStartBundles(new String[] {
					"org.eclipse.core.contenttype_3.4.200.v20120523-2004.jar",
					"org.eclipse.core.jobs_3.5.300.v20120622-204750.jar", });
			assertBundlesResolved(eclipseCore);

			final Bundle[] otherBundles = installAndStartBundles(new String[] {
					// TODO problem with native clause???
					// "com.codeminders.hidapi_1.1.0.jar",
					"org.apache.felix.dependencymanager_3.0.0.jar",
					"org.apache.felix.deploymentadmin_0.9.5.jar",
					"org.json_1.0.0.v201011060100.jar",
					"org.knowhowlab.osgi.monitoradmin_1.0.2.jar",
					"org.tigris.mtoolkit.iagent.rpc_3.0.0.20110411-0918.jar", });
			assertBundlesResolved(otherBundles);

			final String[] bundleNames = new String[] {
					// o.e.kura.api and deps
					"javax.usb.api_1.0.2.jar",
					"org.eclipse.soda.dk.comm_1.2.0.jar",
					"osgi.cmpn_4.3.0.201111022214.jar",
					"org.eclipse.equinox.io_1.0.400.v20120522-2049.jar",
					B_KURA("org.eclipse.kura.api"),

					// o.e.kura.core and deps
					"mqtt-client_0.4.0.jar",
					"org.hsqldb.hsqldb_2.3.0.jar",
					B_KURA("org.eclipse.kura.core"),

					// o.e.kura.core.cloud and deps
					"org.apache.servicemix.bundles.protobuf-java_2.4.1.1.jar",
					B_KURA("org.eclipse.kura.core.cloud"),

					// o.e.kura.core.comm and deps
					B_KURA("org.eclipse.kura.core.comm"),

					// o.e.kura.core.configuration and deps
					B_KURA("org.eclipse.kura.core.configuration"),

					// o.e.k.core.crypto and deps
					B_KURA("org.eclipse.kura.core.crypto"),

					// o.e.kura.core.deployment and deps
					"org.apache.commons.io_2.4.0.jar",
					B_KURA("org.eclipse.kura.deployment.agent"),
					B_KURA("org.eclipse.kura.core.deployment"),

					// o.e.kura.core.net and deps
					B_KURA("org.eclipse.kura.core.net"),

					// o.e.kura.linux
					"org.apache.commons.net_3.1.0.v201205071737.jar",
					B_KURA("org.eclipse.kura.linux.clock"),

					// o.e.kura.linux.command and deps
					B_KURA("org.eclipse.kura.linux.command"),

					// o.e.kura.net.admin and deps
					B_KURA("org.eclipse.kura.linux.net"),
					B_KURA("org.eclipse.kura.net.admin"),

					// o.e.kura.linux.position
					B_KURA("org.eclipse.kura.linux.position"),

					// o.e.kura.linux.usb
					"javax.usb.common_1.0.2.jar",
					B_KURA("org.eclipse.kura.linux.usb"),
					// o.e.kura.linux.watchdog
					B_KURA("org.eclipse.kura.linux.watchdog"),

					// o.e.kura.web
					"org.apache.commons.fileupload_1.2.2.v20111214-1400.jar",
					"com.gwt.user_0.2.0.jar", B_KURA("org.eclipse.kura.web"), };

			final Bundle[] bundles = installAndStartBundles(bundleNames);
			assertBundlesResolved(bundles);
		} finally {
			stopFramework();
		}
	}
}
