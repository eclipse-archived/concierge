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
import org.osgi.framework.dto.BundleDTO;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;
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
		properties.put("osgi.command.function", new String[] {"list","info","metrics","bundles"});
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
		
		int i = 0;
		System.out.println("\tid\t\t\t\t\tcluster\t\ttags");
		for(ServiceReference ref : refs) {
			System.out.printf("[%d]\t%s\t%s\t%s\n",
					i,
					(String)ref.getProperty("osgi.clusterinfo.id"),
					(String)ref.getProperty("osgi.clusterinfo.cluster"),
					ref.getProperty("osgi.clusterinfo.tags") == null 
						? "N/A" : Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.tags")));
			i++;
		}
	}
	
	public void info(int i) {
		ServiceReference[] refs = tracker.getServiceReferences();
		if(i < 0 || refs == null || refs.length <= i) {
			System.out.println("Invalid index");
			return;
		}
	
		printNodeInfo(refs[i]);
	}
	
	public void info(String id) {
		ServiceReference ref = getNodeById(id);
		if(ref == null) {
			System.out.println("No node found with id "+id);
			return;
		}
			
		printNodeInfo(ref);
	}
	
	private void printNodeInfo(ServiceReference ref) {
		System.out.println("id : "+ref.getProperty("osgi.clusterinfo.id"));
		System.out.println("cluster : "+ref.getProperty("osgi.clusterinfo.cluster"));
		
		if(ref.getProperty("osgi.clusterinfo.parentid") != null)
			System.out.println("parent : "+ref.getProperty("osgi.clusterinfo.parentid"));
		
		if(ref.getProperty("osgi.clusterinfo.vendor") != null)
			System.out.println("vendor : "+ref.getProperty("osgi.clusterinfo.vendor"));
		
		if(ref.getProperty("osgi.clusterinfo.version") != null)
			System.out.println("version : "+ref.getProperty("osgi.clusterinfo.version"));
		
		if(ref.getProperty("osgi.clusterinfo.country") != null)
			System.out.println("country : "+ref.getProperty("osgi.clusterinfo.country"));
		
		if(ref.getProperty("osgi.clusterinfo.location") != null)
			System.out.println("location : "+ref.getProperty("osgi.clusterinfo.location"));
		
		if(ref.getProperty("osgi.clusterinfo.region") != null)
			System.out.println("region : "+ref.getProperty("osgi.clusterinfo.region"));
		
		if(ref.getProperty("osgi.clusterinfo.country") != null)
			System.out.println("country : "+ref.getProperty("osgi.clusterinfo.country"));
		
		if(ref.getProperty("osgi.clusterinfo.zone") != null)
			System.out.println("zone : "+ref.getProperty("osgi.clusterinfo.zone"));
		
		if(ref.getProperty("osgi.clusterinfo.endpoints") != null)
			System.out.println("endpoints : "+Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.endpoints")));
		
		if(ref.getProperty("osgi.clusterinfo.privateEndpoints") != null)
			System.out.println("private endpoints : "+Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.privateEndpoints")));
		
		if(ref.getProperty("osgi.clusterinfo.tags") != null)
			System.out.println("tags : "+Arrays.toString((String[])ref.getProperty("osgi.clusterinfo.tags")));
	}
	
	public void metrics(int i) {
		ServiceReference[] refs = tracker.getServiceReferences();
		if(i < 0 || refs == null || refs.length <= i) {
			System.out.println("Invalid index");
			return;
		}
	
		printMetrics(refs[i]);
	}
	
	public void metrics(String id) {
		ServiceReference ref = getNodeById(id);
		if(ref == null) { 
			System.out.println("No node found with id "+id);
			return;
		}
		
		printMetrics(ref);
	}
	
	private void printMetrics(ServiceReference ref) {
		NodeStatus node = (NodeStatus)tracker.getService(ref);
		if(node == null) {
			System.out.println("Invalid node reference");
			return;
		}

		Map<String, Object> metrics = node.getMetrics();
		for(Entry<String, Object> e : metrics.entrySet()) {
			System.out.println(e.getKey()+" : "+e.getValue());
		}
	}
	
	public void bundles(int i) {
		ServiceReference[] refs = tracker.getServiceReferences();
		if(i < 0 || refs == null || refs.length <= i) {
			System.out.println("Invalid index");
			return;
		}
	
		printBundles(refs[i]);
	}
	
	public void bundles(String id) {
		ServiceReference ref = getNodeById(id);
		if(ref == null) { 
			System.out.println("No node found with id "+id);
			return;
		}
		
		printBundles(ref);
	}
	
	private void printBundles(ServiceReference ref) {
		FrameworkNodeStatus node = (FrameworkNodeStatus)tracker.getService(ref);
		if(node == null) {
			System.out.println("Invalid node reference");
			return;
		}

		try {
			for(BundleDTO b : node.getBundles()) {
				System.out.println(String.format("[%d] %s", b.id, b.symbolicName));
			}
		} catch (Exception e) {
			e.printStackTrace();
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
