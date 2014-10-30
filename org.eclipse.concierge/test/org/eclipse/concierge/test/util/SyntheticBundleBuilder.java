package org.eclipse.concierge.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;

/**
 * This class will support to create synthetic bundles. It will provide a fluent
 * interface.
 */
public class SyntheticBundleBuilder {

	private Map<Attributes.Name, String> manifestHeaders;
	private Map<String, File> files;

	public static SyntheticBundleBuilder newBuilder() {
		return new SyntheticBundleBuilder();
	}

	public SyntheticBundleBuilder() {
		// preserve files
		this.files = new HashMap<String, File>();
		// pre-fill default headers
		Map<Attributes.Name, String> headers = new HashMap<Attributes.Name, String>();
		headers.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		headers.put(new Attributes.Name(Constants.BUNDLE_MANIFESTVERSION), "2");
		this.manifestHeaders = headers;

	}

	public SyntheticBundleBuilder bundleSymbolicName(String bsn) {
		this.manifestHeaders.put(new Attributes.Name(
				Constants.BUNDLE_SYMBOLICNAME), bsn);
		return this;
	}

	public SyntheticBundleBuilder singleton() {
		String bsn = this.manifestHeaders.get(new Attributes.Name(
				Constants.BUNDLE_SYMBOLICNAME));
		bsn = bsn + ";singleton:=true";
		this.manifestHeaders.put(new Attributes.Name(
				Constants.BUNDLE_SYMBOLICNAME), bsn);
		return this;
	}

	public SyntheticBundleBuilder bundleVersion(String version) {
		this.manifestHeaders.put(new Attributes.Name(Constants.BUNDLE_VERSION),
				version);
		return this;
	}

	public String getBundleSymbolicName() {
		return this.manifestHeaders.get(new Attributes.Name(
				Constants.BUNDLE_SYMBOLICNAME));
	}

	public String getBundleVersion() {
		String version = this.manifestHeaders.get(new Attributes.Name(
				Constants.BUNDLE_VERSION));
		if (version == null) {
			return "0.0.0";
		}
		return version;
	}

	// public SynteticBundleBuilder manifest(String mf) {
	// this.manifest = mf;
	// return this;
	// }
	//

	public SyntheticBundleBuilder addManifestHeader(final String key,
			final String value) {
		// add the header into Manifest
		this.manifestHeaders.put(new Attributes.Name(key), value);
		return this;
	}

	public SyntheticBundleBuilder addManifestHeaders(
			final Map<String, String> headers) {
		// add the headers into Manifest
		for (Iterator<Map.Entry<String, String>> iter = headers.entrySet()
				.iterator(); iter.hasNext();) {
			final Map.Entry<String, String> entry = iter.next();
			this.manifestHeaders.put(new Attributes.Name(entry.getKey()),
					entry.getValue());
		}
		return this;
	}

	public SyntheticBundleBuilder addFile(String resPath, File f) {
		this.files.put(resPath, f);
		return this;
	}

	public InputStream asInputStream() {
		// copy MANIFEST to a jar file in memory
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final Manifest manifest = new Manifest();

		for (Iterator<Map.Entry<Attributes.Name, String>> iter = this.manifestHeaders
				.entrySet().iterator(); iter.hasNext();) {
			final Map.Entry<Attributes.Name, String> entry = iter.next();
			manifest.getMainAttributes().put(entry.getKey(), entry.getValue());
		}

		JarOutputStream jarStream = null;
		try {
			jarStream = new JarOutputStream(out, manifest);

			// copy files into JAR
			for (Iterator<Map.Entry<String, File>> iter = this.files.entrySet()
					.iterator(); iter.hasNext();) {
				Map.Entry<String, File> entry = iter.next();
				String resPath = entry.getKey();
				File f = entry.getValue();
				FileInputStream fis = new FileInputStream(f);
				JarEntry je = new JarEntry(resPath);
				jarStream.putNextEntry(je);
				TestUtils.copyStream(fis, jarStream);
				jarStream.closeEntry();
				fis.close();
			}

			jarStream.close();
			final InputStream is = new ByteArrayInputStream(out.toByteArray());
			return is;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (jarStream != null) {
				try {
					jarStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public File asFile() {
		final InputStream is = this.asInputStream();
		File destFile;
		try {
			destFile = new File("concierge-" + this.getBundleSymbolicName()
					+ "-" + getBundleVersion() + ".jar");
			TestUtils.copyStreamToFile(is, destFile);
			return destFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
