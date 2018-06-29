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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.clusterinfo.NodeStatus;

/**
 * Implement the NodeStatus using Java JMX MBeans
 * 
 * @author tverbele
 *
 */
public class MBeanMonitor implements NodeStatus {

	private final OperatingSystemMXBean os;
	private final MemoryMXBean memory;
	
	public MBeanMonitor() {
		os = ManagementFactory.getOperatingSystemMXBean();
		memory = ManagementFactory.getMemoryMXBean();
	}
	
	public Map<String, Object> getMetrics(String... names) {
		Map<String, Object> metrics = new HashMap<String, Object>();
		
		if(fill("availableProcessors", names)){
			metrics.put("availableProcessors", os.getAvailableProcessors());
		}
		
		if(fill("systemLoadAverage", names)){
			metrics.put("systemLoadAverage", os.getSystemLoadAverage());
		}
		
		if(fill("heapMemoryUsed", names)){
			metrics.put("heapMemoryUsed", memory.getHeapMemoryUsage().getUsed());
		}
		
		if(fill("heapMemoryMax", names)){
			metrics.put("heapMemoryMax", memory.getHeapMemoryUsage().getMax());
		}
		
		if(fill("nonHeapMemoryUsed", names)){
			metrics.put("nonHeapMemoryUsed", memory.getNonHeapMemoryUsage().getUsed());
		}
		
		if(fill("nonHeapMemoryMax", names)){
			metrics.put("nonHeapMemoryMax", memory.getNonHeapMemoryUsage().getMax());
		}
		
		return metrics;
	}

	private boolean fill(String key, String... names){
		if(names == null || names.length == 0)
			return true;
		
		for(String n : names){
			if(key.equals(n)){
				return true;
			}
		}
		
		return false;
	}
}
