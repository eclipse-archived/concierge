/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Jochen Hiller
 *******************************************************************************/
package org.eclipse.concierge.test.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is a ClassLoader which allows to filter loading of specific
 * classes.
 * 
 * E.g. we can mock different compact profiles by hiding the files used for
 * identification of compact profile.
 * 
 * The "filter" can be an expression like:
 * <ul>
 * <li>org.w3c.dom.Document</li>
 * <li>org.w3c.dom.*</li>
 * </ul>
 * 
 * @author Jochen Hiller
 */
public class FilteredClassLoader extends ClassLoader {

	/** Is protected to give potential subclasses access to this field. */
	protected final String filterExpression;

	public FilteredClassLoader(ClassLoader parent) {
		super(parent);
		this.filterExpression = null; // mark: we do NOT have a filter
	}

	public FilteredClassLoader(ClassLoader parent, String expr) {
		super(parent);
		this.filterExpression = expr;
	}

	/**
	 * This method might be overridden by subclasses to handle special cases.
	 */
	public boolean isFiltered(String className) {
		if (filterExpression != null) {
			if (filterExpression.endsWith(".*")) {
				// include trailing dot in pkgName
				String pkgName = filterExpression.substring(0,
						filterExpression.lastIndexOf(".*") + 1);
				if (className.startsWith(pkgName)) {
					return true;
				} else {
					return false;
				}
			} else {
				if ((className.startsWith(filterExpression))) {
					return true;
				} else {
					return false;
				}
			}
		} else {
			return false;
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		logInfo("loading class '" + name + "'");
		// load java.* classes always from parent class loader
		if (name.startsWith("java.")) {
			return super.loadClass(name);
		}
		// if class is filtered, throw a ClassNotFoundException
		if (isFiltered(name)) {
			String msg = "Could not find the class '" + name
					+ "' due to filter '" + filterExpression + "'";
			logError(msg);
			throw new ClassNotFoundException(msg);
		}
		// otherwise load class themselves
		return loadAndDefineClass(name);
	}

	/**
	 * Load and create a class internally if found.
	 */
	private Class<?> loadAndDefineClass(String classNameWithDots)
			throws ClassNotFoundException {
		String classNameAsFileName = classNameWithDots.replace('.',
				'/') + ".class";
		try {
			byte[] classByteCodeBuffer = loadClassByteCode(classNameAsFileName);
			// define and resolve the class
			Class<?> clazz = defineClass(classNameWithDots, classByteCodeBuffer,
					0, classByteCodeBuffer.length);
			resolveClass(clazz);
			return clazz;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Loads the byte code for class specified by name into a byte array buffer.
	 */
	private byte[] loadClassByteCode(String classNameAsFileName)
			throws IOException {
		InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream(classNameAsFileName);
		int size = inputStream.available();
		byte buffer[] = new byte[size];
		DataInputStream dataInputStream = new DataInputStream(inputStream);
		dataInputStream.readFully(buffer);
		dataInputStream.close();
		return buffer;
	}

	private void logInfo(String msg) {
		// enable to trace class loading
		// System.out.println("[FilteredClassLoader] " + msg);
	}

	private void logError(String msg) {
		// enable to trace class loading
		// System.err.println("[FilteredClassLoader] " + msg);
	}
}