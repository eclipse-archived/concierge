/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.concierge.shell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.eclipse.concierge.shell.commands.ShellCommandGroup;

/**
 * Bundle activator for the shell bundle.
 * 
 * @author Jan S. Rellermeyer
 */
public class ShellActivator implements BundleActivator {
	/**
	 * the shell instance.
	 */
	private Shell shell;

	/**
	 * the bundle context.
	 */
	static BundleContext context;

	/**
	 * called, when the bundle is started.
	 * 
	 * @param context
	 *            the bundle context.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext context) throws Exception {
		ShellActivator.context = context;
		List<ShellCommandGroup> plugins = new ArrayList<ShellCommandGroup>();

		final ServiceReference<?> pkgAdminRef = context
				.getServiceReference("org.osgi.service.packageadmin.PackageAdmin");
		if (pkgAdminRef != null) {
			plugins.add(new PackageAdminCommandGroup(context
					.getService(pkgAdminRef)));
		}

		shell = new Shell(System.out, System.err,
				(ShellCommandGroup[]) plugins
						.toArray(new ShellCommandGroup[plugins.size()]));
		context.addServiceListener(shell, "(" + Constants.OBJECTCLASS + "="
				+ ShellCommandGroup.class.getName() + ")");

		final Collection<ServiceReference<ShellCommandGroup>> existingGroups = context
				.getServiceReferences(ShellCommandGroup.class, null);
		if (existingGroups != null) {
			for (final ServiceReference<ShellCommandGroup> group : existingGroups) {
				shell.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED,
						group));
			}
		}
	}

	/**
	 * called, when the bundle is stopped.
	 * 
	 * @param context
	 *            the bundle context.
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(final BundleContext context) throws Exception {
		Shell.running = false;
		shell.interrupt();
	}

}
