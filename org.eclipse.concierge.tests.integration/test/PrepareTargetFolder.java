import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.concierge.test.util.TestUtils;

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Jochen Hiller
 *******************************************************************************/

public class PrepareTargetFolder {

	private static final String BASE_URL = ""
			+ "https://raw.githubusercontent.com/JochenHiller/concierge-tests"
			+ "/master/target/";

	private static final String[] FILES_TO_TARGET = new String[] {
			"apache-felix/org.apache.felix.fileinstall_3.2.6.jar",
			"patched/com.sun.jersey_1.17.0.v20130314-2020.jar",
			"patched/jetty-osgi-boot-9.2.1.v20140609.jar",
			"patched/org.apache.felix.fileinstall-3.4.0.jar",
			"patched/org.eclipse.core.runtime_3.10.0.201411050014.jar",
			"bundles-fixed/org.eclipse.equinox.console_1.1.100.v20141023-1406.jar",
			"bundles-fixed/org.eclipse.equinox.supplement_1.6.0.v20141009-1504.jar",
			"plugins/concierge.test.version_0.1.0.jar",
			"plugins/concierge.test.version_0.2.0.jar",
			"plugins/concierge.test.version_1.0.0.201407232153.jar",
			"plugins/concierge.test.version_1.0.0.jar",
			"plugins/concierge.test.version_1.1.0.jar",
			"plugins/org.eclipse.concierge.extension.permission_1.0.0.201408052201.jar",
			"plugins/org.eclipse.concierge.service.xmlparser_1.0.0.201407191653.jar",
			"plugins/org.eclipse.concierge.test.support_1.0.0.jar",
			"plugins/shell-1.0.0.jar",
			"xtext-2.6.1/org.eclipse.xtend.lib_2.6.1.v201406120726.jar",
			"xtext-2.6.1/org.eclipse.xtext_2.6.1.v201406120726.jar",
			"xtext-2.6.1/org.eclipse.xtext.common.types_2.6.1.v201406120726.jar",
			"xtext-2.6.1/org.eclipse.xtext.util_2.6.1.v201406120726.jar",
			"xtext-2.6.1/org.eclipse.xtext.xbase_2.6.1.v201406120726.jar",
			"xtext-2.6.1/org.eclipse.xtext.xbase.lib_2.6.1.v201406120726.jar", };

	public static void main(String[] args) throws IOException {
		final int rc = new PrepareTargetFolder().doMain(args);
		System.exit(rc);
	}

	public int doMain(String[] args) throws IOException {
		for (int i = 0; i < FILES_TO_TARGET.length; i++) {
			String s = FILES_TO_TARGET[i];
			System.out.print("Copying " + s + " ...");
			copyUrl(s);
			System.out.println(" done");
		}
		System.out.println("All files copied to target folder !");
		return 0;
	}

	private void copyUrl(String file) throws IOException {
		URL sourceUrl = new URL(BASE_URL + file);
		InputStream inputStream = sourceUrl.openStream();
		// dest: current working dir
		String destPath = "./target/" + file;
		File destFile = new File(destPath);

		TestUtils.copyStreamToFile(inputStream, destFile);

	}
}
