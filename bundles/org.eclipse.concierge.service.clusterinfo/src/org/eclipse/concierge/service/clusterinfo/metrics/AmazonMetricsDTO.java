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
 * Example DTO for monitoring information about an Amazon VM. 
 * 
 * http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/viewing_metrics_with_cloudwatch.html
 */
public class AmazonMetricsDTO extends DTO {
	
	/**
	 * The number of CPU credits consumed during the specified period.
	 */
	public long cpuCreditUsage;
	
	/**
	 * The number of CPU credits that an instance has accumulated.
	 */
	public long cpuCreditBalance;
	
	/**
	 * The percentage of allocated EC2 compute units that are currently in use on the instance. 
	 */
	public float cpuUtilization; 
	
	/**
	 * Completed read operations from all instance store volumes available to the instance 
	 * in a specified period of time.
	 */
	public long diskReadOps; 
	
	/**
	 * Completed write operations to all instance store volumes available to the instance 
	 * in a specified period of time.
	 */
	public long diskWriteOps;
	
	/**
	 * Bytes read from all instance store volumes available to the instance.
	 */
	public long diskReadBytes; 

	/**
	 * Bytes written to all instance store volumes available to the instance.
	 */
	public long diskWriteBytes; 
	
	/**
	 * The number of bytes received on all network interfaces by the instance. 
	 */
	public long networkIn;
	
	/**
	 * The number of bytes sent out on all network interfaces by the instance.
	 */
	public long networkOut;
	
	/**
	 * The number of packets received on all network interfaces by the instance. 
	 */
	public long networkPacketsIn;
	
	/**
	 * The number of packets sent out on all network interfaces by the instance.
	 */
	public long networkPacketsOut;
		
}
