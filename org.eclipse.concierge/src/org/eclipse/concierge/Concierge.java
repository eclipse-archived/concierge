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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
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
import java.util.LinkedHashMap;
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
import org.eclipse.concierge.ConciergeCollections.MultiMap;
import org.eclipse.concierge.ConciergeCollections.ParseResult;
import org.eclipse.concierge.Resources.BundleCapabilityImpl;
import org.eclipse.concierge.Resources.ConciergeBundleWiring;
import org.eclipse.concierge.Resources.HostedBundleCapability;
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
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
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
 * @author Jochen Hiller - added argument parsing
 */
public final class Concierge extends AbstractBundle implements Framework,
		BundleRevision, FrameworkWiring, FrameworkStartLevel, BundleActivator {

	// deprecated core framework constants.

	@SuppressWarnings("deprecation")
	private static final String FRAMEWORK_EXECUTIONENVIRONMENT = Constants.FRAMEWORK_EXECUTIONENVIRONMENT;

	@SuppressWarnings("deprecation")
	private static final String BUNDLE_REQUIREDEXECUTIONENVIRONMENT = Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT;

	private static final String BUNDLE_SYMBOLIC_NAME = "org.eclipse.concierge";

	@SuppressWarnings("deprecation")
	private static Class<?> SERVICE_EVENT_HOOK_CLASS = org.osgi.framework.hooks.service.EventHook.class;

	// URLStreamHandlerFactory

	/**
	 * This static variable contains a URL stream handler factory, which will be
	 * dispatched for the current running Concierge instance.
	 */
	private static ConciergeURLStreamHandlerFactory conciergeURLStreamHandlerFactory = new ConciergeURLStreamHandlerFactory();

	// the runtime args

	public static boolean SUPPORTS_EXTENSIONS;

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
	 * always decompress the bundles, great for testing
	 * 
	 * FIXME: combine with decompress embedded into a single property, e.g.,
	 * with values: NEVER, EMBEDDED_JARS, ALWAYS
	 */
	boolean ALWAYS_DECOMPRESS;

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
	 * the profile.
	 */
	private String PROFILE;

	private final String[] bootdelegationAbs;
	private final String[] bootdelegationPrefix;

	private String[] libraryExtensions;

	private String execPermission;
	private Pattern execPermissionPattern;

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
	final MultiMap<String, ServiceReference<?>> serviceRegistry = new MultiMap<String, ServiceReference<?>>(
			3);

	/**
	 * bundle listeners.
	 */
	protected final List<BundleListener> bundleListeners = new ArrayList<BundleListener>(
			1);

	/**
	 * synchronous bundle listeners.
	 */
	protected final List<SynchronousBundleListener> syncBundleListeners = new ArrayList<SynchronousBundleListener>(
			1);

	protected final MultiMap<BundleContext, BundleListener> bundleListenerMap = new MultiMap<BundleContext, BundleListener>();

	/**
	 * service listeners.
	 */
	protected final List<ServiceListenerEntry> serviceListeners = new ArrayList<ServiceListenerEntry>(
			1);

	/**
	 * Map of unattached fragments in the system. HostName => List of fragments
	 */
	private final MultiMap<String, Revision> fragmentIndex = new MultiMap<String, Revision>(
			1);

	private final ArrayList<BundleImpl> extensionBundles = new ArrayList<BundleImpl>(
			0);

	/**
	 * framework listeners.
	 */
	protected final List<FrameworkListener> frameworkListeners = new ArrayList<FrameworkListener>(
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
	public boolean restart = false;

	// system bundle

	/**
	 * the symbolicName of the system bundle
	 */
	public static final String FRAMEWORK_SYMBOLIC_NAME = "org.eclipse.concierge";

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
	protected final List<ServiceReferenceImpl<org.osgi.framework.hooks.bundle.FindHook>> bundleFindHooks = new ArrayList<ServiceReferenceImpl<org.osgi.framework.hooks.bundle.FindHook>>(0);

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
	protected final HashMap<String, List<?>> hooks = new HashMap<String, List<?>>();
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

	private final Method addURL;

	final ClassLoader parentClassLoader;
	final ClassLoader systemBundleClassLoader;

	protected static final Comparator<? super Capability> BUNDLE_VERSION = new Comparator<Capability>() {

		public int compare(final Capability cap1, final Capability cap2) {
			final Version cap1Version = (Version) cap1
					.getAttributes()
					.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
			final Version cap2Version = (Version) cap2
					.getAttributes()
					.get(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);

			return cap2Version.compareTo(cap1Version);
		}
	};

	protected static final Comparator<? super Capability> EXPORT_ORDER = new Comparator<Capability>() {

		// reverts the order so that we can
		// retrieve the 0st item to get the best
		// match
		public int compare(final Capability c1, final Capability c2) {
			if (!(c1 instanceof BundleCapability && c2 instanceof BundleCapability)) {
				return 0;
			}

			final BundleCapability cap1 = (BundleCapability) c1;
			final BundleCapability cap2 = (BundleCapability) c2;

			final int cap1Resolved = cap1.getResource().getWiring() == null ? 0
					: 1;
			final int cap2Resolved = cap2.getResource().getWiring() == null ? 0
					: 1;
			int score = cap2Resolved - cap1Resolved;
			if (score != 0) {
				return score;
			}

			final Version cap1Version = (Version) cap1.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			final Version cap2Version = (Version) cap2.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);

			score = cap2Version.compareTo(cap1Version);

			if (score != 0) {
				return score;
			}

			final long cap1BundleId = cap1.getRevision().getBundle()
					.getBundleId();
			final long cap2BundleId = cap2.getRevision().getBundle()
					.getBundleId();

			return (int) (cap1BundleId - cap2BundleId);
		}

	};

	public static final String DIR_INTERNAL = "x-int";

	/** Return code from main when OK. */
	private static final int MAIN_RC_OK = 0;

	/** Return code from main when printing usage message. */
	private static final int MAIN_RC_USAGE = 1;

	/**
	 * Main method to start Concierge. This class will delegate that to an
	 * internal method, which will create framework instance, but will not wait
	 * until stopped. This allows better testability.
	 */
	public static void main(final String[] args) throws Exception {
		final Concierge framework = doMain(args);
		if (framework != null) {
			// wait until framework stopped
			framework.waitForStop(0);
			// exit with OK
			System.exit(MAIN_RC_OK);
		} else {
			// exit with usage message
			System.exit(MAIN_RC_USAGE);
		}
	}

	/**
	 * Processing of command line arguments. It will create a framework instance
	 * based on given arguments.
	 * 
	 * @param args
	 *            command line arguments.
	 * @throws Exception
	 */
	public static Concierge doMain(final String[] args) throws Exception {
		// TODO: populate micro-services

		// TODO: temporary solution to use xargs file launcher for argument
		// processing
		final XargsFileLauncher xargsLauncher = new XargsFileLauncher();
		String xargsFile = null;
		final StringBuffer argsBuf = new StringBuffer();
		for (int i = 0; args != null && i < args.length; i++) {
			if ("-help".equalsIgnoreCase(args[i])) {
				// if -help show usage message
				System.err
						.println(""
								+ "Concierge usage: org.eclipse.concierge.Concierge {arguments}\n"
								+ "  {file.xargs}                                 "
								+ "Loads xargs file, must end with .xargs\n"
								+ "  {-install|-start|-istart} {bundle-jar-file}  "
								+ "Install and start one bundle (can be used multiple times, in specified order)\n"
								+ "  {-all} {directory}                           "
								+ "Install and start all bundles from specified directory\n"
								+ "  {-Dprop=value}                               "
								+ "Specify one or more props just for Concierge (can be used multiple times)\n"
								+ "Sample: org.eclipse.concierge.Concierge -Dorg.osgi.framework.bootdelegation=sun.*,javax.* -istart mybundle.jar\n");
				// TODO allow -all with directory location
				return null;
			} else if (args[i].endsWith(".xargs")) {
				// if arg is name of an xargs file: use this, stop further
				// processing
				xargsFile = args[i];
				break;
			} else {
				argsBuf.append(args[i]);
				if (args[i].startsWith("-D")) {
					argsBuf.append('\n');
				} else if (args[i].equalsIgnoreCase("-profile")
						|| args[i].equalsIgnoreCase("-install")
						|| args[i].equalsIgnoreCase("-istart")
						|| args[i].equalsIgnoreCase("-start")
						|| args[i].equalsIgnoreCase("-all")) {
					// append next argument to same line
					if (i - 1 < args.length) {
						i++;
						argsBuf.append(' ');
						argsBuf.append(args[i]);
						argsBuf.append('\n');
					}
				}
			}
		}

		// no arguments? Try to use init.xargs or other file
		if (xargsFile == null && argsBuf.length() == 0) {
			xargsFile = System.getProperty("org.eclipse.concierge.init.xargs");
			if (xargsFile == null) {
				xargsFile = "init.xargs";
			}
		}

		// now start framework with given arguments
		final Concierge fw;
		if (xargsFile != null) {
			// take args from file
			final File xargs = new File(xargsFile);
			if (xargs.exists()) {
				fw = xargsLauncher.processXargsFile(xargs);
			} else {
				System.err.println("Concierge: xargs file '" + xargs.toString()
						+ "' not found, starting without arguments");
				fw = (Concierge) new Factory().newFramework(null);
				fw.init();
				fw.start();
			}
		} else {
			// some arguments have been defined
			InputStream inputStream = new ByteArrayInputStream(argsBuf
					.toString().getBytes(StandardCharsets.UTF_8));
			// TODO support really props as command line args?
			// we have to preserve the properties for later variable and
			// wildcard replacement
			final Map<String, String> passedProperties = xargsLauncher
					.getPropertiesFromXargsInputStream(inputStream);

			// now process again for install/start options with given properties
			inputStream = new ByteArrayInputStream(argsBuf.toString().getBytes(
					StandardCharsets.UTF_8));
			fw = xargsLauncher.processXargsInputStream(passedProperties,
					inputStream);
		}
		return fw;
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
		defaultProperties.setProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
				"1");

		final String feeStr = defaultProperties
				.getProperty(FRAMEWORK_EXECUTIONENVIRONMENT);

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
			case 8:
				myEEs.append("J2SE-1.8,");
				myEEs.append("JavaSE-1.8,");
				seVersionList.append("1.8,");
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
		}

		// TODO: use "reasonable defaults"...
		defaultProperties
				.setProperty(
						Constants.FRAMEWORK_SYSTEMPACKAGES,
						"org.osgi.framework;version=1.7,org.osgi.framework.hooks.bundle;version=1.1,org.osgi.framework.hooks.resolver;version=1.0,org.osgi.framework.hooks.service;version=1.1,org.osgi.framework.hooks.weaving;version=1.0,org.osgi.framework.launch;version=1.1,org.osgi.framework.namespace;version=1.0,org.osgi.framework.startlevel;version=1.0,org.osgi.framework.wiring;version=1.1,org.osgi.resource;version=1.0,org.osgi.service.log;version=1.3,org.osgi.service.packageadmin;version=1.2,org.osgi.service.startlevel;version=1.1,org.osgi.service.url;version=1.0,org.osgi.service.resolver;version=1.0,org.osgi.util.tracker;version=1.5.1,META-INF.services");

		Object obj;
		defaultProperties.put(Constants.FRAMEWORK_OS_NAME,
				(obj = System.getProperty("os.name")) != null ? obj
						: "undefined");

		// Normalize to framework.processor according to OSGi R5 spec table 4.4
		if ("Mac OS X".equals(System.getProperty("os.name"))) {
			defaultProperties.put(Constants.FRAMEWORK_OS_NAME, "MacOSX");
		} else if ("Mac OS".equals(System.getProperty("os.name"))) {
			defaultProperties.put(Constants.FRAMEWORK_OS_NAME, "MacOS");
		}
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

		defaultProperties.setProperty(Constants.FRAMEWORK_UUID, UUID
				.randomUUID().toString());

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

		// set parent classloader
		systemBundleClassLoader = getClass().getClassLoader();
		final String p = properties
				.getProperty(Constants.FRAMEWORK_BUNDLE_PARENT);
		if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equals(p)) {
			parentClassLoader = ClassLoader.getSystemClassLoader();
		} else if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equals(p)) {
			parentClassLoader = systemBundleClassLoader;
		} else if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equals(p)) {
			ClassLoader c = ClassLoader.getSystemClassLoader();
			while (c.getParent() != null) {
				c = c.getParent();
			}
			parentClassLoader = c;
		} else {
			parentClassLoader = new ClassLoader(Object.class.getClassLoader()) {
			};
		}

		properties.setProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION,
				Boolean.toString(false));

		Method m = null;
		if (getClass().getClassLoader() instanceof URLClassLoader) {
			try {
				m = URLClassLoader.class.getDeclaredMethod("addURL",
						new Class[] { URL.class });
				m.setAccessible(true);
				properties.setProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION,
						Boolean.toString(true));
				SUPPORTS_EXTENSIONS = true;
			} catch (final Exception e) {
				logger.log(
						LogService.LOG_WARNING,
						"Could not hijack classloader for framework extensions",
						e);
			}
		}
		addURL = m;

		// apply constants
		properties.setProperty(Constants.FRAMEWORK_VERSION, version.toString());
		properties
				.setProperty(Constants.FRAMEWORK_VENDOR, "Eclipse Foundation");

		properties.setProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
				Boolean.toString(false));
		properties.setProperty(Constants.SUPPORTS_FRAMEWORK_FRAGMENT,
				Boolean.toString(true));
		properties.setProperty(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE,
				Boolean.toString(true));

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

		ALWAYS_DECOMPRESS = getProperty(
				"org.eclipse.concierge.alwaysDecompress", false);
		DECOMPRESS_EMBEDDED = getProperty(
				"org.eclipse.concierge.decompressEmbedded", true);
		SECURITY_ENABLED = getProperty(
				"org.eclipse.concierge.security.enabled", false);

		final String bsl = properties
				.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
		try {
			BEGINNING_STARTLEVEL = Integer.parseInt(bsl);
		} catch (final NumberFormatException nfe) {
			warning("Invalid initial startlevel " + bsl);
			System.err
					.println("FALLING BACK TO DEFAULT BEGINNING STARTLEVEL (=1)");
			BEGINNING_STARTLEVEL = 1;
		}

		// sort out the boot delegations
		final String[] bds = Utils
				.splitString(properties
						.getProperty(Constants.FRAMEWORK_BOOTDELEGATION), ',');
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

		// set UUID
		properties.setProperty(Constants.FRAMEWORK_UUID, UUID.randomUUID()
				.toString());

		// TODO: check if there is a security manager set and
		// Constants.FRAMEWORK_SECURITY; is set

		STORAGE_LOCATION = properties.getProperty(
				"org.eclipse.concierge.storage",
				properties.getProperty(Constants.FRAMEWORK_STORAGE, BASEDIR
						+ File.separatorChar + "storage"))
				+ File.separatorChar + PROFILE + File.separatorChar;

		// clean the storage if requested
		final File storage = new File(STORAGE_LOCATION);
		if (storage.exists()) {
			if (Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(properties
					.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN))) {
				deleteDirectory(storage);
			} else {
				restart = true;
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

		// get the library extensions if set
		final String libExtStr = properties
				.getProperty(Constants.FRAMEWORK_LIBRARY_EXTENSIONS);
		if (libExtStr != null) {
			libraryExtensions = Utils.splitString(libExtStr, ',');
		}

		// set execpermission if set
		execPermission = properties
				.getProperty(Constants.FRAMEWORK_EXECPERMISSION);
		if (execPermission != null) {
			execPermissionPattern = Pattern.compile("\\$\\{"
					+ properties.getProperty(
							Constants.FRAMEWORK_COMMAND_ABSPATH, "abspath")
					+ "\\}");
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
		final String[] framework_pkgs = Utils.splitString(sysPkgs, ',');
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
			final String[] capStrs = Utils.splitString(extraCaps, ',');
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

		// system bundle symbolic name
		final BundleCapabilityImpl sysbundleCap = new BundleCapabilityImpl(
				this, "osgi.wiring.bundle; osgi.wiring.bundle="
						+ Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemBundleCapabilities.add(sysbundleCap);

		// default org.wiring.host property
		final BundleCapabilityImpl sysbundleDefaultHostCap = new BundleCapabilityImpl(
				this, "osgi.wiring.host; osgi.wiring.host="
						+ Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		systemBundleCapabilities.add(sysbundleDefaultHostCap);

		// concierge specific org.wiring.host property
		final BundleCapabilityImpl sysbundleHostCap = new BundleCapabilityImpl(
				this,
				"osgi.wiring.host; osgi.wiring.host=org.eclipse.concierge");
		systemBundleCapabilities.add(sysbundleHostCap);

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
			conciergeURLStreamHandlerFactory.setConcierge(this);
			URL.setURLStreamHandlerFactory(conciergeURLStreamHandlerFactory);
		} catch (final Error e) {
			// already set...
		}

		state = Bundle.STARTING;

		if (restart) {
			restoreProfile();
		}
	}

	private void exportSystemBundlePackages(final String[] pkgs)
			throws BundleException {
		for (final String pkg : pkgs) {
			final String[] literals = Utils.splitString(pkg, ';');

			if (literals.length > 0) {
				final ParseResult parseResult = Utils
						.parseLiterals(literals, 1);
				final HashMap<String, Object> attrs = parseResult
						.getAttributes();
				attrs.put(PackageNamespace.PACKAGE_NAMESPACE,
						literals[0].trim());
				systemBundleCapabilities.add(new BundleCapabilityImpl(this,
						PackageNamespace.PACKAGE_NAMESPACE, parseResult
								.getDirectives(), attrs,
						Constants.EXPORT_PACKAGE + ' ' + pkg));
			}
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

			// resolve all extension bundles
			for (final BundleImpl ext : extensionBundles) {
				ext.state = Bundle.RESOLVED;
			}

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
		final BundleImpl[] bundleArray = bundles.toArray(new BundleImpl[bundles
				.size()]);
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

	/**
	 * restore a profile.
	 * 
	 * @return the startlevel or -1 if the profile could not be restored.
	 */
	private void restoreProfile() {
		try {
			if (DEBUG_BUNDLES) {
				logger.log(LogService.LOG_DEBUG, "restoring profile " + PROFILE);
			}
			final File file = new File(STORAGE_LOCATION, "meta");
			if (!file.exists()) {
				warning("Profile " + PROFILE
						+ " not found, performing clean start ...");
				restart = false;
				return;
			}

			final DataInputStream in = new DataInputStream(new FileInputStream(
					file));
			BEGINNING_STARTLEVEL = in.readInt();
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
							if (DEBUG_BUNDLES) {
								logger.log(LogService.LOG_DEBUG,
										"RESTORED BUNDLE " + bundle.location);
							}
							bundles.add(bundle);
							bundleID_bundles.put(new Long(bundle.bundleId),
									bundle);
						} catch (final Exception e) {
							logger.log(LogService.LOG_ERROR,
									"Framework restart", e);
						}
					}
				}
			}
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

	protected void stop0(final boolean update) {
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
			for (final AbstractBundle bundle : bundles) {
				for (final BundleRevision rev : bundle.getRevisions()) {
					((Revision) rev).close();
				}
			}

			bundles.clear();
			bundleID_bundles.clear();
			serviceRegistry.clear();

			// Reset the used Concierge instance in URL stream handler factory
			conciergeURLStreamHandlerFactory.setConcierge(null);

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
		final int state = Concierge.this.state;

		new Thread() {
			public void run() {

				stop0(true);
				try {
					if (state == Bundle.STARTING) {
						Concierge.this.init();
					} else if (state == Bundle.ACTIVE) {
						Concierge.this.start();
					}
				} catch (final BundleException be) {
					// FIXME: to log
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

		if (type == BundleWiring.class) {
			return (A) wirings.get(this);
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
		return systemBundleClassLoader.getResource(name);
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
		return systemBundleClassLoader.loadClass(name);
	}

	/**
	 * @see org.osgi.framework.Bundle#getResources(java.lang.String)
	 * @category SystemBundle
	 */
	public Enumeration<URL> getResources(final String name) throws IOException {
		return systemBundleClassLoader.getResources(name);
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
	protected void setLevel(final Bundle[] bundleArray, final int targetLevel,
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
					if (be.getNestedException() != null) {
						be.getNestedException().printStackTrace();
					}
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
		final ArrayList<BundleCapability> filteredCapabilities = new ArrayList<BundleCapability>();
		if (namespace != null) {
			for (final BundleCapability c : systemBundleCapabilities) {
				if (c.getNamespace().equals(namespace)) {
					filteredCapabilities.add(c);
				}
			}
		} else {
			filteredCapabilities.addAll(systemBundleCapabilities);
		}
		return Collections.unmodifiableList(filteredCapabilities);
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
		return (BundleWiring) wirings.get(this);
	}

	/**
	 * @see org.osgi.framework.wiring.BundleRevision#getCapabilities(java.lang.String)
	 * @category BundleRevision
	 */
	public List<Capability> getCapabilities(final String namespace) {
		return Collections.unmodifiableList(new ArrayList<Capability>(
				getDeclaredCapabilities(namespace)));
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
				try {
					synchronized (Concierge.this) {
						Bundle[] initial;

						// build the initial set of bundles
						if (bundleCollection == null) {
							initial = bundles
									.toArray(new Bundle[bundles.size()]);
						} else {
							initial = bundleCollection
									.toArray(new Bundle[bundleCollection.size()]);
						}

						final ArrayList<Bundle> toProcess = new ArrayList<Bundle>();

						// filter out those which need to be updated
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
							} else {
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

						if (LOG_ENABLED && DEBUG_PACKAGES) {
							logger.log(LogService.LOG_DEBUG,
									"REFRESHING PACKAGES FROM BUNDLES "
											+ toProcess);
						}

						final Collection<Bundle> updateGraph = getDependencyClosure(toProcess);

						if (LOG_ENABLED && DEBUG_PACKAGES) {
							logger.log(LogService.LOG_DEBUG, "UPDATE GRAPH IS "
									+ updateGraph);
						}

						final ArrayList<Bundle> tmp = new ArrayList<Bundle>(
								updateGraph);
						Collections.sort(tmp);
						final Bundle[] refreshArray = tmp
								.toArray(new Bundle[tmp.size()]);

						// stop all bundles in the restart array regarding their
						// startlevels

						// perform a cleanup for all bundles
						// CLEANUP
						final List<Bundle> restartList = new ArrayList<Bundle>();

						for (int i = 0; i < refreshArray.length; i++) {
							final BundleImpl bu = (BundleImpl) refreshArray[i];
							try {
								if (bu.state == ACTIVE) {
									bu.stop();

									restartList.add(0, bu);
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
									notifyBundleListeners(
											BundleEvent.UNRESOLVED, bu);
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
							final BundleImpl bu = (BundleImpl) resolveIter
									.next();
							try {
								if (bu.state == Bundle.INSTALLED) {
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
						for (final Bundle bu : restartList) {
							try {
								bu.start();
							} catch (final Exception e) {
								notifyListeners(FrameworkEvent.ERROR, bu, e);
							}
						}

						notifyListeners(FrameworkEvent.PACKAGES_REFRESHED,
								Concierge.this, null);
					} // end synchronized statement
				} catch (final Throwable t) {
					// TODO: to log
					t.printStackTrace();
				}
			}

			private void notifyListeners(final int type, final Bundle b,
					final Exception e) {
				if (type == FrameworkEvent.ERROR) {
					// TODO: to log
					e.printStackTrace();
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

		for (final Bundle bundle : bundles == null ? Concierge.this.bundles
				: bundles) {
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

	private HashMap<ResolverHook, ServiceReferenceImpl<ResolverHookFactory>> getResolverHooks(
			final Collection<BundleRevision> bundles) throws Throwable {
		final LinkedHashMap<ResolverHook, ServiceReferenceImpl<ResolverHookFactory>> hooks = new LinkedHashMap<ResolverHook, ServiceReferenceImpl<ResolverHookFactory>>();

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
						hooks.put(hook, sref);
					}
					sref.ungetService(Concierge.this);
				}
			}
		} catch (final Throwable t) {
			for (final ResolverHook hook : hooks.keySet()) {
				hook.end();
			}
			throw t;
		}
		return hooks;
	}

	private void endResolverHooks(
			final HashMap<ResolverHook, ServiceReferenceImpl<ResolverHookFactory>> hooks)
			throws BundleException {
		if (hooks == null) {
			return;
		}

		Throwable error = null;
		for (final Map.Entry<ResolverHook, ServiceReferenceImpl<ResolverHookFactory>> entry : hooks
				.entrySet()) {
			if (entry.getValue().service == null) {
				// unregistered...
				error = new BundleException(
						"Something unregistered a hook that was in use.",
						BundleException.REJECTED_BY_HOOK);
				continue;
			}

			try {
				entry.getKey().end();
			} catch (final Throwable t) {
				error = t;
			}
		}

		if (error != null) {
			throw new BundleException("Error",
					BundleException.REJECTED_BY_HOOK, error);
		}
	}

	List<BundleCapability> resolveDynamic(final BundleRevision trigger,
			final String pkg, final String dynImportPackage,
			final BundleRequirement dynImport, final boolean multiple) {
		Collection<Capability> candidates = null;

		try {
			if (resolver.hooks == null) {
				resolver.hooks = getResolverHooks(Arrays.asList(trigger));
			}

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
				endResolverHooks(resolver.hooks);
				return null;
			}

			filterCandidates(resolver.hooks.keySet(), dynImport, candidates);

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
					// FIXME: cleanup...
					if (((BundleCapability) cap).getRevision().getBundle()
							.getState() == Bundle.INSTALLED) {
						// need to resolve first
						if (!resolve(
								Collections.singletonList(((BundleCapability) cap)
										.getRevision()), false)) {
							continue;
						}
					}

					matches.add((BundleCapability) cap);
				}

			}

			endResolverHooks(resolver.hooks);

			Collections.sort(matches, EXPORT_ORDER);
			return matches;
		} catch (final Throwable t) {
			// TODO: handle
			return null;
		} finally {
			resolver.hooks = null;
		}

	}

	// TODO: simplify
	protected void filterCandidates(final Collection<ResolverHook> hooks,
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

		final ConciergeCollections.RemoveOnlyList<BundleCapability> filteredCandidates = new ConciergeCollections.RemoveOnlyList<BundleCapability>(
				mmap.getAllValues());

		for (final ResolverHook hook : hooks) {
			hook.filterMatches(requirement, filteredCandidates);
		}

		candidates.addAll(filteredCandidates);
	}

	protected void filterResources(final Collection<ResolverHook> hooks,
			final Collection<Resource> resources,
			final Collection<Resource> removed) {
		final ArrayList<BundleRevision> revisions = new ArrayList<BundleRevision>();
		removed.addAll(resources);
		for (final Iterator<Resource> iter = resources.iterator(); iter
				.hasNext();) {
			final Resource res = iter.next();
			if (res instanceof BundleRevision) {
				revisions.add((BundleRevision) res);
				iter.remove();
			}
		}

		final ConciergeCollections.RemoveOnlyList<BundleRevision> filteredResources = new ConciergeCollections.RemoveOnlyList<BundleRevision>(
				revisions);

		for (final ResolverHook hook : resolver.hooks.keySet()) {
			hook.filterResolvable(filteredResources);
		}

		resources.addAll(filteredResources);
		removed.removeAll(filteredResources);
	}

	boolean resolve(final Collection<BundleRevision> bundles,
			final boolean critical) throws BundleException {
		if (inResolve) {
			throw new IllegalStateException("nested resolve call");
		}

		inResolve = true;
		boolean cleanup = false;

		try {

			final MultiMap<Resource, HostedCapability> hostedCapabilities = new MultiMap<Resource, HostedCapability>();

			if (resolver.hooks == null) {
				resolver.hooks = getResolverHooks(bundles);
				cleanup = true;
			}

			final MultiMap<Resource, Wire> solution = new MultiMap<Resource, Wire>();
			final ArrayList<Requirement> unresolvedRequirements = new ArrayList<Requirement>();
			final ArrayList<Resource> unresolvedResources = new ArrayList<Resource>();

			resolver.resolve0(new ResolveContext() {

				public Collection<Resource> getMandatoryResources() {
					return new ArrayList<Resource>(bundles);
				}

				public Collection<Resource> getOptionalResources() {
					return Collections.emptyList();
				}

				@Override
				public List<Capability> findProviders(
						final Requirement requirement) {
					final String filterStr = requirement.getDirectives().get(
							Namespace.REQUIREMENT_FILTER_DIRECTIVE);

					final List<Capability> providers;
					if (filterStr == null) {
						providers = capabilityRegistry.getAll(requirement
								.getNamespace());
					} else {
						try {
							providers = RFC1960Filter.filterWithIndex(
									requirement, filterStr, capabilityRegistry);
						} catch (final InvalidSyntaxException ise) {
							// TODO: debug output
							ise.printStackTrace();
							return Collections.emptyList();
						}
					}

					sortProviders(providers, requirement.getNamespace());

					// check if the resource itself provides a
					// candidate
					/*
					 * for (final Capability capability : requirement
					 * .getResource().getCapabilities(
					 * requirement.getNamespace())) { if (matches(requirement,
					 * capability)) { providers.add(capability); } }
					 */

					return providers;
				}

				private void sortProviders(final List<Capability> providers,
						final String namespace) {
					if (providers.isEmpty()) {
						return;
					}
					if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
						Collections.sort(providers, EXPORT_ORDER);
					}
					if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
						Collections.sort(providers, BUNDLE_VERSION);
					}
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
						Collections.sort(capabilities, EXPORT_ORDER);
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

			// TODO: introduce resolver debug flag
			if (LOG_ENABLED) {
				logger.log(LogService.LOG_DEBUG, "Solution: " + solution);
			}

			final MultiMap<Resource, Wire> reciprocal = new MultiMap<Resource, Wire>();

			// apply solution
			for (final Resource resource : solution.keySet()) {
				final List<Wire> wires = solution.get(resource);

				if (resource instanceof Revision) {
					final Revision revision = (Revision) resource;

					final boolean isFragment = revision.isFragment();

					if (isFragment) {
						boolean attached = false;
						for (final Iterator<Wire> iter = wires.iterator(); iter
								.hasNext();) {
							final Wire wire = iter.next();

							// scan the wires for host namespace wires
							if (HostNamespace.HOST_NAMESPACE.equals(wire
									.getRequirement().getNamespace())) {

								if (wire.getProvider() instanceof Revision) {
									final Revision host = (Revision) wire
											.getProvider();
									try {
										host.attachFragment(revision);
										attached = true;
									} catch (final BundleException be) { // TODO:
																			// remove
										be.printStackTrace();
									}
								} else {
									// host is system bundle, check
									// extensionBundles
									if (extensionBundles.contains(revision
											.getBundle())) {
										attached = true;
									}
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

					for (final Wire wire : wires) {
						final Resource provider = wire.getProvider();
						reciprocal.insertUnique(provider, wire);
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
				} else {
					// this is the system bundle
					// manually add the wires to wirings
					final Concierge systemBundle = (Concierge) resource;
					ConciergeBundleWiring wiring = (ConciergeBundleWiring) wirings
							.get(resource);
					if (wiring == null) {
						wiring = new ConciergeBundleWiring(systemBundle, wires);
						wirings.put(systemBundle, wiring);
					} else {
						for (final Wire wire : wires) {
							wiring.addWire((BundleWire) wire);
						}
					}
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
			t.printStackTrace();
			throw new BundleException("Resolve Error",
					BundleException.REJECTED_BY_HOOK, t);
		} finally {
			try {
				endResolverHooks(resolver.hooks);
			} finally {
				if (cleanup) {
					resolver.hooks = null;
				}
				inResolve = false;
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

		protected HashMap<ResolverHook, ServiceReferenceImpl<ResolverHookFactory>> hooks;

		public synchronized Map<Resource, List<Wire>> resolve(
				final ResolveContext context) throws ResolutionException {
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

			return solution.getFlatMap();
		}

		protected void resolve0(final ResolveContext context,
				final MultiMap<Resource, Wire> solution,
				final ArrayList<Requirement> unresolvedRequirements,
				final ArrayList<Resource> unresolvedResources) {

			final Collection<Resource> mandatory = context
					.getMandatoryResources();
			final Collection<Resource> optional = context
					.getOptionalResources();

			if (hooks != null && !hooks.isEmpty()) {
				filterResources(hooks.keySet(), mandatory, unresolvedResources);
			}

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
						// should not happen for critical==false
						e.printStackTrace();
					}

					if (resource instanceof BundleRevision) {
						if (!checkSingleton((BundleRevision) resource)) {
							unresolvedResources.add(resource);
							continue;
						}
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

		private boolean checkSingleton(final BundleRevision resource) {
			try {
				final List<Capability> identities = resource
						.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);

				if (identities == null || identities.isEmpty()) {
					return true;
				}

				final BundleCapability identity = (BundleCapability) identities
						.get(0);

				if (!"true".equals(identity.getDirectives().get(
						IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE))) {
					return true;
				}

				final List<BundleCapability> col = new ArrayList<BundleCapability>();

				if (DEBUG_BUNDLES) {
					logger.log(LogService.LOG_DEBUG,
							"RESOLVING " + resource.getSymbolicName() + " - "
									+ resource.getVersion() + " /.//"
									+ resource);
				}

				final List<AbstractBundle> existing = new ArrayList<AbstractBundle>(
						getBundleWithSymbolicName(resource.getSymbolicName()));
				existing.remove(resource.getBundle());

				if (existing.isEmpty()) {
					return true;
				}

				for (final AbstractBundle bundle : existing) {
					if (bundle.state != Bundle.INSTALLED) {
						final BundleCapability existingIdentity = (BundleCapability) bundle.currentRevision
								.getCapabilities(
										IdentityNamespace.IDENTITY_NAMESPACE)
								.get(0);
						if ("true"
								.equals(existingIdentity
										.getDirectives()
										.get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE))) {
							col.add(existingIdentity);
						}
					}
				}

				if (hooks != null && hooks.isEmpty()) {
					return col.isEmpty();
				}

				final ConciergeCollections.RemoveOnlyList<BundleCapability> collisions = new ConciergeCollections.RemoveOnlyList<BundleCapability>(
						col);

				for (final ResolverHook hook : hooks.keySet()) {
					hook.filterSingletonCollisions(identity, collisions);
				}

				if (!collisions.isEmpty()) {
					return false;
				}

				for (final BundleCapability cap : col) {
					final ConciergeCollections.RemoveOnlyList<BundleCapability> identityList = new ConciergeCollections.RemoveOnlyList<BundleCapability>(
							Collections.singletonList(identity));
					for (final ResolverHook hook : hooks.keySet()) {
						hook.filterSingletonCollisions(cap, identityList);
					}

					if (!identityList.isEmpty()) {
						return false;
					}
				}

				return true;
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

			if (solution.containsKey(resource)) {
				return Collections.emptyList();
			}

			final Collection<Requirement> unresolvedRequirements = new ArrayList<Requirement>();
			final MultiMap<Resource, Wire> newWires = new MultiMap<Resource, Wire>();

			boolean isFragment = false;

			if (resource instanceof Revision) {
				final Revision revision = (Revision) resource;

				isFragment = revision.isFragment();
				if (!isFragment) {
					// check which fragments can be attached to the bundles
					if (revision.allowsFragmentAttachment()) {
						for (final Revision frag : getFragments(revision)) {
							final ArrayList<Capability> capList = new ArrayList<Capability>();
							capList.add(revision.getCapabilities(
									HostNamespace.HOST_NAMESPACE).get(0));
							filterCandidates(
									hooks.keySet(),
									(BundleRequirement) frag.getRequirements(
											HostNamespace.HOST_NAMESPACE)
											.get(0), capList);
							if (capList.isEmpty()) {
								continue;
							}

							try {
								if (!revision.attachFragment(frag)) {
									continue;
								}

								hostFragment(context, frag, revision, solution);
							} catch (final BundleException e) {
								// does not attach...
								if (LOG_ENABLED) {
									logger.log(LogService.LOG_ERROR,
											"Unsuccessfully attempted to attach "
													+ frag + " to " + revision,
											e);
								}
							}
						}
					}
				}
			} else {
				isFragment = !resource.getRequirements(
						HostNamespace.HOST_NAMESPACE).isEmpty();
			}

			final Collection<Requirement> requirements = resource
					.getRequirements(null);
			final HashSet<Requirement> skip = new HashSet<Requirement>();

			for (final Requirement requirement : requirements) {
				// skip requirements that are already resolved through uses
				// constraints
				if (skip.contains(requirement)) {
					continue;
				}

				// skip requirements which are not effective
				if (!context.isEffective(requirement)) {
					continue;
				}

				if (isFragment
						&& !HostNamespace.HOST_NAMESPACE.equals(requirement
								.getNamespace())) {
					// skip fragment requirements
					continue;
				}

				// find candidates for the requirement
				final Collection<Capability> candidates = context
						.findProviders(requirement);

				// filter through the resolver hooks if there are any
				if (hooks != null && !hooks.isEmpty()
						&& requirement instanceof BundleRequirement) {
					filterCandidates(hooks.keySet(),
							(BundleRequirement) requirement, candidates);
				}

				boolean resolved = false;
				final boolean multiple = Namespace.CARDINALITY_MULTIPLE
						.equals(requirement.getDirectives().get(
								Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE));

				for (final Capability capability : candidates) {
					if (isFragment) {
						final Revision revision = (Revision) resource;
						if (capability.getResource() instanceof Revision) {
							final Revision host = (Revision) capability
									.getResource();
							try {
								if (!host.attachFragment(revision)) {
									resolved = true;
									continue;
								}
							} catch (final BundleException be) {
								// cannot attach
								continue;
							}
						} else {
							// case of system bundle extension is handled in
							// Concierge.addFragment
						}

						resolved = true;

						hostFragment(context, revision,
								(BundleRevision) capability.getResource(),
								solution);

						// dont' trigger resolution of the host
						continue;
					}

					// handling potential uses constraints
					if (capability instanceof BundleCapability) {
						// FIXME: CLEANUP!!!, OPTIMIZE!!!
						final ArrayList<BundleCapability> caps = new ArrayList<BundleCapability>();

						if (BundleNamespace.BUNDLE_NAMESPACE.equals(capability
								.getNamespace())) {
							caps.addAll(((BundleCapability) capability)
									.getResource().getDeclaredCapabilities(
											PackageNamespace.PACKAGE_NAMESPACE));
						} else {
							caps.add((BundleCapability) capability);
						}

						final ArrayList<BundleCapability> impliedConstraints = new ArrayList<BundleCapability>();
						final HashSet<BundleCapability> seen = new HashSet<BundleCapability>();
						while (!caps.isEmpty()) {
							final BundleCapability cap = caps.remove(0);

							if (seen.contains(cap)) {
								continue;
							}

							seen.add(cap);

							final String usesStr = cap.getDirectives().get(
									Namespace.CAPABILITY_USES_DIRECTIVE);

							if (usesStr != null) {
								final String[] usesConstraints = Utils
										.splitString(usesStr, ',');
								final HashSet<String> usesSet = new HashSet<String>();
								usesSet.addAll(Arrays.asList(usesConstraints));

								final BundleWiring wiring = cap.getResource()
										.getWiring();
								// TODO: what does it mean that wiring is null
								// at
								// this point???
								if (wiring != null && wiring.isInUse()) {
									final List<BundleWire> wires = wiring
											.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

									final HashSet<Object> requireSet = new HashSet<Object>();
									for (final BundleWire wire : wires) {
										final Object pkg = wire
												.getCapability()
												.getAttributes()
												.get(PackageNamespace.PACKAGE_NAMESPACE);

										if (usesSet.contains(pkg)) {
											impliedConstraints.add(wire
													.getCapability());
											caps.add(wire.getCapability());
											requireSet.add(pkg);
										}
									}
									final List<BundleCapability> caps2 = wiring
											.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);

									for (final Capability cap2 : caps2) {
										final Object pkg = cap2
												.getAttributes()
												.get(PackageNamespace.PACKAGE_NAMESPACE);
										if (usesSet.contains(pkg)
												&& !requireSet.contains(pkg)) { // don't
																				// include
																				// cap
																				// if
																				// it
																				// was
																				// already
																				// imported
																				// as
																				// requirement
											impliedConstraints
													.add((BundleCapability) cap2);
											caps.add((BundleCapability) cap2);
										}
									}
								}
							}
						}

						if (!impliedConstraints.isEmpty()) {
							// go over implied constraints

							for (final BundleCapability implied : impliedConstraints) {
								for (final Requirement req : requirements) {
									if (matches(req, implied)) {
										for (final Map.Entry<Resource, List<Wire>> entry : newWires
												.entrySet()) {
											for (final Iterator<Wire> iter = entry
													.getValue().iterator(); iter
													.hasNext();) {
												final Wire wire = iter.next();
												if (wire.getRequirement() == req) {
													iter.remove();
												}
											}
										}

										skip.add(req);

										final Wire wire = Resources.createWire(
												implied, req);
										newWires.insert(resource, wire);

									}
								}

							}
						}
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
					((Revision) resource).markResolved();
				}
			}

			return unresolvedRequirements;
		}

		private void hostFragment(final ResolveContext context,
				final BundleRevision fragment, final BundleRevision host,
				final MultiMap<Resource, Wire> solution) {
			// host the capabilities
			for (final Capability cap : fragment.getCapabilities(null)) {
				if (!IdentityNamespace.IDENTITY_NAMESPACE.equals(cap
						.getNamespace())) {
					final HostedBundleCapability hostedCap = new HostedBundleCapability(
							host, cap);

					context.insertHostedCapability(
							new ArrayList<Capability>(
									host.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)),
							hostedCap);
				}
			}

			// create host wire
			final Capability hostCapability = host.getCapabilities(
					HostNamespace.HOST_NAMESPACE).get(0);
			final Requirement hostRequirement = fragment.getRequirements(
					HostNamespace.HOST_NAMESPACE).get(0);

			final Wire wire = Resources.createWire(hostCapability,
					hostRequirement);
			solution.insert(fragment, wire);
			solution.insert(host, wire);
		}
	}

	// URLStreamHandlerFactory

	protected static class ConciergeURLStreamHandlerFactory implements
			URLStreamHandlerFactory {

		Concierge frameworkInstance = null;

		public void setConcierge(final Concierge concierge) {
			this.frameworkInstance = concierge;
		}

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
							final String[] s = Utils.splitString(host, '.');

							final Long bundleId = Long.parseLong(s[0]);
							final int rev = Integer.parseInt(s[1]);

							if (ConciergeURLStreamHandlerFactory.this.frameworkInstance == null) {
								throw new IllegalStateException(
										"ConciergeURLStreamHandlerFactory "
												+ "is not linked to a Concierge framework");
							}

							final BundleImpl bundle = (BundleImpl) ConciergeURLStreamHandlerFactory.this.frameworkInstance.bundleID_bundles
									.get(bundleId);
							if (bundle == null) {
								throw new IllegalStateException(
										"Bundle for URL " + u
												+ " can not be found");
							}
							return new URLConnection(u) {

								private InputStream inputStream;

								private boolean isConnected;

								public void connect() throws IOException {
									inputStream = bundle.getURLResource(u, rev);
									isConnected = true;
								}

								public int getContentLength() {
									return (int) bundle.getResourceLength(u,
											rev);
								}

								public InputStream getInputStream()
										throws IOException {
									if (!isConnected) {
										connect();
									}
									return inputStream;
								}

							};
						} catch (final NumberFormatException nfe) {
							throw new IOException("Malformed host "
									+ u.getHost());
						}
					}
				};
			}

			return null;
		}
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
				Arrays.asList(Utils.splitString(Utils.unQuote(mandatory)
						.toLowerCase(), ',')));
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

	List<AbstractBundle> getBundleWithSymbolicName(final String symbolicName) {
		final List<AbstractBundle> list = symbolicName_bundles
				.lookup(symbolicName);
		return list;
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
		if (fragment.isExtensionBundle()) {
			try {
				addURL.invoke(systemBundleClassLoader,
						fragment.createURL("/", null));
			} catch (final Exception e) {
				// FIXME: to log
				e.printStackTrace();
			}

			extensionBundles.add((BundleImpl) fragment.getBundle());
		}

		final String fragmentHostName = fragment.getFragmentHost();
		fragmentIndex.insert(fragmentHostName, fragment);
	}

	/**
	 * Remove a fragment from the map of unattached fragments.
	 * 
	 * @param fragment
	 *            the fragment bundle to remove
	 */
	void removeFragment(final Revision fragment) {
		fragmentIndex.remove(fragment.getFragmentHost(), fragment);

		final String fragmentHostName = fragment.getFragmentHost();
		if (fragmentHostName.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME)
				|| fragmentHostName.equals(FRAMEWORK_SYMBOLIC_NAME)) {
			extensionBundles.remove(fragment.getBundle());
		}
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
		final List<Revision> candidates = new ArrayList<Revision>(
				fragmentIndex.lookup(hostBundle.getSymbolicName()));

		if (!candidates.isEmpty()) {
			final Capability cap = hostBundle.getCapabilities(
					HostNamespace.HOST_NAMESPACE).get(0);
			for (final Iterator<Revision> iter = candidates.iterator(); iter
					.hasNext();) {
				final Requirement req = iter.next()
						.getRequirements(HostNamespace.HOST_NAMESPACE).get(0);
				if (!matches(req, cap)) {
					iter.remove();
				}
			}
		}

		return candidates;
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

			final ConciergeCollections.DeltaTrackingRemoveOnlyList<BundleContext> contexts = new ConciergeCollections.DeltaTrackingRemoveOnlyList<BundleContext>(
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
			final ConciergeCollections.RemoveOnlyList<Bundle> list = new ConciergeCollections.RemoveOnlyList<Bundle>(
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

			final ConciergeCollections.RemoveOnlyMap<BundleContext, Collection<ListenerInfo>> map = new ConciergeCollections.RemoveOnlyMap<BundleContext, Collection<ListenerInfo>>();
			for (final BundleContext ctx : mmap.keySet()) {
				final Collection<ListenerInfo> col = new ConciergeCollections.RemoveOnlyList<ListenerInfo>(
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
		final ConciergeCollections.RemoveOnlyList<Bundle> list = new ConciergeCollections.RemoveOnlyList<Bundle>(
				bundles);

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

			// if (bundle == Concierge.this) {
			// return;
			// }

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
			if (hooks == null || hooks.isEmpty()) {
				return;
			}

			if (!added) {
				for (final ServiceListenerEntry entry : entries) {
					entry.removed = true;
				}
			}

			final Collection<ListenerInfo> c = new ConciergeCollections.RemoveOnlyList<ListenerInfo>(
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
				// ensure directory is available
				// handle filename = "", create /data folder in this case
				if (filename.isEmpty()) {
					file.mkdirs();
				} else {
					// TODO Hmm, create all subdirs too, or only "/data" folder?
					file.getParentFile().mkdirs();
				}
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

			if (references != null) {
				final ServiceReferenceImpl<?>[] refs = references
						.toArray(new ServiceReferenceImpl[references.size()]);

				for (int i = 0; i < refs.length; i++) {
					if (theFilter.match(refs[i])
							&& (all || refs[i]
									.isAssignableTo(bundle, (String[]) refs[i]
											.getProperty(Constants.OBJECTCLASS)))) {
						result.add(refs[i]);
					}
				}
			}

			if (!serviceFindHooks.isEmpty()) {
				final Collection<ServiceReference<?>> c = new ConciergeCollections.RemoveOnlyList<ServiceReference<?>>(
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
						"Framework: REQUESTED SERVICES "
								+ (clazz == null ? "(no class)" : clazz)
								+ " "
								+ (filter == null ? "(no filter)" : "filter="
										+ filter));
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

			ServiceReference<?>[] list = null;
			try {
				list = getServiceReferences(clazz, null, true);
			} catch (final InvalidSyntaxException e) {
			}

			if (list == null) {
				return null;
			}

			final ServiceReference<?>[] candidates = list;

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
			if (b.registeredFrameworkListeners != null) {
				b.registeredFrameworkListeners.remove(listener);
				if (b.registeredFrameworkListeners.isEmpty()) {
					b.registeredFrameworkListeners = null;
				}
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
			final ServiceReference[] refs = getServiceReferences(
					clazz.getName(), filter);
			if (refs == null) {
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
		protected ServiceListenerEntry(final AbstractBundle bundle,
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
				attributeIndex.insert((String) defaultAttribute, cap);
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
				final boolean success = attributeIndex.remove(defaultAttribute,
						cap);
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

	String[] getLibraryName(final String libname) {
		if (libraryExtensions == null) {
			return new String[] { System.mapLibraryName(libname) };
		}
		final String[] result = new String[libraryExtensions.length + 1];
		result[0] = System.mapLibraryName(libname);
		for (int i = 0; i < libraryExtensions.length; i++) {
			result[i + 1] = libname + "." + libraryExtensions[i];
		}
		return result;
	}

	void execPermission(final File libfile) {
		if (execPermission == null) {
			return;
		}
		final String cmd = execPermissionPattern
				.matcher(execPermission)
				.replaceAll(Matcher.quoteReplacement(libfile.getAbsolutePath()));
		try {
			Runtime.getRuntime().exec(cmd).waitFor();
		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}

}
