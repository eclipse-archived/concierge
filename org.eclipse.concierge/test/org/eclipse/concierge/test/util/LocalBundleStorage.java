package org.eclipse.concierge.test.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LocalBundleStorage {

	/** The singleton instance. */
	private static LocalBundleStorage instance = new LocalBundleStorage();

	private static boolean DEBUG = false;

	/** The properties contains the configuration. */
	private final Properties localStorageConfiguration;

	public static LocalBundleStorage getInstance() {
		return instance;
	}

	/** Constructor is private for singleton. */
	private LocalBundleStorage() {
		// get props from config file "concierge-test.properties"
		// if not present, use default "empty properties"
		Properties props = new Properties();
		InputStream is = this.getClass().getResourceAsStream(
				"/concierge-test.properties");
		if (is != null) {
			try {
				props.load(is);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					// ignore
				}
			}
		}
		this.localStorageConfiguration = props;
	}

	public File findLocalBundle(final String bundleName) {
		String path = (String) this.localStorageConfiguration
				.get("concierge.test.localDirectories");
		path += ":"
				+ (String) this.localStorageConfiguration
						.get("concierge.test.localCache");
		File[] pathElements = pathElements(path);
		for (int i = 0; i < pathElements.length; i++) {
			File dir = pathElements[i];
			FileFilter filter = new FileFilter() {
				public boolean accept(File pathname) {
					return bundleName.equals(pathname.getName());
				}
			};
			File foundFiles[] = dir.listFiles(filter);
			if (foundFiles.length == 1) {
				return foundFiles[0];
			} else {
				// logDebug("local bundle '" + bundleName + "' not found at " +
				// dir);
			}
		}
		return null;
	}

	public String findRemoteBundle(final String bundleName) {
		String urls = (String) this.localStorageConfiguration
				.get("concierge.test.remoteURLs");
		String[] urlElements = urls.split("\n");
		for (int i = 0; i < urlElements.length; i++) {
			String u = urlElements[i]
					+ (urlElements[i].endsWith("/") ? "" : "/") + bundleName;
			try {
				URL url = new URL(u);
				InputStream is = url.openStream();
				is.close();
				return url.toExternalForm();
			} catch (MalformedURLException e) {
				// ignore
			} catch (IOException e) {
				// ignore, bundle not found under URL
				// logDebug("remote bundle '" + bundleName + "' not found at " +
				// u);
			}
		}
		return null;
	}

	private void copy(InputStream in, String filename) throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		final int BUF_SIZE = 1 << 8;
		byte[] buffer = new byte[BUF_SIZE];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) > -1) {
			out.write(buffer, 0, bytesRead);
		}
		in.close();
		out.close();
	}

	public void clearLocalBundleCache() {
		logDebug("Clear Cache !");
		String localCacheDirname = (String) this.localStorageConfiguration
				.get("concierge.test.localCache");
		File cacheDir = new File(localCacheDirname);
		File[] files = cacheDir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File f = files[i];
				f.delete();
			}
		}
	}

	public String getUrlForBundle(final String bundleName) {
		if (new File(bundleName).exists()) {
			return bundleName;
		} else {
			File f = findLocalBundle(bundleName);
			if ((f != null) && (f.exists())) {
				logDebug("found bundle '" + bundleName + "' at " + f.getPath());
				return f.getPath();
			} else {
				if (bundleName.startsWith("http")) {
					return bundleName;
				} else {
					String s = findRemoteBundle(bundleName);
					if (s != null) {
						try {
							logDebug("found bundle '" + bundleName + "' at "
									+ s);

							// cache locally
							String localCacheDirname = (String) this.localStorageConfiguration
									.get("concierge.test.localCache");
							File localCacheDir = new File(localCacheDirname);
							localCacheDir.mkdirs();

							String cachedFilename = localCacheDirname + "/"
									+ bundleName;
							logDebug("caching bundle '" + bundleName + "' at "
									+ cachedFilename);
							URL url = new URL(s);
							copy(url.openStream(), cachedFilename);
							logDebug("use cached bundle '" + bundleName
									+ "' at " + cachedFilename);

							return cachedFilename;
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						logDebug("bundle '" + bundleName + "' not found ");
					}

					return bundleName;
				}
			}
		}
	}

	private void logDebug(String msg) {
		if (DEBUG) {
			System.err.println("LocalBundleStorage: " + msg);
		}
	}

	File[] pathElements(String path) {
		String[] pathElements = path.split(":");
		List<File> fileElements = new ArrayList<File>();
		for (int i = 0; i < pathElements.length; i++) {
			File f = new File(pathElements[i]);
			if (f.exists() && f.isDirectory()) {
				fileElements.add(f);
			}
		}
		return fileElements.toArray(new File[0]);
	}
}
