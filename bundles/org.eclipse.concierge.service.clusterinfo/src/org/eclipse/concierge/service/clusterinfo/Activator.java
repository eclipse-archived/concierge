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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.clusterinfo.ClusterTagPermission;
import org.osgi.service.clusterinfo.FrameworkManager;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;
import org.osgi.service.clusterinfo.NodeStatus;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

	private ServiceRegistration<?> reg;
	private ServiceTracker<?,?> tracker;
	private Map<ServiceReference<?>, List<String>> tagMap = new HashMap<ServiceReference<?>, List<String>>();
	
	private Dictionary<String, Object> properties = new Hashtable<String, Object>();

	private ClusterCommands commands;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void start(BundleContext context) throws Exception {
		FrameworkNodeImpl impl = new FrameworkNodeImpl(context);
		
		// NodeStatus properties
		// for now read from run properties...
		// TODO also allow to configure via ConfigAdmin?
		
		// these are mandatory for a FrameworkNodeStatus, 
		// the id should be the framework uuid
		properties.put("osgi.clusterinfo.id", context.getProperty(Constants.FRAMEWORK_UUID).toString());
		
		String s;
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.cluster");
		properties.put("osgi.clusterinfo.cluster", s == null ? "default" : s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.endpoint");
		properties.put("osgi.clusterinfo.endpoint", s == null ? new String[]{} : s.split(","));
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.private.endpoint");
		properties.put("osgi.clusterinfo.private.endpoint", s == null ? new String[]{} : s.split(","));
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.vendor");
		properties.put("osgi.clusterinfo.vendor", s == null ? "Concierge" : s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.version");
		properties.put("osgi.clusterinfo.version", s == null ? "1.0.0" : s);
		
		// these are optional
		s = context.getProperty("org.eclipse.concierge.clusterinfo.country");
		if(s != null)
			properties.put("osgi.clusterinfo.country", s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.location");
		if(s != null)
			properties.put("osgi.clusterinfo.location", s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.region");
		if(s != null)
			properties.put("osgi.clusterinfo.region", s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.zone");
		if(s != null)
			properties.put("osgi.clusterinfo.zone", s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.tags");
		if(s != null)
			properties.put("osgi.clusterinfo.tags", s.split(","));
		
		// FrameworkNodeStatus properties
		// OSGi Framework properties
		properties.put(Constants.FRAMEWORK_UUID, context.getProperty(Constants.FRAMEWORK_UUID).toString());
		properties.put(Constants.FRAMEWORK_VERSION, context.getProperty(Constants.FRAMEWORK_VERSION));
		properties.put(Constants.FRAMEWORK_PROCESSOR, context.getProperty(Constants.FRAMEWORK_PROCESSOR));
		properties.put(Constants.FRAMEWORK_OS_NAME, context.getProperty(Constants.FRAMEWORK_OS_NAME));
		properties.put(Constants.FRAMEWORK_OS_VERSION, context.getProperty(Constants.FRAMEWORK_OS_VERSION));
		
		// Java properties
		properties.put("java.version", System.getProperty("java.version"));
		properties.put("java.vm.version", System.getProperty("java.vm.version"));
		properties.put("java.specification.version", System.getProperty("java.specification.version"));
		properties.put("java.runtime.version", System.getProperty("java.runtime.version"));
		
		// Export the services via RSA
		properties.put("service.exported.interfaces","*");
		
		reg = context.registerService(new String[]{
				FrameworkManager.class.getName(),
				NodeStatus.class.getName(),
				FrameworkNodeStatus.class.getName()
		}, impl, properties);
		
		if(properties.get("osgi.clusterinfo.tags") != null) {
			List<String> l = new ArrayList<String>();
			for(String t : (String[])properties.get("osgi.clusterinfo.tags")) {
				l.add(t);
			}
			tagMap.put(reg.getReference(), l);
		}
		
		// add additional properties announced by any service
		tracker = new ServiceTracker(context, context.createFilter("(org.osgi.service.clusterinfo.tags=*)"), 
				new ServiceTrackerCustomizer() {

			public Object addingService(ServiceReference reference) {
				addTags(reference);
				updateTags();
				return null;
			}

			public void modifiedService(ServiceReference reference, Object service) {
				addTags(reference);
				updateTags();

			}

			public void removedService(ServiceReference reference, Object service) {
				tagMap.remove(reference);
				updateTags();
			}

		});
		tracker.open();
		
		// register cluster commands for GoGo shell
		commands = new ClusterCommands();
		commands.register(context);
	}

	public void stop(BundleContext context) throws Exception {
		if(commands != null)
			commands.unregister();

		if(reg != null)
			reg.unregister();
		
		tracker.close();
	}

	private void addTags(ServiceReference reference) {
		try {
		String[] tags = (String[])reference.getProperty("org.osgi.service.clusterinfo.tags");
		List<String> l = new ArrayList<String>();
		SecurityManager sm = System.getSecurityManager();
		for(String t : tags) {
			if(sm == null || reference.getBundle().hasPermission(new ClusterTagPermission(t, "ADD"))) {
				l.add(t);
			}
		}
		tagMap.put(reference, l);
		} catch(Throwable t) {t.printStackTrace();}
	}
	
	private void updateTags(){
		try {
		Set<String> tags = new HashSet<String>();
		for(List<String> t : tagMap.values()){
			for(String tag : t){
				tags.add(tag);
			}
		}
		properties.put("osgi.clusterinfo.tags", tags.toArray(new String[tags.size()]));
		reg.setProperties(properties);
		} catch(Throwable t) {t.printStackTrace();}
	}
}
