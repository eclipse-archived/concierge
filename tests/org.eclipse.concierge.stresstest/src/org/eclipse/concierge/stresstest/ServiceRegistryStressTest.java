package org.eclipse.concierge.stresstest;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ServiceRegistryStressTest {

	private static final int NUM = 10000;

	private static final String CLS_NAME = Object.class.getName();

	private Random random = new Random();

	public void run(final BundleContext context) throws Exception {
		System.out.println("generating randomness");

		final byte[] bytes = new byte[NUM];
		random.nextBytes(bytes);

		final byte[] bytes2 = new byte[NUM / 10];
		random.nextBytes(bytes2);

		final ServiceRegistration<?>[] services = new ServiceRegistration[NUM];

		System.out.println("done");

		final float time = System.nanoTime();
		for (int i = 0; i < NUM; i++) {
			final Dictionary<String, Object> props = new Hashtable<String, Object>();

			props.put("key", bytes[i]);

			services[i] = context
					.registerService(CLS_NAME, new Object(), props);
		}
		System.out.println("elapsed time for registration: "
				+ (System.nanoTime() - time) / 1000000);

		final float time2 = System.nanoTime();
		for (int i = 0; i < NUM / 10; i++) {
			context.getServiceReferences((String) null, "(key=" + bytes2[i]
					+ ")");
		}
		System.out.println("elapsed time for lookup: "
				+ (System.nanoTime() - time2) / 1000000);

		final float time3 = System.nanoTime();
		for (int i = 0; i < NUM; i++) {
			services[i].unregister();
		}
		System.out.println("elapsed time for unregistration: "
				+ (System.nanoTime() - time3) / 1000000);

	}

}
