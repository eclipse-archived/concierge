package org.eclipse.concierge.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

	public static InputStream createInputStreamFromString(final String content)
			throws IOException {
		final InputStream stream = new ByteArrayInputStream(
				content.getBytes(StandardCharsets.UTF_8));
		return stream;
	}

	public static String createStringFromFile(final File file)
			throws IOException {
		InputStream in = new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];
		int len = b.length;
		int total = 0;
		while (total < len) {
			int result = in.read(b, total, len - total);
			if (result == -1) {
				break;
			}
			total += result;
		}
		in.close();
		return new String(b, StandardCharsets.UTF_8);
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

	public static void copyStringToFile(String content, File destFile)
			throws IOException {
		// ensure that parent dirs are created
		File parentDir = destFile.getParentFile();
		if (parentDir != null) {
			parentDir.mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(destFile);
		byte[] bytes = content.getBytes();
		fos.write(bytes, 0, bytes.length);
		fos.close();
	}

	public static void copyStreamToFile(InputStream inputStream, File destFile)
			throws IOException {
		// ensure that parent dirs are created
		File parentDir = destFile.getParentFile();
		if (parentDir != null) {
			parentDir.mkdirs();
		}
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

	public static String getContentFromUrl(URL url) {
		try {
			StringBuffer sbuf = new StringBuffer();
			InputStream is = url.openConnection().getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			String line = null;
			while ((line = reader.readLine()) != null) {
				sbuf.append(line);
			}
			reader.close();
			String content = sbuf.toString();
			return content;
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
	}

	/**
	 * Will return empty string in case of errors.
	 */
	public static String getContentFromHttpGetBasicAuth(String urlString,
			String username, String password) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			String userpass = username + ":" + password;
			String basicAuth = "Basic " + base64encode(userpass.getBytes());
			con.setRequestProperty("Authorization", basicAuth);

			// TODO jhi some content is missing at the beginning
			InputStream is = con.getInputStream();
			int responseCode = con.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				System.err.println("TestUtils: getContentFromHttpGet for '"
						+ urlString + "'failed with rc=" + responseCode);
				return "";
			}

			StringBuffer sbuf = new StringBuffer();

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			String line = null;
			while ((line = reader.readLine()) != null) {
				sbuf.append(line);
				sbuf.append('\n');
			}
			reader.close();
			String content = sbuf.toString();
			return content;
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
	}

	/**
	 * Base64 Encode an array of bytes
	 * 
	 * @see https://gist.github.com/EmilHernvall/953733
	 */
	private static String base64encode(byte[] data) {
		char[] tbl = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
				'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
				'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
				'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
				'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6',
				'7', '8', '9', '+', '/' };

		StringBuilder buffer = new StringBuilder();
		int pad = 0;
		for (int i = 0; i < data.length; i += 3) {

			int b = ((data[i] & 0xFF) << 16) & 0xFFFFFF;
			if (i + 1 < data.length) {
				b |= (data[i + 1] & 0xFF) << 8;
			} else {
				pad++;
			}
			if (i + 2 < data.length) {
				b |= (data[i + 2] & 0xFF);
			} else {
				pad++;
			}

			for (int j = 0; j < 4 - pad; j++) {
				int c = (b & 0xFC0000) >> 18;
				buffer.append(tbl[c]);
				b <<= 6;
			}
		}
		for (int j = 0; j < pad; j++) {
			buffer.append("=");
		}

		return buffer.toString();
	}
}
