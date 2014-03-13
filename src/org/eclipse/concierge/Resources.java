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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.concierge.BundleImpl.Revision;
import org.eclipse.concierge.Utils.MultiMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;

public class Resources {

	static Wire createWire(final Capability cap, final Requirement req) {
		return cap instanceof BundleCapability
				&& req instanceof BundleRequirement ? new ConciergeBundleWire(
				(BundleCapability) cap, (BundleRequirement) req)
				: new ConciergeWire(cap, req);
	}

	static Wiring createWiring() {
		return null;
	}

	static abstract class GenericReqCap implements Requirement, Capability {

		private final String namespace;
		private final Map<String, String> directives;
		private final Map<String, Object> attributes;

		protected GenericReqCap(final String str) throws BundleException {
			final String[] literals = str.split(Utils.SPLIT_AT_SEMICOLON_PLUS);

			this.namespace = literals[0].trim();

			final Tuple<HashMap<String, String>, HashMap<String, Object>> tuple = Utils
					.parseLiterals(literals, 1);

			this.directives = tuple.getFormer() == null ? Collections
					.<String, String> emptyMap() : Collections
					.unmodifiableMap(tuple.getFormer());
			this.attributes = tuple.getLatter() == null ? Collections
					.<String, Object> emptyMap() : Collections
					.unmodifiableMap(tuple.getLatter());
		}

		public GenericReqCap(final String namespace,
				final Map<String, String> directives,
				final Map<String, Object> attributes) {
			this.namespace = namespace;
			this.directives = directives == null || directives.isEmpty() ? Collections
					.<String, String> emptyMap() : Collections
					.unmodifiableMap(directives);
			this.attributes = attributes == null || attributes.isEmpty() ? Collections
					.<String, Object> emptyMap() : Collections
					.unmodifiableMap(attributes);
		}

		public final String getNamespace() {
			return namespace;
		}

		public final Map<String, String> getDirectives() {
			return directives;
		}

		public final Map<String, Object> getAttributes() {
			return attributes;
		}

		@Override
		public String toString() {
			if (directives.isEmpty() && attributes.isEmpty()) {
				return namespace;
			}
			String result = namespace + "; ";
			for (final String key : directives.keySet()) {
				result = result + key + ":=" + directives.get(key) + ", ";
			}
			for (final String key : attributes.keySet()) {
				result = result + key + "=" + attributes.get(key) + ", ";
			}

			return result.substring(0, result.length() - 2);
		}

	}

	public static class BundleCapabilityImpl extends GenericReqCap implements
			BundleCapability {

		private final BundleRevision revision;
		private final String prettyPrint;

		private final String[] includes;
		private final String[] excludes;
		private final boolean hasExcludes;

		BundleCapabilityImpl(final BundleRevision revision, final String str)
				throws BundleException {
			super(str);
			this.revision = revision;
			this.prettyPrint = null;

			final String excludeStr = getDirectives().get(
					PackageNamespace.CAPABILITY_EXCLUDE_DIRECTIVE);
			if (excludeStr == null) {
				// TODO: couldn't we stop here???
				excludes = new String[1];
				excludes[0] = "";
				hasExcludes = false;
			} else {
				excludes = Utils.unQuote(excludeStr)
						.split(Utils.SPLIT_AT_COMMA);
				hasExcludes = true;
			}

			final String includeStr = getDirectives().get(
					PackageNamespace.CAPABILITY_INCLUDE_DIRECTIVE);
			if (includeStr == null) {
				includes = new String[1];
				includes[0] = "*";
			} else {
				includes = Utils.unQuote(includeStr)
						.split(Utils.SPLIT_AT_COMMA);
			}
		}

		public BundleCapabilityImpl(final BundleRevision revision,
				final String namespace, final Map<String, String> directives,
				final Map<String, Object> attributes, final String prettyPrint) {
			super(namespace, directives, attributes);
			this.revision = revision;
			this.prettyPrint = prettyPrint;

			final String excludeStr = getDirectives().get(
					PackageNamespace.CAPABILITY_EXCLUDE_DIRECTIVE);
			if (excludeStr == null) {
				// TODO: couldn't we stop here???
				excludes = new String[1];
				excludes[0] = "";
				hasExcludes = false;
			} else {
				excludes = Utils.unQuote(excludeStr)
						.split(Utils.SPLIT_AT_COMMA);
				hasExcludes = true;
			}

			final String includeStr = getDirectives().get(
					PackageNamespace.CAPABILITY_INCLUDE_DIRECTIVE);
			if (includeStr == null) {
				includes = new String[1];
				includes[0] = "*";
			} else {
				includes = Utils.unQuote(includeStr)
						.split(Utils.SPLIT_AT_COMMA);
			}
		}

		public BundleRevision getRevision() {
			return revision;
		}

		public BundleRevision getResource() {
			return revision;
		}

		boolean filter(final String name) {
			boolean matched = false;
			for (int i = 0; i < includes.length; i++) {
				if (RFC1960Filter.stringCompare(includes[i].toCharArray(), 0,
						name.toCharArray(), 0) == 0) {
					matched = true;
					break;
				}
			}

			if (!matched) {
				return false;
			}

			matched = false;
			for (int i = 0; i < excludes.length; i++) {
				if (RFC1960Filter.stringCompare(name.toCharArray(), 0,
						excludes[i].toCharArray(), 0) == 0) {
					matched = true;
					break;
				}
			}

			return !matched;
		}

		boolean hasExcludes() {
			return hasExcludes;
		}

		@Override
		public String toString() {
			return prettyPrint == null ? "BundleCapability {"
					+ super.toString() + "}" : prettyPrint;
		}

	}

	public static class BundleRequirementImpl extends GenericReqCap implements
			BundleRequirement {

		private final BundleRevision revision;
		private final String prettyPrint;

		public BundleRequirementImpl(final BundleRevision revision,
				final String str) throws BundleException {
			super(str);
			this.revision = revision;
			this.prettyPrint = null;
		}

		public BundleRequirementImpl(final BundleRevision revision,
				final String namespace, final Map<String, String> directives,
				final Map<String, Object> attributes, final String prettyPrint) {
			super(namespace, directives, attributes);
			this.revision = revision;
			this.prettyPrint = "BundleRequirement{" + prettyPrint + "}";
		}

		public BundleRevision getRevision() {
			return revision;
		}

		public BundleRevision getResource() {
			return revision;
		}

		public boolean matches(final BundleCapability capability) {
			return Concierge.matches(this, capability);
		}

		@Override
		public String toString() {
			return prettyPrint == null ? "BundleRequirement {"
					+ super.toString() + "}" : prettyPrint;
		}
	}

	static class HostedBundleCapability implements HostedCapability,
			BundleCapability {

		private final Revision host;
		private final BundleCapability cap;

		HostedBundleCapability(final Revision host, final Capability cap) {
			this.host = host;
			this.cap = (BundleCapability) cap;
		}

		public String getNamespace() {
			return cap.getNamespace();
		}

		public Map<String, String> getDirectives() {
			return cap.getDirectives();
		}

		public Map<String, Object> getAttributes() {
			return cap.getAttributes();
		}

		public BundleRevision getResource() {
			return host;
		}

		public Capability getDeclaredCapability() {
			return cap;
		}

		public BundleRevision getRevision() {
			return host;
		}

		public String toString() {
			return "HostedCapability{host=" + host + ", cap=" + cap + ")";
		}

	}

	private static abstract class AbstractWireImpl<C extends Capability, R extends Requirement> {

		protected final C capability;

		protected final R requirement;

		protected AbstractWireImpl(final C capability, final R requirement) {
			this.capability = capability;
			this.requirement = requirement;
		}

		public C getCapability() {
			if (capability instanceof HostedCapability) {
				@SuppressWarnings("unchecked")
				final C declared = (C) ((HostedCapability) capability)
						.getDeclaredCapability();
				return declared;
			}

			return capability;
		}

		public R getRequirement() {
			return requirement;
		}

		@Override
		public String toString() {
			return "{" + requirement + "->" + capability + "}";
		}
	}

	static class ConciergeWire extends
			AbstractWireImpl<Capability, Requirement> implements Wire {

		protected ConciergeWire(final Capability capability,
				final Requirement requirement) {
			super(capability, requirement);
		}

		public Resource getProvider() {
			return capability.getResource();
		}

		public Resource getRequirer() {
			return requirement.getResource();
		}

	}

	static class ConciergeBundleWire extends
			AbstractWireImpl<BundleCapability, BundleRequirement> implements
			BundleWire {

		ConciergeBundleWiring providerWiring;
		ConciergeBundleWiring requirerWiring;

		protected ConciergeBundleWire(final BundleCapability capability,
				final BundleRequirement requirement) {
			super(capability, requirement);
		}

		public BundleWiring getProviderWiring() {
			if (providerWiring == null) {
				providerWiring = (ConciergeBundleWiring) getProvider()
						.getWiring();
			}
			return providerWiring;
		}

		public BundleWiring getRequirerWiring() {
			// final BundleWiring wiring = getRequirer().getWiring();
			// return wiring != null && wiring.isCurrent() ? wiring : null;
			return requirerWiring;
		}

		public BundleRevision getProvider() {
			return capability.getResource();
		}

		public BundleRevision getRequirer() {
			return requirement.getResource();
		}

	}

	static class ConciergeWiring implements Wiring {

		private final Resource resource;

		private final MultiMap<String, Capability> capabilities = new MultiMap<String, Capability>();
		private final MultiMap<String, Requirement> requirements = new MultiMap<String, Requirement>();

		private final MultiMap<String, Wire> providedWires = new MultiMap<String, Wire>();
		private final MultiMap<String, Wire> requiredWires = new MultiMap<String, Wire>();

		ConciergeWiring(final Resource resource, final List<Wire> wires) {
			this.resource = resource;
			for (final Wire wire : wires) {
				addWire(wire);
			}
		}

		private void addWire(final Wire wire) {
			if (wire.getProvider() == resource) {
				final Capability cap = wire.getCapability();
				capabilities.insertUnique(cap.getNamespace(), cap);
				providedWires.insert(cap.getNamespace(), wire);
			} else {
				final Requirement req = wire.getRequirement();
				requirements.insertUnique(req.getNamespace(), req);
				requiredWires.insert(req.getNamespace(), wire);
			}
		}

		public List<Capability> getResourceCapabilities(final String namespace) {
			return namespace == null ? capabilities.getAllValues()
					: capabilities.lookup(namespace);
		}

		public List<Requirement> getResourceRequirements(final String namespace) {
			return namespace == null ? requirements.getAllValues()
					: requirements.lookup(namespace);
		}

		public List<Wire> getProvidedResourceWires(final String namespace) {
			return namespace == null ? providedWires.getAllValues()
					: providedWires.lookup(namespace);
		}

		public List<Wire> getRequiredResourceWires(final String namespace) {
			return namespace == null ? requiredWires.getAllValues()
					: requiredWires.lookup(namespace);
		}

		public Resource getResource() {
			return resource;
		}

	}

	static class ConciergeBundleWiring implements BundleWiring {

		private final BundleRevision revision;

		private final MultiMap<String, BundleCapability> capabilities = new MultiMap<String, BundleCapability>();
		private final MultiMap<String, BundleRequirement> requirements = new MultiMap<String, BundleRequirement>();

		private final Comparator<BundleWire> provComp = new Comparator<BundleWire>() {
			public int compare(final BundleWire w1, final BundleWire w2) {
				final BundleCapability cap1 = w1.getCapability();
				final BundleCapability cap2 = w2.getCapability();

				assert cap1.getNamespace().equals(cap2.getNamespace());

				final List<Capability> caps = revision.getCapabilities(cap1
						.getNamespace());
				return caps.indexOf(cap1) - caps.indexOf(cap2);
			}
		};

		private final Comparator<BundleWire> reqComp = new Comparator<BundleWire>() {
			public int compare(final BundleWire w1, final BundleWire w2) {
				final BundleRequirement req1 = w1.getRequirement();
				final BundleRequirement req2 = w2.getRequirement();

				assert req1.getNamespace().equals(req2.getNamespace());

				final List<Requirement> reqs = revision.getRequirements(req1
						.getNamespace());

				return reqs.indexOf(req1) - reqs.indexOf(req2);
			}
		};

		private final MultiMap<String, BundleWire> providedWires = new MultiMap<String, BundleWire>(
				provComp);
		private final MultiMap<String, BundleWire> requiredWires = new MultiMap<String, BundleWire>(
				reqComp);

		final HashSet<BundleRevision> inUseSet = new HashSet<BundleRevision>();

		ConciergeBundleWiring(final BundleRevision revision,
				final List<Wire> wires) {
			this.revision = revision;

			final HashSet<Requirement> reqCache = new HashSet<Requirement>();

			if (wires != null) {
				for (final Wire wire : wires) {
					addWire((BundleWire) wire);
					if (wire.getRequirer() == revision) {
						reqCache.add(wire.getRequirement());
					}
				}
			}

			for (final BundleCapability cap : revision
					.getDeclaredCapabilities(null)) {
				final String effective = cap.getDirectives().get(
						Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
				if (effective == null
						|| Namespace.EFFECTIVE_RESOLVE.equals(effective)) {
					capabilities.insert(cap.getNamespace(), cap);
				}
			}

			if (revision.getTypes() == BundleRevision.TYPE_FRAGMENT) {
				capabilities.remove(IdentityNamespace.IDENTITY_NAMESPACE);
			}

			for (final BundleRequirement req : revision
					.getDeclaredRequirements(null)) {
				final String effective = req.getDirectives().get(
						Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
				final boolean optional = Namespace.RESOLUTION_OPTIONAL
						.equals(req.getDirectives().get(
								Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
				if (effective == null
						|| Namespace.EFFECTIVE_RESOLVE.equals(effective)
						|| PackageNamespace.RESOLUTION_DYNAMIC
								.equals(req
										.getDirectives()
										.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
					if (!optional || reqCache.contains(req)) {
						requirements.insert(req.getNamespace(), req);
					}
				}
			}
		}

		void addWire(final BundleWire wire) {
			final Capability cap = wire.getCapability();
			final Requirement req = wire.getRequirement();
			if (wire.getProvider() == revision) {
				providedWires.insert(cap.getNamespace(), wire);
				inUseSet.add(wire.getRequirer());
				((ConciergeBundleWire) wire).providerWiring = this;
			} else {
				requiredWires.insert(req.getNamespace(), wire);
				if (HostNamespace.HOST_NAMESPACE.equals(wire.getRequirement()
						.getNamespace())) {
					inUseSet.add(wire.getProvider());
				}
				((ConciergeBundleWire) wire).requirerWiring = this;
			}
		}

		HashMap<String, BundleWire> getPackageImportWires() {
			final List<BundleWire> list = getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
			final HashMap<String, BundleWire> result = new HashMap<String, BundleWire>();

			if (list != null) {
				for (final BundleWire wire : list) {
					result.put((String) wire.getCapability().getAttributes()
							.get(PackageNamespace.PACKAGE_NAMESPACE), wire);
				}
			}

			return result;
		}

		List<BundleWire> getRequireBundleWires() {
			return getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
		}

		public Bundle getBundle() {
			return revision.getBundle();
		}

		/**
		 * @see org.osgi.framework.wiring.BundleWiring#isCurrent()
		 */
		public boolean isCurrent() {
			return ((BundleImpl) revision.getBundle()).currentRevision == revision
					&& revision.getWiring() == this;
		}

		void cleanup() {
			for (final BundleWire requiredWire : requiredWires.getAllValues()) {
				final ConciergeBundleWiring bw = ((ConciergeBundleWire) requiredWire).providerWiring;
				if (bw != null) {
					bw.inUseSet.remove(revision);
				}
			}
			for (final BundleWire hostWire : providedWires
					.lookup(HostNamespace.HOST_NAMESPACE)) {
				final ConciergeBundleWiring bw = ((ConciergeBundleWire) hostWire).requirerWiring;
				if (bw != null) {
					bw.inUseSet.remove(revision);
				}
			}
		}

		public boolean isInUse() {
			return isCurrent() || !inUseSet.isEmpty();
		}

		public List<BundleCapability> getCapabilities(final String namespace) {
			if (!isInUse()) {
				return null;
			}

			return namespace == null ? capabilities.getAllValues()
					: capabilities.lookup(namespace);
		}

		public List<BundleRequirement> getRequirements(final String namespace) {
			if (!isInUse()) {
				return null;
			}

			return namespace == null ? requirements.getAllValues()
					: requirements.lookup(namespace);
		}

		public List<BundleWire> getProvidedWires(final String namespace) {
			if (!isInUse()) {
				return null;
			}

			return namespace == null ? providedWires.getAllValues()
					: providedWires.lookup(namespace);
		}

		public List<BundleWire> getRequiredWires(final String namespace) {
			if (!isInUse()) {
				return null;
			}

			return namespace == null ? requiredWires.getAllValues()
					: requiredWires.lookup(namespace);
		}

		public BundleRevision getRevision() {
			return revision;
		}

		public ClassLoader getClassLoader() {
			if (!isCurrent()) {
				return null;
			}

			if (revision instanceof Revision) {
				return ((Revision) revision).classloader;
			} else {
				return null;
			}
		}

		/**
		 * @see org.osgi.framework.wiring.BundleWiring#findEntries(java.lang.String,
		 *      java.lang.String, int)
		 */
		public List<URL> findEntries(final String path,
				final String filePattern, final int options) {
			if (!isInUse()) {
				return null;
			}

			Enumeration<URL> result = null;

			if (revision instanceof Revision) {
				final Revision rev = (Revision) revision;

				result = rev.findEntries(path, filePattern,
						options == FINDENTRIES_RECURSE);
			}

			return result == null ? Collections.<URL> emptyList() : Collections
					.unmodifiableList(Collections.list(result));
		}

		/**
		 * @see org.osgi.framework.wiring.BundleWiring#listResources(java.lang.String,
		 *      java.lang.String, int)
		 */
		public Collection<String> listResources(final String path,
				final String filePattern, final int options) {
			if (!isCurrent()) {
				return null;
			}

			throw new RuntimeException("not yet implemented");
		}

		public List<Capability> getResourceCapabilities(final String namespace) {
			final List<BundleCapability> bcaps = getCapabilities(namespace);
			return bcaps == null ? null : new ArrayList<Capability>(bcaps);
		}

		public List<Requirement> getResourceRequirements(final String namespace) {
			final List<BundleRequirement> breqs = getRequirements(namespace);
			return breqs == null ? null : new ArrayList<Requirement>(breqs);
		}

		public List<Wire> getProvidedResourceWires(final String namespace) {
			final List<BundleWire> bwires = getProvidedWires(namespace);
			return bwires == null ? null : new ArrayList<Wire>(bwires);
		}

		public List<Wire> getRequiredResourceWires(final String namespace) {
			final List<BundleWire> bwires = getRequiredWires(namespace);
			return bwires == null ? null : new ArrayList<Wire>(bwires);
		}

		public BundleRevision getResource() {
			return revision;
		}

		void addCapability(final HostedCapability hostedCap) {
			capabilities.insert(hostedCap.getNamespace(),
					(BundleCapability) hostedCap.getDeclaredCapability());
		}

		@Override
		public String toString() {
			return "[ConciergeBundleWiring of " + revision + "]";
		}

	}

}
