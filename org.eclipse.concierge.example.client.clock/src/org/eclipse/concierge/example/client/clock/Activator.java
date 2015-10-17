package org.eclipse.concierge.example.client.clock;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.Collection;

import org.eclipse.concierge.example.service.clock.ClockService;

public class Activator implements BundleActivator {

	private ClockClient client;

	public void start(final BundleContext context) throws Exception {
		final Collection<ServiceReference<ClockService>> clockRefs = context.getServiceReferences(ClockService.class,
				"(" + ClockService.IMPLEMENTATION + "=" + ClockService.SIMPLE_IMPL + ")");

		if (clockRefs.isEmpty()) {
			System.err.println("NO CLOCK SERVICE IS AVAILABLE, NO CLIENT WILL BE STARTED");
		} else {
			client = new ClockClient(context.getService(clockRefs.iterator().next()));
		}
	}

	public void stop(final BundleContext context) throws Exception {
		client.stopClient();
	}

}
