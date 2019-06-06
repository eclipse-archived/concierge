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
package org.eclipse.concierge.compat.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.concierge.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the XargsLauncher with its pattern replacement and wildcard support.
 */
public class XargsFileLauncherPropertiesTest {

	private ByteArrayOutputStream baos;
	private XargsFileLauncher l;

	@Before
	public void setUp() {
		createLauncher();
	}

	@After
	public void tearDown() {
		baos = null;
		l = null;
	}

	@Test
	public void testConstructor() {
		new XargsFileLauncher();
	}

	@Test
	public void testReplaceVariable() {
		Map<String, String> p = new HashMap<String, String>();
		Assert.assertEquals("${prop1}", l.replaceVariable("${prop1}", p));
		Assert.assertEquals("XXX${prop1}YYY",
				l.replaceVariable("XXX${prop1}YYY", p));
		p.put("prop1", "value1");
		Assert.assertEquals("value1", l.replaceVariable("${prop1}", p));
		Assert.assertEquals("XXXvalue1YYY",
				l.replaceVariable("XXX${prop1}YYY", p));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testReplaceMultipleVariables() {
		Map<String, String> p = new HashMap<String, String>();
		Assert.assertEquals("${p1}", l.replaceVariable("${p1}", p));
		Assert.assertEquals("XXX${p1}YYY", l.replaceVariable("XXX${p1}YYY", p));
		p.put("p1", "v1");
		p.put("p2", "v2");
		Assert.assertEquals("v1v2", l.replaceVariable("${p1}${p2}", p));
		Assert.assertEquals("XXXv1YYYv2ZZZ",
				l.replaceVariable("XXX${p1}YYY${p2}ZZZ", p));
		Assert.assertEquals("v1v1", l.replaceVariable("${p1}${p1}", p));
		Assert.assertEquals("XXXv1YYYv1ZZZ",
				l.replaceVariable("XXX${p1}YYY${p1}ZZZ", p));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testReplaceVariablesPerformance() {
		Map<String, String> p = new HashMap<String, String>();
		p.put("p1", "v1");
		p.put("p2", "v2");

		int N = 500000;

		System.gc();
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			Assert.assertEquals("v1v2", l.replaceVariable("${p1}${p2}", p));
			Assert.assertEquals("XXXv1YYYv2ZZZ",
					l.replaceVariable("XXX${p1}YYY${p2}ZZZ", p));
		}
		long endTime = System.currentTimeMillis();
		System.out.println("test10ReplaceVariablesPerformance: " + N
				+ " runs in " + (endTime - startTime) + " ms.");
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsFileEmpty() throws IOException {
		Map<String, String> props = processProperties("");
		Assert.assertEquals(0, props.size());
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsFileOneLine() throws IOException {
		Map<String, String> props = processProperties("-Dprop=value");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("value", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsFileTwoLines() throws IOException {
		Map<String, String> props = processProperties(
				"-Dprop1=value1\n-Dprop2=value2");
		Assert.assertEquals(2, props.size());
		Assert.assertEquals("value1", props.get("prop1"));
		Assert.assertEquals("value2", props.get("prop2"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsSplitOverMultipleLines()
			throws IOException {
		Map<String, String> props = processProperties(
				"-Dprop=value1\\\n value2");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("value1value2", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsSplitOverTwoLinesWithComments()
			throws IOException {
		Map<String, String> props = processProperties(
				"-Dprop=value1\\ # comment \n value2  # comment");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("value1value2", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsSplitOverMultipleLinesWithComments()
			throws IOException {
		Map<String, String> props = processProperties(
				"-Dprop=v1\\ # c \n      v2 \\ # c \n\t\tv3 \\ #\n v4 \\ # comment \n v5 # c");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("v1v2v3v4v5", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsAddedByPlus() throws IOException {
		Map<String, String> props = processProperties(
				"-Dprop=v1\n-Dprop+=v2\n-Dprop+=v3");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("v1v2v3", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsAddedByPlusNotInitialized()
			throws IOException {
		Map<String, String> props = processProperties(
				"-Dprop+=v1\n-Dprop+=v2\n-Dprop+=v3");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("v1v2v3", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	@Test
	public void testGetPropertiesFromXargsNoEquals() throws IOException {
		Map<String, String> props = processProperties("-Dprop_v");
		Assert.assertEquals(0, props.size());
		String expectedErrMsg = "[XargsFileLauncher] WRONG PROPERTY DEFINITION: "
				+ "EQUALS for -Dname=value IS MISSING, IGNORE '-Dprop_v'"
				+ System.getProperty( "line.separator");
		Assert.assertThat(getStreamErr(), is(equalTo(expectedErrMsg)));
	}

	@Test
	public void testGetPropertiesFromXargsNoName() throws IOException {
		Map<String, String> props = processProperties("-D=v");
		Assert.assertEquals(0, props.size()); // no name
		props = processProperties("-D+=v");
		Assert.assertEquals(0, props.size());
		String expectedErrMsg = "[XargsFileLauncher] WRONG PROPERTY DEFINITION: "
				+ "NAME for -Dname+=value IS MISSING, IGNORE '-D+=v'"
				+ System.getProperty( "line.separator");
		Assert.assertThat(getStreamErr(), is(equalTo(expectedErrMsg)));
	}

	@Test
	public void testGetPropertiesFromXargsNoValue() throws IOException {
		Map<String, String> props = processProperties("-Dprop=");
		Assert.assertEquals(1, props.size());
		Assert.assertEquals("", props.get("prop"));
		// no errors reported
		Assert.assertThat(getStreamErr(), is(equalTo("")));
	}

	// private helper methods

	private Map<String, String> processProperties(String s) throws IOException {
		createLauncher();
		InputStream is = TestUtils.createInputStreamFromString(s);
		Map<String, String> props = l.getPropertiesFromXargsInputStream(is);
		return props;
	}

	private void createLauncher() {
		this.baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		l = new XargsFileLauncher(ps);
	}

	private String getStreamErr() {
		try {
			return baos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "Error: " + e.getMessage();
		}
	}

}
