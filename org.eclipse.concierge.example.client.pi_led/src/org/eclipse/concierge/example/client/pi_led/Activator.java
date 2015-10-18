package org.eclipse.concierge.example.client.pi_led;

import org.eclipse.concierge.example.service.pi_led.LEDService;
import org.eclipse.concierge.shell.commands.ShellCommandGroup;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;

public class Activator implements BundleActivator {

	private LEDClient client;

	public void start(final BundleContext context) throws Exception {
		final ServiceReference<RemoteOSGiService> sref = context.getServiceReference(RemoteOSGiService.class);
		if (sref == null) {
			System.err.println("Remote service not running. Cannot retrieve LEDService");
			return;
		}

		final RemoteOSGiService remote = context.getService(sref);

		final URI uri = (URI.create("r-osgi://192.168.7.4"));
		remote.connect(uri);
		final RemoteServiceReference[] rrefs = remote.getRemoteServiceReferences(uri, LEDService.class.getName(), null);
		if (rrefs == null || rrefs.length == 0) {
			System.err.println("No LEDService found on the remote device");
		}

		final LEDService service = (LEDService) remote.getRemoteService(rrefs[0]);

		client = new LEDClient(service);
		context.registerService(ShellCommandGroup.class, client, null);
	}

	public void stop(final BundleContext context) throws Exception {

	}

}
