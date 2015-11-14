package org.eclipse.concierge.example.service.pi_led.impl;

import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.concierge.example.service.pi_led.LEDService;

public class LEDServiceImpl implements LEDService {

	private static final String SYS_FS_TRIGGER = "/sys/class/leds/led0/trigger";
	private static final String SYS_FS_LED0 = "/sys/class/leds/led0/brightness";

	LEDServiceImpl() throws IOException {
		final FileOutputStream fos = new FileOutputStream(SYS_FS_TRIGGER);
		fos.write("none".getBytes());
		fos.close();
	}

	public void setStatus(final boolean on) throws IOException {
		final FileOutputStream fos = new FileOutputStream(SYS_FS_LED0);
		fos.write(on ? "1".getBytes() : "0".getBytes());
		fos.close();
	}

	public void stop() throws IOException {
		final FileOutputStream fos = new FileOutputStream(SYS_FS_TRIGGER);
		fos.write("mmc0".getBytes());
		fos.close();
	}
}
