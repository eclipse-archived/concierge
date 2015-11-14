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
package org.eclipse.concierge.compat.startlevel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation")
public class StartLevelImpl implements StartLevel {

	private final BundleContext context;
	
	public StartLevelImpl(BundleContext context){
		this.context = context;
	}

	@Override
	public int getStartLevel() {
		FrameworkStartLevel fsl = getFrameworkStartLevel0();
		return fsl.getStartLevel();
	}

	@Override
	public void setStartLevel(int startlevel) {
		FrameworkStartLevel fsl = getFrameworkStartLevel0();
		fsl.setStartLevel(startlevel);
	}

	@Override
	public int getBundleStartLevel(Bundle bundle) {
		BundleStartLevel bsl = getBundleStartLevel0(bundle);
		return bsl.getStartLevel();
	}

	@Override
	public void setBundleStartLevel(Bundle bundle, int startlevel) {
		BundleStartLevel bsl = getBundleStartLevel0(bundle);
		bsl.setStartLevel(startlevel);
	}

	@Override
	public int getInitialBundleStartLevel() {
		FrameworkStartLevel fsl = getFrameworkStartLevel0();
		return fsl.getInitialBundleStartLevel();
	}

	@Override
	public void setInitialBundleStartLevel(int startlevel) {
		FrameworkStartLevel fsl = getFrameworkStartLevel0();
		fsl.setInitialBundleStartLevel(startlevel);
	}

	@Override
	public boolean isBundlePersistentlyStarted(Bundle bundle) {
		BundleStartLevel bsl = getBundleStartLevel0(bundle);
		return bsl.isPersistentlyStarted();
	}

	@Override
	public boolean isBundleActivationPolicyUsed(Bundle bundle) {
		BundleStartLevel bsl = getBundleStartLevel0(bundle);
		return bsl.isActivationPolicyUsed();
	}
	
	private BundleStartLevel getBundleStartLevel0(Bundle bundle){
		return bundle.adapt(BundleStartLevel.class);
	}
	
	private FrameworkStartLevel getFrameworkStartLevel0(){
		Bundle bundle = context.getBundle(0);
		return bundle.adapt(FrameworkStartLevel.class);
	}
}
