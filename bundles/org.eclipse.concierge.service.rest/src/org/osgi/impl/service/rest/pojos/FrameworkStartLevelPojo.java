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

import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.impl.service.rest.PojoReflector.RootNode;

/**
 * Pojo for the framework start level.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "frameworkStartLevel")
public final class FrameworkStartLevelPojo {

	private int	startLevel;
	private int	initialBundleStartLevel;

	public FrameworkStartLevelPojo(final FrameworkStartLevel fsl) {
		this.startLevel = fsl.getStartLevel();
		this.initialBundleStartLevel = fsl.getInitialBundleStartLevel();
	}

	public FrameworkStartLevelPojo() {

	}

	public int getStartLevel() {
		return startLevel;
	}

	public void setStartLevel(final int sl) {
		this.startLevel = sl;
	}

	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	public void setInitialBundleStartLevel(final int sl) {
		this.initialBundleStartLevel = sl;
	}

	@Override
	public String toString() {
		return "Startlevel " + startLevel;
	}

}
