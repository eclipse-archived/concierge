package org.eclipse.concierge.example.service.pi_led;

import java.io.IOException;

public interface LEDService {

	void setStatus(boolean on) throws IOException;
	
}
