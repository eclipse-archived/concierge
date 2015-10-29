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

package org.osgi.impl.service.rest.pojos;

import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * Pojo for the bundle state.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "bundleState")
public final class BundleStatePojo {

	private int	state;

	private int	options;

	public BundleStatePojo() {

	}

	public BundleStatePojo(final int state) {
		this.state = state;
	}

	public void setState(final int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}

	public void setOptions(final int options) {
		this.options = options;
	}

	public int getOptions() {
		return options;
	}

}
