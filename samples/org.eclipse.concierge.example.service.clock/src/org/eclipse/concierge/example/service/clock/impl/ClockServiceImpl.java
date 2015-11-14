/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package org.eclipse.concierge.example.service.clock.impl;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.concierge.example.service.clock.ClockService;

public class ClockServiceImpl implements ClockService {

	private final Calendar cal = Calendar.getInstance();

	@Override
	public Date getTime() {
		cal.setTimeInMillis(System.currentTimeMillis());
		return cal.getTime();
	}

}
