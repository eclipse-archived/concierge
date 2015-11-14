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

import org.osgi.framework.Bundle;
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * Pojo for bundles.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "bundle")
public final class BundlePojo {

	private long	id;
	private String	location;
	private long	lastModified;
	private int		state;
	private String	symbolicName;
	private String	version;

	public BundlePojo(final Bundle bundle) {
		setId(bundle.getBundleId());
		setLocation(bundle.getLocation());
		setLastModified(bundle.getLastModified());
		setState(bundle.getState());
		setSymbolicName(bundle.getSymbolicName());
		setVersion(bundle.getVersion().toString());
	}

	public void setId(final long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setLocation(final String location) {
		this.location = location;
	}

	public String getLocation() {
		return location;
	}

	public void setLastModified(final long lastModified) {
		this.lastModified = lastModified;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setState(final int state) {
		this.state = state;
	}

	public int getState() {
		return state;
	}

	public void setSymbolicName(final String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(final String version) {
		this.version = version;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

}
