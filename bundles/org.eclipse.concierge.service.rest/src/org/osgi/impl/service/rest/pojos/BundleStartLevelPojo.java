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

import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * Pojo for the bundle start level.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "bundleStartLevel")
public final class BundleStartLevelPojo {

	private int		startLevel;
	private boolean	activationPolicyUsed;
	private boolean	persistentlyStarted;

	public BundleStartLevelPojo(final BundleStartLevel sl) {
		this.startLevel = sl.getStartLevel();
		this.activationPolicyUsed = sl.isActivationPolicyUsed();
		this.persistentlyStarted = sl.isPersistentlyStarted();
	}

	public BundleStartLevelPojo() {

	}

	public int getStartLevel() {
		return startLevel;
	}

	public void setStartLevel(final int sl) {
		this.startLevel = sl;
	}

	public boolean getActivationPolicyUsed() {
		return activationPolicyUsed;
	}

	public void setActivationPolicyUsed(boolean activationPolicyUsed) {
		this.activationPolicyUsed = activationPolicyUsed;
	}

	public boolean getPersistentlyStarted() {
		return persistentlyStarted;
	}

	public void setPersistentlyStarted(boolean persistentlyStarted) {
		this.persistentlyStarted = persistentlyStarted;
	}

	@Override
	public String toString() {
		return "Startlevel " + startLevel;
	}

}
