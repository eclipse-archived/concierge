/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge.compat.packageadmin;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

@SuppressWarnings("deprecation")
final class PackageAdminImpl implements PackageAdmin {

	private final BundleContext context;

	PackageAdminImpl(final BundleContext context) {
		this.context = context;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.packageadmin.PackageAdmin#getExportedPackages(org.osgi.framework.Bundle)
	 */
	public ExportedPackage[] getExportedPackages(final Bundle bundle) {
		if (bundle == null) {
			return getExportedPackages((String) null);
		}

		final ArrayList<ExportedPackage> result = new ArrayList<ExportedPackage>();

		getExportedPackages0(bundle, null, result);

		return toArrayOrNull(result, ExportedPackage.class);
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getExportedPackages(java.lang.String)
	 */
	public ExportedPackage[] getExportedPackages(final String name) {
		final Bundle[] bundles = context.getBundles();

		final ArrayList<ExportedPackage> result = new ArrayList<ExportedPackage>();

		for (final Bundle bundle : bundles) {
			getExportedPackages0(bundle, name, result);
		}

		return toArrayOrNull(result, ExportedPackage.class);
	}

	private void getExportedPackages0(final Bundle bundle, final String name,
			final ArrayList<ExportedPackage> result) {
		if (bundle == null) {
			throw new IllegalArgumentException("bundle==null");
		}
		if (result == null) {
			throw new IllegalArgumentException("result==null");
		}

		final List<BundleRevision> revs = bundle.adapt(BundleRevisions.class)
				.getRevisions();

		if (revs.isEmpty()) {
			return;
		}

		for (final BundleRevision r : revs) {
			final BundleWiring wiring = r.getWiring();
			if (wiring != null && wiring.isInUse()) {
				for (final Capability cap : wiring
						.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
					if (name == null
							|| name.equals(cap.getAttributes().get(
									PackageNamespace.PACKAGE_NAMESPACE))) {
						result.add(new ExportedPackageImpl(
								(BundleCapability) cap));
					}
				}
			}
		}
	}

	protected static final Comparator<ExportedPackage> EXPORT_ORDER = new Comparator<ExportedPackage>() {

		// reverts the order so that we can
		// retrieve the 0st item to get the best
		// match
		public int compare(final ExportedPackage c1, final ExportedPackage c2) {

			final BundleCapability cap1 = ((ExportedPackageImpl) c1).cap;
			final BundleCapability cap2 = ((ExportedPackageImpl) c2).cap;

			final int cap1Resolved = cap1.getResource().getWiring() == null ? 0
					: 1;
			final int cap2Resolved = cap2.getResource().getWiring() == null ? 0
					: 1;
			int score = cap2Resolved - cap1Resolved;
			if (score != 0) {
				return score;
			}

			Version cap1Version = (Version) cap1.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			Version cap2Version = (Version) cap2.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);

			if (cap1Version == null) {
				cap1Version = Version.emptyVersion;
			}
			if (cap2Version == null) {
				cap2Version = Version.emptyVersion;
			}

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

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getExportedPackage(java.lang.String)
	 */
	public ExportedPackage getExportedPackage(final String name) {
		final Bundle[] bundles = context.getBundles();

		final ArrayList<ExportedPackage> result = new ArrayList<ExportedPackage>();

		for (final Bundle bundle : bundles) {
			getExportedPackages0(bundle, name, result);
		}

		if (result.isEmpty()) {
			return null;
		}

		Collections.sort(result, EXPORT_ORDER);

		return result.get(0);
	}

	private FrameworkWiring getFrameworkWiring() {
		return context.getBundle(0).adapt(FrameworkWiring.class);
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#refreshPackages(org.osgi.framework.Bundle[])
	 */
	public void refreshPackages(final Bundle[] bundles) {
		final FrameworkWiring wiring = getFrameworkWiring();

		wiring.refreshBundles(bundles == null ? null : Arrays.asList(bundles));
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#resolveBundles(org.osgi.framework.Bundle[])
	 */
	public boolean resolveBundles(final Bundle[] bundles) {
		final FrameworkWiring wiring = getFrameworkWiring();

		return wiring.resolveBundles(bundles == null ? null : Arrays
				.asList(bundles));
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getRequiredBundles(java.lang.String)
	 */
	public RequiredBundle[] getRequiredBundles(final String symbolicName) {
		final Bundle[] bundles = context.getBundles();

		final ArrayList<RequiredBundle> result = new ArrayList<RequiredBundle>();

		for (final Bundle bundle : bundles) {
			if (bundle.getState() == Bundle.INSTALLED
					|| bundle.getState() == Bundle.UNINSTALLED) {
				continue;
			}

			final BundleRevision rev = bundle.adapt(BundleRevision.class);
			if (isFragment(rev)) {
				continue;
			}

			if (symbolicName == null
					|| symbolicName.equals(rev.getSymbolicName())) {
				result.add(new RequiredBundleImpl(rev));
			}
		}

		return toArrayOrNull(result, RequiredBundle.class);
	}

	private void addRequiringBundles(final BundleWiring wiring,
			final ArrayList<Bundle> result) {
		final List<BundleWire> wires = wiring
				.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);

		for (final BundleWire wire : wires) {
			result.add(wire.getRequirer().getBundle());
			if (BundleNamespace.VISIBILITY_REEXPORT.equals(wire
					.getRequirement().getDirectives()
					.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE))) {
				addRequiringBundles(wire.getRequirer().getWiring(), result);
			}
		}
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getBundles(java.lang.String,
	 *      java.lang.String)
	 */
	public Bundle[] getBundles(final String symbolicName,
			final String versionRange) {
		if (symbolicName == null) {
			throw new IllegalArgumentException("symbolicName is null");
		}

		final VersionRange range = versionRange == null ? null
				: new VersionRange(versionRange);

		final Bundle[] bundles = context.getBundles();
		final ArrayList<Bundle> result = new ArrayList<Bundle>();

		for (final Bundle bundle : bundles) {
			if (symbolicName.equals(bundle.getSymbolicName())) {
				if (range == null || range.includes(bundle.getVersion())) {
					result.add(bundle);
				}
			}
		}

		if (result.isEmpty()) {
			return null;
		}

		Collections.sort(result, new Comparator<Bundle>() {
			public int compare(final Bundle b1, final Bundle b2) {
				return b2.getVersion().compareTo(b1.getVersion());
			}
		});

		return result.toArray(new Bundle[result.size()]);
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getFragments(org.osgi.framework.Bundle)
	 */
	public Bundle[] getFragments(final Bundle bundle) {
		final BundleWiring wiring = bundle.adapt(BundleWiring.class);
		// this will happen if a bundle has no current revision, e.g.
		// is INSTALLED only
		if (wiring == null) {
			// System.err.println("o.e.c.service.packageadmin: getFragments has no wiring for bundle "
			// + bundle.getSymbolicName());
			return null;
		}
		final List<BundleWire> wires = wiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);

		if (wires==null || wires.isEmpty()) {
			return null;
		}

		final Bundle[] result = new Bundle[wires.size()];

		final Iterator<BundleWire> iter = wires.iterator();
		for (int i = 0; i < result.length; i++) {
			final BundleWire wire = iter.next();
			result[i] = wire.getRequirer().getBundle();
		}

		return result;
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getHosts(org.osgi.framework.Bundle)
	 */
	public Bundle[] getHosts(final Bundle bundle) {
		final BundleWiring wiring = bundle.adapt(BundleWiring.class);

		if (wiring == null) {
			return null;
		}

		final List<BundleWire> wires = wiring
				.getRequiredWires(HostNamespace.HOST_NAMESPACE);

		final ArrayList<Bundle> result = new ArrayList<Bundle>();

		for (final BundleWire wire : wires) {
			if (wire.getRequirer().getBundle() == bundle) {
				result.add(wire.getProvider().getBundle());
			}
		}

		return toArrayOrNull(result, Bundle.class);
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getBundle(java.lang.Class)
	 */
	public Bundle getBundle(@SuppressWarnings("rawtypes") final Class clazz) {
		final ClassLoader cl = clazz.getClassLoader();

		if (cl instanceof BundleReference) {
			return ((BundleReference) cl).getBundle();
		}

		return null;
	}

	/**
	 * @see org.osgi.service.packageadmin.PackageAdmin#getBundleType(org.osgi.framework.Bundle)
	 */
	public int getBundleType(final Bundle bundle) {
		final BundleRevision rev = bundle.adapt(BundleRevision.class);
		return isFragment(rev) ? BUNDLE_TYPE_FRAGMENT : 0;
	}

	private boolean isFragment(final BundleRevision rev) {
		return !rev.getRequirements(HostNamespace.HOST_NAMESPACE).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private <T> T[] toArrayOrNull(final List<T> l, final Class<T> t) {
		if (l == null || l.isEmpty()) {
			return null;
		}

		return l.toArray((T[]) Array.newInstance(t, l.size()));
	}

	private class ExportedPackageImpl implements ExportedPackage {

		final BundleCapability cap;

		ExportedPackageImpl(final BundleCapability cap) {
			this.cap = cap;
		}

		/**
		 * @see org.osgi.service.packageadmin.ExportedPackage#getName()
		 */
		public String getName() {
			return (String) cap.getAttributes().get(
					PackageNamespace.PACKAGE_NAMESPACE);
		}

		/**
		 * @see org.osgi.service.packageadmin.ExportedPackage#getExportingBundle()
		 */
		public Bundle getExportingBundle() {
			return cap.getResource().getBundle();
		}

		/**
		 * @see org.osgi.service.packageadmin.ExportedPackage#getImportingBundles()
		 */
		public Bundle[] getImportingBundles() {
			final ArrayList<Bundle> result = new ArrayList<Bundle>();

			final BundleWiring wiring = cap.getResource().getWiring();

			if (wiring != null) {
				final List<BundleWire> wires = wiring
						.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

				for (final BundleWire wire : wires) {
					if (wire.getCapability() == cap) {
						result.add(wire.getRequirer().getBundle());
					}
				}

				addRequiringBundles(wiring, result);
			}

			return toArrayOrNull(result, Bundle.class);
		}

		/**
		 * @see org.osgi.service.packageadmin.ExportedPackage#getSpecificationVersion()
		 */
		public String getSpecificationVersion() {
			final Version version = getVersion();
			return version == null ? null : version.toString();
		}

		/**
		 * @see org.osgi.service.packageadmin.ExportedPackage#getVersion()
		 */
		public Version getVersion() {
			return (Version) cap.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		}

		/**
		 * @see org.osgi.service.packageadmin.ExportedPackage#isRemovalPending()
		 */
		public boolean isRemovalPending() {
			final BundleRevision rev = cap.getResource();
			return rev.getBundle().getState() == Bundle.UNINSTALLED
					|| rev.getBundle().adapt(BundleRevision.class) != rev;
		}

		@Override
		public String toString() {
			return cap.toString();
		}

	}

	private class RequiredBundleImpl implements RequiredBundle {

		private final BundleRevision rev;

		public RequiredBundleImpl(final BundleRevision rev) {
			this.rev = rev;
		}

		/**
		 * @see org.osgi.service.packageadmin.RequiredBundle#getSymbolicName()
		 */
		public String getSymbolicName() {
			return rev.getSymbolicName();
		}

		/**
		 * @see org.osgi.service.packageadmin.RequiredBundle#getBundle()
		 */
		public Bundle getBundle() {
			return rev.getBundle();
		}

		/**
		 * @see org.osgi.service.packageadmin.RequiredBundle#getRequiringBundles()
		 */
		public Bundle[] getRequiringBundles() {
			final ArrayList<Bundle> result = new ArrayList<Bundle>();

			final BundleWiring wiring = rev.getWiring();
			if (wiring != null) {
				addRequiringBundles(wiring, result);
			}

			return toArrayOrNull(result, Bundle.class);
		}

		/**
		 * @see org.osgi.service.packageadmin.RequiredBundle#getVersion()
		 */
		public Version getVersion() {
			return rev.getVersion();
		}

		/**
		 * @see org.osgi.service.packageadmin.RequiredBundle#isRemovalPending()
		 */
		public boolean isRemovalPending() {
			return rev.getBundle().getState() == Bundle.UNINSTALLED
					|| rev.getBundle().adapt(BundleRevision.class) != rev;
		}

		public String toString() {
			return "RequiredBundle{" + rev.toString();
		}
	}

}
