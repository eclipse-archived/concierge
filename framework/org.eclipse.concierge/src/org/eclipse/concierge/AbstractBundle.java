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
 *******************************************************************************/
package org.eclipse.concierge;

import java.io.File;
import java.lang.reflect.Array;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.concierge.BundleImpl.Revision;
import org.eclipse.concierge.Concierge.BundleContextImpl;
import org.eclipse.concierge.Concierge.ServiceListenerEntry;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.framework.wiring.dto.BundleWireDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO.NodeDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.CapabilityRefDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.resource.dto.RequirementRefDTO;
import org.osgi.resource.dto.ResourceDTO;
import org.osgi.resource.dto.WireDTO;

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
		 * FIXME: not the same page anymore --> core specifications page=91: If
		 * the Java runtime supports permissions, a ServiceReference object to a
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
		
		// BundleDTO
		if(type == BundleDTO.class){
			return (A) getBundleDTO();
		}
		
		// ServiceReferenceDTO[]
		if(type == ServiceReferenceDTO[].class){
			if(state != ACTIVE)
				return null;
			
			ServiceReferenceDTO[] dtos = new ServiceReferenceDTO[registeredServices==null ? 0 : registeredServices.size()];
			for(int i=0;i<dtos.length;i++){
				dtos[i] = getServiceReferenceDTO(registeredServices.get(i));
			}
			return (A) dtos;
		}
		
		// BundleRevisionDTO
		if(type == BundleRevisionDTO.class){
			if(currentRevision == null){
				return null;
			} else {
				return (A) getBundleRevisionDTO(currentRevision);
			}
		}
		
		// BundleRevisionDTO[]
		if(type == BundleRevisionDTO[].class){
			if(state == UNINSTALLED)
				return null;
			
			BundleRevisionDTO[] dtos = new BundleRevisionDTO[revisions.size()];
			for(int i=0;i<revisions.size();i++){
				dtos[i] = getBundleRevisionDTO(revisions.get(i));
			}
			return (A) dtos;
		}
		
		// BundleWiringDTO
		if(type == BundleWiringDTO.class){
			if(currentRevision == null){
				return null;
			} else {
				return (A) getBundleWiringDTO(currentRevision);
			}
		}
		
		// BundleWiringDTO[]
		if(type == BundleWiringDTO[].class){
			if(state == UNINSTALLED)
				return null;
			
			BundleWiringDTO[] dtos = new BundleWiringDTO[revisions.size()];
			for(int i=0;i<revisions.size();i++){
				dtos[i] = getBundleWiringDTO(revisions.get(i));
			}
			return (A) dtos;
		}
		
		// BundleStartLevelDTO
		if(type == BundleStartLevelDTO.class){
			return (A) getBundleStartLevelDTO();
		}	
		
		
		// TODO FrameworkStartLevelDTO
		
		// TODO FrameworkDTO

		return null;
	}
	
	protected final BundleDTO getBundleDTO(){
		BundleDTO dto = new BundleDTO();
		dto.id = bundleId;
		dto.lastModified = lastModified;
		dto.state = state;
		dto.symbolicName = getSymbolicName();
		dto.version = getVersion().toString();
		return dto;
	}
	
	protected final BundleStartLevelDTO getBundleStartLevelDTO(){
		BundleStartLevelDTO dto = new BundleStartLevelDTO();
		dto.bundle = bundleId;
		
		BundleStartLevel bsl = adapt(BundleStartLevel.class);
		dto.startLevel = bsl.getStartLevel();
		dto.activationPolicyUsed = bsl.isActivationPolicyUsed();
		dto.persistentlyStarted = bsl.isPersistentlyStarted();
		return dto;
	}
	
	protected final ServiceReferenceDTO getServiceReferenceDTO(ServiceReference ref){
		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		dto.bundle = bundleId;
		dto.id = (Long) ref.getProperty(Constants.SERVICE_ID);
		dto.properties = new HashMap<String, Object>();
		for(String key : ref.getPropertyKeys()){
			Object val = ref.getProperty(key);
			dto.properties.put(key, getDTOValue(val));
		}
		Bundle[] usingBundles = ref.getUsingBundles();
		if(usingBundles == null){
			dto.usingBundles = new long[0];
		} else {
			dto.usingBundles = new long[usingBundles.length];
			for(int j=0;j<usingBundles.length;j++){
				dto.usingBundles[j] = usingBundles[j].getBundleId();
			}
		}
		return dto;
	}
	
	protected final BundleRevisionDTO getBundleRevisionDTO(BundleRevision revision){
		BundleRevisionDTO dto = new BundleRevisionDTO();
		dto.bundle = revision.getBundle().getBundleId();
		dto.id = revision.hashCode();
		dto.symbolicName = revision.getSymbolicName();
		dto.type = revision.getTypes();
		dto.version = getVersion().toString();
		
		// add requirement/capabilities
		List<Capability> caps = revision.getCapabilities(null);
		dto.capabilities = new ArrayList<CapabilityDTO>(caps.size());
		for(Capability c : caps){
			CapabilityDTO capDTO = new CapabilityDTO();
			capDTO.id = c.hashCode();
			capDTO.namespace = c.getNamespace();
			capDTO.resource = c.getResource().hashCode();
			capDTO.attributes = getDTOMap(c.getAttributes());
			capDTO.directives = new HashMap<String, String>(c.getDirectives());
			dto.capabilities.add(capDTO);
		}
		
		List<Requirement> reqs = revision.getRequirements(null);
		dto.requirements = new ArrayList<RequirementDTO>(reqs.size());
		for(Requirement r : reqs){
			RequirementDTO reqDTO = new RequirementDTO();
			reqDTO.id = r.hashCode();
			reqDTO.namespace = r.getNamespace();
			reqDTO.resource = r.getResource().hashCode();
			reqDTO.attributes = getDTOMap(r.getAttributes());
			reqDTO.directives = new HashMap<String, String>(r.getDirectives());
			dto.requirements.add(reqDTO);
		}
		return dto;
	}
	
	protected final BundleWiringDTO getBundleWiringDTO(BundleRevision revision){
		BundleWiringDTO dto = new BundleWiringDTO();
		dto.bundle = revision.getBundle().getBundleId();
		
		BundleWiring wiring = revision.getWiring();
		// TODO what is root
		dto.root = wiring.hashCode();
		
		dto.resources = new HashSet<BundleRevisionDTO>();
		dto.resources.add(getBundleRevisionDTO(revision));
		dto.nodes = new HashSet<BundleWiringDTO.NodeDTO>();
		
		addBundleWiringNodeDTO(wiring, dto.resources, dto.nodes);
		
		return dto;
	}
	
	protected final BundleWiringDTO.NodeDTO addBundleWiringNodeDTO(
			BundleWiring wiring, Set<BundleRevisionDTO> resources, Set<NodeDTO> nodes){
		
		NodeDTO node = new BundleWiringDTO.NodeDTO();
		node.current = wiring.isCurrent();
		node.inUse = wiring.isInUse();
		node.id = wiring.hashCode();
		node.resource = wiring.getResource().hashCode();
		
		nodes.add(node);
		
		if(node.inUse){
			// these things are only not null if wiring is in use?
			
			node.capabilities = new ArrayList<CapabilityRefDTO>();
			for(BundleCapability c : wiring.getCapabilities(null)){
				CapabilityRefDTO caprDTO = new CapabilityRefDTO();
				BundleRevision rev= c.getResource();
				caprDTO.capability = c.hashCode();
				caprDTO.resource = rev.hashCode();
				
				if(!containsResource(resources, caprDTO.resource)){
					resources.add(getBundleRevisionDTO(rev));
				}
				
				node.capabilities.add(caprDTO);
			}
			
			node.requirements = new ArrayList<RequirementRefDTO>();
			for(Requirement r : wiring.getRequirements(null)){
				RequirementRefDTO reqrDTO = new RequirementRefDTO();
				Resource res = r.getResource();
				reqrDTO.requirement = r.hashCode();
				reqrDTO.resource = res.hashCode();
				
				if(!containsResource(resources, reqrDTO.resource)){
					if(res instanceof BundleRevision){
						resources.add(getBundleRevisionDTO((BundleRevision)res));
					}
				}
				
				node.requirements.add(reqrDTO);
			}
			
			node.providedWires = new ArrayList<WireDTO>();
			for(BundleWire w : wiring.getProvidedWires(null)){
				WireDTO wireDTO = getBundleWireDTO(w);
				node.providedWires.add(wireDTO);
				
				// add requirer resource
				BundleRevision requirer = w.getRequirer();
				if(!containsResource(resources, requirer.hashCode())){
					resources.add(getBundleRevisionDTO(requirer));
				}
				
				// add requirer wiring
				BundleWiring requirerWiring = w.getRequirerWiring();
				if(!containsNode(nodes, requirerWiring.hashCode())){
					addBundleWiringNodeDTO(requirerWiring, resources, nodes);
				}
			}
			
			node.requiredWires = new ArrayList<WireDTO>();
			for(BundleWire w : wiring.getRequiredWires(null)){
				WireDTO wireDTO = getBundleWireDTO(w);
				node.requiredWires.add(wireDTO);
				
				// add provider resource
				BundleRevision provider = w.getProvider();
				if(!containsResource(resources, provider.hashCode())){
					resources.add(getBundleRevisionDTO(provider));
				}
				
				// add provider wiring
				BundleWiring providerWiring = w.getProviderWiring();
				if(!containsNode(nodes, providerWiring.hashCode())){
					addBundleWiringNodeDTO(providerWiring, resources, nodes);
				}
				
			}
		}
		
		return node;
	}
	
	protected final WireDTO getBundleWireDTO(BundleWire w){
		BundleWireDTO dto = new BundleWireDTO();
		dto.capability = new CapabilityRefDTO();
		dto.capability.capability = w.getCapability().hashCode();
		dto.capability.resource = w.getCapability().getResource().hashCode();
		
		dto.requirement = new RequirementRefDTO();
		dto.requirement.requirement = w.getRequirement().hashCode();
		dto.requirement.resource = w.getRequirement().getResource().hashCode();
		
		dto.provider = w.getProvider().hashCode();
		dto.requirer = w.getRequirer().hashCode();

		dto.providerWiring = w.getProviderWiring().hashCode();
		dto.requirerWiring = w.getRequirerWiring().hashCode();
		
		return dto;
	}
	
	protected final Map<String, Object> getDTOMap(Map<String, Object> map){
		Map<String, Object> dtoMap = new HashMap<String, Object>();
		for(String key : map.keySet()){
			Object val = map.get(key);
			dtoMap.put(key, getDTOValue(val));
		}
		return dtoMap;
	}
	
	protected final Object getDTOValue(Object value){
		Class c = value.getClass();
		if(c.isArray()){
			c = c.getComponentType();
		}
		if(Number.class.isAssignableFrom(c)
			|| Boolean.class.isAssignableFrom(c)
			|| String.class.isAssignableFrom(c)
			|| DTO.class.isAssignableFrom(c)){
			return value;
		}
		
		if(value.getClass().isArray()){
			int length = Array.getLength(value);
			String[] converted = new String[length];
			for(int i=0;i<length;i++){
				converted[i] = String.valueOf(Array.get(value, i));
			}
			return converted;
		}
		
		return String.valueOf(value);
	}
	
	protected final boolean containsNode(Set<NodeDTO> nodes, int id){
		for(NodeDTO node : nodes){
			if(node.id == id){
				return true;
			}
		}
		return false;
	}
	
	protected final boolean containsResource(Set<? extends ResourceDTO> resources, int id){
		for(ResourceDTO resource : resources){
			if(resource.id == id){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 * @category Bundle
	 */
	public final int compareTo(final Bundle o) {
		return (int) (o.getBundleId() - bundleId);
	}

	// BundleRevisions

	/**
	 * @see org.osgi.framework.wiring.BundleRevisions#getRevisions()
	 * @category BundleRevisions
	 */
	public final List<BundleRevision> getRevisions() {
		return Collections.unmodifiableList(revisions);
	}

	// BundleReference

	/**
	 * @see org.osgi.framework.BundleReference#getBundle()
	 * @category BundleReference
	 */
	public final Bundle getBundle() {
		return this;
	}

	/**
	 * @see org.osgi.framework.Bundle#getDataFile(java.lang.String)
	 * @category Bundle
	 */
	public final File getDataFile(final String filename) {
		// according to OSGi R5 spec 10.1.6.16: return null if fragment
		if (context != null) {
			return context.getDataFile(filename);
		} else {
			return null;
		}
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
