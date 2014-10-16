package org.eclipse.concierge.compat.startlevel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation")
public class Activator implements BundleActivator {

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext context) throws Exception {
		final StartLevel startLevel = new StartLevelImpl(context);
		context.registerService(StartLevel.class, startLevel, null);
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(final BundleContext context) throws Exception {

	}

}
