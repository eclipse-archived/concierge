package org.eclipse.concierge.example.client.pi_led;

import org.eclipse.concierge.example.service.pi_led.LEDService;
import org.eclipse.concierge.shell.commands.ShellCommandGroup;

public class LEDClient implements ShellCommandGroup {

	private LEDService led;

	public LEDClient(final LEDService led) {
		this.led = led;
	}

	public String getHelp() {
		return "led.on or led.off\n";
	}

	public String getGroup() {
		return "led";
	}

	public void handleCommand(final String command, final String[] args) throws Exception {
		if (command.equalsIgnoreCase("on")) {
			led.setStatus(true);
			return;
		} else if (command.equalsIgnoreCase("off")) {
			led.setStatus(false);
			return;
		}
		System.err.println("unknown command " + command);
	}

}
