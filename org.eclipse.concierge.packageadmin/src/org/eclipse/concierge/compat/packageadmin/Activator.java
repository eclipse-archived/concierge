package org.eclipse.concierge.compat.packageadmin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

@SuppressWarnings("deprecation")
public class Activator implements BundleActivator {

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext context) throws Exception {
		final PackageAdmin packageAdmin = new PackageAdminImpl(context);
		context.registerService(PackageAdmin.class, packageAdmin, null);
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(final BundleContext context) throws Exception {

	}

}
