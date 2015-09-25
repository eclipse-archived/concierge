package org.eclipse.concierge.example.client.clock;

import org.eclipse.concierge.example.service.clock.ClockService;

public class ClockClient extends Thread {

	private ClockService clock;

	public ClockClient(final ClockService clock) {
		this.clock = clock;
		start();
	}

	public void run() {
		try {
			while (!isInterrupted()) {
				System.out.println("The current time is " + clock.getTime());
				Thread.sleep(5000);
			}
		} catch (final InterruptedException i) {
			// proceed
		}
	}

	public void stopClient() {
		this.interrupt();
		clock = null;
	}

}
