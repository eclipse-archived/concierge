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

import java.io.File;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.concierge.BundleImpl.Revision;
import org.eclipse.concierge.Concierge.BundleContextImpl;
import org.eclipse.concierge.Concierge.ServiceListenerEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWiring;

public abstract class AbstractBundle implements Bundle, BundleRevisions {

	protected static final short AUTOSTART_STOPPED = 0;
	protected static final short AUTOSTART_STARTED_WITH_DECLARED = 1;
	protected static final short AUTOSTART_STARTED_WITH_EAGER = 2;

	/**
	 * the bundle id.
	 */
	protected long bundleId;

	protected ArrayList<BundleRevision> revisions = new ArrayList<BundleRevision>();

	protected Revision currentRevision;

	/**
	 * the bundle location.
	 */
	protected String location;

	/**
	 * the bundle state.
	 */
	protected int state;

	/**
	 * time when bundle was last modified (milliseconds since Jan. 1. 1970)
	 */
	protected long lastModified;

	/**
	 * List of services registered by this bundle. Is initialized in a lazy way.
	 */
	protected List<ServiceReference<?>> registeredServices;

	/**
	 * the storage location.
	 */
	protected String storageLocation;

	/**
	 * the current start level.
	 */
	protected int startlevel;

	/**
	 * is bundle marked to be started persistently.
	 */
	protected short autostart = AUTOSTART_STOPPED;

	/**
	 * the bundle context.
	 */
	protected BundleContextImpl context;

	/**
	 * the protection domain of this bundle.
	 */
	protected ProtectionDomain domain;

	/**
	 * List of framework listeners registered by this bundle. Is initialized in
	 * a lazy way.
	 */
	protected List<FrameworkListener> registeredFrameworkListeners;
	/**
	 * List of service listeners registered by this bundle. Is initialized in a
	 * lazy way.
	 */
	protected List<ServiceListenerEntry> registeredServiceListeners;

	/**
	 * get the state of the bundle.
	 * 
	 * @return the state.
	 * @see org.osgi.framework.Bundle#getState()
	 * @category Bundle
	 */
	public final int getState() {
		return state;
	}

	/**
	 * @see org.osgi.framework.Bundle#getBundleId()
	 * @category Bundle
	 */
	public final long getBundleId() {
		return bundleId;
	}

	/**
	 * get the bundle location.
	 * 
	 * @return the bundle location.
	 * @see org.osgi.framework.Bundle#getLocation()
	 * @category Bundle
	 */
	public final String getLocation() {
		if (isSecurityEnabled()) {
			// TODO: check AdminPermission(this,METADATA)
		}
		return location;
	}

	/**
	 * get the registered services of the bundle.
	 * 
	 * @return the service reference array.
	 * @see org.osgi.framework.Bundle#getRegisteredServices()
	 * @category Bundle
	 */
	public final ServiceReference<?>[] getRegisteredServices() {
		if (state == UNINSTALLED) {
			throw new IllegalStateException("Bundle " + toString()
					+ "has been uninstalled.");
		}
		if (registeredServices == null) {
			return null;
		}

		/*
		 * FIXME: not the same page anymore core specifications page=91: If the
		 * Java runtime supports permissions, a ServiceReference object to a
		 * service is included in the returned list only if the caller has the
		 * ServicePermission to get the service using at least one of the names
		 * classes the service was registered under.
		 */
		if (isSecurityEnabled()) {
			return checkPermissions(registeredServices
					.toArray(new ServiceReferenceImpl[registeredServices.size()]));
		} else {
			return registeredServices
					.toArray(new ServiceReference[registeredServices.size()]);
		}
	}

	/**
	 * check if the bundle has a certain permission.
	 * 
	 * @param permission
	 *            the permission object
	 * @return true if the bundle has the permission.
	 * @see org.osgi.framework.Bundle#hasPermission(java.lang.Object)
	 * @category Bundle
	 */
	public final boolean hasPermission(final Object permission) {
		checkBundleNotUninstalled();

		if (isSecurityEnabled()) {
			return permission instanceof Permission ? domain.getPermissions()
					.implies((Permission) permission) : false;
		} else {
			return true;
		}
	}

	/**
	 * 
	 * @see org.osgi.framework.Bundle#getLastModified()
	 * @category Bundle
	 */
	public final long getLastModified() {
		return lastModified;
	}

	/**
	 * get bundle context
	 * 
	 * @return the bundle context if it exists, null otherwise
	 * @see org.osgi.framework.Bundle#getBundleContext()
	 * @category Bundle
	 */
	public final BundleContext getBundleContext() {
		// check permissions
		if (isSecurityEnabled()) {
			// TODO: check AdminPermission(this,CONTEXT)
		}
		if (state == STARTING || state == ACTIVE || state == STOPPING) {
			return context;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <A> A adapt(final Class<A> type) {
		// BundleRevisions
		// BundleStartLevel
		if (type.isInstance(this)) {
			return (A) this;
		}

		// AccessControlContext
		// TODO: implement

		// BundleContext
		if (type == BundleContext.class) {
			return (A) context;
		}

		// BundleRevision
		if (type == BundleRevision.class) {
			return (A) currentRevision;
		}

		// BundleWiring
		if (type == BundleWiring.class) {
			return currentRevision != null ? (A) currentRevision.getWiring()
					: null;
		}

		return null;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 * @category Bundle
	 */
	public int compareTo(final Bundle o) {
		return (int) (o.getBundleId() - bundleId);
	}

	// BundleRevisions

	/**
	 * @see org.osgi.framework.wiring.BundleRevisions#getRevisions()
	 * @category BundleRevisions
	 */
	public List<BundleRevision> getRevisions() {
		return Collections.unmodifiableList(revisions);
	}

	// BundleReference

	/**
	 * @see org.osgi.framework.BundleReference#getBundle()
	 * @category BundleReference
	 */
	public Bundle getBundle() {
		return this;
	}

	/**
	 * @see org.osgi.framework.Bundle#getDataFile(java.lang.String)
	 * @category Bundle
	 */
	public File getDataFile(final String filename) {
		return context.getDataFile(filename);
	}

	protected abstract boolean isSecurityEnabled();

	protected final void updateLastModified() {
		final long newMod = System.currentTimeMillis();
		// ensure strict monotonicity on system with a slow clock
		lastModified = newMod > lastModified ? newMod : ++lastModified;
	}

	/**
	 * remove all ServiceReferences for which the requesting bundle does not
	 * have appropriate permissions
	 * 
	 * @param refs
	 *            the references.
	 * @return the permitted references.
	 */
	protected static final ServiceReference<?>[] checkPermissions(
			final ServiceReferenceImpl<?>[] refs) {
		final List<ServiceReferenceImpl<?>[]> results = new ArrayList<ServiceReferenceImpl<?>[]>(
				refs.length);
		final AccessControlContext controller = AccessController.getContext();
		for (int i = 0; i < refs.length; i++) {
			final String[] interfaces = (String[]) refs[i].properties
					.get(Constants.OBJECTCLASS);
			for (int j = 0; j < interfaces.length; j++) {
				try {
					controller.checkPermission(new ServicePermission(
							interfaces[j], ServicePermission.GET));
					results.add(refs);
					break;
				} catch (final SecurityException se) {
					// does not have the permission, try with the next interface
				}
			}
		}
		return results.toArray(new ServiceReference[results.size()]);
	}

	protected final void checkBundleNotUninstalled()
			throws IllegalArgumentException {
		if (state == Bundle.UNINSTALLED) {
			throw new IllegalArgumentException("Bundle " + toString()
					+ " has been uninstalled");
		}
	}

}
