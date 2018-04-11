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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.clusterinfo.NodeStatus;
import org.osgi.util.tracker.ServiceTracker;

public class ClusterCommands  {
	
	private ServiceTracker tracker;
	private ServiceRegistration reg;
	
	public void register(BundleContext context) {
		tracker = new ServiceTracker(context, NodeStatus.class.getName(), null);
		tracker.open();
		
		Dictionary properties = new Hashtable();
		properties.put("osgi.command.scope", "cluster");
		properties.put("osgi.command.function", new String[] {"list","info","metrics"});
		reg  = context.registerService(ClusterCommands.class.getName(), this, properties);
		
	}
	
	public void unregister() {
		if(reg != null)
			reg.unregister();
		
		if(tracker != null)
			tracker.close();
	}
	
	public void list() {
		ServiceReference[] refs = tracker.getServiceReferences();
		if(refs == null)
			return;
		
		System.out.println("id\tcluster\ttags");
		for(ServiceReference ref : refs) {
			System.out.printf("%s\t%s\t%s\n", 
					(String)ref.getProperty("osgi.clusterinfo.id"),
					(String)ref.getProperty("osgi.clusterinfo.cluster"),
					Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.tags")));
		}
	}
	
	public void info(String id) {
		ServiceReference ref = getNodeById(id);
		if(ref == null) 
			System.out.println("No node found with id "+id);
			
		System.out.println("id : "+ref.getProperty("osgi.clusterinfo.id"));
		System.out.println("cluster : "+ref.getProperty("osgi.clusterinfo.cluster"));
		System.out.println("parent : "+ref.getProperty("osgi.clusterinfo.parentid"));
		System.out.println("vendor : "+ref.getProperty("osgi.clusterinfo.vendor"));
		System.out.println("version : "+ref.getProperty("osgi.clusterinfo.version"));
		System.out.println("country : "+ref.getProperty("osgi.clusterinfo.country"));
		System.out.println("location : "+ref.getProperty("osgi.clusterinfo.location"));
		System.out.println("region : "+ref.getProperty("osgi.clusterinfo.region"));
		System.out.println("country : "+ref.getProperty("osgi.clusterinfo.country"));
		System.out.println("zone : "+ref.getProperty("osgi.clusterinfo.zone"));
		System.out.println("endpoints : "+Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.endpoints")));
		System.out.println("private endpoints : "+Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.privateEndpoints")));
		System.out.println("tags : "+Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.tags")));

	}
	
	public void metrics(String id, String... names) {
		ServiceReference ref = getNodeById(id);
		if(ref == null) 
			System.out.println("No node found with id "+id);
		
		NodeStatus node = (NodeStatus)tracker.getService(ref);
		if(node == null)
			System.out.println("No node found with id "+id);

		Map<String, Object> metrics = node.getMetrics(names);
		for(Entry<String, Object> e : metrics.entrySet()) {
			System.out.println(e.getKey()+" : "+e.getValue());
		}
	}
	
	private ServiceReference getNodeById(String id) {
		ServiceReference[] refs = tracker.getServiceReferences();
		if(refs == null)
			return null;
		
		ServiceReference ref = null;
		for(ServiceReference r : refs) {
			if(id.equals(r.getProperty("osgi.clusterinfo.id"))){
				ref = r;
				break;
			}
		}
		return ref;
	}
}
