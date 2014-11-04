/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.test.suite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.concierge.test.util.TestUtils;
import org.junit.Test;

public class EclipseSmartHomeStartWithConcierge {

	private static final String OPENHAB2_RUNTIME_PATH = "../openhab2-runtime/openhab2";
	private static final String START_CONCIERGE_SH = "start_concierge_debug.sh";

	@Test
	public void testRunEclipseSmartHomeViaScript() throws IOException {
		copyPatches("../concierge-tests/patches/openhab2/patches",
				OPENHAB2_RUNTIME_PATH);
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("./" + START_CONCIERGE_SH, new String[] {},
				new File(OPENHAB2_RUNTIME_PATH));

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new InputStreamReader(
				proc.getErrorStream()));

		// read the output from the command
		System.out.println("./" + START_CONCIERGE_SH + "\n");
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}

		// read any errors from the attempted command
		while ((s = stdError.readLine()) != null) {
			System.out.println(s);
		}
	}

	@Test
	public void testCopyOnly() throws IOException {
		copyPatches("../concierge-tests/patches/openhab2/patches",
				OPENHAB2_RUNTIME_PATH);
	}

	private void copyPatches(String src, String dest) throws IOException {
		File srcDir = new File(src);
		File destDir = new File(dest);
		TestUtils.copyDirectory(srcDir, destDir);
		File shellScript = new File(dest + "/start_concierge_debug.sh");
		shellScript.setExecutable(true);
	}
}
