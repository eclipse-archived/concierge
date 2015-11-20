package org.eclipse.concierge.stresstest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.FrameworkWiring;

public class ResolverStressTest {

	private static final int NUM = 1000;
	private static final int NUM_PKGS = 50;
	private static final int MIN_VERSION_MAJOR = 1;
	private static final int MAX_VERSION_MAJOR = 20;
	private static final int MAX_IMPORTS_EXPORTS = 5;

	private static final String[] pkgNames = new String[NUM_PKGS];

	static {
		for (int i = 0; i < NUM_PKGS; i++) {
			pkgNames[i] = "org.eclipse.test.package" + i;
		}
	}

	private NotRandom random = new NotRandom();

	public void run(final BundleContext context) throws BundleException,
			IOException {
		final Bundle[] bundles = new Bundle[NUM];

		long installationTime = 0;

		for (int i = 0; i < NUM; i++) {
			final BundleGenerator gen = new BundleGenerator("bundle" + i,
					new Version(1, 0, i));

			final int dirs = random.nextInt(MAX_IMPORTS_EXPORTS);

			final Set<String> imports = new HashSet<String>();
			final Set<String> exports = new HashSet<String>();

			for (int j = 0; j < dirs; j++) {
				if (random.nextBoolean()) {
					// IMPORT
					final int v1Major = MIN_VERSION_MAJOR
							+ random.nextInt(MAX_VERSION_MAJOR
									- MIN_VERSION_MAJOR + 1);
					final int v2Major = MIN_VERSION_MAJOR
							+ random.nextInt(MAX_VERSION_MAJOR
									- MIN_VERSION_MAJOR + 1);

					final Version v1 = new Version(v1Major, random.nextInt(10),
							random.nextInt(10));

					final Version v2 = new Version(v2Major, random.nextInt(10),
							random.nextInt(10));

					final Version lowerBound = v1.compareTo(v2) < 1 ? v1 : v2;
					final Version upperBound = v1.compareTo(v2) >= 1 ? v1 : v2;

					gen.addPackageImport(drawPackage(imports),
							new VersionRange('[', lowerBound, upperBound, ')'));
				} else {
					// EXPORT
					final Version version = new Version(
							random.nextInt(MAX_VERSION_MAJOR
									- MIN_VERSION_MAJOR + 1),
							random.nextInt(10), random.nextInt(10));

					gen.addPackageExport(drawPackage(exports), version);
				}
			}

			final long t = System.nanoTime();
			bundles[i] = gen.install(context);
			installationTime += (System.nanoTime() - t);
		}

		System.err.println("INSTALLATION TIME " + (installationTime / 1000000));

		final FrameworkWiring fw = context.getBundle(0).adapt(
				FrameworkWiring.class);

		System.err.println("RESOLVING");
		final long time = System.nanoTime();
		fw.resolveBundles(Arrays.asList(bundles));
		System.err.println("RESOLVE TIME " + (System.nanoTime() - time)
				/ 1000000);

	}

	private String drawPackage(final Set<String> history) {
		String drawn;
		do {
			drawn = pkgNames[random.nextInt(NUM_PKGS)];
		} while (history.contains(drawn));
		history.add(drawn);
		return drawn;
	}

	@SuppressWarnings("unused")
	private void printHeaders(Dictionary<String, String> headers) {
		System.err.println("HEADERS:");
		for (final Enumeration<String> e = headers.keys(); e.hasMoreElements();) {
			final String key = e.nextElement();
			System.err.println(key + " " + headers.get(key));
		}
	}

}
