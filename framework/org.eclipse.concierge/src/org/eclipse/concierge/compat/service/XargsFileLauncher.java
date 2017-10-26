/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge.compat.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.concierge.BundleImpl.Revision;
import org.eclipse.concierge.Concierge;
import org.eclipse.concierge.Factory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;

public class XargsFileLauncher {

	protected static final boolean WIN = System.getProperty("os.name")
			.toLowerCase().startsWith("win");

	private PrintStream streamErr;

	public XargsFileLauncher() {
		this(System.err);
	}

	public XargsFileLauncher(PrintStream err) {
		streamErr = err;
	}

	/**
	 * process an init.xargs-style file.
	 * 
	 * @param file
	 *            the file.
	 * @return the startlevel.
	 * @throws BundleException
	 * @throws FileNotFoundException
	 * @throws Throwable
	 *             if something goes wrong. For example, if strict startup is
	 *             set and the installation of a bundle fails.
	 */
	public Concierge processXargsFile(final File file)
			throws BundleException, FileNotFoundException {
		InputStream inputStream = new FileInputStream(file);
		// we have to preserve the properties for later variable and wildcard
		// replacement
		final Map<String, String> passedProperties = getPropertiesFromXargsInputStream(
				inputStream);

		// now process again for install/start options with given properties
		inputStream = new FileInputStream(file);
		return processXargsInputStream(passedProperties, inputStream);
	}

	public Concierge processXargsInputStream(
			final Map<String, String> passedProperties,
			final InputStream inputStream)
					throws BundleException, FileNotFoundException {

		// create framework with given properties
		final Concierge concierge = (Concierge) new Factory()
				.newFramework(passedProperties);
		concierge.init();

		// we will start Concierge immediately BEFORE installing
		// any bundles into it.
		// This will result in a natural order of installed bundles.
		concierge.start();

		if (concierge.restart) {
			return concierge;
		}

		final BundleContext context = concierge.getBundleContext();

		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(inputStream));

		try {
			final HashMap<String, Bundle> memory = new HashMap<String, Bundle>(
					0);
			String token;
			int initLevel = 1;
			boolean skipProcessing = false;

			while (!skipProcessing && ((token = reader.readLine()) != null)) {
				token = token.trim();
				if (token.equals("")) {
					continue;
				} else if (token.charAt(0) == '#') {
					continue;
				} else if (token.startsWith("-initlevel")) {
					token = getArg(token, 10);
					initLevel = Integer.parseInt(token);
					continue;
				} else if (token.startsWith("-all")) {
					token = getArg(token, 4);
					final File jardir;
					if (token.isEmpty()) {
						jardir = new File(
								new URL(concierge.BUNDLE_LOCATION).getFile());
					} else {
						jardir = new File(token);
					}
					final File files[];
					files = jardir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(".jar")
									|| name.toLowerCase().endsWith(".zip");
						}
					});
					if (files == null) {
						printErr("NO FILES FOUND IN " + jardir.getPath());
						break;
					}
					// first install all bundles
					final List<Bundle> bundlesToStart = new ArrayList<Bundle>();
					for (int i = 0; i < files.length; i++) {
						if (files[i].isDirectory()) {
							continue;
						}
						final Bundle b = (Bundle) context
								.installBundle(files[i].getPath());
						// adapt to BundleStartLevel
						final BundleStartLevel bundleStartLevel = b
								.adapt(BundleStartLevel.class);
						bundleStartLevel.setStartLevel(initLevel);
						bundlesToStart.add(b);
					}
					// then start all bundles (if not a fragment)
					for (Iterator<Bundle> iter = bundlesToStart.iterator(); iter
							.hasNext();) {
						Bundle b = iter.next();
						// is it a fragment?
						final Revision rev = (Revision) b
								.adapt(BundleRevision.class);
						if (!rev.isFragment()) {
							b.start();
						}
					}
					continue;
				} else if (token.startsWith("-istart")) {
					String bundleLocation = getArg(token, 7);
					bundleLocation = replaceVariable(bundleLocation,
							passedProperties);
					bundleLocation = resolveWildcardName(bundleLocation);
					final Bundle bundle = context.installBundle(bundleLocation);
					// adapt to BundleStartLevel
					final BundleStartLevel bundleStartLevel = bundle
							.adapt(BundleStartLevel.class);
					bundleStartLevel.setStartLevel(initLevel);
					bundle.start();
				} else if (token.startsWith("-install")) {
					String bundleLocation = getArg(token, 8);
					bundleLocation = replaceVariable(bundleLocation,
							passedProperties);
					bundleLocation = resolveWildcardName(bundleLocation);
					final Bundle bundle = context.installBundle(bundleLocation);
					// adapt to BundleStartLevel
					final BundleStartLevel bundleStartLevel = bundle
							.adapt(BundleStartLevel.class);
					bundleStartLevel.setStartLevel(initLevel);
					memory.put(bundleLocation, bundle);
				} else if (token.startsWith("-start")) {
					String bundleLocation = getArg(token, 6);
					bundleLocation = replaceVariable(bundleLocation,
							passedProperties);
					bundleLocation = resolveWildcardName(bundleLocation);
					final Bundle bundle = memory.remove(bundleLocation);
					if (bundle == null) {
						printErr("Bundle " + bundleLocation
								+ " is marked to be started but has not been "
								+ "installed before. Ignoring the command !");
					} else {
						// set start level again in case it has been changed
						// meanwhile
						final BundleStartLevel bundleStartLevel = bundle
								.adapt(BundleStartLevel.class);
						bundleStartLevel.setStartLevel(initLevel);
						bundle.start();
					}
				} else if (token.startsWith("-skip")) {
					// skip the remaining part of the xargs file
					skipProcessing = true;
				}
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException ioe) {

			}
			// formerly Concierge was started here.
			// we moved that to start Concierge at beginning
		}

		return concierge;
	}

	public Map<String, String> getPropertiesFromXargsInputStream(
			final InputStream inputStream) {
		final Map<String, String> properties = new HashMap<String, String>();
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(inputStream));

		try {
			String line;
			String token;
			while ((line = reader.readLine()) != null) {
				token = line.trim();
				if (token.equals("")) {
					continue;
				} else if (token.charAt(0) == '#') {
					continue;
				} else if (token.startsWith("-D")) {
					token = getArg(token, 2);
					// get key and value
					int pos = token.indexOf("=");
					if (pos < 0) {
						printErr("WRONG PROPERTY DEFINITION: "
								+ "EQUALS for -Dname=value IS MISSING, IGNORE '"
								+ line + "'");
					} else if (pos == 0) {
						printErr("WRONG PROPERTY DEFINITION: "
								+ "NAME for -Dname=value IS MISSING, IGNORE '"
								+ line + "'");
					} else if (pos > 0) {
						// do we have "+=" syntax?
						boolean doAdd = token.charAt(pos - 1) == '+';
						String key = token.substring(0, doAdd ? pos - 1 : pos);
						if (key.length() == 0) {
							printErr("WRONG PROPERTY DEFINITION: "
									+ "NAME for -Dname+=value IS MISSING, IGNORE '"
									+ line + "'");
							continue;
						}
						String value = token.substring(pos + 1);
						// handle multi line properties
						while (value.endsWith("\\")) {
							token = reader.readLine();
							// filter out comment and trim string
							token = getArg(token, 0);
							// append trimmed value without backslash plus next
							// line
							value = value.substring(0, value.length() - 1)
									.trim() + token.trim();
						}
						value = replaceVariable(value, properties);
						if (doAdd) {
							String oldValue = properties.get(key);
							properties.put(key,
									(oldValue == null ? "" : oldValue) + value);
						} else {
							properties.put(key, value);
						}
					}
					continue;
				} else if (token.startsWith("-profile")) {
					token = getArg(token, 8);
					properties.put("ch.ethz.systems.concierge.profile", token);
					continue;
				} else if (token.equals("-init")) {
					properties.put(Constants.FRAMEWORK_STORAGE_CLEAN,
							Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
				} else if (token.startsWith("-startlevel")) {
					token = getArg(token, 11);
					properties.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
							token);
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException ioe) {

			}
		}
		return properties;
	}

	/**
	 * get the argument from a start list entry.
	 * 
	 * @param entry
	 *            the entry.
	 * @param cmdLength
	 *            length of command.
	 * @return the argument.
	 */
	private static String getArg(final String entry, final int cmdLength) {
		// strip command
		final String str = entry.substring(cmdLength);
		// strip comments
		int pos = str.indexOf("#");
		return pos > -1 ? str.substring(0, pos).trim() : str.trim();
	}

	// package scope for testing support

	/** Regex pattern for finding ${property} variable. */
	static final String regex = "\\$\\{([^}]*)\\}";
	/** Precompiler pattern for regex. */
	static final Pattern pattern = Pattern.compile(regex);

	/**
	 * Replace all ${propertyName} entries via its property value. The
	 * implementation has been optimized to use regex pattern matcher.
	 */
	String replaceVariable(final String line,
			final Map<String, String> properties) {
		final Matcher matcher = pattern.matcher(line);
		String replacedLine = line;
		int pos = 0;
		while (matcher.find(pos)) {
			pos = matcher.end();
			String variable = matcher.group();
			String propertyName = variable.substring(2, variable.length() - 1);
			String propertyValue = properties.get(propertyName);
			if (propertyValue != null) {
				replacedLine = replacedLine.replace(variable, propertyValue);
			}
		}
		return replacedLine;
	}

	/**
	 * Resolve bundle names with wildcards included.
	 */
	String resolveWildcardName(final String bundleName) {
		if (!bundleName.contains("*")) {
			return bundleName;
		}
		// TODO how to check http protocol?
		final File dir = new File(
				bundleName.substring(0, bundleName.lastIndexOf("/")));
		// try to use a file filter
		final FileFilter filter = new FileFilter() {
			public boolean accept(final File pathname) {
				final String preStar = bundleName.substring(0,
						bundleName.lastIndexOf("*"));
				final String postStar = bundleName
						.substring(bundleName.lastIndexOf("*") + 1);

				final String path = WIN ? pathname.getPath().replace('\\', '/')
						: pathname.getPath();

				return path.startsWith(preStar) && path.endsWith(postStar);
			}
		};
		final File foundFiles[] = dir.listFiles(filter);
		if ((foundFiles == null) || foundFiles.length == 0) {
			return bundleName; // use default name in case nothing found
		} else if (foundFiles.length == 1) {
			return foundFiles[0].getPath(); // exact match
		} else if (foundFiles.length > 1) {
			// sort the list of found files, takes the "newest" one
			final ArrayList<String> sortedFiles = new ArrayList<String>();
			for (int i = 0; i < foundFiles.length; i++) {
				sortedFiles.add(foundFiles[i].getPath());
			}
			Collections.sort(sortedFiles, Collections.reverseOrder());
			return sortedFiles.get(0);
		}
		return bundleName;
	}

	private void printErr(String msg) {
		streamErr.println("[XargsFileLauncher] " + msg);
	}
}
