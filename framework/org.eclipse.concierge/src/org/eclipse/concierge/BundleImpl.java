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
 *     Tim Verbelen - Bugfixing
 *     Jochen Hiller - Bugfixing
 *******************************************************************************/
package org.eclipse.concierge;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
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
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.concierge.ConciergeCollections.MultiMap;
import org.eclipse.concierge.ConciergeCollections.ParseResult;
import org.eclipse.concierge.ConciergeCollections.Tuple;
import org.eclipse.concierge.Resources.BundleCapabilityImpl;
import org.eclipse.concierge.Resources.BundleRequirementImpl;
import org.eclipse.concierge.Resources.ConciergeBundleWire;
import org.eclipse.concierge.Resources.ConciergeBundleWiring;
import org.eclipse.concierge.compat.LegacyBundleProcessing;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.HostedCapability;

/**
 * @author Jan Rellermeyer - Initial Contribution
 * @author Tim Verbelen - Bugfixing
 * @author Jochen Hiller - Bugfixing
 */
public class BundleImpl extends AbstractBundle implements BundleStartLevel {

	/*
	 * Helper methods for loading classes from a .dex file on Dalvik VM
	 */
	protected static final Method dexFileLoader;
	protected static final Method dexClassLoader;

	static {
		Method classloader;
		Method fileloader;
		try {
			final Class<?> dexFileClass = Class
					.forName("dalvik.system.DexFile");

			classloader = dexFileClass.getMethod("loadClass",
					new Class[] { String.class, ClassLoader.class });
			fileloader = dexFileClass.getMethod("loadDex",
					new Class[] { String.class, String.class, Integer.TYPE });
		} catch (final Throwable ignore) {
			classloader = null;
			fileloader = null;
		}
		dexClassLoader = classloader;
		dexFileLoader = fileloader;
	}

	protected static final Pattern DIRECTIVE_LIST = Pattern
			.compile("\\s*([^:]*)\\s*:=\\s*\"\\s*(.+)*?\\s*\"\\s*");

	private static final int TIMEOUT = 10000;

	/**
	 * the default name of stored bundles.
	 */
	private static final String BUNDLE_FILE_NAME = "bundle";

	/**
	 * the default name of the content directory for bundles stored directly in
	 * the file system.
	 */
	private static final String CONTENT_DIRECTORY_NAME = "content";

	private static final short FRAGMENT_ATTACHMENT_NEVER = -1;

	private static final short FRAGMENT_ATTACHMENT_RESOLVETIME = 1;

	private static final short FRAGMENT_ATTACHMENT_ALWAYS = 2;

	private String symbolicName;

	private Version version;

	protected static ThreadLocal<ArrayList<AbstractBundle>> activationChain = new ThreadLocal<ArrayList<AbstractBundle>>();

	/**
	 * the host to which this bundle is attached (if any), only valid for
	 * fragment bundles
	 */
	protected List<BundleImpl> hostBundles;

	protected final Concierge framework;

	/**
	 * is bundle marked to start with lazy activation policy? (otherwise use
	 * eager activation)
	 */
	protected boolean lazyActivation;
	protected boolean beingLazy = false;

	protected String[] activationIncludes;

	protected String[] activationExcludes;

	protected String bundleLocalizationBaseDir;
	protected String bundleLocalizationBaseFilename;

	protected HeaderDictionary headers;

	private Locale lastDefaultLocale;

	private int currentRevisionNumber = -1;

	public BundleImpl(final Concierge framework,
			final BundleContext installingContext, final String location,
			final long bundleId, final InputStream stream)
					throws BundleException {
		this.framework = framework;
		this.location = location;
		this.bundleId = bundleId;
		this.startlevel = framework.initStartlevel;
		this.lastModified = System.currentTimeMillis();

		if (framework.SECURITY_ENABLED) {
			try {
				final PermissionCollection permissions = new Permissions();
				permissions.add(new FilePermission(
						framework.STORAGE_LOCATION + bundleId,
						"read,write,execute,delete"));
				domain = new ProtectionDomain(
						new CodeSource(
								new URL("file:" + framework.STORAGE_LOCATION
										+ bundleId),
								(java.security.cert.Certificate[]) null),
						permissions);
			} catch (final Exception e) {
				e.printStackTrace();
				throw new BundleException("Exception while installing bundle",
						BundleException.STATECHANGE_ERROR, e);
			}
		}

		this.storageLocation = framework.STORAGE_LOCATION + bundleId
				+ File.separatorChar;

		currentRevision = readAndProcessInputStream(stream);
		symbolicName = currentRevision.getSymbolicName();
		version = currentRevision.getVersion();
		revisions.add(0, currentRevision);

		// check is same version is already installed
		framework.checkForCollision(CollisionHook.INSTALLING,
				installingContext.getBundle(), currentRevision);

		this.state = INSTALLED;

		// if we are not during startup or shutdown, update the metadata
		if (framework.state != Bundle.STARTING
				&& framework.state != Bundle.STOPPING) {
			updateMetadata();
		}
	}

	// framework restart case
	public BundleImpl(final Concierge framework, final File metadata)
			throws IOException, BundleException {
		this.framework = framework;

		// this.content = new JarBundle(new JarFile(file));
		final DataInputStream in = new DataInputStream(
				new FileInputStream(metadata));
		// read current revision from metadata
		this.currentRevisionNumber = in.readInt();

		this.bundleId = in.readLong();
		this.location = in.readUTF();

		this.storageLocation = framework.STORAGE_LOCATION + bundleId
				+ File.separatorChar;

		// locate current revision
		final File file = new File(storageLocation,
				BUNDLE_FILE_NAME + currentRevisionNumber);
		final File contentDir = new File(storageLocation
				+ CONTENT_DIRECTORY_NAME + currentRevisionNumber);

		if (file.exists() && file.isFile()) {
			final JarFile jarFile = new JarFile(file);
			final Manifest manifest = jarFile.getManifest();
			final String[] classpathStrings = readProperties(
					manifest.getMainAttributes(), Constants.BUNDLE_CLASSPATH,
					new String[] { "." });

			this.currentRevision = new JarBundleRevision(currentRevisionNumber,
					jarFile, manifest, classpathStrings);
		} else if (contentDir.exists() && contentDir.isDirectory()) {
			final Manifest manifest = new Manifest(new FileInputStream(
					new File(contentDir, JarFile.MANIFEST_NAME)));
			final String[] classpathStrings = readProperties(
					manifest.getMainAttributes(), Constants.BUNDLE_CLASSPATH,
					new String[] { "." });

			this.currentRevision = new ExplodedJarBundleRevision(
					currentRevisionNumber, contentDir.getAbsolutePath(),
					manifest, classpathStrings);
		} else {
			in.close();
			throw new BundleException("Bundle revision " + currentRevisionNumber
					+ " does not exist", BundleException.READ_ERROR);
		}
		
		this.symbolicName = currentRevision.getSymbolicName();
		this.version = currentRevision.getVersion();
		this.revisions.add(0, currentRevision);
		this.startlevel = in.readInt();
		this.state = Bundle.INSTALLED;
		this.autostart = in.readShort();
		this.lazyActivation = in.readBoolean();
		this.lastModified = in.readLong();
		in.close();
		this.context = framework.createBundleContext(this);

		// System.err.println("RESTORED BUNDLE " + toString() + " WITH SL " + this.startlevel + " and autostart " + this.autostart);
		
		if (framework.SECURITY_ENABLED) {
			domain = new ProtectionDomain(null, null);
		}
	}

	/**
	 * update the bundle's metadata on the storage.
	 */
	void updateMetadata() {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(
					new FileOutputStream(new File(storageLocation, "meta")));
			out.writeInt(currentRevisionNumber);
			out.writeLong(bundleId);
			out.writeUTF(location);
			out.writeInt(startlevel);
			out.writeShort(autostart);
			out.writeBoolean(lazyActivation);
			out.writeLong(lastModified);
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (final Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Reads and processes input stream: - writes bundle to storage - processes
	 * manifest
	 * 
	 * @param inStream
	 *            the input stream of the bundle
	 * @throws BundleException
	 */
	private Revision readAndProcessInputStream(final InputStream inStream)
			throws BundleException {

		final int revisionNumber = ++currentRevisionNumber;

		try {
			// write the JAR file to the storage
			final File file = new File(storageLocation,
					BUNDLE_FILE_NAME + revisionNumber);

			storeFile(file, inStream);

			// and open a JarFile
			// TODO: check when verification is really required...
			final JarFile jar = new JarFile(file, false);

			// process the manifest
			final Manifest manifest = jar.getManifest();

			// get the classpath
			final String[] classpathStrings = readProperties(
					manifest.getMainAttributes(), Constants.BUNDLE_CLASSPATH,
					new String[] { "." });

			if (framework.ALWAYS_DECOMPRESS || framework.DECOMPRESS_EMBEDDED
					&& classpathStrings.length > 1) {
				final File contentDir = new File(storageLocation
						+ CONTENT_DIRECTORY_NAME + revisionNumber);
				if (contentDir.exists()) {
					Concierge.deleteDirectory(contentDir);
				}

				// decompress the bundle
				for (final Enumeration<JarEntry> entries = jar
						.entries(); entries.hasMoreElements();) {
					final JarEntry entry = entries.nextElement();
					if (entry.isDirectory()) {
						continue;
					}
					final File embeddedJar = new File(contentDir,
							entry.getName());
					storeFile(embeddedJar, jar.getInputStream(entry));
				}
				// delete the bundle jar
				jar.close();
				new File(jar.getName()).delete();
				return new ExplodedJarBundleRevision(revisionNumber,
						contentDir.getAbsolutePath(), manifest,
						classpathStrings);
			} else {
				return new JarBundleRevision(revisionNumber, jar, manifest,
						classpathStrings);
			}
		} catch (final IOException ioe) {
			ioe.printStackTrace();
			Concierge.deleteDirectory(new File(storageLocation));
			throw new BundleException(
					"Not a valid bundle: " + location + " (tried to write to "
							+ new File(storageLocation,
									BUNDLE_FILE_NAME + revisionNumber)
							+ ")",
					BundleException.READ_ERROR, ioe);
		}
	}

	// FIXME: can't this be called from constructor???
	void install() throws BundleException {
		// we are just installing the bundle, if it is
		// possible, resolve it, if not, wait until the
		// exports are really needed (i.e., they become critical)
		// if (!currentRevision.isFragment()) {
		// currentRevision.resolve(false);
		// }

		// register bundle with framework:
		synchronized (framework) {
			framework.bundles.add(this);
			framework.bundleID_bundles.put(new Long(getBundleId()), this);
			framework.symbolicName_bundles
					.insert(currentRevision.getSymbolicName(), this);
			framework.location_bundles.put(location, this);
		}

		// resolve if it is a framework extension
		if (currentRevision.isFrameworkExtension()) {
			currentRevision.resolve(false);
		}
	}

	/**
	 * start the bundle.
	 * 
	 * @throws BundleException
	 *             if the bundle cannot be resolved or the Activator throws an
	 *             exception.
	 * @see org.osgi.framework.Bundle#start()
	 * @category Bundle
	 */
	public void start() throws BundleException {
		start(0);
	}

	/**
	 * start the bundle with options.
	 * 
	 * @throws BundleException
	 *             if the bundle cannot be resolved or the Activator throws an
	 *             exception.
	 * @see org.osgi.framework.Bundle#start(int)
	 * @category Bundle
	 */
	public void start(final int options) throws BundleException {
		if (framework.SECURITY_ENABLED) {
			// TODO: check AdminPermission(this, EXECUTE)
		}

		if (state == UNINSTALLED) {
			throw new IllegalStateException(
					"Cannot start uninstalled bundle " + toString());
		}

		if (currentRevision.isFragment()) {
			throw new BundleException(
					"The fragment bundle " + toString() + " cannot be started",
					BundleException.INVALID_OPERATION);
		}

		if (!lazyActivation
				&& (state == Bundle.STARTING || state == Bundle.STOPPING)) {
			try {
				synchronized (this) {
					wait(TIMEOUT);
				}
			} catch (final InterruptedException ie) {
				// ignore and proceed
			}

			if (state == Bundle.STARTING || state == Bundle.STOPPING) {
				// hit the timeout
				throw new BundleException(
						"Timeout occurred. Bundle was unable to start.",
						BundleException.STATECHANGE_ERROR);
			}
		}

		if ((options & Bundle.START_TRANSIENT) > 0) {
			if (startlevel > framework.startlevel) {
				throw new BundleException(
						"Bundle (with start level " + startlevel
								+ ") cannot be started due to the framework's current start level of "
								+ framework.startlevel,
						BundleException.START_TRANSIENT_ERROR);
			}
		} else {
			if ((options & Bundle.START_TRANSIENT) == 0) {
				autostart = options == 0 ? AUTOSTART_STARTED_WITH_EAGER
						: AUTOSTART_STARTED_WITH_DECLARED;
			}
			updateMetadata();
		}
		if (startlevel <= framework.startlevel) {
			activate(options);
		}
	}

	/**
	 * the actual starting happens here. This method does not modify the
	 * persistent metadata.
	 * 
	 * @throws BundleException
	 *             if the bundle cannot be resolved or the Activator throws an
	 *             exception.
	 */
	synchronized void activate(final int options) throws BundleException {
		if (state == ACTIVE) {
			return;
		}
		if (currentRevision.isFragment()) {
			return;
		}

		// step4
		if (state == INSTALLED) {
			// this time, it is critical to get the bundle resolved
			// so if we need exports from other unresolved bundles,
			// we will try to resolve them (recursively) to get the bundle
			// started
			currentRevision.resolve(true);
		}

		// step5
		this.context = framework.createBundleContext(this);
		if ((options & Bundle.START_ACTIVATION_POLICY) > 0 && lazyActivation) {
			if (state != STARTING) {
				beingLazy = true;
				state = STARTING;
				framework.notifyBundleListeners(BundleEvent.LAZY_ACTIVATION,
						this);
			}
			synchronized (this) {
				notify();
			}
			return;
		} else {
			beingLazy = false;
		}

		activate0();
	}

	private void activate0() throws BundleException {
		assert state != INSTALLED && state != UNINSTALLED;

		// step6
		state = STARTING;
		// step7
		framework.notifyBundleListeners(BundleEvent.STARTING, this);
		// step8 (part 1)
		try {
			context.isValid = true;
			final String activatorClassName = currentRevision.activatorClassName;
			if (activatorClassName != null) {
				@SuppressWarnings("unchecked")
				final Class<BundleActivator> activatorClass = (Class<BundleActivator>) currentRevision.classloader
						.loadClass(activatorClassName);
				if (activatorClass == null) {
					throw new ClassNotFoundException(activatorClassName);
				}
				currentRevision.activatorInstance = activatorClass
						.newInstance();
				currentRevision.activatorInstance.start(context);
				// step 9
				if (state == UNINSTALLED) {
					throw new BundleException(
							"Activator.start uninstalled the bundle!",
							BundleException.ACTIVATOR_ERROR);
				}
			}
			// step10
			state = ACTIVE;
			// step11
			framework.notifyBundleListeners(BundleEvent.STARTED, this);
			if (framework.DEBUG_BUNDLES) {
				framework.logger.log(LogService.LOG_INFO,
						"framework: Bundle " + toString() + " started.");
			}
			synchronized (this) {
				notify();
			}
		} catch (final Throwable t) {
			// step8 (part2)
			framework.notifyBundleListeners(BundleEvent.STOPPING, this);
			framework.clearBundleTrace(this);
			state = RESOLVED;
			framework.notifyBundleListeners(BundleEvent.STOPPED, this);
			throw new BundleException("Error starting bundle " + toString(),
					BundleException.ACTIVATOR_ERROR, t);
		}
	}

	/**
	 * stop the bundle.
	 * 
	 * @throws BundleException
	 *             if the bundle has been uninstalled before.
	 * 
	 * @see org.osgi.framework.Bundle#stop()
	 * @category Bundle
	 */
	public void stop() throws BundleException {
		stop(0);
	}

	/**
	 * stop the bundle.
	 * 
	 * @throws BundleException
	 *             if the bundle has been uninstalled before.
	 * 
	 * @param options
	 *            for stopping the bundle.
	 * @see org.osgi.framework.Bundle#stop(int)
	 * @category Bundle
	 */
	public void stop(final int options) throws BundleException {
		if (framework.SECURITY_ENABLED) {
			// TODO: check AdminPermission(this, EXECUTE);
		}

		if (state == UNINSTALLED) {
			throw new IllegalStateException(
					"Cannot stop uninstalled bundle " + toString());
		}

		if (currentRevision.isFragment()) {
			throw new BundleException(
					"The fragment bundle " + toString() + " cannot be stopped",
					BundleException.INVALID_OPERATION);
		}

		if (state == Bundle.STARTING || state == Bundle.STOPPING) {
			try {
				synchronized (this) {
					wait(TIMEOUT);
				}
			} catch (final InterruptedException ie) {
				// ignore
			}
			if (state == UNINSTALLED) {
				throw new IllegalStateException(
						"Cannot stop uninstalled bundle " + toString());
			}
			if (state == Bundle.STARTING || state == Bundle.STOPPING) {
				// timeout occurred!
				throw new BundleException(
						"Timeout occurred. Bundle was unable to stop!",
						BundleException.STATECHANGE_ERROR);
			}
		}
		if (options != Bundle.STOP_TRANSIENT) {
			// change persistent autostart configuration
			autostart = AUTOSTART_STOPPED;
			updateMetadata();
		}
		if (state != ACTIVE && state != STARTING) {
			return;
		}

		stopBundle();
	}

	/**
	 * the actual starting happens here. This method does not modify the
	 * persistent meta data.
	 * 
	 * @throws BundleException
	 *             if the bundle has been uninstalled before.
	 */
	synchronized void stopBundle() throws BundleException {
		if (state == INSTALLED) {
			return;
		}

		final int oldState = state;
		// step 5
		state = STOPPING;
		// step 6
		framework.notifyBundleListeners(BundleEvent.STOPPING, this);
		try {
			if (oldState == ACTIVE) {
				if (currentRevision.activatorInstance != null) {
					currentRevision.activatorInstance.stop(context);
				}
				if (state == UNINSTALLED) {
					throw new BundleException(
							"Activator.stop() uninstalled this bundle!",
							BundleException.ACTIVATOR_ERROR);
				}
			}
		} catch (final Throwable t) {
			throw new BundleException("Error stopping bundle " + toString(),
					BundleException.STATECHANGE_ERROR, t);
		} finally {
			if (currentRevision != null
					&& currentRevision.activatorInstance != null) {
				currentRevision.activatorInstance = null;
			}
			framework.clearBundleTrace(this);
			state = RESOLVED;
			framework.notifyBundleListeners(BundleEvent.STOPPED, this);
			if (framework.DEBUG_BUNDLES) {
				framework.logger.log(LogService.LOG_INFO,
						"framework: Bundle " + toString() + " stopped.");
			}
			if (context != null) {
				context.isValid = false;
			}
			context = null;
			synchronized (this) {
				notify();
			}
		}
	}

	/**
	 * uninstall the bundle.
	 * 
	 * @throws BundleException
	 *             if bundle is already uninstalled
	 * @see org.osgi.framework.Bundle#uninstall()
	 * @category Bundle
	 */
	public synchronized void uninstall() throws BundleException {
		if (framework.SECURITY_ENABLED) {
			// TODO: check AdminPermission(this, LIFECYCLE)
		}

		if (state == UNINSTALLED) {
			throw new IllegalStateException(
					"Bundle " + toString() + " is already uninstalled.");
		}
		if (state == ACTIVE) {
			try {
				stopBundle();
			} catch (final Throwable t) {
				framework.notifyFrameworkListeners(FrameworkEvent.ERROR, this,
						t);
			}
		}

		if (currentRevision.isFragment()) {
			// fragment becomes unresolved
			framework.removeFragment(currentRevision);
		}

		// reset locale
		lastDefaultLocale = Locale.getDefault();
		headers.headerCache = null;

		framework.notifyBundleListeners(BundleEvent.UNRESOLVED, this);

		state = UNINSTALLED;
		synchronized (framework) {
			updateLastModified();

			new File(storageLocation, "meta").delete();

			framework.symbolicName_bundles
					.remove(currentRevision.getSymbolicName(), this);
			currentRevision.cleanup(true);
			currentRevision = null;

			framework.location_bundles.remove(location);
		}

		framework.notifyBundleListeners(BundleEvent.UNINSTALLED, this);

		if (context != null) {
			context.isValid = false;
			context = null;
		}
	}

	/**
	 * update the bundle from its update location or the location from where it
	 * was originally installed.
	 * 
	 * @throws BundleException
	 *             if something goes wrong.
	 * @see org.osgi.framework.Bundle#update()
	 * @category Bundle
	 */
	public synchronized void update() throws BundleException {
		final String updateLocation = headers
				.get(Constants.BUNDLE_UPDATELOCATION);
		try {
			update(new URL(updateLocation == null ? location : updateLocation)
					.openConnection().getInputStream());
		} catch (final IOException ioe) {
			throw new BundleException("Could not update " + toString()
					+ " from " + updateLocation, BundleException.READ_ERROR,
					ioe);
		}
	}

	/**
	 * update the bundle from an input stream.
	 * 
	 * @param stream
	 *            the stream.
	 * @throws BundleException
	 *             if something goes wrong.
	 * @see org.osgi.framework.Bundle#update(java.io.InputStream)
	 * @category Bundle
	 */
	public synchronized void update(final InputStream stream)
			throws BundleException {
		lastModified = System.currentTimeMillis();

		try {
			if (framework.SECURITY_ENABLED) {
				// TODO: check AdminPermission(this, LIFECYCLE)
			}

			if (state == UNINSTALLED) {
				throw new IllegalStateException(
						"Cannot update uninstalled bundle " + toString());
			}

			boolean wasActive = false;
			if (state == ACTIVE) {
				// so we have to restart it after update
				wasActive = true;
				stop();
			}

			if (currentRevision.isFragment()) {
				state = INSTALLED;
				framework.removeFragment(currentRevision);
				framework.notifyBundleListeners(BundleEvent.UPDATED, this);
			} else {
				updateLastModified();

				if (currentRevision != null) {
					// do not close here, e.g. old wirings should still be
					// available
					currentRevision.cleanup(false);
				}
			}

			final Revision updatedRevision = readAndProcessInputStream(stream);

			framework.checkForCollision(CollisionHook.UPDATING, this,
					updatedRevision);

			framework.symbolicName_bundles
					.remove(currentRevision.getSymbolicName(), this);
			currentRevision = updatedRevision;
			symbolicName = currentRevision.getSymbolicName();
			version = currentRevision.getVersion();
			revisions.add(0, updatedRevision);
			framework.symbolicName_bundles
					.insert(currentRevision.getSymbolicName(), this);

			if (!currentRevision.isFragment()) {
				currentRevision.resolve(false);
			}

			framework.notifyBundleListeners(BundleEvent.UPDATED, this);

			if (wasActive) {
				try {
					start();
				} catch (final BundleException be) {
					// TODO: to log
				}
			}
			if (framework.state != Bundle.STARTING
					&& framework.state != Bundle.STOPPING) {
				updateMetadata();
			}
		} finally {
			try {
				stream.close();
			} catch (final IOException e) {
				// ignore
			}
		}
	}

	void refresh() {
		// iterate over old and current revisions
		for (final BundleRevision brev : revisions) {
			final Revision rev = (Revision) brev;

			if (rev.wiring != null) {
				rev.wiring.cleanup();
				rev.wiring = null;
			}
		}

		revisions.clear();

		if (currentRevision != null) {
			revisions.add(currentRevision);

			// detach fragments (if any) and reset classloader
			currentRevision.refresh();

			// remove from framework wirings
			framework.wirings.remove(currentRevision);

			// clear and restore dynamic imports
			currentRevision.dynamicImports.clear();
			for (final BundleRequirement req : currentRevision.requirements
					.lookup(PackageNamespace.PACKAGE_NAMESPACE)) {
				if (PackageNamespace.RESOLUTION_DYNAMIC
						.equals(req.getDirectives().get(
								Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
					currentRevision.dynamicImports.add(req);
				}

			}
		}
	}

	/**
	 * This is a test to filter out classes which should not trigger bundle
	 * activation in case of a lazy activation policy.
	 * 
	 * @param pkgName
	 *            Name of the class to test
	 * @return true if the class should trigger activation
	 */
	boolean checkActivation(final String pkgName) {
		if (activationExcludes != null) {
			for (int i = 0; i < activationExcludes.length; i++) {
				if (RFC1960Filter.stringCompare(
						activationExcludes[i].toCharArray(), 0,
						pkgName.toCharArray(), 0) == 0) {
					return false;
				}
			}
		}

		if (activationIncludes != null) {
			boolean trigger = false;
			for (int i = 0; i < activationIncludes.length; i++) {
				if (RFC1960Filter.stringCompare(
						activationIncludes[i].toCharArray(), 0,
						pkgName.toCharArray(), 0) == 0) {
					trigger = true;
					break;
				}
			}
			if (!trigger) {
				return false;
			}
		}
		return true;
	}

	void triggerActivation() {
		try {
			activate0();
		} catch (final BundleException be) {
			// see spec 4.4.6.2 lazy activation policy
			state = Bundle.STOPPING;
			framework.notifyBundleListeners(BundleEvent.STOPPING, this);
			state = Bundle.RESOLVED;
			framework.notifyBundleListeners(BundleEvent.STOPPED, this);
		}
	}

	/**
	 * get the bundle symbolic name.
	 * 
	 * @return the bundle symbolic name.
	 * @see org.osgi.framework.Bundle#getSymbolicName()
	 * @category Bundle
	 */
	public final String getSymbolicName() {
		return symbolicName;
	}

	/**
	 * @see org.osgi.framework.Bundle#getLocation()
	 * @category Bundle
	 */
	public final Version getVersion() {
		return version;
	}

	/**
	 * @see org.osgi.framework.Bundle#getHeaders()
	 * @category Bundle
	 */
	public Dictionary<String, String> getHeaders() {
		if (framework.SECURITY_ENABLED) {
			// check AdminPermission(this,METADATA)
		}

		return headers.localize(lastDefaultLocale == null ? Locale.getDefault()
				: lastDefaultLocale);
	}

	/**
	 * @see org.osgi.framework.Bundle#getServicesInUse()
	 * @category Bundle
	 */
	public ServiceReference<?>[] getServicesInUse() {
		if (state == UNINSTALLED) {
			throw new IllegalStateException(
					"Bundle " + toString() + "has been unregistered.");
		}

		if (registeredServices == null) {
			return null;
		}

		final ArrayList<ServiceReference<?>> result = new ArrayList<ServiceReference<?>>();
		final ServiceReference<?>[] srefs = registeredServices
				.toArray(new ServiceReference[registeredServices.size()]);

		for (int i = 0; i < srefs.length; i++) {
			synchronized (((ServiceReferenceImpl<?>) srefs[i]).useCounters) {
				if (((ServiceReferenceImpl<?>) srefs[i]).useCounters
						.get(this) != null) {
					result.add(srefs[i]);
				}
			}
		}

		if (framework.SECURITY_ENABLED) {
			// permissions for the interfaces have to be checked
			return checkPermissions(
					result.toArray(new ServiceReferenceImpl[result.size()]));
		} else {
			return result.toArray(new ServiceReference[result.size()]);
		}
	}

	/**
	 * @see org.osgi.framework.Bundle#getResource(java.lang.String)
	 * @category Bundle
	 */
	public URL getResource(final String name) {
		if (state == UNINSTALLED) {
			throw new IllegalStateException("Bundle is uninstalled");
		}

		if (currentRevision.isFragment()) {
			// bundle is Fragment, return null
			return null;
		}

		if (state == INSTALLED) {
			try {
				if (!currentRevision.resolve(false)) {
					// search in this bundle:
					try {
						if ("/".equals(name)) {
							return currentRevision.createURL("/", null);
						}
						for (int i = 0; i < currentRevision.classpath.length; i++) {
							final URL url = currentRevision.lookupFile(
									currentRevision.classpath[i], name);
							if (url != null) {
								return url;
							}
						}
					} catch (final IOException e) {
						// TODO: to log
						e.printStackTrace();
						return null;
					}
					return null;
				}
			} catch (final BundleException e) {
				// TODO: to log
				e.printStackTrace();
				return null;
			}
		}
		return currentRevision.classloader.findResource(name);
	}

	/**
	 * 
	 * @see org.osgi.framework.Bundle#getHeaders(java.lang.String)
	 * @category Bundle
	 */
	public Dictionary<String, String> getHeaders(final String locale) {
		if (locale == null || lastDefaultLocale != null) {
			return getHeaders();
		}

		if (locale.length() == 0) {
			return headers;
		}

		final String[] vars = locale.split("_");
		final Locale loc;
		if (vars.length > 2) {
			loc = new Locale(vars[0], vars[1], vars[2]);
		} else if (vars.length > 1) {
			loc = new Locale(vars[0], vars[1]);
		} else {
			loc = new Locale(vars[0]);
		}

		return headers.localize(loc);
	}

	/**
	 * @see org.osgi.framework.Bundle#loadClass(java.lang.String)
	 * @category Bundle
	 */
	public Class<?> loadClass(final String name) throws ClassNotFoundException {
		if (state == Bundle.UNINSTALLED) {
			throw new IllegalStateException("Bundle is uninstalled");
		}

		// is this actually a fragment?
		if (currentRevision.isFragment()) {
			throw new ClassNotFoundException(
					"This bundle is a fragment and cannot load any classes.");
		}

		if (state == Bundle.INSTALLED) {
			try {
				currentRevision.resolve(true);
			} catch (final BundleException be) {
				framework.notifyFrameworkListeners(FrameworkEvent.ERROR, this,
						be);
				throw new ClassNotFoundException(name, be);
			}
		}

		return currentRevision.classloader.findClass(name);
	}

	/**
	 * @see org.osgi.framework.Bundle#getResources(String)
	 * @category Bundle
	 */
	public Enumeration<URL> getResources(final String name) throws IOException {
		if (state == UNINSTALLED) {
			throw new IllegalStateException("Bundle is uninstalled");
		}

		if (currentRevision.isFragment()) {
			// bundle is fragment, return null
			return null;
		}
		if (state == INSTALLED) {
			try {
				if (!currentRevision.resolve(false)) {
					final Vector<URL> result = new Vector<URL>();

					for (int i = 0; i < currentRevision.classpath.length; i++) {
						final URL url = currentRevision
								.lookupFile(currentRevision.classpath[i], name);
						if (url != null) {
							result.add(url);
						}
					}

					return result.isEmpty() ? null : result.elements();
				}
			} catch (final BundleException e) {
				// TODO: to log
				e.printStackTrace();
				return null;
			}
		}

		return currentRevision.classloader.findResources0(name);
	}

	/**
	 * @see org.osgi.framework.Bundle#getEntryPaths(String)
	 * @category Bundle
	 */
	public Enumeration<String> getEntryPaths(final String path)
			throws IllegalStateException {
		if (state == Bundle.UNINSTALLED) {
			throw new IllegalStateException(
					"Bundle has been uninstalled. Cannot retrieve entry.");
		}
		if (framework.SECURITY_ENABLED) {
			// check AdminPermission(this,RESOURCE)
		}

		final Vector<URL> urls = currentRevision.searchFiles(null, path, "*",
				false);

		return urls.isEmpty() ? null : new Enumeration<String>() {

			final Enumeration<URL> urlEnumeration = urls.elements();

			public boolean hasMoreElements() {
				return urlEnumeration.hasMoreElements();
			}

			public String nextElement() {
				return urlEnumeration.nextElement().getFile().substring(1);
			}

		};
	}

	/**
	 * @see org.osgi.framework.Bundle#getEntry(String)
	 * @category Bundle
	 */
	public URL getEntry(final String path) throws IllegalStateException {
		if (state == Bundle.UNINSTALLED) {
			throw new IllegalStateException(
					"Bundle has been uninstalled. Cannot retrieve entry.");
		}
		if (framework.SECURITY_ENABLED) {
			// TODO: check AdminPermission(this,RESOURCE)
		}

		try {
			if ("/".equals(path)) {
				return currentRevision.createURL("/", null);
			}
			return currentRevision.lookupFile(null, path);
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @see org.osgi.framework.Bundle#findEntries(String, String, boolean)
	 * @category Bundle
	 */
	public Enumeration<URL> findEntries(final String path,
			final String filePattern, final boolean recurse) {
		// try to resolve bundle if state is installed
		if (state == Bundle.INSTALLED) {
			try {
				currentRevision.resolve(false);
			} catch (final BundleException ex) {
				// ignore and search only in this bundles jar file
			}
		}

		final Revision revision = currentRevision == null
				? (Revision) revisions.get(0) : currentRevision;
		return revision.findEntries(path, filePattern, recurse);
	}

	/**
	 * TODO: implement
	 * 
	 * @see org.osgi.framework.Bundle#getSignerCertificates(int)
	 * @category Bundle
	 */
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
			final int signersType) {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	protected boolean isSecurityEnabled() {
		return framework.isSecurityEnabled();
	}

	final InputStream getURLResource(final URL url, final int rev)
			throws IOException {
		String frag;
		try {
			frag = url.toURI().getFragment();
		} catch (final URISyntaxException e) {
			e.printStackTrace();
			frag = null;
		}

		for (final BundleRevision brevision : revisions) {
			final Revision revision = (Revision) brevision;
			if (revision.revId == rev) {
				if (frag == null) {
					return revision.retrieveFile(null, url.getPath());
				} else {
					return revision.retrieveFile(url.getPath(), frag);
				}
			}
		}

		return null;
	}

	final long getResourceLength(final URL url, final int rev) {
		String frag;
		try {
			frag = url.toURI().getFragment();
		} catch (final URISyntaxException e) {
			e.printStackTrace();
			frag = null;
		}

		try {
			for (final BundleRevision brevision : revisions) {
				final Revision revision = (Revision) brevision;
				if (revision.revId == rev) {
					if (frag == null) {
						return revision.retrieveFileLength(null, url.getPath());
					} else {
						return revision.retrieveFileLength(url.getPath(), frag);
					}
				}
			}
		} catch (final IOException e) {
		}

		return -1;
	}

	// BundleStartLevel

	/**
	 * @see org.osgi.framework.startlevel.BundleStartLevel#getStartLevel()
	 * @category BundleStartLevel
	 */
	public int getStartLevel() {
		return startlevel;
	}

	/**
	 * @see org.osgi.framework.startlevel.BundleStartLevel#isPersistentlyStarted()
	 * @category BundleStartLevel
	 */
	public boolean isPersistentlyStarted() {
		checkBundleNotUninstalled();

		return autostart != AUTOSTART_STOPPED;
	}

	/**
	 * @see org.osgi.framework.startlevel.BundleStartLevel#isActivationPolicyUsed()
	 * @category BundleStartLevel
	 */
	public boolean isActivationPolicyUsed() {
		checkBundleNotUninstalled();

		return autostart == AUTOSTART_STARTED_WITH_DECLARED;
	}

	/**
	 * @see org.osgi.framework.startlevel.BundleStartLevel#setStartLevel(int)
	 * @category BundleStartLevel
	 */
	public void setStartLevel(final int targetStartLevel) {
		checkBundleNotUninstalled();

		if (targetStartLevel <= 0) {
			throw new IllegalArgumentException("Start level " + targetStartLevel
					+ " is not a valid level");
		}

		final int oldStartlevel = startlevel;
		startlevel = targetStartLevel;

		updateMetadata();
		if (targetStartLevel <= oldStartlevel && state != Bundle.ACTIVE
				&& autostart != AbstractBundle.AUTOSTART_STOPPED) {
			final int options = isActivationPolicyUsed()
					? Bundle.START_ACTIVATION_POLICY & Bundle.START_TRANSIENT
					: Bundle.START_TRANSIENT;
			new Thread() {
				public void run() {
					try {
						activate(options);
					} catch (final BundleException be) {
						// TODO: remove debug output
						be.printStackTrace();
						framework.notifyFrameworkListeners(FrameworkEvent.ERROR,
								BundleImpl.this, be);
					}
				};
			}.start();
		} else if (targetStartLevel > oldStartlevel && state != Bundle.RESOLVED
				&& state != Bundle.INSTALLED) {
			new Thread() {
				public void run() {
					try {
						stopBundle();
					} catch (final BundleException be) {
						framework.notifyFrameworkListeners(FrameworkEvent.ERROR,
								BundleImpl.this, be);
					}
				}
			}.start();
		}
	}

	protected static String[] readProperties(final Attributes attrs,
			final String property, final String[] defaultValue)
					throws BundleException {
		final String propString = readProperty(attrs, property);
		return propString == null ? defaultValue
				: Utils.splitString(propString, ',');
	}

	protected static String readProperty(final Attributes attrs,
			final String property) throws BundleException {
		final String value = attrs.getValue(property);
		if (value != null && value.equals("")) {
			throw new BundleException(
					"Broken manifest, " + property + " is empty.",
					BundleException.MANIFEST_ERROR);
		}
		return value;
	}

	protected Properties getLocalizationFile(final Locale locale,
			final String baseDir, final String baseFile) {
		if (hostBundles != null) {
			// fragment
			return hostBundles.get(0).getLocalizationFile(locale, baseDir,
					baseFile);
		}
		final Locale[] locales = new Locale[] { lastDefaultLocale == null
				? Locale.getDefault() : lastDefaultLocale, locale };
		final Properties props = new Properties();
		final String[] choices = new String[7];
		int counter = 0;
		choices[0] = "";
		for (int i = 0; i < 2; i++) {
			choices[++counter] = "_" + locales[i].getLanguage();
			if (locales[i].getCountry().length() > 0) {
				choices[++counter] = choices[counter - 1] + "_"
						+ locales[i].getCountry();
				if (locales[i].getVariant().length() > 0) {
					choices[++counter] = choices[counter - 1] + "_"
							+ locales[i].getVariant();
				}
			}
		}

		for (int i = counter; i >= 0; i--) {
			final Enumeration<URL> urls = findEntries(baseDir,
					baseFile + choices[i] + ".properties", false);
			if (urls != null) {
				while (urls.hasMoreElements()) {
					try {
						final URL url = urls.nextElement();
						final InputStream stream = url.openStream();
						props.load(stream);
						return props;
					} catch (final IOException ioe) {
						// ignore and continue
					}
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "[" + getSymbolicName() + "-" + getVersion() + "]";
	}

	public abstract class Revision
			implements BundleRevision, Comparable<Revision> {

		protected static final int GET_URL = 0;
		protected static final int RETRIEVE_INPUT_STREAM = 1;
		protected static final int GET_CONTENT_LENGTH = 2;

		protected final int revId;
		protected final MultiMap<String, BundleCapability> capabilities;
		protected final MultiMap<String, BundleRequirement> requirements;
		private final List<HostedCapability> hostedCapabilities = new ArrayList<HostedCapability>();
		protected final List<BundleRequirement> dynamicImports;

		private BundleCapability identity;

		BundleClassLoader classloader;

		protected final String activatorClassName;
		private String[] nativeCodeStrings;
		protected BundleActivator activatorInstance;
		protected String[] classpath;
		protected Map<String, String> nativeLibraries;
		protected List<Revision> fragments;
		private final String[] classpathStrings;
		private final short fragmentAttachmentPolicy;

		protected ConciergeBundleWiring wiring;
		protected HashMap<String, BundleWire> packageImportWires;
		protected List<BundleWire> requireBundleWires;
		protected final HashSet<String> exportIndex;

		protected Revision(final int revId, final Manifest manifest,
				final String[] classpathStrings) throws BundleException {
			this.revId = revId;
			this.classpathStrings = classpathStrings;

			this.classloader = new BundleClassLoader();

			final Attributes attrs = manifest.getMainAttributes();

			// bundle manifest version
			final String mfVerStr = attrs
					.getValue(Constants.BUNDLE_MANIFESTVERSION);
			final int mfVer;
			try {
				mfVer = mfVerStr == null ? 1
						: Integer.parseInt(mfVerStr.trim());
			} catch (final NumberFormatException nfe) {
				throw new BundleException(
						"Illegal value for " + Constants.BUNDLE_MANIFESTVERSION
								+ ": `" + mfVerStr + "`",
						BundleException.MANIFEST_ERROR);
			}

			// process generic requirements and capabilities
			final String reqStr = attrs.getValue(Constants.REQUIRE_CAPABILITY);
			this.requirements = parseRequirements(reqStr);

			final String capStr = attrs.getValue(Constants.PROVIDE_CAPABILITY);
			this.capabilities = parseCapabilities(capStr);

			this.dynamicImports = new ArrayList<BundleRequirement>();

			LegacyBundleProcessing proc;

			switch (mfVer) {
			default:
				proc = framework.getService(LegacyBundleProcessing.class,
						LegacyBundleProcessing.VERSION_ONE);
				if (proc == null) {
					throw new BundleException(
							"Bundle manifest version 1 is not supported by this deployment",
							BundleException.UNSUPPORTED_OPERATION);
				}

				final Tuple<List<BundleCapability>, List<BundleRequirement>> tuple = proc
						.processManifest(this, manifest);

				for (final BundleCapability cap : tuple.getFormer()) {
					capabilities.insert(cap.getNamespace(), cap);
				}

				for (final BundleRequirement req : tuple.getLatter()) {
					final String namespace = req.getNamespace();

					requirements.insert(namespace, req);

					if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)
							&& PackageNamespace.RESOLUTION_DYNAMIC.equals(req
									.getDirectives()
									.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
						dynamicImports.add(req);
					}
				}

				break;
			case 2:
				proc = framework.getService(LegacyBundleProcessing.class,
						LegacyBundleProcessing.VERSION_TWO);
				if (proc == null) {
					throw new BundleException(
							"Bundle manifest version 2 is not supported by this deployment",
							BundleException.UNSUPPORTED_OPERATION);
				}

				final Tuple<List<BundleCapability>, List<BundleRequirement>> tuple2 = proc
						.processManifest(this, manifest);

				for (final BundleCapability cap : tuple2.getFormer()) {
					capabilities.insert(cap.getNamespace(), cap);
				}

				for (final BundleRequirement req : tuple2.getLatter()) {
					final String namespace = req.getNamespace();

					requirements.insert(namespace, req);

					if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)
							&& PackageNamespace.RESOLUTION_DYNAMIC.equals(req
									.getDirectives()
									.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
						dynamicImports.add(req);
					}
				}
			}

			// create export index
			exportIndex = createExportIndex();

			// remove dynamic imports for exported packages
			if (!dynamicImports.isEmpty()) {
				final Iterator<BundleRequirement> iter = dynamicImports
						.iterator();
				while (iter.hasNext()) {
					final BundleRequirement req = iter.next();
					if (exportIndex.contains(
							req.getDirectives().get(Concierge.DIR_INTERNAL))) {
						iter.remove();
					}
				}
			}

			// get the native libraries
			nativeCodeStrings = readProperties(attrs,
					Constants.BUNDLE_NATIVECODE, null);

			// get the activator
			activatorClassName = attrs.getValue(Constants.BUNDLE_ACTIVATOR);

			// get start_activation_policy, if any
			final String activationPolicy = readProperty(attrs,
					Constants.BUNDLE_ACTIVATIONPOLICY);
			if (activationPolicy != null) {
				final String[] literals = Utils.splitString(activationPolicy,
						';');
				if (Constants.ACTIVATION_LAZY.equals(literals[0])) {
					lazyActivation = true;
				}

				for (int i = 1; i < literals.length; i++) {
					final Matcher matcher = DIRECTIVE_LIST.matcher(literals[i]);
					if (matcher.matches()) {
						final String directive = matcher.group(1);
						final String list = matcher.group(2);
						final String[] elems = Utils.splitString(list, ',');

						if (Constants.INCLUDE_DIRECTIVE.equals(directive)) {
							activationIncludes = elems;
						} else
							if (Constants.EXCLUDE_DIRECTIVE.equals(directive)) {
							activationExcludes = elems;
						}
					}
				}

			}

			// set bundle_localization_path
			String bundleLocalizationBaseName = attrs
					.getValue(Constants.BUNDLE_LOCALIZATION);
			if (bundleLocalizationBaseName == null) {
				bundleLocalizationBaseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
			}

			final int pos = bundleLocalizationBaseName.lastIndexOf("/");
			if (pos > -1) {
				bundleLocalizationBaseDir = bundleLocalizationBaseName
						.substring(0, pos);
				bundleLocalizationBaseFilename = bundleLocalizationBaseName
						.substring(pos + 1);
			} else {
				bundleLocalizationBaseDir = "/";
				bundleLocalizationBaseFilename = bundleLocalizationBaseName;
			}

			// set the bundle headers
			final HeaderDictionary headers = new HeaderDictionary(attrs.size());
			final Object[] entries = attrs.keySet()
					.toArray(new Object[attrs.keySet().size()]);
			for (int i = 0; i < entries.length; i++) {
				headers.put(entries[i].toString(),
						attrs.get(entries[i]).toString());
			}
			BundleImpl.this.headers = headers;

			final List<BundleCapability> identities = capabilities
					.get(IdentityNamespace.IDENTITY_NAMESPACE);
			if (identities != null) {
				this.identity = capabilities
						.get(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
			}

			final List<BundleCapability> hosts = capabilities
					.get(HostNamespace.HOST_NAMESPACE);

			if (hosts == null) {
				fragmentAttachmentPolicy = FRAGMENT_ATTACHMENT_NEVER;
			} else {
				final String policy = hosts.get(0).getDirectives()
						.get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);

				if (identity == null || policy == null
						|| Constants.FRAGMENT_ATTACHMENT_ALWAYS
								.equals(policy)) {
					fragmentAttachmentPolicy = FRAGMENT_ATTACHMENT_ALWAYS;
				} else if (Constants.FRAGMENT_ATTACHMENT_RESOLVETIME
						.equals(policy)) {
					fragmentAttachmentPolicy = FRAGMENT_ATTACHMENT_RESOLVETIME;
				} else if (Constants.FRAGMENT_ATTACHMENT_NEVER.equals(policy)) {
					fragmentAttachmentPolicy = FRAGMENT_ATTACHMENT_NEVER;
				} else {
					fragmentAttachmentPolicy = FRAGMENT_ATTACHMENT_ALWAYS;
				}
			}

			if (isFragment()) {
				framework.addFragment(this);
			} else {
				framework.publishCapabilities(capabilities.getAllValues());
			}
		}

		protected void refresh() {
			if (fragments != null) {
				fragments = null;
			}

			classloader = new BundleClassLoader();
		}

		private MultiMap<String, BundleRequirement> parseRequirements(
				final String str) throws BundleException {
			final MultiMap<String, BundleRequirement> result = new MultiMap<String, BundleRequirement>();

			if (str == null) {
				return result;
			}

			final String[] reqStrs = Utils.splitString(str, ',');
			for (int i = 0; i < reqStrs.length; i++) {
				final BundleRequirementImpl req = new BundleRequirementImpl(
						this, reqStrs[i]);
				final String namespace = req.getNamespace();
				result.insert(namespace, req);
			}
			return result;
		}

		private MultiMap<String, BundleCapability> parseCapabilities(
				final String str) throws BundleException {
			final MultiMap<String, BundleCapability> result = new MultiMap<String, BundleCapability>();

			if (str == null) {
				return result;
			}

			final String[] reqStrs = Utils.splitString(str, ',');
			for (int i = 0; i < reqStrs.length; i++) {
				final BundleCapabilityImpl cap = new BundleCapabilityImpl(this,
						reqStrs[i]);
				final String namespace = cap.getNamespace();
				result.insert(namespace, cap);
			}
			return result;
		}

		private HashSet<String> createExportIndex() {
			final List<BundleCapability> packageReqs = capabilities
					.get(PackageNamespace.PACKAGE_NAMESPACE);
			final HashSet<String> index = new HashSet<String>();

			if (packageReqs != null) {
				for (final BundleCapability req : packageReqs) {
					index.add((String) req.getAttributes()
							.get(PackageNamespace.PACKAGE_NAMESPACE));
				}
			}

			return index;
		}

		public boolean isFragment() {
			return requirements.get(HostNamespace.HOST_NAMESPACE) != null;
		}

		boolean isExtensionBundle() {
			final String fragmentHostName = getFragmentHost();
			return fragmentHostName.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME)
					|| fragmentHostName
							.equals(Concierge.FRAMEWORK_SYMBOLIC_NAME);
		}

		protected boolean isCurrent() {
			return BundleImpl.this.currentRevision == this;
		}

		/**
		 * @see org.osgi.framework.BundleReference#getBundle()
		 * @category BundleReference
		 */
		public Bundle getBundle() {
			return BundleImpl.this;
		}

		/**
		 * @see org.osgi.framework.wiring.BundleRevision#getSymbolicName()
		 * @category BundleRevision
		 */
		public String getSymbolicName() {
			return identity == null ? null
					: (String) identity.getAttributes()
							.get(IdentityNamespace.IDENTITY_NAMESPACE);
		}

		/**
		 * @see org.osgi.framework.wiring.BundleRevision#getVersion()
		 * @category BundleRevision
		 */
		public Version getVersion() {
			return identity == null ? Version.emptyVersion
					: (Version) identity.getAttributes().get(
							IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		}

		/**
		 * @see org.osgi.framework.wiring.BundleRevision#getDeclaredCapabilities(java.lang.String)
		 * @BundleRevision
		 */
		public List<BundleCapability> getDeclaredCapabilities(
				final String namespace) {
			return namespace == null ? capabilities.getAllValues()
					: capabilities.lookup(namespace);
		}

		/**
		 * @see org.osgi.framework.wiring.BundleRevision#getDeclaredRequirements(java.lang.String)
		 * @category BundleRevision
		 */
		public List<BundleRequirement> getDeclaredRequirements(
				final String namespace) {
			return namespace == null ? requirements.getAllValues()
					: requirements.lookup(namespace);
		}

		/**
		 * @see org.osgi.framework.wiring.BundleRevision#getTypes()
		 * @category BundleRevision
		 */
		public int getTypes() {
			return identity == null ? 0
					: IdentityNamespace.TYPE_FRAGMENT
							.equals(identity.getAttributes()
									.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))
											? BundleRevision.TYPE_FRAGMENT : 0;
		}

		/**
		 * @see org.osgi.framework.wiring.BundleRevision#getWiring()
		 * @category BundleRevision
		 */
		public BundleWiring getWiring() {
			return wiring;
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
		 * @see org.osgi.framework.wiring.BundleRevision#getRequirements(java.lang.String)
		 * @category BundleRevision
		 */
		public List<Requirement> getRequirements(final String namespace) {
			return Collections.unmodifiableList(new ArrayList<Requirement>(
					getDeclaredRequirements(namespace)));
		}

		protected boolean resolve(final boolean critical)
				throws BundleException {
			if (!resolveMetadata(critical)) {
				return false;
			}

			if (!framework.resolve(
					Collections.<BundleRevision> singletonList(this),
					critical)) {
				return false;
			}

			if (state == Bundle.INSTALLED) {
				state = Bundle.RESOLVED;
				framework.notifyBundleListeners(BundleEvent.RESOLVED,
						BundleImpl.this);
			}
			return true;
		}

		protected boolean resolveMetadata(final boolean critical)
				throws BundleException {
			try {
				/*
				 * resolve the bundle's internal classpath. <specs>The framework
				 * must ignore missing files in the Bundle-Classpath headers.
				 * However, a Framework should publish a Framework Event of type
				 * ERROR for each file that is not found in the bundle's JAR
				 * with an appropriate message</specs>
				 */
				if (classpath == null) {
					for (int i = 0; i < classpathStrings.length; i++) {
						if (classpathStrings[i].equals(".")) {
							// '.' is always fine
							continue;
						}
						try {
							if (null == retrieveFile(null,
									classpathStrings[i])) {
								framework.notifyFrameworkListeners(
										FrameworkEvent.ERROR, BundleImpl.this,
										new BundleException(
												"Missing file in bundle classpath "
														+ classpathStrings[i],
												BundleException.RESOLVE_ERROR));
							}
						} catch (final IOException ioe) {
							framework.notifyFrameworkListeners(
									FrameworkEvent.ERROR, BundleImpl.this, ioe);
						}
					}
					classpath = classpathStrings;
				}

				// resolve native code dependencies
				if (nativeCodeStrings != null) {
					nativeLibraries = new HashMap<String, String>(
							nativeCodeStrings.length);
					if (!processNativeLibraries(nativeCodeStrings)) {
						if (critical) {
							throw new BundleException(
									"No matching native clause");
						} else {
							return false;
						}
					}
				}

				return true;
			} catch (final IllegalArgumentException iae) {
				throw new BundleException(
						"Error while resolving bundle "
								+ currentRevision.getSymbolicName(),
						BundleException.RESOLVE_ERROR, iae);
			}
		}

		protected boolean isFrameworkExtension() {
			final List<BundleRequirement> hostReqs = requirements
					.get(HostNamespace.HOST_NAMESPACE);
			if (hostReqs == null) {
				return false;
			}
			return HostNamespace.EXTENSION_FRAMEWORK
					.equals(hostReqs.get(0).getDirectives().get(
							HostNamespace.REQUIREMENT_EXTENSION_DIRECTIVE));
		}

		/**
		 * process the native libraries declarations from the manifest. Only
		 * register natives that comply with stated OS/version/languages
		 * constraints.
		 * 
		 * @param nativeStrings
		 *            the native library declarations and constraints.
		 * @param nativeLibraries
		 *            the map.
		 * @throws BundleException
		 */
		private boolean processNativeLibraries(final String[] nativeStrings)
				throws BundleException {
			int pos = -1;

			boolean n = false;
			boolean no_n = true;
			boolean l = false;
			boolean no_l = true;
			boolean v = false;
			boolean no_v = true;
			boolean p = false;
			boolean no_p = false;
			boolean s = false;
			boolean no_s = true;
			boolean hasOptional = false;
			boolean hasMatch = false;
			final List<String> libs = new ArrayList<String>();

			for (int i = 0; i < nativeStrings.length; i++) {
				if (nativeStrings[i].indexOf(";") == -1) {
					if (nativeStrings[i].equals("*")) {
						hasOptional = true;
					} else {
						nativeLibraries.put(
								(pos = nativeStrings[i].lastIndexOf("/")) > -1
										? nativeStrings[i].substring(pos + 1)
										: nativeStrings[i],
								stripLeadingSlash(nativeStrings[i]));
					}
				} else {
					final StringTokenizer tokenizer = new StringTokenizer(
							nativeStrings[i], ";");

					while (tokenizer.hasMoreTokens()) {
						final String token = tokenizer.nextToken();
						final int a = token.indexOf("=");
						if (a > -1) {
							final String criterium = token.substring(0, a)
									.trim().intern();
							// trim and remove all double quotes,
							// like in osname "mac os x" where quotes are part of string
							final String value = token.substring(a + 1).trim().replace("\"", "");;
							if (criterium == Constants.BUNDLE_NATIVECODE_OSNAME) {
								if (framework.osname.startsWith("Windows")) {
									n |= value.toLowerCase().startsWith("win");
								} else {
									n |= value
											.equalsIgnoreCase(framework.osname);
									// support aliases for "Mac OS" and
									// "Mac OS X"
									if (framework.osname.equals("MacOSX")) {
										n |= value.equalsIgnoreCase("Mac OS X");
									} else
										if (framework.osname.equals("MacOS")) {
										n |= value.equalsIgnoreCase("Mac OS");
									}
									// TODO add other alias missing in OSGi R5
									// spec Table 4.4
								}
								no_n = false;
							} else
								if (criterium == Constants.BUNDLE_NATIVECODE_OSVERSION) {
								v |= new VersionRange(Utils.unQuote(value))
										.includes(framework.osversion);
								no_v = false;
							} else if (criterium == Constants.BUNDLE_NATIVECODE_LANGUAGE) {
								l |= new Locale(value, "").getLanguage()
										.equals(framework.language);
								no_l = false;
							} else
								if (criterium == Constants.BUNDLE_NATIVECODE_PROCESSOR) {
								if (framework.processor.equals("x86")) {
									p |= value.equals("x86")
											|| value.equals("pentium")
											|| value.equals("i386")
											|| value.equals("i486")
											|| value.equals("i586")
											|| value.equals("i686");
								} else
									if (framework.processor.equals("x86-64")) {
									p |= value.equals("amd64")
											|| value.equals("em64t")
											|| value.equals("x86_64")
											|| value.equals("x86-64");
								} else if (framework.processor.equals("ppc")) {
									p |= value.equals("ppc");
								} else {
									p |= value.equalsIgnoreCase(
											framework.processor);
								}
								no_p = false;
							} else if (criterium == Constants.SELECTION_FILTER_ATTRIBUTE) {
								try {
									s |= RFC1960Filter
											.fromString(Utils.unQuote(value))
											.match(Concierge.props2Dict(
													framework.properties));
									no_s = false;
								} catch (final InvalidSyntaxException e) {
									e.printStackTrace();
								}
							}
						} else if ("*".equals(token.trim())
								&& !tokenizer.hasMoreTokens()) {
							// wildcard
							hasMatch = true;
						} else {
							libs.add(token.trim());
						}
					}
					if (!libs.isEmpty() && (no_p || p) && (no_n || n)
							&& (no_v || v) && (no_l || l) && (no_s || s)) {
						final String[] libraries = libs
								.toArray(new String[libs.size()]);
						for (int c = 0; c < libraries.length; c++) {
							nativeLibraries.put(
									(pos = libraries[c].lastIndexOf("/")) > -1
											? libraries[c].substring(pos + 1)
											: libraries[c],
									stripLeadingSlash(libraries[c]));
						}
						hasMatch = true;
					}
					p = n = v = l = s = false;
					no_p = no_n = no_v = no_l = no_s = true;
					libs.clear();
				}
			}

			return hasMatch || hasOptional;
		}

		/**
		 * @param uninstall
		 *            if false, the bundle is only prepared for an update or
		 *            refresh. If true, it is prepared for the uninstalled
		 *            state.
		 */
		void cleanup(final boolean uninstall) {
			framework.removeCapabilities(this);

			// if this is the final cleanup, remove this resource from all other
			// inUse lists
			if (currentRevision == null) {
				if (wiring != null) {
					wiring.cleanup();
					framework.wirings.remove(this);
				}
				wiring = null;
				packageImportWires = null;
				requireBundleWires = null;
				fragments = null;
			}

			if (!uninstall) {
				currentRevision = (Revision) revisions.get(0);
			}
		}

		void markResolved() {
			state = Bundle.RESOLVED;
			framework.notifyBundleListeners(BundleEvent.RESOLVED,
					BundleImpl.this);
		}

		void setWiring(final ConciergeBundleWiring wiring) {
			this.wiring = wiring;
			packageImportWires = wiring.getPackageImportWires();
			requireBundleWires = wiring.getRequireBundleWires();
		}

		ConciergeBundleWiring addAdditionalWires(final List<Wire> wires) {
			for (final Wire wire : wires) {
				wiring.addWire((BundleWire) wire);
			}

			packageImportWires = wiring.getPackageImportWires();
			requireBundleWires = wiring.getRequireBundleWires();
			return wiring;
		}

		String getFragmentHost() {
			final List<BundleRequirement> hostReqs = requirements
					.get(HostNamespace.HOST_NAMESPACE);
			if (hostReqs == null) {
				return null;
			}

			return hostReqs.get(0).getDirectives()
					.get(HostNamespace.HOST_NAMESPACE);
		}

		final boolean allowsFragmentAttachment() {
			return fragmentAttachmentPolicy != FRAGMENT_ATTACHMENT_NEVER;
		}

		List<Revision> getAttachedFragments() {
			return fragments;
		}

		/**
		 * 
		 * @param fragment
		 * @return false if the fragment was already attached.
		 * @throws BundleException
		 */
		boolean attachFragment(final Revision fragment) throws BundleException {
			if (fragments != null) {
				if (fragments.contains(fragment)) {
					return false;
				}
			}

			if (state == Bundle.ACTIVE || state == Bundle.STARTING) {
				// attaching fragment at runtime
				// test if host allows attaching at runtime:
				if (fragmentAttachmentPolicy == FRAGMENT_ATTACHMENT_RESOLVETIME) {
					throw new BundleException(
							"Host bundle does not allow to attach fragment at runtime",
							BundleException.RESOLVE_ERROR);
				}

				final List<Requirement> imports = fragment
						.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
				final Set<String> importPkgs = new HashSet<String>();
				for (final Requirement pkgImport : imports) {
					importPkgs.add(pkgImport.getDirectives()
							.get(Concierge.DIR_INTERNAL));
				}
				importPkgs.remove("org.osgi.framework");

				if (!importPkgs.isEmpty()) {
					final List<BundleRequirement> wiredImports = wiring
							.getRequirements(
									PackageNamespace.PACKAGE_NAMESPACE);
					for (final Requirement wiredImport : wiredImports) {
						importPkgs.remove(wiredImport.getDirectives()
								.get(Concierge.DIR_INTERNAL));
					}

					if (!importPkgs.isEmpty()) {
						// bundle needs to be resolved anew if this fragment
						// would be attached
						throw new BundleException(
								"Imports of this Fragment are not satisfiable without restart of the host bundle",
								BundleException.RESOLVE_ERROR);
					}
				}

				// test for require bundles
				if (!fragment.getRequirements(BundleNamespace.BUNDLE_NAMESPACE)
						.isEmpty()) {
					throw new BundleException(
							"Fragment must not add new require bundle entries",
							BundleException.RESOLVE_ERROR);
				}
			}

			final List<String> newClasspaths = new ArrayList<String>(
					classpath != null ? classpath.length : 0);
			if (classpath != null) {
				for (int k = 0; k < classpath.length; k++) {
					newClasspaths.add(classpath[k]);
				}
			}

			// prepare the attachment

			// imports
			final List<BundleRequirement> newImports = fragment.requirements
					.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (newImports != null) {
				checkConflicts(newImports,
						requirements.get(PackageNamespace.PACKAGE_NAMESPACE),
						PackageNamespace.PACKAGE_NAMESPACE, "import");
			}

			// require bundles
			final List<BundleRequirement> newRequiredBundle = fragment.requirements
					.get(BundleNamespace.BUNDLE_NAMESPACE);
			if (newRequiredBundle != null) {
				checkConflicts(newRequiredBundle,
						requirements.get(BundleNamespace.BUNDLE_NAMESPACE),
						BundleNamespace.BUNDLE_NAMESPACE, "requireBundle");
			}

			// native code
			final String[] newNativeStrings;
			if (fragment.nativeCodeStrings == null) {
				newNativeStrings = nativeCodeStrings;
			} else {
				final ArrayList<String> temp = new ArrayList<String>();
				if (nativeCodeStrings != null) {
					temp.addAll(Arrays.asList(nativeCodeStrings));
				}
				temp.addAll(Arrays.asList(fragment.nativeCodeStrings));
				newNativeStrings = temp.toArray(new String[temp.size()]);
			}

			// commit the changes
			final BundleImpl fragmentBundle = (BundleImpl) fragment.getBundle();

			if (newImports != null) {
				requirements.insertAll(PackageNamespace.PACKAGE_NAMESPACE,
						newImports);
			}

			if (newRequiredBundle != null) {
				requirements.insertAll(BundleNamespace.BUNDLE_NAMESPACE,
						newRequiredBundle);
			}

			final List<Capability> newExports = fragment
					.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
			if (newExports != null) {
				for (final Capability cap : newExports) {
					exportIndex.add((String) cap.getAttributes()
							.get(PackageNamespace.PACKAGE_NAMESPACE));
				}
			}

			// add fragment to fragments-array
			if (fragments == null) {
				fragments = new ArrayList<Revision>();
			}
			fragments.add(fragment);
			Collections.sort(fragments);

			// register the host
			if (fragmentBundle.hostBundles == null) {
				fragmentBundle.hostBundles = new ArrayList<BundleImpl>();
			}
			fragmentBundle.hostBundles.add(BundleImpl.this);

			if (fragmentBundle.state != RESOLVED) {
				fragment.wiring = new ConciergeBundleWiring(fragment, null);
				fragmentBundle.state = RESOLVED;
				framework.notifyBundleListeners(BundleEvent.RESOLVED,
						fragmentBundle);
			}

			// add classpath
			for (int n = 0; n < fragment.classpathStrings.length; n++) {
				if (!newClasspaths.contains(fragment.classpathStrings[n])) {
					newClasspaths.add(fragment.classpathStrings[n]);
				}
			}
			if (newClasspaths.size() > 0) {
				classpath = newClasspaths
						.toArray(new String[newClasspaths.size()]);
			}

			// add native code
			nativeCodeStrings = newNativeStrings;
			if (nativeCodeStrings != null) {
				nativeLibraries = new HashMap<String, String>(
						nativeCodeStrings.length);
				processNativeLibraries(newNativeStrings);
			}

			return true;
		}

		private <T extends Requirement> void checkConflicts(final List<T> list1,
				final List<T> list2, final String namespace, final String s)
						throws BundleException {
			if (list1 == null) {
				throw new IllegalArgumentException("list1 == null");
			}
			if (list2 == null) {
				return;
			}

			final int list1size = list1.size();
			final int list2size = list2.size();
			if (list1size == 0 || list2size == 0) {
				return;
			}

			final List<T> shorter;
			final List<T> longer;
			if (list1size > list2size) {
				shorter = list2;
				longer = list1;
			} else {
				shorter = list1;
				longer = list2;
			}

			// remember element and not only attribute as string
			final Map<String, T> index = new HashMap<String, T>();
			for (final T element : longer) {
				final String attr = element.getDirectives()
						.get(Concierge.DIR_INTERNAL);
				if (attr != null) {
					index.put(attr, element);
				}
			}

			for (final T element : shorter) {
				final String attr = element.getDirectives()
						.get(Concierge.DIR_INTERNAL);
				if (attr != null && index.containsKey(attr)) {
					// JR: temporarily disabled due to TCK regression
					//
					// T element2 = index.get(attr);
					// check if directives are equals
					// TODO must compare version statements including semantics
					// if (!element.getDirectives().equals(
					// element2.getDirectives())) {
					throw new BundleException(
							"Conflicting " + s + " statement "
									+ element.getAttributes().get(namespace)
									+ " from " + element,
							BundleException.RESOLVE_ERROR);
					// }
				}
			}
		}

		protected Object checkActivationChain(final Object result) {
			final ArrayList<AbstractBundle> activationList = activationChain
					.get();
			if (activationList != null && activationList.size() > 0
					&& activationList.get(0) == BundleImpl.this) {
				activationChain.set(new ArrayList<AbstractBundle>());
				for (int i = activationList.size() - 1; i >= 0; i--) {
					final BundleImpl toActivate = (BundleImpl) activationList
							.get(i);
					if (toActivate.beingLazy) {
						toActivate.triggerActivation();
					}
				}
				activationChain.set(null);
			}
			return result;
		}

		/**
		 * get a string representation of the object.
		 * 
		 * @return a string.
		 * @see java.lang.Object#toString()
		 * @category Object
		 */
		public String toString() {
			return "[Revision " + revId + " of " + BundleImpl.this + "]";
		}

		protected Enumeration<URL> findEntries(final String path,
				final String filePattern, final boolean recurse) {
			final Vector<URL> result = searchFiles(null, path, filePattern,
					recurse);

			// get results from fragments:
			if (fragments != null) {
				for (final Revision fragment : fragments) {
					final Vector<URL> fragResult = fragment.searchFiles(null,
							path, filePattern, recurse);
					result.addAll(fragResult);
				}
			}

			return result.isEmpty() ? null : result.elements();
		}

		protected abstract URL lookupFile(final String classpath,
				final String filename) throws IOException;

		protected abstract URL lookupFile(final String classpath,
				final String filename, final HashSet<String> visited)
						throws IOException;

		protected abstract Vector<URL> searchFiles(final String classpath,
				final String path, final String filePattern, boolean recurse);

		protected abstract InputStream retrieveFile(final String classpath,
				final String filename) throws IOException;

		protected abstract long retrieveFileLength(final String classpath,
				final String filename) throws IOException;

		protected abstract void close() throws IOException;

		URL createURL(final String name1, final String fragment)
				throws MalformedURLException {
			final String name = name1.replace('\\', '/');

			return new URL("bundle", bundleId + "." + revId,
					(name.charAt(0) == '/' ? name : "/" + name)
							+ (fragment == null ? "" : "#" + fragment));
		}

		class BundleClassLoader extends ClassLoader implements BundleReference {

			public BundleClassLoader() {
				// set Concierge Classloader as parent of BundleClassLoader
				// super(Concierge.class.getClassLoader());
				super(framework.parentClassLoader);
			}

			/**
			 * 
			 * @see java.lang.ClassLoader#loadClass(java.lang.String)
			 * @category ClassLoader
			 */
			public final Class<?> loadClass(final String name)
					throws ClassNotFoundException {
				return findClass(name);
			}

			/**
			 * find a class.
			 * 
			 * @param classname
			 *            the name of the class.
			 * @return the <code>Class</code> object, if the class could be
			 *         found.
			 * @throws ClassNotFoundException
			 *             if the class could not be found.
			 * @see java.lang.ClassLoader#findClass(java.lang.String)
			 * @category ClassLoader
			 * 
			 */
			protected final Class<?> findClass(final String name)
					throws ClassNotFoundException {
				final Class<?> result = (Class<?>) findResource0(
						packageOf(name), name, true, false);
				if (result == null) {
					throw new ClassNotFoundException(name);
				}
				return result;
			}

			@Override
			public Enumeration<URL> getResources(final String name) {
				return findResources(name);
			}

			/**
			 * 
			 * @see java.lang.ClassLoader#getResource(java.lang.String)
			 * @category ClassLoader
			 */
			public URL getResource(final String name) {
				return findResource(name);
			}

			/**
			 * @see org.osgi.framework.BundleReference#getBundle()
			 * @category BundleReference
			 */
			public Bundle getBundle() {
				return BundleImpl.this;
			}

			/**
			 * find a single resource.
			 * 
			 * @param filename
			 *            the name of the resource.
			 * @return the URL to the resource.
			 * @see java.lang.ClassLoader#findResource(java.lang.String)
			 * @category ClassLoader
			 * 
			 */
			protected URL findResource(final String name) {
				final String strippedName = stripLeadingSlash(name);
				try {
					return (URL) findResource0(
							packageOf(pseudoClassname(strippedName)),
							strippedName, false, false);
				} catch (final ClassNotFoundException e) {
					// does not happen
					e.printStackTrace();
					return null;
				}
			}

			/**
			 * find multiple resources.
			 * 
			 * @param filename
			 *            the name of the resource.
			 * @return an <code>Enumeration</code> over <code>URL</code>
			 *         objects.
			 * @see java.lang.ClassLoader#findResources(java.lang.String)
			 * @category ClassLoader
			 */
			protected Enumeration<URL> findResources(final String name) {
				final Enumeration<URL> result = findResources0(name);
				return result == null
						? Collections.enumeration(Collections.<URL> emptyList())
						: result;
			}

			protected Enumeration<URL> findResources0(final String name) {
				final String strippedName = stripLeadingSlash(name);
				try {
					@SuppressWarnings("unchecked")
					final Vector<URL> results = (Vector<URL>) findResource0(
							packageOf(pseudoClassname(strippedName)),
							strippedName, false, true);
					return results == null || results.isEmpty() ? null
							: results.elements();
				} catch (final ClassNotFoundException e) {
					// does not happen
					e.printStackTrace();
					return null;
				}
			}

			/**
			 * 
			 * @param pkg
			 * @param name
			 * @param isClass
			 * @param multiple
			 * @return
			 * @throws ClassNotFoundException
			 */
			@SuppressWarnings("null")
			private synchronized Object findResource0(final String pkg,
					final String name, final boolean isClass,
					final boolean multiple) throws ClassNotFoundException {
				final Vector<URL> resources = multiple ? new Vector<URL>()
						: null;

				// is the bundle uninstalled?
				if (state == Bundle.UNINSTALLED) {
					throw new IllegalStateException("Cannot "
							+ (isClass ? "load class" : "find resource")
							+ ", bundle " + getSymbolicName()
							+ " has been uninstalled.");
				}

				// try to resolve bundle if state is installed
				if (state == Bundle.INSTALLED) {
					try {
						resolve(true);
					} catch (final BundleException be) {
						if (isClass) {
							framework.notifyFrameworkListeners(
									FrameworkEvent.ERROR, BundleImpl.this, be);
							throw new ClassNotFoundException(name, be);
						}
					}
				}

				// Step 1: delegate java.* to the parent class loader
				// Step 2: delegate org.osgi.framework.bootdelegation to the
				// parent class loader
				if (pkg.startsWith("java.") || framework.bootdelegation(pkg)) {
					if (isClass) {
						return getParent().loadClass(name);
					} else {
						if (multiple) {
							try {
								final Enumeration<URL> e = getParent()
										.getResources(name);
								while (e.hasMoreElements()) {
									resources.add(e.nextElement());
								}
							} catch (final IOException ioe) {
								// nothing we can do about it
							}
						} else {
							return getParent().getResource(name);
						}
					}
				}

				// Step 3: if wires exist, check if the resource is imported
				if (wiring != null) {
					final BundleWire delegation = packageImportWires.get(pkg);
					if (delegation != null) {

						final BundleCapabilityImpl cap = (BundleCapabilityImpl) delegation
								.getCapability();
						if (!cap.hasExcludes() || cap.filter(classOf(name))) {
							if (delegation.getProvider().getBundle()
									.getBundleId() == 0) {
								// system bundle
								if (isClass) {
									return framework.systemBundleClassLoader
											.loadClass(name);
								} else {
									if (multiple) {
										try {
											final Enumeration<URL> e = framework.systemBundleClassLoader
													.getResources(name);
											while (e.hasMoreElements()) {
												resources.add(e.nextElement());
											}
										} catch (final IOException ioe) {
											// nothing we can do about it
											// FIXME: to log
										}
									} else {
										return framework.systemBundleClassLoader
												.getResource(name);
									}
								}
							} else {
								return ((Revision) delegation
										.getProvider()).classloader
												.findResource1(pkg, name,
														isClass, multiple,
														resources);
							}
						}
					}
				}
				return findResource1(pkg, name, isClass, multiple, resources);
			}

			/**
			 * 
			 * @param pkg
			 * @param name
			 * @param isClass
			 * @param multiple
			 * @param resources
			 * @return
			 * @throws ClassNotFoundException
			 */
			private synchronized Object findResource1(final String pkg,
					final String name, final boolean isClass,
					final boolean multiple, final Vector<URL> resources)
							throws ClassNotFoundException {
				// trigger lazy activation if required
				if (isClass && lazyActivation && getState() == Bundle.STARTING
						&& checkActivation(pkg)) {
					ArrayList<AbstractBundle> activationList = activationChain
							.get();
					if (activationList == null) {
						activationList = new ArrayList<AbstractBundle>();
						activationList.add(BundleImpl.this);
						activationChain.set(activationList);
					} else if (!activationList.isEmpty()
							&& !activationList.contains(BundleImpl.this)) {
						activationList.add(BundleImpl.this);
					}
				}

				// Step 4: check required bundles, depth first
				if (requireBundleWires != null) {
					final HashSet<Bundle> visited = new HashSet<Bundle>();
					visited.add(BundleImpl.this);
					for (final BundleWire wire : requireBundleWires) {
						if (wire.getProvider().getBundle().getBundleId() == 0) {
							// if provider is system bundle: nothing
							// to do as system bundle is already loaded
						} else {
							final Object result = ((Revision) wire
									.getProvider()).classloader
											.requireBundleLookup(pkg, name,
													isClass, multiple,
													resources, visited);
							if (!multiple && result != null) {
								return isClass ? checkActivationChain(result)
										: result;
							}
						}
					}
				}

				// Step 5: search the bundle class path
				// Step 6: search fragments bundle class path
				if (isClass) {
					final Class<?> clazz = findOwnClass(name);
					if (clazz != null) {
						return checkActivationChain(clazz);
					}
				} else {
					final Object result = findOwnResources(stripLeadingSlash(name),
							true, multiple, resources);
					if (!multiple && result != null) {
						return result;
					}
				}

				// Step 7: if the package is exported, fail
				if (exportIndex.contains(pkg)) {
					return null;
				}

				// Step 8: check dynamic imports
				if (!dynamicImports.isEmpty()) {

					for (final Iterator<BundleRequirement> iter = dynamicImports
							.iterator(); iter.hasNext();) {
						final BundleRequirement dynImport = iter.next();

						// TODO: think of something better
						final String dynImportPackage = dynImport
								.getDirectives().get(Concierge.DIR_INTERNAL);

						// TODO: first check if dynImport could apply to the
						// requested package!!!
						if (RFC1960Filter.stringCompare(
								dynImportPackage.toCharArray(), 0,
								pkg.toCharArray(), 0) != 0) {
							continue;
						}

						final boolean wildcard = Namespace.CARDINALITY_MULTIPLE
								.equals(dynImport.getDirectives().get(
										Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE));
						List<BundleCapability> matches;
						matches = framework.resolveDynamic(Revision.this, pkg,
								dynImportPackage, dynImport, wildcard);
						if (matches != null && matches.size() > 0) {
							final BundleCapability bundleCap = matches.get(0);

							final BundleWire wire = new ConciergeBundleWire(
									bundleCap, dynImport);
							if (wiring == null) {
								setWiring(new ConciergeBundleWiring(
										Revision.this, null));
							}
							wiring.addWire(wire);

							((ConciergeBundleWiring) bundleCap.getRevision()
									.getWiring()).addWire(wire);

							packageImportWires.put(
									(String) bundleCap.getAttributes()
											.get(PackageNamespace.PACKAGE_NAMESPACE),
									wire);

							if (!wildcard) {
								// FIXME:
								iter.remove();
							}

							final BundleRevision rev = bundleCap.getRevision();
							if (!(rev instanceof Revision)) {
								if (isClass) {
									return framework.systemBundleClassLoader
											.loadClass(name);
								} else {
									if (multiple) {
										try {
											final Enumeration<URL> e = framework.systemBundleClassLoader
													.getResources(name);
											while (e.hasMoreElements()) {
												resources.add(e.nextElement());
											}
										} catch (final IOException ioe) {
											// nothing we can do about it
											// FIXME: to log
										}
									} else {
										return framework.systemBundleClassLoader
												.getResource(name);
									}
								}
							} else {
								return ((Revision) bundleCap
										.getRevision()).classloader
												.findResource1(pkg, name,
														isClass, multiple,
														resources);
							}
						}
					}
				}

				// convenience for resources: delegate to boot class path as
				// final fallback
				if ("".equals(pkg) && !isClass && !multiple) {
					return getParent().getResource(name);
				}

				return resources;
			}

			Set<String> listResources(final String path,
					final String filePattern, final int options,
					final HashSet<String> visited) {
				final String pkg = pseudoClassname(
						stripLeadingSlash(path.endsWith("/")
								? path.substring(0, path.length() - 1) : path));

				final HashSet<String> result = new HashSet<String>();

				if (wiring != null) {
					if ((options & BundleWiring.LISTRESOURCES_RECURSE) != 0) {
						for (final Map.Entry<String, BundleWire> entry : packageImportWires
								.entrySet()) {
							final String importPackage = entry.getKey();
							if (importPackage.startsWith(pkg)) {
								final BundleWire delegation = entry.getValue();
								final BundleCapabilityImpl cap = (BundleCapabilityImpl) delegation
										.getCapability();
								if (!cap.hasExcludes()
										|| cap.filter(classOf(filePattern))) {

									// if LISTRESOURCES_LOCAL not set : add to
									// results
									if ((options
											& BundleWiring.LISTRESOURCES_LOCAL) == 0) {
										result.addAll(((Revision) delegation
												.getProvider()).classloader
														.listResources(
																importPackage
																		.replace(
																				'.',
																				'/'),
																filePattern,
																options,
																visited));
									}
									// always search imports to ignore
									// overridden packages from imports
									visited.add(importPackage);
								}
							}
						}

					} else {
						final BundleWire delegation = packageImportWires
								.get(pkg);
						if (delegation != null) {
							final BundleCapabilityImpl cap = (BundleCapabilityImpl) delegation
									.getCapability();
							if (!cap.hasExcludes()
									|| cap.filter(classOf(filePattern))) {
								if ((options
										& BundleWiring.LISTRESOURCES_LOCAL) == 0) {
									result.addAll(((Revision) delegation
											.getProvider()).classloader
													.listResources(path,
															filePattern,
															options, visited));
								}
								visited.add(pkg);
							}
						}
					}

					if (requireBundleWires != null && (options
							& BundleWiring.LISTRESOURCES_LOCAL) == 0) {
						for (final BundleWire wire : requireBundleWires) {
							result.addAll(
									((Revision) wire.getProvider()).classloader
											.listResources(path, filePattern,
													options,
													new HashSet<String>()));
						}
					}

					// Step 5: search the bundle class path
					// Step 6: search fragments bundle class path
					// if (!visited.isEmpty()) {
					result.addAll(listOwnResources(path, filePattern, options,
							visited));
				}
				return new HashSet<String>(result);

			}

			/**
			 * find a native code library.
			 * 
			 * @param libname
			 *            the name of the library.
			 * @return the String of a path name to the library or
			 *         <code>null</code> .
			 * @see java.lang.ClassLoader#findLibrary(java.lang.String)
			 * @category ClassLoader
			 */
			protected String findLibrary(final String libname) {
				if (nativeLibraries == null) {
					throw new UnsatisfiedLinkError(libname);
				}

				String lib = null;
				final String[] libnames = framework.getLibraryName(libname);
				for (int i = 0; i < libnames.length; i++) {
					lib = nativeLibraries.get(libnames[i]);
					if (lib != null) {
						break;
					}
				}

				if (framework.DEBUG_CLASSLOADING) {
					framework.logger.log(LogService.LOG_DEBUG,
							"Requested " + libname);
					framework.logger.log(LogService.LOG_INFO,
							"Native libraries " + nativeLibraries);
				}

				if (lib == null) {
					throw new UnsatisfiedLinkError(libname);
				}

				try {
					final File libfile = new File(storageLocation + "lib", lib);
					/*
					 * If a native library already exists by that name the newer
					 * library in the bundle will not be stored on disc
					 */
					// if (!libfile.exists()) {
					final URL url = (URL) findOwnResources(lib, true, false,
							null);
					storeFile(libfile, url.openStream());

					framework.execPermission(libfile);

					// }
					return libfile.getAbsolutePath();
				} catch (final IOException ioe) {
					ioe.printStackTrace();
				}
				return null;
			}

			/**
			 * Find a class in the bundle scope.
			 * 
			 * @param classname
			 *            the name of the class.
			 * @return the <code>Class</code> object if the class could be
			 *         found. <code>null</code> otherwise.
			 */
			private synchronized Class<?> findOwnClass(final String classname) {
				final Class<?> clazz;
				if (dexClassLoader != null) {
					clazz = findDexClass(classname);
				} else {
					clazz = findLoadedClass(classname);
				}
				if (clazz != null) {
					definePackage(packageOf(classname));
					return clazz;
				}
				try {
					final String filename = classToFile(classname);
					for (int i = 0; i < classpath.length; i++) {
						final InputStream input = retrieveFile(classpath[i],
								filename);
						if (input == null) {
							continue;
						}
						try {
							int len;
							final ByteArrayOutputStream out = new ByteArrayOutputStream();
							final BufferedInputStream bis = new BufferedInputStream(
									input);
							final byte[] chunk = new byte[Concierge.CLASSLOADER_BUFFER_SIZE];
							while ((len = bis.read(chunk, 0,
									Concierge.CLASSLOADER_BUFFER_SIZE)) > 0) {
								out.write(chunk, 0, len);
							}

							byte[] bytes = out.toByteArray();

							// call weaving hooks here
							if (framework.hasWeavingHooks()) {
								final WovenClassImpl wovenClass = new WovenClassImpl(
										classname, bytes, Revision.this,
										domain);
								framework.callWeavingHooks(wovenClass);
								bytes = wovenClass.getBytes();

								requirements.insertAll(
										PackageNamespace.PACKAGE_NAMESPACE,
										wovenClass.dynamicImportRequirements);
								dynamicImports.addAll(
										wovenClass.dynamicImportRequirements);

								// define package
								definePackage(packageOf(classname));
								final Class<?> ownClazz = defineClass(classname,
										bytes, 0, bytes.length, domain);

								wovenClass.setDefinedClass(ownClazz);
								wovenClass.setProtectionDomain(
										ownClazz.getProtectionDomain());

								return ownClazz;
							}

							// define package
							definePackage(packageOf(classname));
							return defineClass(classname, bytes, 0,
									bytes.length, domain);
						} catch (final IOException ioe) {
							ioe.printStackTrace();
							return null;
						} catch (final LinkageError le) {
							if (framework.DEBUG_CLASSLOADING) {
								framework.logger
										.log(LogService.LOG_DEBUG,
												"Error during loading class="
														+ classname
														+ " from bundle="
														+ this.getBundle()
																.getSymbolicName(),
												le);
							}
							throw le;
						}
					}

					if (fragments != null) {
						for (final Revision fragment : fragments) {
							for (int i = 0; i < classpath.length; i++) {
								final InputStream input = fragment
										.retrieveFile(classpath[i], filename);
								if (input == null) {
									continue;
								}
								try {
									int len;
									final ByteArrayOutputStream out = new ByteArrayOutputStream();
									final BufferedInputStream bis = new BufferedInputStream(
											input);
									final byte[] chunk = new byte[Concierge.CLASSLOADER_BUFFER_SIZE];
									while ((len = bis.read(chunk, 0,
											Concierge.CLASSLOADER_BUFFER_SIZE)) > 0) {
										out.write(chunk, 0, len);
									}

									// fixes https://github.com/eclipse/concierge/issues/48
									// define package
									definePackage(packageOf(classname));
									return defineClass(classname,
											out.toByteArray(), 0, out.size(),
											((AbstractBundle) fragment
													.getBundle()).domain);
								} catch (final IOException ioe) {
									ioe.printStackTrace();
									return null;
								} catch (final LinkageError le) {
									if (framework.DEBUG_CLASSLOADING) {
										framework.logger.log(
												LogService.LOG_DEBUG,
												"Error during loading class="
														+ classname
														+ " from bundle="
														+ this.getBundle()
																.getSymbolicName(),
												le);
									}
									throw le;
								}
							}
						}
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			/**
			 * find a class from .dex embedded in the bundle when running on
			 * Android
			 */
			private Object dexFile = null;

			private Class<?> findDexClass(final String classname) {
				try {
					if (dexFile == null) {
						final String fileName = storageLocation
								+ BUNDLE_FILE_NAME + revId;
						dexFile = dexFileLoader.invoke(null,
								new Object[] { fileName,
										storageLocation + "classes.dex",
										new Integer(0) });
					}

					if (dexFile != null) {
						return (Class<?>) dexClassLoader
								.invoke(dexFile,
										new Object[] {
												classname.replace('.', '/'),
												this });
					}
				} catch (final Exception e) {
					return null;
				}
				return null;
			}

			List<String> listOwnResources(final String path,
					final String filePattern, final int options,
					final HashSet<String> visited) {

				final List<String> result = new ArrayList<String>();
				for (int i = 0; i < classpath.length; i++) {
					final Vector<URL> urls = searchFiles(classpath[i], path,
							filePattern, (options
									& BundleWiring.LISTRESOURCES_RECURSE) != 0);
					if (urls != null) {
						for (final URL url : urls) {
							final int pos = url.getPath().lastIndexOf('/');
							final String pkg = pseudoClassname(
									url.getPath().substring(1, pos));
							if (!visited.contains(pkg)) {
								result.add(url.getPath().substring(1));
							}
						}
					}
				}

				if (fragments != null) {
					for (final Revision fragment : fragments) {
						for (int i = 0; i < classpath.length; i++) {
							final Vector<URL> urls = fragment.searchFiles(
									classpath[i], path, filePattern, (options
											& BundleWiring.LISTRESOURCES_RECURSE) != 0);
							if (urls != null) {
								for (final URL url : urls) {
									final int pos = url.getPath()
											.lastIndexOf('/');
									final String pkg = pseudoClassname(
											url.getPath().substring(1, pos));
									if (!visited.contains(pkg)) {
										result.add(url.getPath().substring(1));
									}
								}
							}
						}
					}
				}

				return result;
			}

			/**
			 * find one or more resources in the scope of the own class loader.
			 * 
			 * @param name
			 *            the name of the resource
			 * @param multiple
			 *            if false, the search terminates if the first result
			 *            has been found.
			 * @return a <code>Vector</code> of <code>URL</code> elements.
			 */
			Object findOwnResources(final String name,
					final boolean useFragments, final boolean multiple,
					final Vector<URL> resources) {
				if ("".equals(name)) {
					return resources;
				}
				final Vector<URL> results = resources == null
						? new Vector<URL>() : resources;
				try {
					for (int i = 0; i < classpath.length; i++) {
						final URL url = lookupFile(classpath[i], name);
						if (url != null) {
							if (!multiple) {
								return url;
							} else {
								results.add(url);
							}
						}
					}

					if (useFragments && fragments != null) {
						// look in fragments
						for (final Revision fragment : fragments) {
							if (fragment == null) {
								throw new IllegalStateException(
										"REVISION IS NULL");
							}

							for (int i = 0; i < classpath.length; i++) {
								final URL url = fragment
										.lookupFile(classpath[i], name);
								if (url != null) {
									if (!multiple) {
										return url;
									} else {
										results.add(url);
									}
								}
							}
						}
					}
				} catch (final IOException ioe) {
					ioe.printStackTrace();
				}
				return results.isEmpty() ? resources : results;
			}

			/**
			 * 
			 * @param pkg
			 * @param name
			 * @param isClass
			 * @param multiple
			 * @param resources
			 * @param visited
			 * @return
			 */
			@SuppressWarnings("unchecked")
			private Object requireBundleLookup(final String pkg,
					final String name, final boolean isClass,
					final boolean multiple, final Vector<URL> resources,
					final Set<Bundle> visited) throws ClassNotFoundException {
				if (visited.contains(BundleImpl.this)) {
					return null;
				}

				// depth-first: descent into re-exports
				visited.add(BundleImpl.this);
				if (requireBundleWires != null) {
					for (final BundleWire wire : requireBundleWires) {
						if (BundleNamespace.VISIBILITY_REEXPORT.equals(wire
								.getRequirement().getDirectives()
								.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE))) {
							final Object result = ((Revision) wire
									.getProvider()).classloader
											.requireBundleLookup(pkg, name,
													isClass, multiple,
													resources, visited);
							if (!multiple && result != null) {
								return result;
							} else if (result != null) {
								resources.addAll((Vector<URL>) result);
							}
						}
					}
				}

				if (exportIndex.contains(pkg)) {
					// could be delegated when the export was imported as well,
					// so check packageImportWires first
					BundleClassLoader exportLoader;
					final BundleWire delegation = packageImportWires.get(pkg);
					if (delegation != null) {
						exportLoader = ((Revision) delegation
								.getProvider()).classloader;
					} else {
						exportLoader = this;
					}

					if (isClass) {
						return exportLoader.findOwnClass(name);
					} else {
						final Object result = exportLoader.findOwnResources(
								name, true, multiple, resources);
						if (!multiple) {
							return result;
						} else if (result != null) {
							resources.addAll((Vector<URL>) result);
						}
					}
				}

				// instead of inspecting exportIndex and imports, just delegate
				// findResources?
				// return findResource0(pkg, name, isClass, multiple);

				return null;
			}

			private void definePackage(final String pkg) {
				// TODO fill in version/spec/vendor attributes according to
				// bundle manifest headers?
				try {
					definePackage(pkg, null, null, null, null, null, null,
							null);
				} catch (final IllegalArgumentException e) {
					// ignore
				}
			}
		}

		class WovenClassImpl implements WovenClass {

			private final String clazzName;

			private byte[] bytes;

			private boolean weavingComplete;

			private Class<?> clazz;

			private List<String> dynamicImports;

			protected List<BundleRequirement> dynamicImportRequirements;

			private ProtectionDomain domain;

			WovenClassImpl(final String clazzName, final byte[] bytes,
					final Revision revision, final ProtectionDomain domain) {
				this.bytes = bytes;
				this.clazzName = clazzName;
				this.dynamicImportRequirements = new ArrayList<BundleRequirement>();
				this.dynamicImports = new ArrayList<String>() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 975783807443126126L;

					@Override
					public boolean add(final String dynImport) {
						checkDynamicImport(dynImport);

						return super.add(dynImport);
					}

					@Override
					public boolean addAll(
							final Collection<? extends String> c) {
						for (final String dynImport : c) {
							checkDynamicImport(dynImport);
						}

						return super.addAll(c);
					}

					private void checkDynamicImport(final String dynImport)
							throws IllegalArgumentException {
						try {
							final String[] literals = Utils
									.splitString(dynImport, ';');

							if (literals[0].contains(";")) {
								throw new IllegalArgumentException(dynImport);
							}

							final ParseResult parseResult = Utils
									.parseLiterals(literals, 1);
							final HashMap<String, String> dirs = parseResult
									.getDirectives();

							dirs.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE,
									Utils.createFilter(
											PackageNamespace.PACKAGE_NAMESPACE,
											literals[0],
											parseResult.getLatter()));
							dirs.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE,
									PackageNamespace.RESOLUTION_DYNAMIC);

							dirs.put(Concierge.DIR_INTERNAL, literals[0]);

							final BundleRequirement req = new BundleRequirementImpl(
									revision,
									PackageNamespace.PACKAGE_NAMESPACE, dirs,
									null, Constants.DYNAMICIMPORT_PACKAGE + ' '
											+ dynImport);

							dynamicImportRequirements.add(req);
						} catch (final BundleException be) {
							throw new IllegalArgumentException(
									"Unvalid dynamic import " + dynImport);
						}
					}

				};
				this.domain = domain;
			}

			public byte[] getBytes() {
				return bytes;
			}

			public void setBytes(final byte[] newBytes) {
				if (newBytes == null) {
					throw new NullPointerException("newBytes");
				}
				if (weavingComplete) {
					throw new IllegalStateException("Weaving is complete");
				}

				bytes = newBytes;
			}

			public List<String> getDynamicImports() {
				return dynamicImports;
			}

			public boolean isWeavingComplete() {
				return weavingComplete;
			}

			public String getClassName() {
				return clazzName;
			}

			public ProtectionDomain getProtectionDomain() {
				return domain;
			}

			public Class<?> getDefinedClass() {
				return clazz;
			}

			public BundleWiring getBundleWiring() {
				return getWiring();
			}

			void setDefinedClass(final Class<?> clazz) {
				this.clazz = clazz;
			}

			void setProtectionDomain(final ProtectionDomain protectionDomain) {
				this.domain = protectionDomain;
			}

			void setComplete() {
				weavingComplete = true;
				bytes = bytes.clone();
				dynamicImports = Collections.unmodifiableList(dynamicImports);
			}

		}

		void addHostedCapability(final HostedCapability hostedCap) {
			hostedCapabilities.add(hostedCap);
		}

		List<HostedCapability> getHostedCapabilities() {
			return hostedCapabilities;
		}

		public int compareTo(final Revision other) {
			final int ids = (int) (bundleId - other.getBundle().getBundleId());
			return ids != 0 ? ids : revId - other.revId;
		}

	}

	class HeaderDictionary extends Hashtable<String, String> {

		private static final long serialVersionUID = 6688251578575649710L;

		// lazily initialized
		protected WeakHashMap<Locale, HeaderDictionary> headerCache;

		private final HashMap<String, String> index = new HashMap<String, String>();

		private boolean hasLocalizedValues;

		public HeaderDictionary(final int size) {
			super(size);
		}

		HeaderDictionary localize(final Locale locale) {
			if (!hasLocalizedValues) {
				return this;
			}

			if (headerCache != null) {
				final HeaderDictionary cached = headerCache.get(locale);
				if (cached != null) {
					return cached;
				}
			}

			final Properties props = getLocalizationFile(locale,
					bundleLocalizationBaseDir, bundleLocalizationBaseFilename);

			final HeaderDictionary localized = (HeaderDictionary) clone();
			final Enumeration<String> keys = localized.keys();
			while (keys.hasMoreElements()) {
				final String key = keys.nextElement();
				final String value = localized.get(key);
				if (value != null && value.charAt(0) == '%') {
					final String rawValue = value.substring(1).trim();
					final String localizedValue = props == null ? null
							: (String) props.get(rawValue);
					localized.put(key,
							localizedValue == null ? rawValue : localizedValue);
				}
			}

			if (headerCache == null) {
				headerCache = new WeakHashMap<Locale, HeaderDictionary>();
				headerCache.put(locale, localized);
			}

			return localized;
		}

		@Override
		public String put(final String key, final String value) {
			if (value.length() > 0 && value.charAt(0) == '%') {
				hasLocalizedValues = true;
			}

			index.put(key.toLowerCase(), key);
			return super.put(key, value);
		}

		@Override
		public String get(final Object key) {
			final String result = super.get(key);

			if (result != null) {
				return result;
			}

			final String indexedKey = index.get(((String) key).toLowerCase());
			return indexedKey == null ? null : super.get(indexedKey);
		}

	}

	class JarBundleRevision extends Revision {

		private final JarFile jarFile;

		protected JarBundleRevision(final int revId, final JarFile jar,
				final Manifest manifest, final String[] classpathStrings)
						throws BundleException {
			super(revId, manifest, classpathStrings);
			this.jarFile = jar;
		}

		protected URL lookupFile(final String classpath, final String filename)
				throws IOException {
			return (URL) findFile(classpath, filename, GET_URL);
		}

		protected URL lookupFile(final String classpath, final String filename,
				final HashSet<String> visited) throws IOException {
			return (URL) findFile(classpath, filename, GET_URL);
		}

		public InputStream retrieveFile(final String classpath,
				final String filename) throws IOException {
			return (InputStream) findFile(classpath, filename,
					RETRIEVE_INPUT_STREAM);
		}

		public long retrieveFileLength(final String classpath,
				final String filename) throws IOException {
			final Object res = findFile(classpath, filename,
					GET_CONTENT_LENGTH);
			if (res == null) {
				if (framework.DEBUG_CLASSLOADING) {
					framework.logger.log(LogService.LOG_DEBUG,
							"Could not retrieveFileLength for filename="
									+ filename + " from bundle="
									+ this.toString());
				}
				return -1;
			} else {
				return (Long) res;
			}
		}

		private Object findFile(final String classpath, String filename,
				final int mode) throws IOException {

			// strip trailing separator
			if (filename.charAt(0) == '/') {
				filename = filename.substring(1);
			}

			if (classpath == null || classpath.equals(".")) {
				final ZipEntry entry = jarFile.getEntry(filename);
				if (entry == null) {
					return null;
				}
				switch (mode) {
				case GET_URL:
					return createURL(entry.getName(), null);
				case RETRIEVE_INPUT_STREAM:
					return jarFile.getInputStream(entry);
				case GET_CONTENT_LENGTH:
					return entry.getSize();
				}
			} else {
				final ZipEntry entry = jarFile.getEntry(classpath);
				if (entry == null) {
					return null;
				}

				final InputStream in = jarFile.getInputStream(entry);
				if (in == null) {
					// classpath is a directory
					final ZipEntry entry2 = jarFile
							.getEntry(classpath + "/" + filename);
					if (entry2 == null) {
						return null;
					}
					switch (mode) {
					case GET_URL:
						return createURL(entry2.getName(), null);
					case RETRIEVE_INPUT_STREAM:
						return jarFile.getInputStream(entry2);
					case GET_CONTENT_LENGTH:
						return entry2.getSize();
					}
				}

				final JarInputStream embeddedJar = new JarInputStream(
						jarFile.getInputStream(entry));

				JarEntry embeddedEntry;
				while ((embeddedEntry = embeddedJar
						.getNextJarEntry()) != null) {
					if (embeddedEntry.getName().equals(filename)) {
						switch (mode) {
						case GET_URL:
							return createURL(entry.getName(),
									embeddedEntry.getName());
						case RETRIEVE_INPUT_STREAM:
							return embeddedJar;
						case GET_CONTENT_LENGTH:
							return embeddedEntry.getSize();
						}
					}
				}
			}
			return null;
		}

		protected Vector<URL> searchFiles(final String classpath,
				final String path, final String filePattern,
				final boolean recurse) {
			final Vector<URL> results = new Vector<URL>();
			
			String pathString = path.length() > 0 && path.charAt(0) == '/'
					? path.substring(1) : path;
			pathString = path.length() == 0
					|| path.charAt(path.length() - 1) == '/' ? pathString
							: pathString + "/";

			final int cpOffset;
			final String comp = pathString;
			if (classpath != null && !".".equals(classpath)) {
				pathString = classpath + "/" + pathString;
				cpOffset = classpath.length() + 1;
			} else {
				cpOffset = 0;
			}

			final Enumeration<JarEntry> enums = jarFile.entries();
			while (enums.hasMoreElements()) {
				final JarEntry ze = enums.nextElement();
				final String name;
				if (cpOffset == 0 || cpOffset >= ze.getName().length()) {
					name = ze.getName().replace('\\', '/');
				} else if (ze.getName().length() < cpOffset) {
					continue;
				} else {
					name = ze.getName().substring(cpOffset).replace('\\', '/');
				}

				if (name.startsWith(comp)) {
					final String rest = name.substring(comp.length(),
							name.length());

					if (rest.length() > 0) {
						final File file = new File(rest);

						if (file.getParent() != null & !recurse) {
							continue;
						}

						if (filePattern == null || RFC1960Filter.stringCompare(
								filePattern.toCharArray(), 0,
								file.getName().toCharArray(), 0) == 0) {
							try {
								results.add(createURL(
										name.charAt(0) == '/'
												? name.substring(1) : name,
										null));
							} catch (final IOException ex) {
								// do nothing, URL will not be added to
								// results
							}
						}
					}
				}
			}
			return results;
		}

		protected void close() throws IOException {
			jarFile.close();
		}

		public String toString() {
			return "JarBundleResource {" + jarFile.getName() + " of bundle "
					+ BundleImpl.this.toString() + "}";
		}
	}

	class ExplodedJarBundleRevision extends Revision {

		private final String storageLocation;

		ExplodedJarBundleRevision(final int revId, final String location,
				final Manifest manifest, final String[] classpathStrings)
						throws BundleException {
			super(revId, manifest, classpathStrings);
			this.storageLocation = location;
		}

		protected URL lookupFile(final String classpath, final String filename,
				final HashSet<String> visited) throws IOException {
			return (URL) findFile(classpath, filename, 0);
		}

		@Override
		protected InputStream retrieveFile(final String classpath,
				final String filename) throws IOException {
			return (InputStream) findFile(classpath, filename, 1);
		}

		@Override
		protected long retrieveFileLength(final String classpath,
				final String filename) throws IOException {
			return (Long) findFile(classpath, filename, 2);
		}

		@Override
		protected URL lookupFile(final String classpath, final String filename)
				throws IOException {
			return (URL) findFile(classpath, filename, 0);
		}

		private Object findFile(final String classpath, String filename,
				final int mode) throws IOException {
			// strip trailing separator
			if (filename.charAt(0) == '/') {
				filename = filename.substring(1);
			}

			if (classpath == null || classpath.equals(".")) {
				final File file = new File(storageLocation, filename);
				try {
					if (file.exists()) {
						switch (mode) {
						case GET_URL:
							return createURL(filename, null);
						case RETRIEVE_INPUT_STREAM:
							return new FileInputStream(file);
						case GET_CONTENT_LENGTH:
							return file.length();
						}
					} else {
						return null;
					}
				} catch (final FileNotFoundException ex) {
					return null;
				}
			} else {
				final File file = new File(storageLocation, classpath);
				if (file.exists()) {
					if (!file.isDirectory()) {
						// TODO check when security check must be done
						final ZipFile jar = new ZipFile(file);
						try {
							final ZipEntry entry = jar.getEntry(filename);
							if (entry == null) {
								return null;
							}
							switch (mode) {
							case GET_URL:
								return createURL(classpath, filename);
							case RETRIEVE_INPUT_STREAM:
								return jar.getInputStream(entry);
							case GET_CONTENT_LENGTH:
								return entry.getSize();
							}
						} finally {
							if (mode != RETRIEVE_INPUT_STREAM) {
								jar.close();
							}
						}
					} else {
						// file is a directory
						try {
							final File source = new File(file, filename);
							if (file.exists()) {
								switch (mode) {
								case GET_URL:
									return createURL(filename, null);
								case RETRIEVE_INPUT_STREAM:
									return new FileInputStream(source);
								case GET_CONTENT_LENGTH:
									return source.length();
								}
							} else {
								return null;
							}
						} catch (final FileNotFoundException e) {
							return null;
						}
					}
				}
			}
			return null;

		}

		/**
		 * Search for files on an exploded jar file. Which means:
		 * <ul>
		 * <li>search for every classpath component: this method will be called
		 * for every classpath component</li>
		 * <li>if classpath is null, search in exploded bundle in root</li>
		 * <li>if classpath refers to a JAR file, search within jar file</li>
		 * <li>if classpath is a local path, search in exploded bundle directory
		 * + classpath</li>
		 * </ul>
		 */
		public Vector<URL> searchFiles(final String classpath,
				final String path, final String filePattern,
				final boolean recurse) {
			final Vector<URL> results = new Vector<URL>();

			if (framework.DEBUG_CLASSLOADING) {
				framework.logger.log(LogService.LOG_DEBUG,
						"search files on exploded jar: storageLocation="
								+ storageLocation + ", classpath=" + classpath
								+ ", path=" + path + ", filePattern="
								+ filePattern + ", recurse=" + recurse);
			}

			String pathString = stripLeadingSlash(path);

			// now search in exploded jar file, which is in storage location
			if (classpath == null) {
				// no classpath, search in exploded storage location
				pathString = stripTrailingSlash(pathString); // File will add slash in between
				testFiles(new File(storageLocation, pathString), results,
						recurse, filePattern);
			} else if (classpath.endsWith(".jar")) {
				// if classpath refers to an inner jar file, search in jar file
				// which is exploded in storage location
				String jarFileName = classpath;
				JarFile jarFile = null;
				try {
					jarFile = new JarFile(
							new File(storageLocation, jarFileName));
					final Enumeration<JarEntry> enums = jarFile.entries();
					while (enums.hasMoreElements()) {
						final String jarEntryName = enums.nextElement()
								.getName();

						if (jarEntryName.startsWith(pathString)) {
							final String remaining = jarEntryName.substring(
									pathString.length(), jarEntryName.length());

							if (remaining.length() > 0) {
								final File file = new File(remaining);

								// TODO do we need that? Needs test cases
								if (file.getParent() != null & !recurse) {
									continue;
								}

								if (filePattern == null
										|| RFC1960Filter.stringCompare(
												filePattern.toCharArray(), 0,
												file.getName().toCharArray(),
												0) == 0) {
									try {
										results.add(createURL(
												stripLeadingSlash(jarEntryName),
												null));
									} catch (final IOException ex) {
										// do nothing, URL will not be added to
										// results
									}
								}
							}
						}
					}
				} catch (IOException e) {
					framework.logger.log(LogService.LOG_ERROR,
							"search files on exploded jar: could not open inner jar file"
									+ jarFileName + " from storage "
									+ storageLocation,
							e);
				} finally {
					if (jarFile != null) {
						try {
							jarFile.close();
						} catch (IOException e) {
							// silently ignore
						}
					}
 				}
			} else {
				// classpath is set use it within storage location
				String fullPathString = classpath.endsWith("/")
						? classpath + pathString
						: classpath + "/" + pathString;
				testFiles(new File(storageLocation, fullPathString), results,
						recurse, filePattern);
 			}
 
			if (framework.DEBUG_CLASSLOADING) {
				if (results.size() == 0) {
					framework.logger.log(LogService.LOG_DEBUG,
							"search files on exploded jar: NO RESULTS");
				} else {
					for (int i = 0; i < results.size(); i++) {
						framework.logger.log(LogService.LOG_DEBUG,
								"search files on exploded jar: result[" + i
										+ "]=" + results.get(i).toString());
					}
				}
			}
			return results;
		}

		private void testFiles(final File directory, final Vector<URL> results,
				final boolean recurse, final String filePattern) {
			if (directory.isDirectory()) {
				final File[] files = directory.listFiles();
				for (int i = 0; i < files.length; i++) {
					final File toTest = files[i];

					if (framework.DEBUG_CLASSLOADING) {
						framework.logger.log(LogService.LOG_DEBUG,
								"testing " + toTest.getAbsolutePath()
										+ (toTest.isDirectory() ? "/" : ""));
					}

					// get basename
					final String basename = toTest.getName();

					if (filePattern == null || RFC1960Filter.stringCompare(
							filePattern.toCharArray(), 0,
							basename.toCharArray(), 0) == 0) {
						try {
							final String absPath = toTest.getAbsolutePath();
							results.add(createURL(absPath
									.substring(absPath.indexOf(storageLocation)
											+ storageLocation.length() + 1)
									// TODO File.separatorChar instead of "/" ?
									+ (toTest.isDirectory() ? "/" : ""), null));
						} catch (final IOException ex) {
							// do nothing, URL will not be added to
							// results
						}
					}

					if (recurse && toTest.isDirectory()) {
						testFiles(toTest, results, recurse, filePattern);
					}
				}
			}
		}

		protected void close() throws IOException {
			// nop
		}

	}

	/*
	 * static methods
	 */

	/**
	 * get the package of a class.
	 * 
	 * @param classname
	 * @return the package.
	 */
	static String packageOf(final String classname) {
		final int pos = classname.lastIndexOf('.');
		return pos > -1 ? classname.substring(0, pos) : "";
	}

	protected static String classOf(final String classname) {
		final int pos = classname.lastIndexOf('.');
		return pos > -1 ? classname.substring(pos + 1, classname.length())
				: classname;
	}

	/**
	 * create a pseudo classname from a file.
	 * 
	 * @param filename
	 *            the filename.
	 * @return the pseudo classname.
	 */
	protected static String pseudoClassname(final String filename) {
		return filename.replace('.', '-').replace('/', '.').replace('\\', '.');
	}

	protected static String stripLeadingSlash(final String filename) {
		return filename.startsWith("/") || filename.startsWith("\\")
				? filename.substring(1) : filename;
	}
	
	protected static String stripTrailingSlash(final String filename) {
		return filename.endsWith("/") || filename.endsWith("\\")
				? filename.substring(0, filename.length() - 1)
				: filename;
	}
	
	protected static String appendTrailingSlash(final String filename) {
		return !filename.endsWith("/") ? filename.concat("/") : filename;
	}


	/**
	 * get a file from a class name.
	 * 
	 * @param fqc
	 *            the fully qualified class name.
	 * @return the file name.
	 */
	protected static String classToFile(final String fqc) {
		return fqc.replace('.', '/') + ".class";
	}

	/**
	 * store a file on the storage.
	 * 
	 * @param file
	 *            the file.
	 * @param input
	 *            the input stream.
	 */
	static void storeFile(final File file, final InputStream input) {
		try {
			file.getParentFile().mkdirs();
			final FileOutputStream fos = new FileOutputStream(file);

			final byte[] buffer = new byte[Concierge.CLASSLOADER_BUFFER_SIZE];
			int read;
			while ((read = input.read(buffer, 0,
					Concierge.CLASSLOADER_BUFFER_SIZE)) > -1) {
				fos.write(buffer, 0, read);
			}
			input.close();
			fos.close();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}

}
