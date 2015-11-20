package org.eclipse.concierge.stresstest;

import java.util.Random;

public class TopologyGenerator {

	public static void main(String... args) {
		Random random = new Random();

		final byte[] bytes = new byte[10240];
		random.nextBytes(bytes);

		sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
		System.out.println(encoder.encode(bytes));
	}

}
