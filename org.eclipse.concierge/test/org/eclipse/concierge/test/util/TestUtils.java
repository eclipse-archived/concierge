package org.eclipse.concierge.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Test utilities to avoid dependencies to external libraries.
 */
public class TestUtils {

	public static File createFileFromString(final String content)
			throws IOException {
		return createFileFromString(content, "tmp");
	}

	public static File createFileFromString(final String content,
			final String suffix) throws IOException {
		final File file = File.createTempFile("concierge-", "." + suffix);
		final FileOutputStream fos = new FileOutputStream(file);
		final PrintStream ps = new PrintStream(fos);
		ps.println(content);
		ps.close();
		fos.close();
		return file;
	}

	public static void copyFile(File srcFile, File destFile) throws IOException {
		FileInputStream fis = new FileInputStream(srcFile);
		FileOutputStream fos = new FileOutputStream(destFile);
		copyStream(fis, fos);
		fos.close();
		fis.close();
	}

	public static void copyDirectory(File srcDir, File destDir)
			throws IOException {
		File[] srcFiles = srcDir.listFiles();
		if (srcFiles == null) {
			throw new IOException("Failed to list contents of " + srcDir);
		}
		if (destDir.exists()) {
			if (!(destDir.isDirectory())) {
				throw new IOException("Destination '" + destDir
						+ "' exists but is not a directory");
			}
		} else if ((!(destDir.mkdirs())) && (!(destDir.isDirectory()))) {
			throw new IOException("Destination '" + destDir
					+ "' directory cannot be created");
		}
		if (!(destDir.canWrite())) {
			throw new IOException("Destination '" + destDir
					+ "' cannot be written to");
		}

		for (File srcFile : srcFiles) {
			File dstFile = new File(destDir, srcFile.getName());
			if (srcFile.isDirectory())
				copyDirectory(srcFile, dstFile);
			else {
				copyFile(srcFile, dstFile);
			}
		}
	}

	public static void copyStreamToFile(InputStream inputStream, File destFile)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(destFile);
		copyStream(inputStream, fos);
		fos.close();
	}

	public static void copyStream(InputStream inputStream,
			OutputStream outputStream) throws IOException {
		byte[] bytes = new byte[4096];
		int read = inputStream.read(bytes, 0, 4096);
		while (read > 0) {
			outputStream.write(bytes, 0, read);
			read = inputStream.read(bytes, 0, 4096);
		}
	}

}
