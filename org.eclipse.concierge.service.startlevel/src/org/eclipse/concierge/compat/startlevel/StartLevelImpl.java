package org.eclipse.concierge.compat.startlevel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.startlevel.StartLevel;

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
