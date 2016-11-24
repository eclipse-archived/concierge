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
package org.eclipse.concierge.service.clusterinfo.metrics;

import org.osgi.dto.DTO;

/**
 * Monitoring information about a JVM in the ecosystem 
 * 
 * These values are available through JMX
 */
public class JMXMetricsDTO extends DTO {
	
	/**
	 * The number of processors available
	 */
	public int availableProcessors;
	
	/**
	 * The average system load
	 */
	public float systemLoadAverage; 
	
	/**
	 * The maximal amount of heap memory available to the JVM
	 */
	public long heapMemoryMax;
	
	/**
	 * The amount of heap memory used by the JVM
	 */
	public long heapMemoryUsed;
	
	/**
	 * The maximal amount of non-heap memory available to the JVM
	 */
	public long nonHeapMemoryMax;
	
	/**
	 * The amount of non-heap memory used by the JVM
	 */
	public long nonHeapMemoryUsed; 

}
