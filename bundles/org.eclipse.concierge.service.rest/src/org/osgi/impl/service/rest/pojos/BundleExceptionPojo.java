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

import org.osgi.framework.BundleException;
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * Pojo for bundle exceptions.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "bundleException")
public class BundleExceptionPojo {

	private int		typecode;
	private String	message;

	public BundleExceptionPojo(final BundleException be) {
		this.typecode = be.getType();
		this.message = be.toString();
	}

	public int getTypecode() {
		return typecode;
	}

	public void setTypecode(final int typecode) {
		this.typecode = typecode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

}
