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

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.clusterinfo.FrameworkManager;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;
import org.osgi.service.clusterinfo.NodeStatus;

public class Activator implements BundleActivator {

	private ServiceRegistration<?> reg;
	
	public void start(BundleContext context) throws Exception {
		FrameworkNodeImpl impl = new FrameworkNodeImpl(context);
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		
		// NodeStatus properties
		// TODO where to fetch all required properties? - for now read from run properties...
		
		// these are mandatory
		// for a FrameworkNodeStatus, the id should be the framework uuid
		properties.put("id", context.getProperty(Constants.FRAMEWORK_UUID).toString());
		
		String s;
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.cluster");
		properties.put("osgi.clusterinfo.cluster", s == null ? "Default Cluster" : s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.endpoints");
		properties.put("osgi.clusterinfo.endpoints", s == null ? new String[]{} : s.split(","));
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.privateEndpoints");
		properties.put("osgi.clusterinfo.privateEndpoints", s == null ? new String[]{} : s.split(","));
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.vendor");
		properties.put("osgi.clusterinfo.vendor", s == null ? "Concierge" : s);
		
		s = context.getProperty("org.eclipse.concierge.clusterinfo.version");
		properties.put("osgi.clusterinfo.version", s == null ? "0.0.0" : s);
		
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
		
	}

	public void stop(BundleContext context) throws Exception {
		if(reg != null)
			reg.unregister();
	}

}
