package org.eclipse.concierge.stresstest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class BundleGenerator {

	private final String symbolicName;
	private final Version version;

	private final List<String> imports;
	private final List<String> exports;

	public BundleGenerator(final String symbolicName, final Version version) {
		this.symbolicName = symbolicName;
		this.version = version;
		this.imports = new ArrayList<String>();
		this.exports = new ArrayList<String>();
	}

	public BundleGenerator addPackageImport(final String pkgImport) {
		imports.add(pkgImport);
		return this;
	}

	public BundleGenerator addPackageImport(final String pkgImport,
			final VersionRange versionRange) {
		imports.add(pkgImport + ";version=\"" + versionRange.toString() + "\"");
		return this;
	}

	public BundleGenerator addPackageExport(final String pkgExport) {
		exports.add(pkgExport);
		return this;
	}

	public BundleGenerator addPackageExport(final String pkgExport,
			final Version version) {
		exports.add(pkgExport + ";version=" + version.toString());
		return this;
	}

	public InputStream getInputStream() throws IOException {
		final Manifest mf = new Manifest();
		final Attributes attrs = mf.getMainAttributes();
		attrs.put(Attributes.Name.MANIFEST_VERSION, "1");
		attrs.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		attrs.putValue(Constants.BUNDLE_NAME, symbolicName);
		attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		attrs.putValue(Constants.BUNDLE_VERSION, version.toString());
		if (!imports.isEmpty()) {
			attrs.putValue(Constants.IMPORT_PACKAGE, join(imports));
		}
		if (!exports.isEmpty()) {
			attrs.putValue(Constants.EXPORT_PACKAGE, join(exports));
		}

		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try (JarOutputStream jout = new JarOutputStream(bout, mf)) {
			jout.flush();
			jout.close();
			final byte[] b = bout.toByteArray();
			return new ByteArrayInputStream(b);
		}
	}

	public Bundle install(final BundleContext context) throws BundleException,
			IOException {
		return context.installBundle(symbolicName, getInputStream());
	}

	private String join(final List<String> list) {
		final StringBuilder builder = new StringBuilder();
		final String[] strs = list.toArray(new String[list.size()]);
		for (int i = 0; i < strs.length - 1; i++) {
			builder.append(strs[i]);
			builder.append(", ");
		}
		builder.append(strs[strs.length - 1]);
		return builder.toString();
	}

}
