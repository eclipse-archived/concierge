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
 *     Tim Verbelen, iMinds
 *******************************************************************************/
package org.eclipse.concierge.service.clusterinfo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;
import org.osgi.service.clusterinfo.NodeStatus;

public class FrameworkNodeImpl implements FrameworkNodeStatus {

	private final BundleContext context;
	
	private final NodeStatus monitor = new MBeanMonitor();
	
	public FrameworkNodeImpl(BundleContext context) {
		this.context = context;
	}
	
	
	public Map<String, Object> getMetrics(String... names) {
		return monitor.getMetrics(names);
	}

	public BundleDTO getBundle(long id) {
		return context.getBundle(id).adapt(BundleDTO.class);
	}

	
	public Map<String, String> getBundleHeaders(long id) {
		Map<String, String> map = new HashMap<String, String>();
		Dictionary<String, String> headers = context.getBundle(id).getHeaders();
		Enumeration<String> e = headers.keys();
		while(e.hasMoreElements()){
			String key = e.nextElement();
			map.put(key, headers.get(key));
		}
		return map;
	}

	
	public Collection<BundleDTO> getBundles() {
		FrameworkDTO framework = context.getBundle(0).adapt(FrameworkDTO.class);
		return framework.bundles;
	}

	
	public BundleStartLevelDTO getBundleStartLevel(long id) {
		return context.getBundle(id).adapt(BundleStartLevelDTO.class);
	}

	
	public int getBundleState(long id) {
		return context.getBundle(id).getState();
	}

	
	public FrameworkStartLevelDTO getFrameworkStartLevel() {
		return context.getBundle(0).adapt(FrameworkStartLevelDTO.class);
	}

	
	public ServiceReferenceDTO getServiceReference(long id) {
		ServiceReferenceDTO s = null;
		try {
			s = getServiceReferences("service.id="+id).iterator().next();
		} catch(InvalidSyntaxException e){}
		return s;
	}

	
	public Collection<ServiceReferenceDTO> getServiceReferences() {
		FrameworkDTO framework = context.getBundle(0).adapt(FrameworkDTO.class);
		return framework.services;
	}

	
	public Collection<ServiceReferenceDTO> getServiceReferences(String filter) throws InvalidSyntaxException {
		Filter f = context.createFilter(filter);
		List<ServiceReferenceDTO> filtered = new ArrayList<ServiceReferenceDTO>();
		for(ServiceReferenceDTO r : getServiceReferences()){
			if(f.matches(r.properties)){
				filtered.add(r);
			}
		}
		return filtered; 
	}

	
	public BundleDTO installBundle(String location) throws BundleException {
		Bundle b = context.installBundle(location);
		return b.adapt(BundleDTO.class);
	}

	
	public void setBundleStartLevel(long id, int startLevel) {
		Bundle b = context.getBundle(id);
		if(b == null)
			return;
		
		BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
		bsl.setStartLevel(startLevel);
	}

	
	public void setFrameworkStartLevel(FrameworkStartLevelDTO startLevel) {
		Bundle fw = context.getBundle(0);
		FrameworkStartLevel fwsl = fw.adapt(FrameworkStartLevel.class);
		fwsl.setInitialBundleStartLevel(startLevel.initialBundleStartLevel);
		fwsl.setStartLevel(startLevel.startLevel);
	}

	
	public void startBundle(long id) throws BundleException {
		Bundle b = context.getBundle(id);
		if(b == null)
			return;
		
		b.start();
	}

	
	public void startBundle(long id, int options) throws BundleException {
		Bundle b = context.getBundle(id);
		if(b == null)
			return;
		
		b.start(options);
	}

	
	public void stopBundle(long id) throws BundleException {
		Bundle b = context.getBundle(id);
		if(b == null)
			return;
		
		b.stop();
	}

	
	public void stopBundle(long id, int options) throws BundleException {
		Bundle b = context.getBundle(id);
		if(b == null)
			return;
		
		b.stop(options);
	}

	
	public BundleDTO uninstallBundle(long id) throws BundleException {
		Bundle b  = context.getBundle();
		if(b == null)
			return null;
		
		b.uninstall();
		return b.adapt(BundleDTO.class);
	}

	
	public BundleDTO updateBundle(long id) throws BundleException {
		Bundle b  = context.getBundle();
		if(b == null)
			return null;
		
		b.update();
		return b.adapt(BundleDTO.class);
	}


	public BundleDTO updateBundle(long id, String url) throws BundleException, MalformedURLException, IOException {
		Bundle b  = context.getBundle();
		if(b == null)
			return null;
		
		URL u = new URL(url);
		b.update(u.openStream());
		return b.adapt(BundleDTO.class);
	}
}
