/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.concierge.BundleImpl.Revision;
import org.eclipse.concierge.BundleImpl.Revision.WovenClassImpl;
import org.eclipse.concierge.Resources.BundleCapabilityImpl;
import org.eclipse.concierge.Resources.ConciergeBundleWiring;
import org.eclipse.concierge.Resources.HostedBundleCapability;
import org.eclipse.concierge.Utils.MultiMap;
import org.eclipse.concierge.Utils.RemoveOnlyList;
import org.eclipse.concierge.Utils.RemoveOnlyMap;
import org.eclipse.concierge.compat.service.BundleManifestTwo;
import org.eclipse.concierge.compat.service.XargsFileLauncher;
import org.eclipse.concierge.service.log.LogServiceImpl;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.UnfilteredServiceListener;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

/**
 * The core class of the Concierge OSGi framework. Implements the system bundle,
 * maintains the central bundle and service registry.
 * 
 * @author Jan S. Rellermeyer
 */
public final class Concierge extends AbstractBundle implements Framework,
		Bundle, BundleRevision, FrameworkWiring, FrameworkStartLevel,
		URLStreamHandlerFactory, BundleActivator {

	// deprecated core framework constants.

	@SuppressWarnings("deprecation")
	private static final String FRAMEWORK_EXECUTIONENVIRONMENT = Constants.FRAMEWORK_EXECUTIONENVIRONMENT;

	@SuppressWarnings("deprecation")
	private static final String BUNDLE_REQUIREDEXECUTIONENVIRONMENT = Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT;

	private static final String BUNDLE_SYMBOLIC_NAME = "org.eclipse.concierge";

	@SuppressWarnings("deprecation")
	private static Class<?> SERVICE_EVENT_HOOK_CLASS = org.osgi.framework.hooks.service.EventHook.class;

	// the runtime args
	/**
	 * framework basedir.
	 */
	private String BASEDIR;

	/**
	 * bundle location.
	 */
	public String BUNDLE_LOCATION = "file:.";

	private int BEGINNING_STARTLEVEL;

	/**
	 * the location where the storage resides.
	 */
	String STORAGE_LOCATION;

	/**
	 * classloader buffer size.
	 */
	static int CLASSLOADER_BUFFER_SIZE;

	/**
	 * logging enabled.
	 */
	boolean LOG_ENABLED = true;

	/**
	 * log service.
	 */
	LogService logger;

	/**
	 * log buffer size.
	 */
	int LOG_BUFFER_SIZE;

	/**
	 * log quiet ? (= no logging to System.out)
	 */
	boolean LOG_QUIET;

	/**
	 * decompress bundles with embedded jars.
	 */
	boolean DECOMPRESS_EMBEDDED;

	/**
	 * log level.
	 */
	int LOG_LEVEL;

	/**
	 * security.
	 */
	boolean SECURITY_ENABLED;

	/**
	 * debug outputs from bundles ?
	 */
	boolean DEBUG_BUNDLES;

	/**
	 * debug outputs from packages ?
	 */
	boolean DEBUG_PACKAGES;

	/**
	 * debug outputs from class loading ?
	 */
	boolean DEBUG_CLASSLOADING;

	/**
	 * debug outputs from services ?
	 */
	boolean DEBUG_SERVICES;

	/**
	 * path to the init.xargs file
	 */
	static String INIT_XARGS_FILE_PATH;

	/**
	 * the profile.
	 */
	private String PROFILE;

	private final String[] bootdelegationAbs;
	private final String[] bootdelegationPrefix;

	private static final int COLLISION_POLICY_SINGLE = -1;
	private static final int COLLISION_POLICY_NONE = 0;
	private static final int COLLISION_POLICY_MULTIPLE = 1;
	private static final int COLLISION_POLICY_MANAGED = 2;

	int collisionPolicy;

	private Hashtable<String, String> headers;

	private final Version version = new Version("1.5.0");

	/**
	 * Version displayed upon startup and returned by System Bundle
	 */
	private static final String FRAMEWORK_VERSION = "5.0.0.alpha";

	static final HashSet<String> SUPPORTED_EE = new HashSet<String>();

	static Comparator<AbstractBundle> VERSION_COMPARATOR = new Comparator<AbstractBundle>() {
		public int compare(final AbstractBundle b1, final AbstractBundle b2) {
			return b2.getVersion().compareTo(b1.getVersion());
		}
	};

	protected final static Pattern FILTER_ASSERT_MATCHER = Pattern
			.compile("\\(([^&\\!|=<>~\\(\\)]*)[=|<=|>=|~=]");

	// registry data structures

	/**
	 * the bundles.
	 */
	List<AbstractBundle> bundles = new ArrayList<AbstractBundle>(2);

	/**
	 * bundleID -> bundle.
	 */
	Map<Long, AbstractBundle> bundleID_bundles = new HashMap<Long, AbstractBundle>(
			2);

	/**
	 * location -> bundle.
	 */
	Map<String, AbstractBundle> location_bundles = Collections
			.synchronizedMap(new HashMap<String, AbstractBundle>(2));

	/**
	 * symbolicName -> List of bundles
	 */
	MultiMap<String, AbstractBundle> symbolicName_bundles = new MultiMap<String, AbstractBundle>(
			2);

	/**
	 * class name string -> service reference.
	 */
	// FIXME: synchronized...
	final MultiMap<String, ServiceReference<?>> serviceRegistry = new MultiMap<String, ServiceReference<?>>(
			3);

	/**
	 * bundle listeners.
	 */
	private final List<BundleListener> bundleListeners = new ArrayList<BundleListener>(
			1);

	/**
	 * synchronous bundle listeners.
	 */
	private final List<SynchronousBundleListener> syncBundleListeners = new ArrayList<SynchronousBundleListener>(
			1);

	private final MultiMap<BundleContext, BundleListener> bundleListenerMap = new MultiMap<BundleContext, BundleListener>();

	/**
	 * service listeners.
	 */
	private final List<ServiceListenerEntry> serviceListeners = new ArrayList<ServiceListenerEntry>(
			1);

	/**
	 * Map of unattached fragments in the system. HostName => List of fragments
	 */
	private final MultiMap<String, Revision> fragmentIndex = new MultiMap<String, Revision>(
			1);

	/**
	 * framework listeners.
	 */
	private final List<FrameworkListener> frameworkListeners = new ArrayList<FrameworkListener>(
			1);

	CapabilityRegistry capabilityRegistry = new CapabilityRegistry();

	Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();

	// the fields

	/**
	 * next bundle ID.
	 */
	private long nextBundleID = 1;

	/**
	 * the initial startlevel for installed bundles.
	 */
	int initStartlevel = 1;

	/**
	 * restart ?
	 */
	private final boolean restart = false;

	// system bundle

	/**
	 * the symbolicName of the system bundle
	 */
	private static final String FRAMEWORK_SYMBOLIC_NAME = "org.eclipse.concierge";

	private final List<BundleCapability> systemBundleCapabilities = new ArrayList<BundleCapability>();

	private static Properties defaultProperties;

	String osname;

	Version osversion;

	String language;

	String processor;

	final Properties properties;

	/*
	 * hook support
	 */

	// @formatter:off
	// bundle hooks
	private final List<ServiceReferenceImpl<CollisionHook>> bundleCollisionHooks = new ArrayList<ServiceReferenceImpl<CollisionHook>>(0);
	private final List<ServiceReferenceImpl<org.osgi.framework.hooks.bundle.EventHook>> bundleEventHooks = new ArrayList<ServiceReferenceImpl<org.osgi.framework.hooks.bundle.EventHook>>(0);
	private final List<ServiceReferenceImpl<org.osgi.framework.hooks.bundle.FindHook>> bundleFindHooks = new ArrayList<ServiceReferenceImpl<org.osgi.framework.hooks.bundle.FindHook>>(0);

	// resolver hook
	protected List<ServiceReferenceImpl<ResolverHookFactory>> resolverHookFactories = new ArrayList<ServiceReferenceImpl<ResolverHookFactory>>(0);

	// service hooks
	@SuppressWarnings("deprecation")
	protected List<ServiceReferenceImpl<org.osgi.framework.hooks.service.EventHook>> serviceEventHooks = new ArrayList<ServiceReferenceImpl<org.osgi.framework.hooks.service.EventHook>>(0);
	protected List<ServiceReferenceImpl<ListenerHook>> serviceListenerHooks = new ArrayList<ServiceReferenceImpl<ListenerHook>>(0);
	protected List<ServiceReferenceImpl<EventListenerHook>> serviceEventListenerHooks = new ArrayList<ServiceReferenceImpl<EventListenerHook>>(0);
	protected List<ServiceReferenceImpl<FindHook>> serviceFindHooks = new ArrayList<ServiceReferenceImpl<FindHook>>(0);

	// weaving hooks
	private final List<ServiceReferenceImpl<WeavingHook>> weavingHooks = new ArrayList<ServiceReferenceImpl<WeavingHook>>(0);

	// "hooks registry"
	private final HashMap<String, List<?>> hooks = new HashMap<String, List<?>>();
	// @formatter:on

	static final Dictionary<String, Object> props2Dict(final Properties props) {
		final Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (final Object key : Collections.list(props.propertyNames())) {
			final String keyStr = (String) key;
			table.put(keyStr, props.getProperty(keyStr));
		}
		return table;
	}

	private FrameworkEvent stopEvent = new FrameworkEvent(
			FrameworkEvent.STOPPED, this, null);

	private final BundleStartLevel systemBundleStartLevel = new SystemBundleStartLevel();

	private final ResolverImpl resolver = new ResolverImpl();

	/**
	 * start method.
	 * 
	 * @param args
	 *            command line arguments.
	 * @throws Exception
	 * @throws Throwable
	 *             if something goes wrong.
	 */
	public static void main(final String[] args) throws Exception {
		// TODO: populate micro-services

		// TODO: re-enable profile and restart...
		// TODO: implement the -install, -start, ...

		// TODO: temporary hack

		final XargsFileLauncher xargsLauncher = new XargsFileLauncher();

		// TODO: this is a temporary hack...
		INIT_XARGS_FILE_PATH = System
				.getProperty("org.eclipse.concierge.init.xargs");
		if (INIT_XARGS_FILE_PATH == null) {
			INIT_XARGS_FILE_PATH = "init.xargs";
		}

		final File xargs = new File(INIT_XARGS_FILE_PATH);
		final Concierge fw;
		if(xargs.exists()){
			fw = xargsLauncher.processXargsFile(xargs);
		} else {
			fw = (Concierge) new Factory().newFramework(null);
			fw.init();
		}
		fw.waitForStop(0);
	}

	Concierge(final Map<String, String> passedProperties) {
		hooks.put(CollisionHook.class.getName(), bundleCollisionHooks);
		hooks.put(org.osgi.framework.hooks.bundle.FindHook.class.getName(),
				bundleFindHooks);
		hooks.put(EventHook.class.getName(), bundleEventHooks);
		hooks.put(ResolverHookFactory.class.getName(), resolverHookFactories);
		hooks.put(SERVICE_EVENT_HOOK_CLASS.getName(), serviceEventHooks);
		hooks.put(EventListenerHook.class.getName(), serviceEventListenerHooks);
		hooks.put(FindHook.class.getName(), serviceFindHooks);
		hooks.put(ListenerHook.class.getName(), serviceListenerHooks);
		hooks.put(WeavingHook.class.getName(), weavingHooks);

		defaultProperties = new Properties(System.getProperties());

		defaultProperties.setProperty(Constants.FRAMEWORK_BOOTDELEGATION,
				"java.*, sun.*, com.sun.*");
		defaultProperties.setProperty(Constants.FRAMEWORK_BUNDLE_PARENT,
				Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
		defaultProperties.setProperty(Constants.FRAMEWORK_EXECPERMISSION, "");
		defaultProperties.setProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
				"1");

		final String feeStr = defaultProperties
				.getProperty(FRAMEWORK_EXECUTIONENVIRONMENT);
		System.out.println("feeStr " + feeStr);

		final StringBuffer myEEs = new StringBuffer();

		final StringBuffer seVersionList = new StringBuffer();
		final StringBuffer minVersionList = new StringBuffer();

		final int minor;
		int parsed = 0;
		try {
			parsed = Integer.parseInt(System.getProperty(
					"java.specification.version").substring(2));
		} catch (final NumberFormatException nfe) {
			nfe.printStackTrace();
		} finally {
			minor = parsed;
		}
		if (System.getProperty("java.specification.name").equals(
				"J2ME Foundation Specification")) {
			switch (minor) {
			case 1:
				myEEs.append("CDC-1.1/Foundation-1.1,");
			case 0:
				myEEs.append("CDC-1.0/Foundation-1.0");
			}
		} else {
			switch (minor) {
			case 7:
				myEEs.append("J2SE-1.7,");
				myEEs.append("JavaSE-1.7,");
				seVersionList.append("1.7,");
			case 6:
				myEEs.append("J2SE-1.6,");
				myEEs.append("JavaSE-1.6,");
				seVersionList.append("1.6,");
			case 5:
				myEEs.append("J2SE-1.5,");
				seVersionList.append("1.5,");
			case 4:
				myEEs.append("J2SE-1.4,");
				myEEs.append("OSGi/Minimum-1.1,");
				seVersionList.append("1.4,");
				minVersionList.append("1.2,1.1,");
			case 3:
				myEEs.append("J2SE-1.3,");
				seVersionList.append("1.3,");
			case 2:
				myEEs.append("J2SE-1.2,");
				myEEs.append("OSGi/Minimum-1.0,");
				seVersionList.append("1.2,");
				minVersionList.append("1.0");
			case 1:
				myEEs.append("JRE-1.1");
				seVersionList.append("1.1");
			}
		}

		defaultProperties.setProperty(FRAMEWORK_EXECUTIONENVIRONMENT,
				myEEs.toString());

		// populate osgi.ee namespace
		try {
			if (seVersionList.length() > 0) {
				final BundleCapabilityImpl eeCap = new BundleCapabilityImpl(
						this,
						"osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\""
								+ seVersionList.toString() + "\"");
				systemBundleCapabilities.add(eeCap);
			}
			if (minVersionList.length() > 0) {
				final BundleCapabilityImpl eeCap = new BundleCapabilityImpl(
						this,
						"osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\""
								+ minVersionList.toString() + "\"");
				systemBundleCapabilities.add(eeCap);
			}
		} catch (final BundleException be) {
			// TODO: to log
			be.printStackTrace();
		}

		if (feeStr != null) {
			// TODO: get microservice
			final List<BundleCapability> feeCaps = new BundleManifestTwo()
					.translateToCapability(this,
							FRAMEWORK_EXECUTIONENVIRONMENT, feeStr);
			systemBundleCapabilities.addAll(feeCaps);
			System.out.println("NEW CAPABILITIES " + feeCaps);
		}

		// TODO: use "reasonable defaults"...
		defaultProperties
				.setProperty(
						Constants.FRAMEWORK_SYSTEMPACKAGES,
						"org.osgi.framework;version=1.7,org.osgi.framework.hooks.bundle;version=1.1,org.osgi.framework.hooks.resolver;version=1.0,org.osgi.framework.hooks.service;version=1.1,org.osgi.framework.hooks.weaving;version=1.0,org.osgi.framework.launch;version=1.1,org.osgi.framework.namespace;version=1.0,org.osgi.framework.startlevel;version=1.0,org.osgi.framework.wiring;version=1.1,org.osgi.resource;version=1.0,org.osgi.service.log;version=1.3,org.osgi.service.packageadmin;version=1.2,org.osgi.service.startlevel;version=1.1,org.osgi.service.url;version=1.0,org.osgi.service.resolver;version=1.0,org.osgi.util.tracker;version=1.5,META-INF.services");

		Object obj;
		defaultProperties.put(Constants.FRAMEWORK_OS_NAME,
				(obj = System.getProperty("os.name")) != null ? obj
						: "undefined");
		defaultProperties.put(Constants.FRAMEWORK_OS_VERSION,
				(obj = System.getProperty("os.version")) != null ? obj
						: "undefined");
		defaultProperties.put(Constants.FRAMEWORK_PROCESSOR,
				(obj = System.getProperty("os.arch")) != null ? obj
						: "undefined");

		final String lang = java.util.Locale.getDefault().getLanguage();
		defaultProperties.setProperty(Constants.FRAMEWORK_LANGUAGE,
				lang != null ? lang : "en");
		defaultProperties.setProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
				"3");
		defaultProperties.setProperty(Constants.FRAMEWORK_STORAGE, "storage");
		
		defaultProperties.setProperty(Constants.FRAMEWORK_UUID, UUID.randomUUID().toString());
		
		// properties
		properties = new Properties(defaultProperties) {

			private static final long serialVersionUID = -3319768973242656809L;

			public String getProperty(final String key) {
				final Object launchO = get(key);
				final String launchS = launchO instanceof String ? (String) launchO
						: null;
				if (launchS != null) {
					return launchS;
				}
				final String system = System.getProperty(key);
				return system == null ? defaults.getProperty(key) : system;
			}

		};

		if (passedProperties != null) {
			for (final Map.Entry<String, String> entry : passedProperties
					.entrySet()) {
				if (entry.getValue() != null) {
					properties.put(entry.getKey(), entry.getValue());
				}
			}
		}

		// apply constants
		properties.setProperty(Constants.FRAMEWORK_VERSION, "1.5");
		properties
				.setProperty(Constants.FRAMEWORK_VENDOR, "Jan S. Rellermeyer");
		properties.setProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION, "true");
		properties.setProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
				"false");
		properties.setProperty(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, "true");
		properties.setProperty(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE,
				"true");

		// set instance properties
		PROFILE = properties.getProperty("org.eclipse.concierge.profile",
				"default");
		BASEDIR = properties.getProperty("org.eclipse.concierge.basedir", ".");
		BUNDLE_LOCATION = properties.getProperty("org.eclipse.concierge.jars",
				"file:" + BASEDIR);
		CLASSLOADER_BUFFER_SIZE = getProperty(
				"org.eclipse.concierge.classloader.buffersize", 2048);
		LOG_ENABLED = getProperty("org.eclipse.concierge.log.enabled", false);
		LOG_QUIET = getProperty("org.eclipse.concierge.log.quiet", false);
		LOG_BUFFER_SIZE = getProperty("org.eclipse.concierge.log.buffersize",
				10);
		LOG_LEVEL = getProperty("org.eclipse.concierge.log.level",
				LogService.LOG_ERROR);
		DEBUG_BUNDLES = getProperty("org.eclipse.concierge.debug.bundles",
				false);
		DEBUG_PACKAGES = getProperty("org.eclipse.concierge.debug.packages",
				false);
		DEBUG_SERVICES = getProperty("org.eclipse.concierge.debug.services",
				false);
		DEBUG_CLASSLOADING = getProperty(
				"org.eclipse.concierge.debug.classloading", false);
		if (getProperty("org.eclipse.concierge.debug", false)) {
			System.out.println("SETTING ALL DEBUG FLAGS");
			LOG_ENABLED = true;
			LOG_LEVEL = LogService.LOG_DEBUG;
			DEBUG_BUNDLES = true;
			DEBUG_PACKAGES = true;
			DEBUG_SERVICES = true;
			DEBUG_CLASSLOADING = true;
			LOG_LEVEL = 4;
		}
		DECOMPRESS_EMBEDDED = getProperty(
				"org.eclipse.concierge.decompressEmbedded", true);
		SECURITY_ENABLED = getProperty(
				"org.eclipse.concierge.security.enabled", false);

		final String bsl = properties
				.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
		try {
			BEGINNING_STARTLEVEL = Integer.parseInt(bsl);
		} catch (final NumberFormatException nfe) {
			nfe.printStackTrace();
			System.err
					.println("FALLING BACK TO DEFAULT BEGINNING STARTLEVEL (=1)");
			BEGINNING_STARTLEVEL = 1;
		}

		// sort out the boot delegations
		final String[] bds = Utils
				.splitString(properties
						.getProperty(Constants.FRAMEWORK_BOOTDELEGATION), ",");
		final ArrayList<String> bdsAbs = new ArrayList<String>();
		final ArrayList<String> bdsRel = new ArrayList<String>();
		int pos;
		for (int i = 0; i < bds.length; i++) {
			if ((pos = bds[i].indexOf('*')) < 0) {
				bdsAbs.add(bds[i]);
			} else {
				if (pos < bds[i].length() - 1) {
					throw new IllegalArgumentException(
							"Framework bootdelegation " + bds[i]
									+ " is not supported");
				}
				bdsRel.add(bds[i].substring(0, pos));
			}
		}

		// remove the fast paths
		bdsRel.remove("java.");
		bdsRel.remove("sun.");
		bdsRel.remove("com.sun.");

		bootdelegationAbs = bdsAbs.toArray(new String[bdsAbs.size()]);
		bootdelegationPrefix = bdsRel.toArray(new String[bdsRel.size()]);

		// sanity checks
		if (!LOG_ENABLED) {
			if (DEBUG_BUNDLES || DEBUG_PACKAGES || DEBUG_SERVICES
					|| DEBUG_CLASSLOADING) {
				System.err.println("Logger disabled, ignoring debug flags.");
				DEBUG_BUNDLES = false;
				DEBUG_PACKAGES = false;
				DEBUG_SERVICES = false;
				DEBUG_CLASSLOADING = false;
			}
		}
		if (System.getSecurityManager() == null) {
			if (SECURITY_ENABLED) {
				warning("No security manager set, ignoring security flag.");
				SECURITY_ENABLED = false;
			}
		}

		location = Constants.SYSTEM_BUNDLE_LOCATION;
		state = Bundle.INSTALLED;
		context = new BundleContextImpl(this);
		domain = Concierge.class.getProtectionDomain();
		headers = new Hashtable<String, String>(5);
	}

	/**
	 * get a boolean property.
	 * 
	 * @param key
	 *            the key.
	 * @param defaultVal
	 *            the default.
	 * @return the value.
	 */
	private boolean getProperty(final String key, final boolean defaultVal) {
		final String val = properties.getProperty(key);
		return val != null ? Boolean.valueOf(val).booleanValue() : defaultVal;
	}

	/**
	 * get an int property.
	 * 
	 * @param key
	 *            the key.
	 * @param defaultVal
	 *            the default.
	 * @return the value.
	 */
	private int getProperty(final String key, final int defaultVal) {
		final String val = properties.getProperty(key);
		return val != null ? Integer.parseInt(val) : defaultVal;
	}

	/**
	 * write a warning or throw an Exception
	 * 
	 * @param message
	 * @throws BundleException
	 */
	private void warning(final String message) throws RuntimeException {
		if (getProperty("org.eclipse.concierge.strictStartup", false)) {
			throw new RuntimeException(message);
		}
		System.err.println("WARNING: " + message);
	}

	// Framework

	/**
	 * 
	 * @see org.osgi.framework.launch.Framework#init()
	 * @category Framework
	 */
	public void init() throws BundleException {
		if (state == Bundle.ACTIVE || state == Bundle.STARTING
				|| state == Bundle.STOPPING) {
			return;
		}

		final StringTokenizer t = new StringTokenizer(
				properties.getProperty(FRAMEWORK_EXECUTIONENVIRONMENT), ",");
		while (t.hasMoreTokens()) {
			SUPPORTED_EE.add(t.nextToken().trim());
		}

		// TODO: check if there is a security manager set and
		// Constants.FRAMEWORK_SECURITY; is set

		STORAGE_LOCATION = properties.getProperty(
				"org.eclipse.concierge.storage",
				properties.getProperty(Constants.FRAMEWORK_STORAGE, BASEDIR
						+ File.separatorChar + "storage"))
				+ File.separatorChar + PROFILE + File.separatorChar;

		// clean the storage if requested
		final File storage = new File(STORAGE_LOCATION);
		if (Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(properties
				.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN))) {

			if (storage.exists()) {
				deleteDirectory(storage);
			}
		}

		if (!storage.exists() && !storage.mkdirs()) {
			throw new BundleException("Could not create storage directory "
					+ storage);
		}

		// set start level 0
		startlevel = 0;
		// enable event handling

		// have bundle objects for all installed bundles

		// set the collision policy
		final String bsnversion = properties
				.getProperty(Constants.FRAMEWORK_BSNVERSION);
		if (bsnversion == null) {
			collisionPolicy = COLLISION_POLICY_NONE;
		} else {
			if (bsnversion.equals(Constants.FRAMEWORK_BSNVERSION_SINGLE)) {
				collisionPolicy = COLLISION_POLICY_SINGLE;
			} else if (bsnversion
					.equals(Constants.FRAMEWORK_BSNVERSION_MULTIPLE)) {
				collisionPolicy = COLLISION_POLICY_MULTIPLE;
			} else if (bsnversion
					.equals(Constants.FRAMEWORK_BSNVERSION_MANAGED)) {
				collisionPolicy = COLLISION_POLICY_MANAGED;
			}
		}

		// set the framework props
		final String os = properties.getProperty(Constants.FRAMEWORK_OS_NAME);
		osversion = new Version(
				sanitizeVersion(properties
						.getProperty(Constants.FRAMEWORK_OS_VERSION)));
		language = new Locale(
				properties.getProperty(Constants.FRAMEWORK_LANGUAGE), "")
				.getLanguage();
		final String cpu = properties
				.getProperty(Constants.FRAMEWORK_PROCESSOR).intern();
		if (os.toLowerCase().startsWith("win")) {
			osname = "Windows";
		} else {
			osname = os;
		}

		if (cpu == "pentium" || cpu == "i386" || cpu == "i486" || cpu == "i586"
				|| cpu == "i686") {
			processor = "x86";
		} else if (cpu == "amd64" || cpu == "em64t" || cpu == "x86_64"
				|| cpu == "x86-64") {
			processor = "x86-64";
		} else {
			processor = cpu;
		}

		// create the system bundle
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_NAME, BUNDLE_SYMBOLIC_NAME);
		headers.put(Constants.BUNDLE_VERSION, FRAMEWORK_VERSION);
		headers.put(BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
				"OSGi/Minimum-1.1, CDC-1.1/Foundation-1.1");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.concierge");
		final String extraPkgs = properties
				.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		final String sysPkgs = extraPkgs == null ? properties
				.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES) : properties
				.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES)
				+ ","
				+ extraPkgs;

		// initialize the system bundle capabilities
		final String[] framework_pkgs = sysPkgs.split(Utils.SPLIT_AT_COMMA);
		exportSystemBundlePackages(framework_pkgs);

		headers.put(Constants.EXPORT_PACKAGE, sysPkgs);
		headers.put(Constants.BUNDLE_VENDOR, "Eclipse Foundation");
		bundleID_bundles.put(new Long(0), this);
		location_bundles.put(Constants.SYSTEM_BUNDLE_LOCATION, this);
		symbolicName_bundles.insert(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, this);
		symbolicName_bundles.insert(BUNDLE_SYMBOLIC_NAME, this);

		final String extraCaps = properties
				.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA);
		if (extraCaps != null) {
			final String[] capStrs = extraCaps.split(Utils.SPLIT_AT_COMMA_PLUS);
			for (final String capStr : capStrs) {
				try {
					final BundleCapabilityImpl cap = new BundleCapabilityImpl(
							this, capStr);
					systemBundleCapabilities.add(cap);
				} catch (final BundleException be) {
					// TODO: to log
					be.printStackTrace();
				}
			}

		}

		publishCapabilities(systemBundleCapabilities);

		// add to framework wiring
		wirings.put(this, new ConciergeBundleWiring(this, null));

		// initialize the system bundle services
		registeredServices = new ArrayList<ServiceReference<?>>(2);

		// start the logger
		if (LOG_ENABLED) {
			logger = new LogServiceImpl(LOG_BUFFER_SIZE, LOG_LEVEL, LOG_QUIET);
			final ServiceReference<LogService> logref = new ServiceReferenceImpl<LogService>(
					Concierge.this, this, logger, null, new String[] {
							LogService.class.getName(),
							LogReaderService.class.getName() });
			synchronized (serviceRegistry) {
				serviceRegistry.insert(LogService.class.getName(), logref);
				serviceRegistry
						.insert(LogReaderService.class.getName(), logref);
			}

			registeredServices.add(logref);
			if (DEBUG_SERVICES) {
				logger.log(LogService.LOG_DEBUG,
						"Framework has registered LogService and LogReaderService.");
			}
		}

		// set the URLStreamHandlerFactory
		try {
			URL.setURLStreamHandlerFactory(this);
		} catch (final Error e) {
			// already set...
		}

		state = Bundle.STARTING;
	}

	private void exportSystemBundlePackages(final String[] pkgs)
			throws BundleException {
		for (final String pkg : pkgs) {
			final String[] literals = pkg.split(Utils.SPLIT_AT_SEMICOLON);

			final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
					.parseLiterals(literals, 1);
			final HashMap<String, Object> attrs = tuple.getLatter();
			attrs.put(PackageNamespace.PACKAGE_NAMESPACE, literals[0].trim());
			systemBundleCapabilities.add(new BundleCapabilityImpl(this,
					PackageNamespace.PACKAGE_NAMESPACE, tuple.getFormer(),
					tuple.getLatter(), Constants.EXPORT_PACKAGE + ' ' + pkg));

		}
	}

	private String sanitizeVersion(final String verStr) {
		int dot = 0;
		final int len = verStr.length();
		final char[] chars = verStr.toCharArray();
		for (int c = 0; c < len; c++) {
			if (chars[c] == '.' && dot < 2) {
				dot++;
				continue;
			}
			if (!Character.isDigit(chars[c])) {
				return verStr.substring(0, c);
			}
		}
		return verStr;
	}

	/**
	 * 
	 * @see org.osgi.framework.launch.Framework#waitForStop(long)
	 * @category Framework
	 */
	public synchronized FrameworkEvent waitForStop(final long timeout)
			throws InterruptedException {
		if (state == Bundle.STARTING || state == Bundle.STOPPING
				|| state == Bundle.ACTIVE) {
			synchronized (this) {
				wait(timeout);
			}
			if (state != Bundle.RESOLVED && state != Bundle.INSTALLED) {
				return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this,
						null);
			}
		}
		final FrameworkEvent event = stopEvent;
		stopEvent = new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
		return event;
	}

	/**
	 * 
	 * @see org.osgi.framework.launch.Framework#start()
	 * @category Framework
	 * @category SystemBundle
	 */
	public void start() throws BundleException {
		// TODO: check for AdminPermission(this,EXECUTE)
		if (state != Bundle.STARTING) {
			init();
		}
		if (state == Bundle.ACTIVE) {
			// does nothing because the system bundle is already started
			return;
		}
		try {
			System.out.println("------------------"
					+ "---------------------------------------");
			System.out.println("  Concierge OSGi " + FRAMEWORK_VERSION + " on "
					+ System.getProperty("os.name") + " "
					+ System.getProperty("os.version") + " starting ... ("
					+ PROFILE + ") startlevel=" + BEGINNING_STARTLEVEL);
			System.out.println("-------------------"
					+ "--------------------------------------");
			final long time = System.currentTimeMillis();

			// start System bundle
			start(context);
			
			// set startlevel and start all bundles that are marked to be
			// started up to the intended startlevel
			setLevel(bundles.toArray(new Bundle[bundles.size()]),
					BEGINNING_STARTLEVEL, false);

			// save the metadata
			if (!restart) {
				storeProfile();
			}

			final float timediff = (System.currentTimeMillis() - time)
					/ (float) 1000.00;
			System.out.println("-----------------------"
					+ "----------------------------------");
			System.out.println("  Framework "
					+ (restart ? "restarted" : "started") + " in " + timediff
					+ " seconds.");
			System.out.println("---------------------------"
					+ "------------------------------");
			System.out.flush();
		} catch (final Exception e) {
			notifyFrameworkListeners(FrameworkEvent.ERROR, this,
					new BundleException("Exception during framework start",
							BundleException.STATECHANGE_ERROR, e));
		}
		state = Bundle.ACTIVE;
		notifyFrameworkListeners(FrameworkEvent.STARTED, this, null);
	}

	/**
	 * store the profile.
	 * 
	 */
	private void storeProfile() {
		final BundleImpl[] bundleArray = (BundleImpl[]) bundles
				.toArray(new AbstractBundle[bundles.size()]);
		for (int i = 0; i < bundleArray.length; i++) {
			if (bundleArray[i].state != Bundle.UNINSTALLED) {
				bundleArray[i].updateMetadata();
			}
		}
		storeMetadata();
	}

	/**
	 * store the framework metadata.
	 * 
	 */
	void storeMetadata() {
		try {
			final DataOutputStream out = new DataOutputStream(
					new FileOutputStream(new File(STORAGE_LOCATION, "meta")));
			out.writeInt(startlevel);
			out.writeLong(nextBundleID);
			out.close();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}

	boolean bootdelegation(final String pkg) {
		for (int i = 0; i < bootdelegationPrefix.length; i++) {
			if (pkg.startsWith(bootdelegationPrefix[i])) {
				return true;
			}
		}
		for (int i = 0; i < bootdelegationAbs.length; i++) {
			if (pkg.equals(bootdelegationAbs[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see org.osgi.framework.launch.Framework#start(int)
	 * @category Framework
	 * @category SystemBundle
	 */
	public void start(final int options) throws BundleException {
		start();
	}

	/**
	 * @see org.osgi.framework.launch.Framework#stop()
	 * @category Framework
	 * @category SystemBundle
	 */
	public void stop() throws BundleException {
		// TODO: check for AdminPermission(this,EXECUTE)
		new Thread() {
			public void run() {
				stop0(false);
			}
		}.start();
	}

	/**
	 * @see org.osgi.framework.launch.Framework#stop(int)
	 * @category Framework
	 * @category SystemBundle
	 */
	public void stop(final int options) throws BundleException {
		stop();
	}

	private void stop0(final boolean update) {
		state = Bundle.STOPPING;

		if (!update) {
			System.out.println("----------------------------"
					+ "-----------------------------");
			System.out.println("  Concierge OSGi shutting down ...");
			System.out.println("  Bye !");
			System.out.println("----------------------------"
					+ "-----------------------------");
		}

		try {
			setLevel(bundles.toArray(new Bundle[bundles.size()]), 0, true);
			state = Bundle.RESOLVED;
			
			// stop System bundle
			stop(context);
			
			// release all resources
			bundles.clear();
			bundleID_bundles.clear();
			serviceRegistry.clear();
			// TODO: more resources to clear?

			stopEvent = new FrameworkEvent(
					update ? FrameworkEvent.STOPPED_UPDATE
							: FrameworkEvent.STOPPED, this, null);

			// notify waiting threads
			synchronized (Concierge.this) {
				Concierge.this.notify();
			}
		} catch (final Exception e) {
			stopEvent = new FrameworkEvent(FrameworkEvent.ERROR,
					Concierge.this, new BundleException(
							"Exception during framework start", e));
			Concierge.this.notify();
		}
	}

	/**
	 * @see org.osgi.framework.launch.Framework#uninstall()
	 * @category Framework
	 * @category SystemBundle
	 */
	public void uninstall() throws BundleException {
		// TODO: check for AdminPermission(this,LIFECYCLE)
		throw new BundleException("System bundle cannot be uninstalled.");
	}

	/**
	 * @see org.osgi.framework.launch.Framework#update()
	 * @category Framework
	 * @category SystemBundle
	 */
	public void update() throws BundleException {
		// TODO: check for AdminPermission(this,EXECUTE)
		new Thread() {
			public void run() {
				final int state = Concierge.this.state;
				stop0(true);
				try {
					Concierge.this.start();
				} catch (final BundleException be) {
					be.printStackTrace();
				}
				Concierge.this.state = state;
			}
		}.start();
	}

	/**
	 * @see org.osgi.framework.launch.Framework#update(java.io.InputStream)
	 * @category Framework
	 * @category SystemBundle
	 */
	public void update(final InputStream in) throws BundleException {
		try {
			in.close();
		} catch (final IOException ioe) {
			// silently ignore
		}
		update();
	}

	// public long getBundleId() through AbstractBundle

	// public String getLocation() through AbstractBundle

	/**
	 * @see org.osgi.framework.Bundle#getSymbolicName()
	 * @category Framework SystemBundle
	 */
	public String getSymbolicName() {
		return FRAMEWORK_SYMBOLIC_NAME;
	}

	/**
	 * 
	 * @see org.osgi.framework.Bundle#getEntryPaths(java.lang.String)
	 * @category Framework
	 * @category SystemBundle
	 */
	public Enumeration<String> getEntryPaths(final String path) {
		return null;
	}

	/**
	 * @see org.osgi.framework.Bundle#findEntries(java.lang.String,
	 *      java.lang.String, boolean)
	 * @category Framework
	 * @category SystemBundle
	 */
	public Enumeration<URL> findEntries(final String path,
			final String filePattern, final boolean recurse) {
		return null;
	}

	/**
	 * 
	 * @see org.osgi.framework.Bundle#getEntry(java.lang.String)
	 * @category Framework
	 * @category SystemBundle
	 */
	public URL getEntry(final String path) {
		return null;
	}

	/**
	 * @see org.osgi.framework.Bundle#adapt(java.lang.Class)
	 * @category Framework
	 * @category SystemBundle
	 */
	@SuppressWarnings("unchecked")
	public <A> A adapt(final Class<A> type) {
		if (type == BundleStartLevel.class) {
			return (A) systemBundleStartLevel;
		}

		if (type.isInstance(this)) {
			return (A) this;
		}

		return null;
	}

	// Bundle

	// public final int getState() in AbstractBundle

	/**
	 * 
	 * @see org.osgi.framework.Bundle#getHeaders()
	 * @category SystemBundle
	 */
	public Dictionary<String, String> getHeaders() {
		return headers;
	}

	// public long getBundleId() in AbstractBundle

	// public String getLocation() in AbstractBundle

	// public ServiceReference<?>[] getRegisteredServices() in AbstractBundle

	/**
	 * 
	 * @see org.osgi.framework.Bundle#getServicesInUse()
	 * @category SystemBundle
	 */
	public ServiceReference<?>[] getServicesInUse() {
		return null;
	}

	// public boolean hasPermission(final Object permission) in AbstractBundle

	/**
	 * @see org.osgi.framework.Bundle#getResource(java.lang.String)
	 * @category SystemBundle
	 */
	public URL getResource(final String name) {
		return getClass().getClassLoader().getResource(name);
	}

	/**
	 * @see org.osgi.framework.Bundle#getHeaders(java.lang.String)
	 * @category SystemBundle
	 */
	public Dictionary<String, String> getHeaders(final String locale) {
		return headers;
	}

	/**
	 * @see org.osgi.framework.Bundle#loadClass(java.lang.String)
	 * @category SystemBundle
	 */
	public Class<?> loadClass(final String name) throws ClassNotFoundException {
		return getClass().getClassLoader().loadClass(name);
	}

	/**
	 * @see org.osgi.framework.Bundle#getResources(java.lang.String)
	 * @category SystemBundle
	 */
	public Enumeration<URL> getResources(final String name) throws IOException {
		return getClass().getClassLoader().getResources(name);
	}

	// public long getLastModified() in AbstractBundle

	// public final BundleContext getBundleContext() in AbstractBundle

	/**
	 * @see org.osgi.framework.Bundle#getSignerCertificates(int)
	 * @category SystemBundle
	 */
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
			final int signersType) {
		return null;
	}

	/**
	 * @see org.osgi.framework.Bundle#getVersion()
	 * @category SystemBundle
	 */
	public Version getVersion() {
		return version;
	}

	// public File getDataFile(final String filename) in AbstractBundle

	/**
	 * @see java.lang.Object#toString()
	 * @category SystemBundle
	 */
	public String toString() {
		return "Concierge System Bundle";
	}

	// BundleStartLevel

	final class SystemBundleStartLevel implements BundleStartLevel {

		/**
		 * @see org.osgi.framework.BundleReference#getBundle()
		 * @category BundleStartLevel
		 */
		public Bundle getBundle() {
			return Concierge.this;
		}

		/**
		 * @see org.osgi.framework.startlevel.BundleStartLevel#getStartLevel()
		 * @category BundleStartLevel
		 */
		public int getStartLevel() {
			return 0;
		}

		/**
		 * @see org.osgi.framework.startlevel.BundleStartLevel#setStartLevel(int)
		 * @category BundleStartLevel
		 */
		public void setStartLevel(final int startlevel) {
			throw new IllegalArgumentException();
		}

		/**
		 * @see org.osgi.framework.startlevel.BundleStartLevel#isPersistentlyStarted()
		 * @category BundleStartLevel
		 */
		public boolean isPersistentlyStarted() {
			return true;
		}

		/**
		 * @see org.osgi.framework.startlevel.BundleStartLevel#isActivationPolicyUsed()
		 * @category BundleStartLevel
		 */
		public boolean isActivationPolicyUsed() {
			return true;
		}
	}

	// FrameworkStartLevel

	/**
	 * @see org.osgi.framework.startlevel.FrameworkStartLevel#getStartLevel()
	 * @category FrameworkStartLevel
	 */
	public int getStartLevel() {
		return startlevel;
	}

	/**
	 * @see org.osgi.framework.startlevel.FrameworkStartLevel#setStartLevel(int,
	 *      org.osgi.framework.FrameworkListener[])
	 * @category FrameworkStartLevel
	 */
	public void setStartLevel(final int targetLevel,
			final FrameworkListener... listeners) {
		// TODO: check AdminPermission(this, STARTLEVEL);

		if (targetLevel <= 0) {
			throw new IllegalArgumentException("Start level " + targetLevel
					+ " is not a valid level");
		}

		new Thread() {
			public void run() {
				setLevel(bundles.toArray(new Bundle[bundles.size()]),
						targetLevel, false);
				notifyFrameworkListeners(FrameworkEvent.STARTLEVEL_CHANGED,
						Concierge.this, null);
				if (listeners != null) {
					notifyFrameworkListeners(listeners,
							FrameworkEvent.STARTLEVEL_CHANGED, Concierge.this,
							null);
				}
				storeMetadata();
			}
		}.start();
	}

	/**
	 * @see org.osgi.framework.startlevel.FrameworkStartLevel#getInitialBundleStartLevel()
	 * @category FrameworkStartLevel
	 */
	public int getInitialBundleStartLevel() {
		return initStartlevel;
	}

	/**
	 * @see org.osgi.framework.startlevel.FrameworkStartLevel#setInitialBundleStartLevel(int)
	 * @category FrameworkStartLevel
	 */
	public void setInitialBundleStartLevel(final int targetStartLevel) {
		// TODO: check AdminPermission(this, STARTLEVEL);

		if (targetStartLevel <= 0) {
			throw new IllegalArgumentException("Start level "
					+ targetStartLevel + " is not a valid level");
		}
		initStartlevel = targetStartLevel;
	}

	/**
	 * set the current startlevel but does not update the metadata.
	 * 
	 * @param targetLevel
	 *            the startlevel.
	 * 
	 */
	private void setLevel(final Bundle[] bundleArray, final int targetLevel,
			final boolean all) {
		if (startlevel == targetLevel) {
			return;
		}
		final boolean up = targetLevel > startlevel;

		final int levels = up ? targetLevel - startlevel : startlevel
				- targetLevel;
		final MultiMap<Integer, AbstractBundle> startLevels = new MultiMap<Integer, AbstractBundle>(
				0);
		// prepare startlevels
		for (int i = 0; i < bundleArray.length; i++) {
			final AbstractBundle bundle = (AbstractBundle) bundleArray[i];
			if (bundle == Concierge.this || bundle.state == Bundle.UNINSTALLED
					|| up && bundle.autostart == AUTOSTART_STOPPED || !up
					&& bundle.state == Bundle.RESOLVED) {
				continue;
			}
			final int offset;
			if (up) {
				offset = bundle.startlevel - startlevel - 1;
			} else {
				offset = startlevel - bundle.startlevel;
			}
			if (offset >= 0 && offset < levels) {
				startLevels.insert(new Integer(offset), bundle);
			}
		}

		for (int i = 0; i < levels; i++) {
			if (up) {
				startlevel++;
			} else {
				startlevel--;
			}
			final List<AbstractBundle> list = startLevels.get(new Integer(i));
			if (list == null) {
				continue;
			}
			final BundleImpl[] toProcess = list.toArray(new BundleImpl[list
					.size()]);
			for (int j = 0; j < toProcess.length; j++) {
				try {
					if (up) {
						// transient is implicit
						toProcess[j]
								.activate(toProcess[j].isActivationPolicyUsed() ? Bundle.START_ACTIVATION_POLICY
										: 0);
					} else {
						if (toProcess[toProcess.length - j - 1].getState() == Bundle.UNINSTALLED) {
							continue;
						}
						// transient is implicit
						toProcess[toProcess.length - j - 1].stopBundle();
					}
				} catch (final BundleException be) {
					be.getNestedException().printStackTrace();
					be.printStackTrace();
					notifyFrameworkListeners(FrameworkEvent.ERROR,
							up ? toProcess[j] : toProcess[toProcess.length - j
									- 1], be);
				} catch (final Throwable t) {
					t.printStackTrace();
					notifyFrameworkListeners(FrameworkEvent.ERROR,
							up ? toProcess[j] : toProcess[toProcess.length - j
									- 1], t);
				}
			}
		}

		startlevel = targetLevel;
	}

	// BundleRevision

	/**
	 * @see org.osgi.framework.wiring.BundleRevision#getDeclaredCapabilities(java.lang.String)
	 * @category BundleRevision
	 */
	public List<BundleCapability> getDeclaredCapabilities(final String namespace) {
		// FIXME:
		return Collections.unmodifiableList(systemBundleCapabilities);
	}

	/**
	 * @see org.osgi.framework.wiring.BundleRevision#getDeclaredRequirements(java.lang.String)
	 * @category BundleRevision
	 */
	public List<BundleRequirement> getDeclaredRequirements(
			final String namespace) {
		return Collections.emptyList();
	}

	/**
	 * @see org.osgi.framework.wiring.BundleRevision#getTypes()
	 * @category BundleRevision
	 */
	public int getTypes() {
		return 0;
	}

	/**
	 * @see org.osgi.framework.wiring.BundleRevision#getWiring()
	 * @category BundleRevision
	 */
	public BundleWiring getWiring() {
		// FIXME: implement
		return new ConciergeBundleWiring(this, null);
	}

	/**
	 * @see org.osgi.framework.wiring.BundleRevision#getCapabilities(java.lang.String)
	 * @category BundleRevision
	 */
	public List<Capability> getCapabilities(final String namespace) {
		return Collections.unmodifiableList(new ArrayList<Capability>(
				systemBundleCapabilities));
	}

	/**
	 * 
	 * @category BundleRevision
	 */
	public List<Requirement> getRequirements(final String namespace) {
		return Collections.emptyList();
	}

	// FrameworkWiring
	/**
	 * @see org.osgi.framework.wiring.FrameworkWiring#refreshBundles(java.util.Collection,
	 *      org.osgi.framework.FrameworkListener[])
	 */
	public void refreshBundles(final Collection<Bundle> bundleCollection,
			final FrameworkListener... listeners) {
		// TODO: check AdminPermission(this, RESOLVE)

		new Thread() {
			public void run() {
				synchronized (Concierge.this) {
					Bundle[] initial;

					// build the initial set of bundles
					if (bundleCollection == null) {
						initial = bundles.toArray(new Bundle[bundles.size()]);
					} else {
						initial = bundleCollection
								.toArray(new Bundle[bundleCollection.size()]);
					}

					final ArrayList<Bundle> toProcess = new ArrayList<Bundle>();

					// filter out those who need to be updated
					for (int i = 0; i < initial.length; i++) {
						if (initial[i] == Concierge.this) {
							// don't process (stop/start)
							continue;
						}
						if (initial[i].getState() == Bundle.INSTALLED) {
							continue;
						}
						final BundleImpl theBundle = (BundleImpl) initial[i];
						if (bundleCollection == null) {
							if (theBundle.currentRevision == null
									|| theBundle.currentRevision != theBundle.revisions
											.get(0)) {
								toProcess.add(theBundle);
							} else if (theBundle.currentRevision.fragments != null) {
								for (final Revision fragment : theBundle.currentRevision.fragments) {
									if (fragment.getBundle().getState() == Bundle.UNINSTALLED) {
										toProcess.add(initial[i]);
										break;
									}
								}
							}
						} else if (bundleCollection != null) {
							// bundleArray has entries which should be
							// processed anyway
							toProcess.add(initial[i]);
						}
					}

					// nothing to do ? fine, so we are done.
					if (toProcess.isEmpty()) {
						notifyListeners(FrameworkEvent.PACKAGES_REFRESHED,
								Concierge.this, null);

						return;
					}

					System.err.println("REFRESHING PACKAGES FROM BUNDLES "
							+ toProcess);

					if (LOG_ENABLED && DEBUG_PACKAGES) {
						logger.log(LogService.LOG_DEBUG,
								"REFRESHING PACKAGES FROM BUNDLES " + toProcess);
					}

					final Collection<Bundle> updateGraph = getDependencyClosure(toProcess);

					System.err.println("UPDATE GRAPH IS " + updateGraph);

					if (LOG_ENABLED && DEBUG_PACKAGES) {
						logger.log(LogService.LOG_DEBUG, "UPDATE GRAPH IS "
								+ updateGraph);
					}

					final ArrayList<Bundle> tmp = new ArrayList<Bundle>(
							updateGraph);
					Collections.sort(tmp);
					final Bundle[] refreshArray = tmp.toArray(new Bundle[tmp
							.size()]);

					// stop all bundles in the restart array regarding their
					// startlevels

					// perform a cleanup for all bundles
					// CLEANUP
					final List<Bundle> restartList = new ArrayList<Bundle>(1);

					for (int i = 0; i < refreshArray.length; i++) {
						final BundleImpl bu = (BundleImpl) refreshArray[i];
						try {
							if (bu.state == ACTIVE) {
								bu.stop();
								restartList.add(bu);
							}
							if (bu.state == RESOLVED) {
								bu.state = INSTALLED;
							}

							// bundle needs to be refreshed
							bu.refresh();

							if (bu.state == UNINSTALLED) {
								// bundle is uninstalled
								bundles.remove(bu);
							} else {
								notifyBundleListeners(BundleEvent.UNRESOLVED,
										bu);
							}
						} catch (final Exception e) {
							notifyListeners(FrameworkEvent.ERROR,
									refreshArray[i], e);
						}
					}

					// resolve, if possible
					// FIXME: should be bulk operation

					for (final Iterator<Bundle> resolveIter = restartList
							.iterator(); resolveIter.hasNext();) {
						final BundleImpl bu = (BundleImpl) resolveIter.next();
						try {
							if (bu.state == Bundle.INSTALLED) {
								System.err.println("UPDATE RESOLVING " + bu);
								final boolean success = bu.currentRevision
										.resolve(false);
								if (!success) {
									resolveIter.remove();
								}
							}
						} catch (final Exception e) {
							resolveIter.remove();
							notifyListeners(FrameworkEvent.ERROR, bu, e);
						}
					}

					// restart all bundles regarding their startlevels
					final AbstractBundle[] restartArray = restartList
							.toArray(new AbstractBundle[restartList.size()]);
					for (int i = 0; i < restartArray.length; i++) {
						try {
							restartArray[i].start();
						} catch (final Exception e) {
							notifyListeners(FrameworkEvent.ERROR,
									restartArray[i], e);
						}
					}

					notifyListeners(FrameworkEvent.PACKAGES_REFRESHED,
							Concierge.this, null);
				}
			} // end synchronized statement

			private void notifyListeners(final int type, final Bundle b,
					final Exception e) {
				switch (type) {
				case FrameworkEvent.PACKAGES_REFRESHED:
					System.out.println("PACKAGES_REFRESHED");
					break;
				case FrameworkEvent.ERROR:
					System.out.println("ERROR ");
					e.printStackTrace();
					break;
				default:
					System.out.println(type);
				}

				notifyFrameworkListeners(type, b, e);

				if (listeners != null) {
					notifyFrameworkListeners(listeners, type, b, e);
				}
			}

		}.start();
	}

	/**
	 * @see org.osgi.framework.wiring.FrameworkWiring#resolveBundles(java.util.Collection)
	 * @category FrameworkWiring
	 */
	public boolean resolveBundles(final Collection<Bundle> bundles) {
		final ArrayList<BundleRevision> resources = new ArrayList<BundleRevision>();
		boolean resolved = true;

		for (final Bundle bundle : bundles) {
			if (bundle.getState() == UNINSTALLED) {
				resolved = false;
				continue;
			}

			resources.add(bundle.adapt(BundleRevision.class));
		}

		try {
			resolved &= resolve(resources, false);
			return resolved;
		} catch (final BundleException e) {
			// should not be thrown for critical==false
			return false;
		}
	}

	private boolean inResolve;

	private ArrayList<ResolverHook> getResolverHooks(
			final Collection<BundleRevision> bundles) throws Throwable {
		final ArrayList<ResolverHook> hooks = new ArrayList<ResolverHook>();
		@SuppressWarnings("unchecked")
		final ServiceReferenceImpl<ResolverHookFactory>[] factories = resolverHookFactories
				.toArray(new ServiceReferenceImpl[resolverHookFactories.size()]);
		try {
			for (int i = 0; i < factories.length; i++) {
				final ServiceReferenceImpl<ResolverHookFactory> sref = factories[i];
				final ResolverHookFactory factory = sref
						.getService(Concierge.this);
				if (factory != null) {
					final ResolverHook hook = factory.begin(Collections
							.unmodifiableCollection(bundles));
					if (hook != null) {
						hooks.add(hook);
					}
					sref.ungetService(Concierge.this);
				}
			}
		} catch (final Throwable t) {
			for (ResolverHook hook : hooks) {
				hook.end();
			}
			throw t;
		}
		return hooks;
	}

	List<BundleCapability> resolveDynamic(final BundleRevision trigger,
			final String pkg, final String dynImportPackage,
			final BundleRequirement dynImport, final boolean multiple) {
		Collection<Capability> candidates = null;

		try {
			final ArrayList<ResolverHook> hooks = getResolverHooks(Arrays
					.asList(trigger));

			final String filterStr = dynImport.getDirectives().get(
					Namespace.REQUIREMENT_FILTER_DIRECTIVE);

			if (multiple) {
				// we have a wildcard, this means scanning
				candidates = capabilityRegistry
						.getAll(PackageNamespace.PACKAGE_NAMESPACE);
			} else {
				// we don't have a wildcard, use the index
				if (filterStr == null) {
					candidates = capabilityRegistry.getByValue(
							PackageNamespace.PACKAGE_NAMESPACE,
							dynImportPackage);
				} else {
					try {
						candidates = RFC1960Filter.filterWithIndex(dynImport,
								filterStr, capabilityRegistry);
					} catch (final InvalidSyntaxException e) {
						e.printStackTrace();
					}
				}
			}

			if (candidates == null || candidates.isEmpty()) {
				for (final ResolverHook hook : hooks) {
					hook.end();
				}
				return null;
			}

			filterCandidates(hooks, dynImport, candidates);

			final ArrayList<BundleCapability> matches = new ArrayList<BundleCapability>();

			for (final Capability cap : candidates) {
				final String candidatePackage = (String) cap.getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE);

				assert candidatePackage != null;

				if (multiple
						&& RFC1960Filter.stringCompare(pkg.toCharArray(), 0,
								candidatePackage.toCharArray(), 0) != 0) {
					continue;
				}

				if (cap instanceof BundleCapability
						&& Concierge.matches0(
								PackageNamespace.PACKAGE_NAMESPACE, dynImport,
								cap, filterStr)) {
					// we have a match
					matches.add((BundleCapability) cap);
				}

			}

			for (final ResolverHook hook : hooks) {
				hook.end();
			}

			Collections.sort(matches, Utils.EXPORT_ORDER);
			return matches;
		} catch (final Throwable t) {
			// TODO: handle
			return null;
		}

	}

	private void filterCandidates(final ArrayList<ResolverHook> hooks,
			final BundleRequirement requirement,
			final Collection<Capability> candidates) {
		// sort candidates by providing resources
		final MultiMap<BundleRevision, BundleCapability> mmap = new MultiMap<BundleRevision, BundleCapability>();

		for (final Iterator<Capability> iter = candidates.iterator(); iter
				.hasNext();) {
			final Capability cap = iter.next();
			final Resource res = cap.getResource();
			if (res instanceof BundleRevision) {
				mmap.insert((BundleRevision) cap.getResource(),
						(BundleCapability) cap);
				iter.remove();
			}
		}

		for (final ResolverHook hook : hooks) {
			hook.filterResolvable(mmap.keySet());
		}

		final RemoveOnlyList<BundleCapability> filteredCandidates = new RemoveOnlyList<BundleCapability>(
				mmap.getAllValues());

		for (final ResolverHook hook : hooks) {
			hook.filterMatches(requirement, filteredCandidates);
		}

		candidates.addAll(filteredCandidates);
	}

	boolean resolve(final Collection<BundleRevision> bundles,
			final boolean critical) throws BundleException {
		if (inResolve) {
			throw new IllegalStateException("nested resolve call");
		}

		inResolve = true;

		try {
			final Collection<Resource> fragments = new ArrayList<Resource>();

			// check which fragments can be attached to the bundles
			for (final Resource bundle : bundles) {
				if (bundle instanceof Revision) {
					final Revision revision = (Revision) bundle;

					// potential host ?
					if (revision.allowsFragmentAttachment()) {
						fragments.addAll(getFragments(revision));
					}
				}
			}

			final MultiMap<Resource, HostedCapability> hostedCapabilities = new MultiMap<Resource, HostedCapability>();

			resolver.hooks = getResolverHooks(bundles);

			final MultiMap<Resource, Wire> solution = new MultiMap<Resource, Wire>();
			final ArrayList<Requirement> unresolvedRequirements = new ArrayList<Requirement>();
			final ArrayList<Resource> unresolvedResources = new ArrayList<Resource>();

			resolver.resolve0(new ResolveContext() {

				public Collection<Resource> getMandatoryResources() {
					return new ArrayList<Resource>(bundles);
				}

				public Collection<Resource> getOptionalResources() {
					return fragments;
				}

				@Override
				public List<Capability> findProviders(
						final Requirement requirement) {
					final String filterStr = requirement.getDirectives().get(
							Namespace.REQUIREMENT_FILTER_DIRECTIVE);

					if (filterStr == null) {
						final List<Capability> providers = capabilityRegistry
								.getAll(requirement.getNamespace());
						Collections.sort(providers, Utils.EXPORT_ORDER);
						return providers;
					}

					try {
						final List<Capability> providers = RFC1960Filter
								.filterWithIndex(requirement, filterStr,
										capabilityRegistry);
						if (!providers.isEmpty()) {
							if (PackageNamespace.PACKAGE_NAMESPACE
									.equals(requirement.getNamespace())) {
								Collections.sort(providers, Utils.EXPORT_ORDER);
							}
							return providers;
						}

						// check if the resource itself provides a
						// candidate
						for (final Capability capability : requirement
								.getResource().getCapabilities(
										requirement.getNamespace())) {
							if (matches(requirement, capability)) {
								return Collections.singletonList(capability);
							}
						}
					} catch (final InvalidSyntaxException ise) {
						// TODO: debug output
						ise.printStackTrace();
						return Collections.emptyList();
					}

					return Collections.emptyList();
				}

				@Override
				public int insertHostedCapability(
						final List<Capability> capabilities,
						final HostedCapability hostedCapability) {
					publishCapabilities(Collections
							.singletonList(hostedCapability));

					capabilities.add(hostedCapability);

					hostedCapabilities.insert(hostedCapability.getResource(),
							hostedCapability);

					if (PackageNamespace.PACKAGE_NAMESPACE
							.equals(hostedCapability.getNamespace())) {
						Collections.sort(capabilities, Utils.EXPORT_ORDER);
						return capabilities.indexOf(hostedCapability);
					}
					return capabilities.size();
				}

				@Override
				public boolean isEffective(final Requirement requirement) {
					final String effective = requirement.getDirectives().get(
							Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
					return effective == null
							|| effective.equals(Namespace.EFFECTIVE_RESOLVE);
				}

				@Override
				public Map<Resource, Wiring> getWirings() {
					return wirings;
				}

			}, solution, unresolvedRequirements, unresolvedResources);

			// FIXME: DEBUG OUTPUT
			System.err.println("Solution: " + solution);

			// apply solution
			for (final Resource resource : solution.keySet()) {
				if (resource instanceof Revision) {
					final Revision revision = (Revision) resource;

					final List<Wire> wires = solution.get(resource);

					final boolean isFragment = revision.isFragment();
					if (isFragment) {
						boolean attached = false;
						for (final Iterator<Wire> iter = wires.iterator(); iter
								.hasNext();) {
							final Wire wire = iter.next();

							// scan the wires for host namespace wires
							if (HostNamespace.HOST_NAMESPACE.equals(wire
									.getRequirement().getNamespace())) {
								final Revision host = (Revision) wire
										.getProvider();
								try {
									host.attachFragment(revision);
									attached = true;
								} catch (final BundleException be) {
									notifyFrameworkListeners(
											FrameworkEvent.ERROR,
											revision.getBundle(), be);
									iter.remove();
								}
							}
						}
						if (!attached) {
							continue;
						}

						// fragment has been attached to at least one host =>
						// becomes resolved.
						revision.markResolved();
					}

					final ConciergeBundleWiring wiring;
					if (revision.getWiring() == null) {
						// set wiring for this bundle
						wiring = new ConciergeBundleWiring(revision, wires);
						revision.setWiring(wiring);
					} else {
						wiring = revision.addAdditionalWires(wires);
					}

					if (!isFragment) {
						final List<HostedCapability> hostedCaps = hostedCapabilities
								.lookup(resource);
						for (final HostedCapability hostedCap : hostedCaps) {
							// add hosted capability
							wiring.addCapability(hostedCap);
							revision.addHostedCapability(hostedCap);
						}
					}

					wirings.put(resource, wiring);
				}
			}

			if (unresolvedRequirements.isEmpty()
					&& unresolvedResources.isEmpty()) {
				return true;
			}

			if (critical) {
				throw new BundleException("Resolution failed "
						+ unresolvedRequirements, BundleException.RESOLVE_ERROR);
			}
			return false;
		} catch (final BundleException be) {
			throw be;
		} catch (final Throwable t) {
			throw new BundleException("Resolve Error",
					BundleException.REJECTED_BY_HOOK, t);
		} finally {
			Throwable error = null;
			if (resolver.hooks != null) {
				for (final ResolverHook hook : resolver.hooks) {
					try {
						hook.end();
					} catch (final Throwable t) {
						error = t;
					}
				}
			}
			resolver.hooks = null;
			inResolve = false;

			if (error != null) {
				throw new BundleException("Error",
						BundleException.REJECTED_BY_HOOK, error);
			}
		}
	}

	/**
	 * @see org.osgi.framework.wiring.FrameworkWiring#getRemovalPendingBundles()
	 * @category FrameworkWiring
	 */
	public Collection<Bundle> getRemovalPendingBundles() {
		final ArrayList<Bundle> removalPending = new ArrayList<Bundle>();

		bundleLoop: for (final AbstractBundle bundle : bundles) {
			if (bundle instanceof BundleImpl) {
				final List<BundleRevision> revisions = bundle.getRevisions();
				for (final BundleRevision rev : revisions) {
					final BundleWiring wiring = rev.getWiring();
					if (wiring != null && !wiring.isCurrent()
							&& wiring.isInUse()) {
						removalPending.add(bundle);
						continue bundleLoop;
					}
				}
			}
		}

		return removalPending;
	}

	/**
	 * @see org.osgi.framework.wiring.FrameworkWiring#getDependencyClosure(java.util.Collection)
	 * @category FrameworkWiring
	 */
	public Collection<Bundle> getDependencyClosure(
			final Collection<Bundle> bundles) {
		return getDependencies(bundles, false);
	}

	private Collection<Bundle> getDependencies(
			final Collection<Bundle> bundles, final boolean allRevisions) {
		// build up the dependency graph. See specs for details.
		final ArrayList<Bundle> toProcess = new ArrayList<Bundle>(bundles);

		final Set<Bundle> dependencySet = new HashSet<Bundle>();
		while (!toProcess.isEmpty()) {
			final Bundle b = toProcess.remove(0);

			if (b == this) {
				dependencySet.add(b);
				continue;
			}

			if (dependencySet.contains(b)) {
				continue;
			}

			if (!(b instanceof BundleImpl)) {
				throw new IllegalArgumentException(
						"Bundles were not created by this framework instance "
								+ b.getClass().getName());
			}

			dependencySet.add(b);

			final BundleImpl bundle = (BundleImpl) b;

			for (final BundleRevision brev : bundle.revisions) {
				// FIXME: why null???
				// if (brev == null)
				// continue;

				// final BundleWiring wiring = bundle.currentRevision == null ?
				// bundle
				// .getRevisions().get(0).getWiring()
				// : bundle.currentRevision.getWiring();
				final BundleWiring wiring = brev.getWiring();

				// all package exports
				if (wiring != null) {

					for (final BundleRevision rev : ((ConciergeBundleWiring) wiring).inUseSet) {
						toProcess.add(rev.getBundle());
					}

					/*
					 * final List<BundleWire> importWires = wiring
					 * .getProvidedWires(null);
					 * 
					 * if (importWires != null) { for (final BundleWire
					 * importWire : importWires) {
					 * toProcess.add(importWire.getRequirer().getBundle()); } }
					 */
					final List<BundleWire> hostWires = wiring
							.getRequiredWires(HostNamespace.HOST_NAMESPACE);
					if (hostWires != null) {
						for (final BundleWire hostWire : hostWires) {
							toProcess.add(hostWire.getProvider().getBundle());
						}
					}
				}
			}
		}

		return dependencySet;
	}

	public class ResolverImpl implements Resolver {

		protected ArrayList<ResolverHook> hooks;

		public Map<Resource, List<Wire>> resolve(final ResolveContext context)
				throws ResolutionException {
			if (context == null) {
				throw new IllegalArgumentException("context is null");
			}

			final MultiMap<Resource, Wire> solution = new MultiMap<Resource, Wire>();
			final ArrayList<Requirement> unresolvedRequirements = new ArrayList<Requirement>();
			final ArrayList<Resource> unresolvedResources = new ArrayList<Resource>();

			resolve0(context, solution, unresolvedRequirements,
					unresolvedResources);

			if (!unresolvedRequirements.isEmpty()
					|| !unresolvedResources.isEmpty()) {
				throw new ResolutionException("Could not resolve.", null,
						unresolvedRequirements);
			}

			return solution;
		}

		protected void resolve0(final ResolveContext context,
				final MultiMap<Resource, Wire> solution,
				final ArrayList<Requirement> unresolvedRequirements,
				final ArrayList<Resource> unresolvedResources) {
			final Collection<Resource> mandatory = context
					.getMandatoryResources();
			final Collection<Resource> optional = context
					.getOptionalResources();

			if (!(mandatory.isEmpty() && optional.isEmpty())) {
				final Map<Resource, Wiring> existingWirings = context
						.getWirings();

				for (final Resource resource : mandatory) {
					if (resource == null) {
						continue;
					}

					try {
						if (resource instanceof Revision
								&& !((Revision) resource)
										.resolveMetadata(false)) {
							unresolvedResources.add(resource);
							continue;
						}
					} catch (final BundleException e) {
						e.printStackTrace();
					}
					final Collection<Requirement> unres = resolveResource(
							context, resource, existingWirings, solution,
							new HashSet<Resource>());
					unresolvedRequirements.addAll(unres);
				}

				if (!unresolvedRequirements.isEmpty()
						|| !unresolvedResources.isEmpty()) {
					return;
				}

				for (final Resource resource : optional) {
					resolveResource(context, resource, existingWirings,
							solution, new HashSet<Resource>());
				}

			}
		}

		private void checkSingleton(final Revision resource) {
			try {
				final List<Capability> identities = resource
						.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);

				if (identities == null || identities.isEmpty()) {
					return;
				}

				final BundleCapability identity = (BundleCapability) identities
						.get(0);

				if (!"true".equals(identity.getDirectives().get(
						IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE))) {
					return;
				}

				final List<Capability> candidates = capabilityRegistry
						.getByKey(
								IdentityNamespace.IDENTITY_NAMESPACE,
								(String) identity.getAttributes().get(
										IdentityNamespace.IDENTITY_NAMESPACE));
				final List<BundleCapability> collisions = new ArrayList<BundleCapability>();
				for (final Capability candidate : candidates) {
					if (candidate instanceof BundleCapability) {
						collisions.add((BundleCapability) candidate);
					}
				}

				for (final ResolverHook hook : hooks) {
					hook.filterSingletonCollisions(identity, collisions);
				}
			} catch (final Throwable t) {
				t.printStackTrace();
				throw new RuntimeException(t.getMessage());
			}
		}

		private final Collection<Requirement> resolveResource(
				final ResolveContext context, final Resource resource,
				final Map<Resource, Wiring> existingWirings,
				final MultiMap<Resource, Wire> solution,
				final HashSet<Resource> inResolution) {
			inResolution.add(resource);

			if (existingWirings.containsKey(resource)
					|| solution.containsKey(resource)) {
				return Collections.emptyList();
			}

			final Collection<Requirement> unresolvedRequirements = new ArrayList<Requirement>();
			final Collection<Requirement> requirements = resource
					.getRequirements(null);

			final MultiMap<Resource, Wire> newWires = new MultiMap<Resource, Wire>();
			final List<Resource> hosts = new ArrayList<Resource>();

			// TODO: debug output
			System.out.println("resolving " + resource);

			if (resource instanceof Revision) {
				checkSingleton((Revision) resource);
			}

			for (final Requirement requirement : requirements) {
				// skip requirements which are not effective
				if (!context.isEffective(requirement)) {
					continue;
				}

				// find candidates for the requirement
				final Collection<Capability> candidates = context
						.findProviders(requirement);

				// filter through the resolver hooks if there are any
				if (hooks!=null && !hooks.isEmpty()
						&& requirement instanceof BundleRequirement) {
					filterCandidates(hooks, (BundleRequirement) requirement,
							candidates);
				}

				boolean resolved = false;
				final boolean multiple = Namespace.CARDINALITY_MULTIPLE
						.equals(requirement.getDirectives().get(
								Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE));

				for (final Capability capability : candidates) {

					if (HostNamespace.HOST_NAMESPACE.equals(capability
							.getNamespace())) {

						if (!((Revision) capability.getResource())
								.checkFragment((Revision) resource)) {
							resolved = true;
							continue;
						}

						hosts.add(capability.getResource());
					}

					// check if the provider is already resolved
					if (existingWirings.get(capability.getResource()) != null) {
						final Wire wire = Resources.createWire(capability,
								requirement);
						newWires.insert(resource, wire);
						newWires.insertUnique(capability.getResource(), wire);
						resolved = true;

						if (!multiple) {
							break;
						}
					} else {
						// try to recursively resolve the provider
						try {
							if (inResolution.contains(capability.getResource())
									|| (!(capability.getResource() instanceof Revision) || ((Revision) capability
											.getResource())
											.resolveMetadata(false))
									&& resolveResource(context,
											capability.getResource(),
											existingWirings, solution,
											inResolution).isEmpty()) {

								final Wire wire = Resources.createWire(
										capability, requirement);
								newWires.insert(resource, wire);
								newWires.insertUnique(capability.getResource(),
										wire);
								resolved = true;

								if (!multiple) {
									break;
								}
							}
						} catch (final BundleException be) {
							// ignore
						}
					}
				}

				if (!resolved
						&& !Namespace.RESOLUTION_OPTIONAL
								.equals(requirement
										.getDirectives()
										.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
					System.err.println("COULD NOT RESOLVE REQUIREMENT "
							+ requirement + " CANDIDATES WERE " + candidates);
					unresolvedRequirements.add(requirement);
				}

			}

			if (unresolvedRequirements.isEmpty()) {
				// resolution successful, add wires to solution
				if (newWires.isEmpty()) {
					solution.insertEmpty(resource);
				} else {
					solution.insertMap(newWires);
				}

				if (resource instanceof Revision) {
					final Revision revision = (Revision) resource;
					if (revision.isFragment() && !hosts.isEmpty()) {
						// host the capabilities
						for (final Capability cap : resource
								.getCapabilities(null)) {
							if (!IdentityNamespace.IDENTITY_NAMESPACE
									.equals(cap.getNamespace())) {
								for (final Resource host : hosts) {
									final HostedBundleCapability hostedCap = new HostedBundleCapability(
											(Revision) host, cap);

									context.insertHostedCapability(
											host.getCapabilities(null),
											hostedCap);
								}
							}

						}
					} else {
						revision.markResolved();
					}
				}
			}

			return unresolvedRequirements;
		}
	}

	// URLStreamHandlerFactory

	/**
	 * @see java.net.URLStreamHandlerFactory#createURLStreamHandler(java.lang.String)
	 * @category URLStreamHandlerFactory
	 */
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		// check the service registry, java.protocol.handler.pkgs, etc.
		if ("bundle".equals(protocol)) {
			return new URLStreamHandler() {

				protected URLConnection openConnection(final URL u)
						throws IOException {
					try {
						final String host = u.getHost();
						// FIXME: unsafe!
						final String[] s = Utils.splitString(host, ".");

						final Long bundleId = Long.parseLong(s[0]);
						final int rev = Integer.parseInt(s[1]);

						final BundleImpl bundle = (BundleImpl) bundleID_bundles
								.get(bundleId);
						return new URLConnection(u) {

							private InputStream inputStream;

							private boolean isConnected;

							public void connect() throws IOException {
								inputStream = bundle.getURLResource(u, rev);
								isConnected = true;
							}

							/*
							 * 
							 * @see java.net.URLConnection#getInputStream()
							 */
							public InputStream getInputStream()
									throws IOException {
								if (!isConnected) {
									connect();
								}
								return inputStream;
							}

						};
					} catch (final NumberFormatException nfe) {
						throw new IOException("Malformed host " + u.getHost());
					}
				}
			};
		}

		return null;
	}

	// full match
	static boolean matches(final Requirement req, final Capability cap) {
		final String reqNamespace = req.getNamespace();
		final String capNamespace = cap.getNamespace();

		if (!reqNamespace.equals(capNamespace)) {
			return false;
		}

		/*
		 * final String effective = req.getDirectives().get(
		 * Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE); if (!(effective == null
		 * || effective .equals(Namespace.EFFECTIVE_RESOLVE))) { return false; }
		 */

		final String filter = req.getDirectives().get(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE);

		try {
			if (!(filter == null || RFC1960Filter.fromString(filter).matches(
					cap.getAttributes()))) {
				return false;
			}

			return matches0(capNamespace, req, cap, filter);
		} catch (final InvalidSyntaxException e) {
			// TODO: to log
			e.printStackTrace();
			return false;
		}
	}

	// match on a prefiltered result
	static boolean matches0(final String namespace, final Requirement req,
			final Capability cap, final String filterStr) {
		if (!namespace.startsWith("osgi.wiring.")) {
			return true;
		}

		final String mandatory = cap.getDirectives().get(
				Namespace.RESOLUTION_MANDATORY);

		if (mandatory == null) {
			return true;
		}

		final Set<String> mandatoryAttributes = new HashSet<String>(
				Arrays.asList(Utils.unQuote(mandatory).toLowerCase()
						.split(Utils.SPLIT_AT_COMMA)));
		final Matcher matcher = FILTER_ASSERT_MATCHER
				.matcher(filterStr == null ? "" : filterStr);
		while (matcher.find()) {
			mandatoryAttributes.remove(matcher.group(1));
		}
		return mandatoryAttributes.isEmpty();
	}

	BundleContextImpl createBundleContext(final AbstractBundle bundle) {
		return new BundleContextImpl(bundle);
	}

	/**
	 * restore a profile.
	 * 
	 * @return the startlevel or -1 if the profile could not be restored.
	 */
	@SuppressWarnings("unused")
	private int restoreProfile() {
		try {
			System.out.println("restoring profile " + PROFILE);
			final File file = new File(STORAGE_LOCATION, "meta");
			if (!file.exists()) {
				System.out.println("Profile " + PROFILE
						+ " not found, performing clean start ...");
				return -1;
			}

			final DataInputStream in = new DataInputStream(new FileInputStream(
					file));
			final int targetStartlevel = in.readInt();
			nextBundleID = in.readLong();
			in.close();

			final File storageDir = new File(STORAGE_LOCATION);
			final File[] bundleDirs = storageDir.listFiles();

			for (int i = 0; i < bundleDirs.length; i++) {
				if (bundleDirs[i].isDirectory()) {
					final File meta = new File(bundleDirs[i], "meta");
					if (meta.exists()) {
						try {
							final AbstractBundle bundle = new BundleImpl(this,
									meta);
							System.out.println("RESTORED BUNDLE "
									+ bundle.location);
							bundles.add(bundle);
							bundleID_bundles.put(new Long(bundle.bundleId),
									bundle);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			return targetStartlevel;

		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}

		return 0;
	}

	AbstractBundle[] getBundleWithSymbolicName(final String symbolicName) {
		final List<AbstractBundle> list = symbolicName_bundles
				.get(symbolicName);
		return list == null ? new AbstractBundle[0] : list
				.toArray(new AbstractBundle[list.size()]);
	}

	/**
	 * Add an installed Fragment to the framework. From here it can be attached
	 * to potential host bundles.
	 * 
	 * @param bundle
	 *            the bundle object of the fragment
	 * @param fragmentStr
	 *            the fragment-host description of the fragment
	 * @param exports
	 *            the fragment's exports
	 * @param imports
	 *            the fragment's imports
	 * @param dynamicImports
	 *            the fragment's dynamic imports
	 * @param requireBundles
	 *            the fragment's require bundle
	 * @param classpathStrings
	 *            the fragment's class paths
	 */
	void addFragment(final Revision fragment) throws BundleException {
		final String fragmentHostName = fragment.getFragmentHost().getFormer();
		if (fragmentHostName.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME)
				|| fragmentHostName.equals(FRAMEWORK_SYMBOLIC_NAME)) {
			// TODO: process framework extensions fragments
		}

		fragmentIndex.insert(fragmentHostName, fragment);
	}

	/**
	 * Remove a fragment from the map of unattached fragments.
	 * 
	 * @param fragment
	 *            the fragment bundle to remove
	 */
	void removeFragment(final Revision fragment) {
		fragmentIndex.remove(fragment.getFragmentHost().getFormer(), fragment);
	}

	/**
	 * Get Fragments matching a host bundle.
	 * 
	 * @param hostBundle
	 *            , the host bundle, for which fragments should be found
	 * @return an array of fragments, which should be attached to the host
	 *         bundle
	 */
	List<Revision> getFragments(final BundleRevision hostBundle) {
		// TODO: evaluate filter!
		return fragmentIndex.lookup(hostBundle.getSymbolicName());
	}

	/**
	 * delete a directory with all subdirs.
	 * 
	 * @param path
	 *            the directory.
	 */
	static void deleteDirectory(final File path) {
		final File[] files = path.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				deleteDirectory(files[i]);
			} else {
				files[i].delete();
			}
		}
		path.delete();
	}

	/*
	 * framework operations
	 */

	/**
	 * unregister a service.
	 * 
	 * @param sref
	 *            the service reference.
	 */
	void unregisterService(final ServiceReference<?> sref) {
		// remove all class entries
		final String[] clazzes = (String[]) sref
				.getProperty(Constants.OBJECTCLASS);
		serviceRegistry.removeAll(clazzes, sref);

		boolean isHook = false;

		for (int i = 0; i < clazzes.length; i++) {
			@SuppressWarnings("unchecked")
			final List<ServiceReference<?>> hookList = (List<ServiceReference<?>>) hooks
					.get(clazzes[i]);
			if (hookList != null) {
				isHook = true;
				hookList.remove(sref);
			}
		}

		final AbstractBundle bundle = (AbstractBundle) sref.getBundle();
		bundle.registeredServices.remove(sref);

		// dispose list, if empty
		if (bundle.registeredServices.isEmpty()) {
			bundle.registeredServices = null;
		}

		if (!isHook) {
			notifyServiceListeners(ServiceEvent.UNREGISTERING, sref, null);
		}

		if (LOG_ENABLED && DEBUG_SERVICES) {
			logger.log(LogService.LOG_INFO, "Framework: UNREGISTERED SERVICE "
					+ sref);
		}
	}

	void notifyBundleListeners(final int state, final Bundle bundle) {
		notifyBundleListeners(state, bundle, bundle);
	}

	/**
	 * notify all bundle listeners.
	 * 
	 * @param state
	 *            the new state.
	 * @param bundle
	 *            the bundle.
	 */
	void notifyBundleListeners(final int state, final Bundle bundle,
			final Bundle origin) {
		if (syncBundleListeners.isEmpty() && bundleListeners.isEmpty()) {
			return;
		}

		final BundleEvent event = new BundleEvent(state, bundle, origin);

		final SynchronousBundleListener[] syncs;
		final BundleListener[] asyncs;

		// call the hooks, if any
		if (!bundleEventHooks.isEmpty()) {
			final ArrayList<SynchronousBundleListener> syncListeners = new ArrayList<SynchronousBundleListener>(
					syncBundleListeners);
			final ArrayList<BundleListener> asyncListeners = new ArrayList<BundleListener>(
					bundleListeners);

			final RemoveOnlyList<BundleContext> contexts = new RemoveOnlyList<BundleContext>(
					bundleListenerMap.keySet());

			for (final ServiceReferenceImpl<org.osgi.framework.hooks.bundle.EventHook> sref : bundleEventHooks) {
				final org.osgi.framework.hooks.bundle.EventHook eventHook = sref
						.getService(Concierge.this);
				if (eventHook != null) {
					try {
						eventHook.event(event, contexts);
					} catch (final Throwable t) {
						// TODO: to log?
					}
				}
				sref.ungetService(Concierge.this);
			}

			for (final BundleContext removed : contexts.getRemoved()) {
				for (final BundleListener listener : bundleListenerMap
						.get(removed)) {
					syncListeners.remove(listener);
					asyncListeners.remove(listener);
				}
			}

			syncs = syncListeners
					.toArray(new SynchronousBundleListener[syncListeners.size()]);
			asyncs = asyncListeners.toArray(new BundleListener[asyncListeners
					.size()]);
		} else {
			syncs = syncBundleListeners
					.toArray(new SynchronousBundleListener[syncBundleListeners
							.size()]);
			asyncs = bundleListeners.toArray(new BundleListener[bundleListeners
					.size()]);
		}

		for (int i = 0; i < syncs.length; i++) {
			syncs[i].bundleChanged(event);
		}

		// asynchronous listeners do not get these events
		final int type = event.getType();
		if (bundleListeners.isEmpty()
				|| (type & (BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.LAZY_ACTIVATION)) > 0) {
			return;
		}

		for (int i = 0; i < asyncs.length; i++) {
			asyncs[i].bundleChanged(event);
		}
	}

	void notifyFrameworkListeners(final int state, final Bundle bundle,
			final Throwable throwable) {
		notifyFrameworkListeners(
				frameworkListeners.toArray(new FrameworkListener[frameworkListeners
						.size()]), state, bundle, throwable);
	}

	void publishCapabilities(final List<? extends Capability> caps) {
		for (final Capability cap : caps) {
			capabilityRegistry.add(cap);
		}
	}

	// void removeCapabilities(final List<? extends Capability> caps) {
	// for (final Capability cap : caps) {
	// capabilityRegistry.remove(cap);
	// }
	// }

	void removeCapabilities(final Revision resource) {
		capabilityRegistry.removeAll(resource);

		for (final HostedCapability hosted : resource.getHostedCapabilities()) {
			capabilityRegistry.remove(hosted);
		}

		System.out.println("CAPABILITIES AFTER " + capabilityRegistry);
	}

	void checkForCollision(final int operation, final Bundle contextOwner,
			final BundleRevision revision) throws BundleException {
		assert context != null;

		if (revision == null) {
			throw new IllegalArgumentException("revision==null");
		}

		if (collisionPolicy == COLLISION_POLICY_MULTIPLE) {
			return;
		}

		final Version version = revision.getVersion();
		final ArrayList<Bundle> collisions = new ArrayList<Bundle>();
		final List<AbstractBundle> existing = symbolicName_bundles.get(revision
				.getSymbolicName());

		if (existing == null) {
			return;
		}

		if (version == null) {
			throw new IllegalStateException("version==null");
		}

		for (final AbstractBundle b : existing) {
			if (version.equals(b.getVersion())) {
				collisions.add(b);
			}
		}

		if (operation == CollisionHook.UPDATING) {
			collisions.remove(revision.getBundle());
		}

		if (collisions.isEmpty()) {
			return;
		}

		if (collisionPolicy == COLLISION_POLICY_SINGLE) {
			throw new BundleException(
					"Bundle with same symbolic name and same version is already installed",
					BundleException.DUPLICATE_BUNDLE_ERROR);
		} else if (collisionPolicy == COLLISION_POLICY_NONE) {
			final RemoveOnlyList<Bundle> list = new RemoveOnlyList<Bundle>(
					collisions);

			for (final ServiceReferenceImpl<CollisionHook> hookRef : bundleCollisionHooks) {
				final CollisionHook hook = hookRef.getService(this);

				if (hook != null) {
					hook.filterCollisions(operation, contextOwner, list);
				}

				hookRef.ungetService(this);
			}

			if (!list.isEmpty()) {
				throw new BundleException(
						"Bundle with same symbolic name and same version is already installed",
						BundleException.DUPLICATE_BUNDLE_ERROR);
			}
		}

	}

	/**
	 * notify all framework listeners.
	 * 
	 * @param state
	 *            the new state.
	 * @param bundle
	 *            the bundle.
	 * @param throwable
	 *            a throwable.
	 */
	protected void notifyFrameworkListeners(
			final FrameworkListener[] listeners, final int state,
			final Bundle bundle, final Throwable throwable) {

		if (listeners.length == 0) {
			return;
		}

		final FrameworkEvent event = new FrameworkEvent(state, bundle,
				throwable);

		for (int i = 0; i < listeners.length; i++) {
			final FrameworkListener listener = listeners[i];
			if (SECURITY_ENABLED) {
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						listener.frameworkEvent(event);
						return null;
					}
				});
			} else {
				listener.frameworkEvent(event);
			}
		}
	}

	/**
	 * notify all service listeners.
	 * 
	 * @param state
	 *            the new state.
	 * @param reference
	 *            the service reference.
	 */
	@SuppressWarnings("deprecation")
	void notifyServiceListeners(final int state,
			final ServiceReference<?> reference,
			final Dictionary<String, ?> oldProperties) {
		if (serviceListeners.isEmpty()) {
			return;
		}

		final ServiceEvent event = new ServiceEvent(state, reference);
		final ServiceEvent endmatchEvent = state == ServiceEvent.MODIFIED ? new ServiceEvent(
				ServiceEvent.MODIFIED_ENDMATCH, reference) : null;

		final ServiceListenerEntry[] entries;

		if (serviceEventListenerHooks.isEmpty() && serviceEventHooks.isEmpty()) {
			entries = serviceListeners
					.toArray(new ServiceListenerEntry[serviceListeners.size()]);
		} else {
			// prepare the data structures
			final MultiMap<BundleContext, ListenerInfo> mmap = new MultiMap<BundleContext, ListenerInfo>();

			for (final Iterator<ServiceListenerEntry> iter = serviceListeners
					.iterator(); iter.hasNext();) {
				final ServiceListenerEntry entry = iter.next();
				mmap.insert(entry.bundle.context, entry);
			}

			final RemoveOnlyMap<BundleContext, Collection<ListenerInfo>> map = new RemoveOnlyMap<BundleContext, Collection<ListenerInfo>>();
			for (final BundleContext ctx : mmap.keySet()) {
				final Collection<ListenerInfo> col = new RemoveOnlyList<ListenerInfo>(
						mmap.get(ctx));
				map.put(ctx, col);
			}
			map.seal();

			// first call the event hooks
			for (final ServiceReferenceImpl<org.osgi.framework.hooks.service.EventHook> eventHook : serviceEventHooks) {
				try {
					final org.osgi.framework.hooks.service.EventHook hook = eventHook
							.getService(this);
					hook.event(event, map.keySet());
				} catch (final Throwable t) {
					notifyFrameworkListeners(FrameworkEvent.ERROR,
							Concierge.this, t);
				} finally {
					eventHook.ungetService(this);
				}
			}

			// then call the event listener hooks
			for (final Iterator<ServiceReferenceImpl<EventListenerHook>> iter = serviceEventListenerHooks
					.iterator(); iter.hasNext();) {
				final ServiceReferenceImpl<EventListenerHook> hookRef = iter
						.next();
				try {
					final EventListenerHook hook = hookRef.getService(this);
					hook.event(event, map);
				} catch (final Throwable t) {
					notifyFrameworkListeners(FrameworkEvent.ERROR,
							Concierge.this, t);
				} finally {
					hookRef.ungetService(this);
				}
			}

			final ArrayList<ServiceListenerEntry> list = new ArrayList<ServiceListenerEntry>();
			for (final Iterator<ServiceListenerEntry> iter = serviceListeners
					.iterator(); iter.hasNext();) {
				final ServiceListenerEntry entry = iter.next();
				final Collection<ListenerInfo> listeners = map
						.get(entry.bundle.context);
				if (listeners != null && listeners.contains(entry)) {
					list.add(entry);
				}
			}
			entries = list.toArray(new ServiceListenerEntry[list.size()]);
		}

		final ServiceReferenceImpl<?> ref = (ServiceReferenceImpl<?>) reference;

		for (int i = 0; i < entries.length; i++) {
			// check if the listener can receive the service event
			if (!(entries[i].listener instanceof AllServiceListener)) {
				final String[] clazzes = (String[]) reference
						.getProperty(Constants.OBJECTCLASS);
				if (!ref.isAssignableTo(entries[i].bundle, clazzes)) {
					continue;
				}
			}
			if (entries[i].listener instanceof UnfilteredServiceListener
					|| entries[i].filter == null
					|| entries[i].filter.match(ref.properties)) {
				final ServiceListener listener = entries[i].listener;
				if (SECURITY_ENABLED) {
					AccessController
							.doPrivileged(new PrivilegedAction<Object>() {
								public Object run() {
									listener.serviceChanged(event);
									return null;
								}
							});
				} else {
					listener.serviceChanged(event);
				}
			} else if (state == ServiceEvent.MODIFIED) {
				if (entries[i].filter.match(oldProperties)) {
					entries[i].listener.serviceChanged(endmatchEvent);
				}
			}
		}
	}

	/**
	 * clear all traces of a bundle.
	 * 
	 * @param bundle
	 *            the bundle.
	 */
	void clearBundleTrace(final AbstractBundle bundle) {
		// remove all registered listeners
		if (bundle.registeredFrameworkListeners != null) {
			frameworkListeners.removeAll(bundle.registeredFrameworkListeners);
			bundle.registeredFrameworkListeners = null;
		}
		if (bundle.registeredServiceListeners != null) {
			serviceListeners.removeAll(bundle.registeredServiceListeners);
			bundle.registeredServiceListeners = null;
		}
		final List<BundleListener> bundleListeners = bundleListenerMap
				.get(bundle.context);
		if (bundleListeners != null) {
			bundleListeners.removeAll(bundleListeners);
			syncBundleListeners.removeAll(bundleListeners);
			bundleListenerMap.remove(bundle.context);
		}

		// unregister registered services
		final ServiceReference<?>[] regs = bundle.getRegisteredServices();

		if (regs != null) {
			for (int i = 0; i < regs.length; i++) {
				unregisterService(regs[i]);
				((ServiceReferenceImpl<?>) regs[i]).invalidate();
			}
			bundle.registeredServices = null;
		}

		// unget all using services
		final ServiceReference<?>[] refs = bundle.getServicesInUse();
		if (refs != null) {
			for (int i = 0; i < refs.length; i++) {
				((ServiceReferenceImpl<?>) refs[i]).ungetService(bundle);
			}
		}
	}

	/**
	 * install a bundle.
	 * 
	 * @param location
	 *            the bundle location.
	 * @return a Bundle object.
	 * @throws BundleException
	 *             if the installation failed.
	 */
	BundleImpl installNewBundle(final BundleContext context,
			final String location) throws BundleException {
		try {
			final String location2 = location.indexOf(":") > -1 ? location
					: BUNDLE_LOCATION + File.separatorChar + location;
			return installNewBundle(context, location2, new URL(location2)
					.openConnection().getInputStream());
		} catch (final IOException e) {
			throw new BundleException(
					"Cannot retrieve bundle from " + location,
					BundleException.READ_ERROR, e);
		}
	}

	/**
	 * install a bundle from input stream.
	 * 
	 * @param location
	 *            the bundle location.
	 * @param in
	 *            the input stream.
	 * @return a Bundle object.
	 * @throws BundleException
	 *             if the installation failed.
	 */
	synchronized BundleImpl installNewBundle(final BundleContext context,
			final String location, final InputStream in) throws BundleException {
		final AbstractBundle cached;
		if ((cached = location_bundles.get(location)) != null) {
			if (!bundleFindHooks.isEmpty()) {
				final Bundle[] test = filterWithBundleHooks(context,
						Arrays.asList((Bundle) cached));
				if (test.length == 0) {
					throw new BundleException(
							"Existing bundle rejected by find hooks",
							BundleException.REJECTED_BY_HOOK);
				}
			}

			return (BundleImpl) cached;
		}

		final BundleImpl bundle = new BundleImpl(this, context, location,
				nextBundleID++, in);

		// notify the listeners
		notifyBundleListeners(BundleEvent.INSTALLED, bundle,
				context.getBundle());

		bundle.install();
		storeMetadata();
		return bundle;
	}

	protected Bundle[] filterWithBundleHooks(final BundleContext context,
			final Collection<Bundle> bundles) {
		final RemoveOnlyList<Bundle> list = new RemoveOnlyList<Bundle>(bundles);

		for (final ServiceReferenceImpl<org.osgi.framework.hooks.bundle.FindHook> sref : bundleFindHooks) {
			final org.osgi.framework.hooks.bundle.FindHook findHook = sref
					.getService(Concierge.this);
			if (findHook != null) {
				try {
					findHook.find(context, list);
				} catch (final Throwable t) {
					// TODO: log?
				}
			}
			sref.ungetService(Concierge.this);
		}

		return list.toArray(new Bundle[list.size()]);
	}

	/*
	 * inner classes
	 */

	/**
	 * The bundle context implementation.
	 * 
	 * @author Jan S. Rellermeyer
	 * 
	 */
	final class BundleContextImpl implements BundleContext {

		/**
		 * is the context valid ?
		 */
		boolean isValid = true;

		/**
		 * the bundle.
		 */
		final AbstractBundle bundle;

		protected BundleContextImpl(final AbstractBundle bundle) {
			this.bundle = bundle;
		}

		/**
		 * check, if the context is valid.
		 */
		private void checkValid() {
			if (!isValid) {
				throw new IllegalStateException("BundleContext of bundle "
						+ bundle
						+ " used after bundle has been stopped or uninstalled.");
			}
		}

		/**
		 * add a bundle listener.
		 * 
		 * @param listener
		 *            a bundle listener.
		 * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
		 */
		public void addBundleListener(final BundleListener listener) {
			checkValid();

			final List<BundleListener> registered = bundleListenerMap.get(this);

			if (registered == null || !registered.contains(listener)) {
				if (listener instanceof SynchronousBundleListener) {
					syncBundleListeners
							.add((SynchronousBundleListener) listener);
				} else {
					bundleListeners.add(listener);
				}
				bundleListenerMap.insert(this, listener);
			}
		}

		/**
		 * add a framework listener.
		 * 
		 * @param listener
		 *            a framework listener.
		 * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
		 * 
		 */
		public void addFrameworkListener(final FrameworkListener listener) {
			checkValid();

			if (bundle == Concierge.this) {
				return;
			}

			if (bundle.registeredFrameworkListeners == null) {
				bundle.registeredFrameworkListeners = new ArrayList<FrameworkListener>(
						1);
			}
			if (!bundle.registeredFrameworkListeners.contains(listener)) {
				frameworkListeners.add(listener);
				bundle.registeredFrameworkListeners.add(listener);
			}
		}

		/**
		 * add a service listener.
		 * 
		 * @param listener
		 *            the service listener.
		 * @param filterExpr
		 *            the filter String.
		 * @throws InvalidSyntaxException
		 *             if the filter string is invalid.
		 * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener,
		 *      java.lang.String)
		 * 
		 */
		public void addServiceListener(final ServiceListener listener,
				final String filterExpr) throws InvalidSyntaxException {
			checkValid();

			final ServiceListenerEntry entry = new ServiceListenerEntry(bundle,
					listener, filterExpr);

			if (bundle.registeredServiceListeners == null) {
				bundle.registeredServiceListeners = new ArrayList<ServiceListenerEntry>(
						1);
			}
			final ServiceListenerEntry existing = getRegisteredServiceListener(listener);
			if (existing != null) {
				removeServiceListener(listener);
			}
			bundle.registeredServiceListeners.add(entry);
			serviceListeners.add(entry);

			informListenerHooks(serviceListenerHooks,
					new ServiceListenerEntry[] { entry }, true);
		}

		private void informListenerHooks(
				final Collection<ServiceReferenceImpl<ListenerHook>> hooks,
				final ServiceListenerEntry[] entries, final boolean added) {
			if (hooks.isEmpty()) {
				return;
			}

			if (!added) {
				for (final ServiceListenerEntry entry : entries) {
					entry.removed = true;
				}
			}

			final Collection<ListenerInfo> c = new RemoveOnlyList<ListenerInfo>(
					Arrays.asList(entries));

			for (final Iterator<ServiceReferenceImpl<ListenerHook>> iter = hooks
					.iterator(); iter.hasNext();) {
				final ServiceReferenceImpl<ListenerHook> hookRef = iter.next();
				final ListenerHook hook = getService(hookRef);
				try {
					if (added) {
						hook.added(c);
					} else {
						hook.removed(c);
					}
				} catch (final Throwable t) {
					notifyFrameworkListeners(FrameworkEvent.ERROR,
							Concierge.this, t);
				}
				ungetService(hookRef);
			}
		}

		/**
		 * Determine if given service listener has been registered.
		 * 
		 * @param listener
		 * @return <code>true</code> if the listener is registered.
		 */
		private ServiceListenerEntry getRegisteredServiceListener(
				final ServiceListener listener) {
			final ServiceListenerEntry[] listeners = bundle.registeredServiceListeners
					.toArray(new ServiceListenerEntry[bundle.registeredServiceListeners
							.size()]);
			for (int i = 0; i < listeners.length; i++) {
				if (listeners[i].bundle == bundle
						&& listeners[i].listener == listener) {
					return listeners[i];
				}
			}
			return null;
		}

		/**
		 * add a service listener.
		 * 
		 * @param listener
		 *            the service listener.
		 * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
		 * 
		 */
		public void addServiceListener(final ServiceListener listener) {
			checkValid();
			try {
				addServiceListener(listener, null);
			} catch (final InvalidSyntaxException e) {
				// does not happen
			}
		}

		/**
		 * create a filter.
		 * 
		 * @param filter
		 *            the filter string.
		 * @return a Filter object.
		 * @throws InvalidSyntaxException
		 *             if the filter string is invalid.
		 * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
		 * 
		 */
		public Filter createFilter(final String filter)
				throws InvalidSyntaxException {
			if (filter == null) {
				throw new NullPointerException();
			}
			return org.osgi.framework.FrameworkUtil.createFilter(filter);
			// return RFC1960Filter.fromString(filter);
		}

		/**
		 * get the bundle.
		 * 
		 * @return the bundle.
		 * @see org.osgi.framework.BundleContext#getBundle()
		 * 
		 */
		public Bundle getBundle() {
			return bundle;
		}

		/**
		 * get a bundle by id.
		 * 
		 * @param id
		 *            the bundle id.
		 * @return the bundle object.
		 * @see org.osgi.framework.BundleContext#getBundle(long)
		 * 
		 */
		public Bundle getBundle(final long id) {
			checkValid();

			final Bundle bundle = bundleID_bundles.get(new Long(id));
			if (bundle == null || bundleFindHooks.isEmpty()) {
				return bundle;
			}

			final Bundle[] bundles = filterWithBundleHooks(this,
					Arrays.asList(bundle));

			return bundles.length == 0 ? null : bundles[0];
		}

		/**
		 * get all bundles.
		 * 
		 * @return the array of bundles.
		 * @see org.osgi.framework.BundleContext#getBundles()
		 * 
		 */
		public Bundle[] getBundles() {
			checkValid();

			final ArrayList<Bundle> bundleList = new ArrayList<Bundle>(bundles);
			bundleList.add(0, Concierge.this);

			if (bundleFindHooks.isEmpty()) {
				return bundleList.toArray(new Bundle[bundleList.size()]);
			}

			return filterWithBundleHooks(this, bundleList);
		}

		/**
		 * get a data file.
		 * 
		 * @param filename
		 *            the name of the file
		 * @return a File object.
		 * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
		 * 
		 */
		public File getDataFile(final String filename) {
			checkValid();

			final String path;
			if (bundle == Concierge.this) {
				path = Concierge.this.STORAGE_LOCATION;
			} else {
				path = bundle.storageLocation;
			}

			try {
				final File file = new File(path + "/data", filename);
				file.getParentFile().mkdirs();
				return file;
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		/**
		 * get a system property.
		 * 
		 * @param key
		 *            the key.
		 * @return the value.
		 * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
		 * 
		 */
		public String getProperty(final String key) {
			return Concierge.this.properties.getProperty(key);
		}

		/**
		 * get the service object.
		 * 
		 * @param reference
		 *            the service reference.
		 * @return the service object.
		 * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
		 * 
		 */
		public <S> S getService(final ServiceReference<S> reference) {
			checkValid();
			if (reference == null) {
				throw new NullPointerException("Null service reference.");
			}

			if (SECURITY_ENABLED) {
				final String[] clazzes = (String[]) reference
						.getProperty(Constants.OBJECTCLASS);
				for (int i = 0; i < clazzes.length; i++) {
					try {
						AccessController.checkPermission(new ServicePermission(
								clazzes[i], ServicePermission.GET));
						return ((ServiceReferenceImpl<S>) reference)
								.getService(bundle);
					} catch (final SecurityException se) {
						continue;
					}
				}
				throw new SecurityException(
						"Caller does not have permissions for getting service from "
								+ reference);
			}

			return ((ServiceReferenceImpl<S>) reference).getService(bundle);
		}

		/**
		 * get all service references matching a filter.
		 * 
		 * @param clazz
		 *            The class name with which the service was registered or
		 *            <code>null</code> for all services.
		 * @param filter
		 *            The filter criteria.
		 * @return An array of <code>ServiceReference</code> objects or
		 *         <code>null</code> if no services are registered which satisfy
		 *         the search.
		 * @throws InvalidSyntaxException
		 *             If <code>filter</code> contains an invalid filter string
		 *             that cannot be parsed.
		 * @throws IllegalStateException
		 *             If this BundleContext is no longer valid.
		 * @since 1.3
		 * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String,
		 *      java.lang.String)
		 * 
		 */
		public ServiceReference<?>[] getAllServiceReferences(
				final String clazz, final String filter)
				throws InvalidSyntaxException {
			return getServiceReferences(clazz, filter, true);
		}

		/**
		 * get all service references matching a filter.
		 * 
		 * @param clazz
		 *            the class name.
		 * @param filter
		 *            the filter.
		 * @return the array of matching service references.
		 * @throws InvalidSyntaxException
		 *             if the filter string is invalid.
		 * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String,
		 *      java.lang.String)
		 * 
		 */
		public ServiceReference<?>[] getServiceReferences(final String clazz,
				final String filter) throws InvalidSyntaxException {
			return getServiceReferences(clazz, filter, false);
		}

		private final ServiceReference<?>[] getServiceReferences(
				final String clazz, final String filter, final boolean all)
				throws InvalidSyntaxException {
			checkValid();

			// final Filter theFilter = FrameworkUtil.createFilter(filter);
			final Filter theFilter = RFC1960Filter.fromString(filter);
			final Collection<ServiceReference<?>> references;

			if (clazz == null) {
				references = serviceRegistry.getAllValues();
			} else {
				references = serviceRegistry.get(clazz);
			}

			final List<ServiceReference<?>> result = new ArrayList<ServiceReference<?>>();
			
			if(references!=null){
				final ServiceReferenceImpl<?>[] refs = references
						.toArray(new ServiceReferenceImpl[references.size()]);
	
				for (int i = 0; i < refs.length; i++) {
					if (theFilter.match(refs[i])
							&& (all || refs[i].isAssignableTo(bundle,
									(String[]) refs[i]
											.getProperty(Constants.OBJECTCLASS)))) {
						result.add(refs[i]);
					}
				}
			}

			if (!serviceFindHooks.isEmpty()) {
				final Collection<ServiceReference<?>> c = new RemoveOnlyList<ServiceReference<?>>(
						result);
				for (final Iterator<ServiceReferenceImpl<FindHook>> iter = serviceFindHooks
						.iterator(); iter.hasNext();) {
					final ServiceReferenceImpl<FindHook> hookRef = iter.next();
					final FindHook hook = getService(hookRef);
					try {
						hook.find(this, clazz, filter, all, c);
					} catch (final Throwable t) {
						notifyFrameworkListeners(FrameworkEvent.ERROR,
								Concierge.this, t);
					}
					ungetService(hookRef);
				}

				return c.size() == 0 ? null : (ServiceReference[]) c
						.toArray(new ServiceReference[c.size()]);
			}

			if (LOG_ENABLED && DEBUG_SERVICES) {
				logger.log(LogService.LOG_INFO,
						"Framework: REQUESTED SERVICES " + clazz + " " + filter);
				logger.log(LogService.LOG_INFO, "\tRETURNED " + result);
			}

			return result.size() == 0 ? null : (ServiceReference[]) result
					.toArray(new ServiceReference[result.size()]);
		}

		/**
		 * get a service reference.
		 * 
		 * @param clazz
		 *            the class name.
		 * @return the service reference or null if no such service is
		 *         registered.
		 * 
		 * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
		 * 
		 */
		public ServiceReference<?> getServiceReference(final String clazz) {
			checkValid();

			ServiceReference<?> winner = null;
			int maxRanking = -1;
			long lastServiceID = Long.MAX_VALUE;
			final List<ServiceReference<?>> list = serviceRegistry.get(clazz);
			if (list == null) {
				return null;
			}

			final ServiceReference<?>[] candidates = list
					.toArray(new ServiceReference[list.size()]);

			for (int i = 0; i < candidates.length; i++) {
				final Integer rankProp = (Integer) candidates[i]
						.getProperty(Constants.SERVICE_RANKING);

				final int ranking = rankProp != null ? rankProp.intValue() : 0;
				final long serviceID = ((Long) candidates[i]
						.getProperty(Constants.SERVICE_ID)).longValue();

				if (ranking > maxRanking || ranking == maxRanking
						&& serviceID < lastServiceID) {
					winner = candidates[i];
					maxRanking = ranking;
					lastServiceID = serviceID;
				}
			}
			if (LOG_ENABLED && DEBUG_SERVICES) {
				logger.log(LogService.LOG_INFO, "Framework: REQUESTED SERVICE "
						+ clazz);
				logger.log(LogService.LOG_INFO, "\tRETURNED " + winner);
			}
			return winner;
		}

		/**
		 * install a new bundle.
		 * 
		 * @param location
		 *            the bundle location.
		 * @return the bundle object.
		 * @throws BundleException
		 *             if something goes wrong.
		 * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
		 * 
		 */
		public Bundle installBundle(final String location)
				throws BundleException {
			if (location == null) {
				throw new IllegalArgumentException("Location must not be null");
			}
			checkValid();

			// TODO: check AdminPermission(new bundle, LIFECYCLE)

			return installNewBundle(this, location);
		}

		/**
		 * install a new bundle from input stream.
		 * 
		 * @param location
		 *            the location.
		 * @param in
		 *            the input stream.
		 * @return the bundle object.
		 * @throws BundleException
		 *             if something goes wrong.
		 * @see org.osgi.framework.BundleContext#installBundle(java.lang.String,
		 *      java.io.InputStream)
		 * 
		 */
		public Bundle installBundle(final String location, final InputStream in)
				throws BundleException {
			if (location == null) {
				throw new IllegalArgumentException("Location must not be null");
			}
			checkValid();

			// TODO: check AdminPermission(new bundle, LIFECYCLE)

			return installNewBundle(this, location, in);
		}

		/**
		 * register a new service.
		 * 
		 * @param clazzes
		 *            the classes under which the service is registered.
		 * @param service
		 *            the service object
		 * @param serviceProperties
		 *            the properties.
		 * @return the service registration.
		 * @see org.osgi.framework.BundleContext#registerService(java.lang.String[],
		 *      java.lang.Object, java.util.Dictionary)
		 * @context BundleContext
		 */
		public ServiceRegistration<?> registerService(final String[] clazzes,
				final Object service,
				final Dictionary<String, ?> serviceProperties) {
			checkValid();

			if (service == null) {
				throw new IllegalArgumentException(
						"Cannot register a null service");
			}

			if (SECURITY_ENABLED) {
				for (int i = 0; i < clazzes.length; i++) {
					AccessController.checkPermission(new ServicePermission(
							clazzes[i], ServicePermission.REGISTER));
				}
			}

			final ServiceReferenceImpl<?> sref = new ServiceReferenceImpl<Object>(
					Concierge.this, bundle, service, serviceProperties, clazzes);

			// lazy initialization
			if (bundle.registeredServices == null) {
				bundle.registeredServices = new ArrayList<ServiceReference<?>>(
						1);
			}
			bundle.registeredServices.add(sref);

			boolean isHook = false;

			// and now register the service for all classes ...
			for (int counter = 0; counter < clazzes.length; counter++) {
				final String clazz = clazzes[counter];

				isHook = checkHook(clazz, sref, true);

				serviceRegistry.insert(clazz, sref);
			}

			if (LOG_ENABLED && DEBUG_SERVICES) {
				logger.log(LogService.LOG_INFO,
						"Framework: REGISTERED SERVICE " + clazzes[0]);
			}

			if (!isHook) {
				notifyServiceListeners(ServiceEvent.REGISTERED, sref, null);
			}

			return sref.registration;
		}

		private boolean checkHook(final String clazz,
				final ServiceReference<?> sref, final boolean add) {
			@SuppressWarnings("unchecked")
			final List<ServiceReference<?>> hookList = (List<ServiceReference<?>>) hooks
					.get(clazz);
			if (hookList == null) {
				return false;
			}

			if (add) {
				hookList.add(sref);
				// not required for collision hook, weaving hook,
				// resolverHookFactory...
				Collections.sort(hookList, Collections.reverseOrder());
			} else {
				// FIXME: remove!
			}

			// special case: ListenerHook
			if (add && (Object) hookList == (Object) serviceListenerHooks) {
				@SuppressWarnings("unchecked")
				final ServiceReferenceImpl<ListenerHook> hookRef = (ServiceReferenceImpl<ListenerHook>) sref;
				if (serviceListeners != null) {
					try {
						informListenerHooks(
								Collections.singletonList(hookRef),
								serviceListeners
										.toArray(new ServiceListenerEntry[serviceListeners
												.size()]), true);
					} catch (final Throwable t) {
						notifyFrameworkListeners(FrameworkEvent.ERROR,
								sref.getBundle(), t);
					}
				}
			}

			return true;
		}

		/**
		 * register a new service.
		 * 
		 * @param clazz
		 *            the class under which the service is registered.
		 * @param service
		 *            the service object.
		 * @param properties
		 *            the properties.
		 * @return the service registration.
		 * @see org.osgi.framework.BundleContext#registerService(java.lang.String,
		 *      java.lang.Object, java.util.Dictionary)
		 * 
		 */
		public ServiceRegistration<?> registerService(final String clazz,
				final Object service, final Dictionary<String, ?> properties) {
			return registerService(new String[] { clazz }, service, properties);
		}

		/**
		 * remove a bundle listener.
		 * 
		 * @param listener
		 *            a bundle listener.
		 * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
		 * 
		 */
		public void removeBundleListener(final BundleListener listener) {
			checkValid();

			if (bundle == Concierge.this) {
				return;
			}

			(listener instanceof SynchronousBundleListener ? syncBundleListeners
					: bundleListeners).remove(listener);
			bundleListenerMap.remove(this, listener);
		}

		/**
		 * remove a framework listener.
		 * 
		 * @param listener
		 *            a framework listener.
		 * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
		 * 
		 */
		public void removeFrameworkListener(final FrameworkListener listener) {
			checkValid();

			if (bundle == Concierge.this) {
				return;
			}
			final AbstractBundle b = bundle;

			frameworkListeners.remove(listener);
			b.registeredFrameworkListeners.remove(listener);
			if (b.registeredFrameworkListeners.isEmpty()) {
				b.registeredFrameworkListeners = null;
			}
		}

		/**
		 * remove a service listener.
		 * 
		 * @param listener
		 *            the service listener.
		 * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
		 * 
		 */
		public void removeServiceListener(final ServiceListener listener) {
			checkValid();

			final ServiceListenerEntry entry = getRegisteredServiceListener(listener);
			if (entry != null) {
				entry.removed = true;
				serviceListeners.remove(entry);
				bundle.registeredServiceListeners.remove(entry);
				if (bundle.registeredServiceListeners.isEmpty()) {
					bundle.registeredServiceListeners = null;
				}

				informListenerHooks(serviceListenerHooks,
						new ServiceListenerEntry[] { entry }, false);
			}
		}

		/**
		 * unget a service.
		 * 
		 * @param reference
		 *            the service reference of the service
		 * @return true is the service is still in use by other bundles, false
		 *         otherwise.
		 * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
		 * 
		 */
		public synchronized boolean ungetService(
				final ServiceReference<?> reference) {
			checkValid();
			return ((ServiceReferenceImpl<?>) reference).ungetService(bundle);
		}

		// FIXME: should be the other way around...
		@SuppressWarnings("unchecked")
		public <S> ServiceRegistration<S> registerService(final Class<S> clazz,
				final S service, final Dictionary<String, ?> properties) {
			return (ServiceRegistration<S>) registerService(clazz.getName(),
					service, properties);
		}

		// FIXME: should be the other way around...
		@SuppressWarnings("unchecked")
		public <S> ServiceReference<S> getServiceReference(final Class<S> clazz) {
			return (ServiceReference<S>) getServiceReference(clazz == null ? null
					: clazz.getName());
		}

		// FIXME: should be the other way around...
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <S> Collection<ServiceReference<S>> getServiceReferences(
				final Class<S> clazz, final String filter)
				throws InvalidSyntaxException {
			final ServiceReference[] refs = getServiceReferences(clazz.getName(), filter);
			if (refs == null){
				return Collections.EMPTY_LIST;
			} else {
				return (Collection) Arrays.asList(refs);
			}
		}

		/**
		 * @see org.osgi.framework.BundleContext#getBundle(java.lang.String)
		 * @since 1.6
		 */
		public Bundle getBundle(final String location) {
			return location_bundles.get(location);
		}
	}

	/**
	 * An entry consisting of service listener and filter.
	 * 
	 * @author Jan S. Rellermeyer
	 */
	static final class ServiceListenerEntry implements EventListener,
			ListenerHook.ListenerInfo {

		final AbstractBundle bundle;

		/**
		 * the listener.
		 */
		final ServiceListener listener;

		/**
		 * the filter.
		 */
		final Filter filter;

		boolean removed;

		/**
		 * create a new entry.
		 * 
		 * @param listener
		 *            the listener.
		 * @param filter
		 *            the filter.
		 * @throws InvalidSyntaxException
		 *             if the filter cannot be parsed.
		 */
		private ServiceListenerEntry(final AbstractBundle bundle,
				final ServiceListener listener, final String filter)
				throws InvalidSyntaxException {
			this.bundle = bundle;
			this.listener = listener;
			// this.filter = filter == null ? null : FrameworkUtil
			// .createFilter(filter);
			this.removed = false;
			this.filter = filter == null ? null : RFC1960Filter
					.fromString(filter);
		}

		/**
		 * check for equality.
		 * 
		 * @param other
		 *            the other object.
		 * @return true, if the two objects are equal.
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(final Object other) {
			if (other instanceof ServiceListenerEntry) {
				final ServiceListenerEntry entry = (ServiceListenerEntry) other;
				return bundle == entry.bundle
						&& listener.equals(entry.listener);
			}
			return false;
		}

		/**
		 * get the hash code.
		 * 
		 * @return the hash code.
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return listener.hashCode()
					+ (filter != null ? filter.hashCode() >> 8 : 0);
		}

		/**
		 * get a string representation.
		 * 
		 * @return a string representation.
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return listener + " " + filter;
		}

		public BundleContext getBundleContext() {
			return bundle.context;
		}

		public String getFilter() {
			return filter == null ? null : filter.toString();
		}

		public boolean isRemoved() {
			return removed;
		}
	}

	protected final boolean isSecurityEnabled() {
		return SECURITY_ENABLED;
	}

	static class CapabilityRegistry {

		// namespace -> list of capability
		private final MultiMap<String, Capability> capabilities = new MultiMap<String, Capability>();

		// namespace -> value (of canonical attribute) -> list of capability
		private final HashMap<String, MultiMap<String, Capability>> defaultAttributeIndex = new HashMap<String, MultiMap<String, Capability>>();

		void add(final Capability cap) {
			final String namespace = cap.getNamespace();
			capabilities.insert(namespace, cap);

			final Object defaultAttribute = cap.getAttributes().get(namespace);
			if (defaultAttribute instanceof String) {
				MultiMap<String, Capability> attributeIndex = defaultAttributeIndex
						.get(namespace);
				if (attributeIndex == null) {
					attributeIndex = new MultiMap<String, Capability>();
					defaultAttributeIndex.put(namespace, attributeIndex);
				}
				if (defaultAttribute != null) {
					attributeIndex.insert((String) defaultAttribute, cap);
				}
			}
		}

		void addAll(final Resource res) {
			for (final Capability cap : res.getCapabilities(null)) {
				add(cap);
			}
		}

		boolean remove(final Capability cap) {
			final String namespace = cap.getNamespace();
			capabilities.remove(namespace, cap);

			final Object defaultAttribute = cap.getAttributes().get(namespace);

			final MultiMap<String, Capability> attributeIndex = defaultAttributeIndex
					.get(namespace);
			if (attributeIndex == null) {
				return false;
			}

			if (defaultAttribute != null && defaultAttribute instanceof String) {
				final boolean success = attributeIndex.remove(
						(String) defaultAttribute, cap);
				if (success) {
					if (attributeIndex.isEmpty()) {
						defaultAttributeIndex.remove(namespace);
					}
				}
				return success;
			} else {
				return false;
			}
		}

		void removeAll(final Resource res) {
			for (final Capability cap : res.getCapabilities(null)) {
				remove(cap);
			}
		}

		public List<Capability> getByValue(final String namespace,
				final String value) {
			final MultiMap<String, Capability> attributeIndex = defaultAttributeIndex
					.get(namespace);
			final List<Capability> result = attributeIndex.get(value);
			return result == null ? Collections.<Capability> emptyList()
					: new ArrayList<Capability>(result);
		}

		public List<Capability> getByKey(final String namespace,
				final String value) {
			final MultiMap<String, Capability> caps = defaultAttributeIndex
					.get(namespace);
			if (caps == null) {
				return Collections.emptyList();
			}
			return caps == null ? Collections.<Capability> emptyList() : caps
					.get(value);
		}

		public List<Capability> getAll(final String namespace) {
			final List<Capability> result = capabilities.get(namespace);
			return result == null ? Collections.<Capability> emptyList()
					: new ArrayList<Capability>(result);
		}

		@Override
		public String toString() {
			return capabilities.toString();
		}

	}

	boolean hasWeavingHooks() {
		return !weavingHooks.isEmpty();
	}

	void callWeavingHooks(final WovenClassImpl wovenClass) {
		Collections.sort(weavingHooks, Collections.reverseOrder());

		for (final ServiceReferenceImpl<WeavingHook> sref : weavingHooks) {
			final WeavingHook hook = sref.getService(this);

			// FIXME: why do I see null here?
			if (hook == null) {
				continue;
			}

			try {
				hook.weave(wovenClass);
			} catch (final Throwable t) {
				if (!(t instanceof WeavingException)) {
					// blacklist the hook
					weavingHooks.remove(sref);
				}

				// framework event
				notifyFrameworkListeners(FrameworkEvent.ERROR, sref.bundle, t);

				// mark as complete
				wovenClass.setComplete();

				final ClassFormatError err = new ClassFormatError(
						"Error while invoking weaving hook");
				err.initCause(t);
				throw err;
			} finally {
				sref.ungetService(this);
			}
		}
		wovenClass.setComplete();
	}

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 * @category BundleActivator
	 */
	public void start(final BundleContext context) throws Exception {
		context.registerService(Resolver.class, resolver, null);
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 * @category BundleActivator
	 */
	public void stop(final BundleContext context) throws Exception {

	}

}
