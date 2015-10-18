package org.eclipse.concierge.example.service.pi_led.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.concierge.example.service.pi_led.LEDService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.RemoteOSGiService;

public class Activator implements BundleActivator {

	private LEDServiceImpl service;

	public void start(final BundleContext context) throws Exception {
		service = new LEDServiceImpl();

		final Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);

		context.registerService(LEDService.class, service, props);
	}

	public void stop(final BundleContext context) throws Exception {
		service.stop();
		service = null;
	}

}
