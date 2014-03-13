/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.resource.Capability;

public final class Utils {

	public static final String SPLIT_AT_COMMA = ",\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
	static final String SPLIT_AT_COMMA_PLUS = "(?<!\\\\),(?=(([^\"\\\\]|\\\\.)*\"([^\"\\\\]|\\\\.)*\")*([^\"\\\\]|\\\\.)*$)";
	public static final String SPLIT_AT_SEMICOLON = ";\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
	static final String SPLIT_AT_SEMICOLON_PLUS = "(?<!\\\\);(?=(([^\"\\\\]|\\\\.)*\"([^\"\\\\]|\\\\.)*\")*([^\"\\\\]|\\\\.)*$)";
	static final String SPLIT_AT_COLON = ":\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	private static final String SPLIT_AT_EQUALS = "=";
	private static final Pattern LIST_TYPE_PATTERN = Pattern
			.compile("List\\s*<\\s*([^\\s]*)\\s*>");

	@SuppressWarnings("deprecation")
	private static final String SPECIFICATION_VERSION = Constants.PACKAGE_SPECIFICATION_VERSION;

	public static final Comparator<? super Capability> EXPORT_ORDER = new Comparator<Capability>() {

		// reverts the order so that we can
		// retrieve the 0st item to get the best
		// match
		public int compare(final Capability c1, final Capability c2) {
			if (!(c1 instanceof BundleCapability && c2 instanceof BundleCapability)) {
				return 0;
			}

			final BundleCapability cap1 = (BundleCapability) c1;
			final BundleCapability cap2 = (BundleCapability) c2;

			final int cap1Resolved = cap1.getResource().getWiring() == null ? 0
					: 1;
			final int cap2Resolved = cap2.getResource().getWiring() == null ? 0
					: 1;
			int score = cap1Resolved - cap2Resolved;
			if (score != 0) {
				return score;
			}

			Version cap1Version = (Version) cap1.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			Version cap2Version = (Version) cap2.getAttributes().get(
					PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);

			if (cap1Version == null) {
				cap1Version = Version.emptyVersion;
			}
			if (cap2Version == null) {
				cap2Version = Version.emptyVersion;
			}

			score = cap2Version.compareTo(cap1Version);

			if (score != 0) {
				return score;
			}

			final long cap1BundleId = cap1.getRevision().getBundle()
					.getBundleId();
			final long cap2BundleId = cap2.getRevision().getBundle()
					.getBundleId();

			return (int) (cap1BundleId - cap2BundleId);
		}

	};

	public static Tuple<HashMap<String, String>, HashMap<String, Object>> parseLiterals(
			final String[] literals, final int start) throws BundleException {

		final HashMap<String, String> directives = new HashMap<String, String>();
		final HashMap<String, Object> attributes = new HashMap<String, Object>();

		for (int i = start; i < literals.length; i++) {

			final String[] parts = literals[i].split(SPLIT_AT_EQUALS, 2);
			final String name = parts[0].trim();
			final int e = name.length() - 1;
			if (name.charAt(e) == ':') {
				// directive
				final String directive = name.substring(0, e).trim();

				if (directives.containsKey(directive)) {
					throw new BundleException("Duplicate directive "
							+ directive);
				}

				directives.put(directive, unQuote(parts[1].trim()));
			} else {
				// attribute

				if (attributes.containsKey(name)) {
					throw new BundleException("Duplicate attribute " + name);
				}

				final String[] nameParts = splitString(name, ":");
				if (nameParts.length > 1) {
					if (nameParts.length != 2) {
						throw new BundleException("Illegal attribute name "
								+ name);
					}

					attributes
							.put(nameParts[0],
									createValue(nameParts[1].trim(),
											unQuote(parts[1])));
				} else {
					if (Constants.VERSION_ATTRIBUTE.equals(name)
							&& parts[1].indexOf(',') == -1) {
						attributes.put(name,
								new Version(unQuote(parts[1].trim())));
					} else {
						attributes.put(name, unQuote(parts[1].trim()));
					}
				}
			}
		}
		return new Tuple<HashMap<String, String>, HashMap<String, Object>>(
				directives, attributes);
	}

	private static final short STRING_TYPE = 0;
	private static final short VERSION_TYPE = 1;
	private static final short LONG_TYPE = 2;
	private static final short DOUBLE_TYPE = 3;

	private static Object createValue(final String type, final String valueStr)
			throws BundleException {
		final Matcher matcher = LIST_TYPE_PATTERN.matcher(type);
		if (matcher.matches() || "List".equals(type)) {
			final short elementType = matcher.matches() ? getType(matcher
					.group(1)) : STRING_TYPE;
			final List<Object> list = new ArrayList<Object>();
			final String[] valueStrs = valueStr.split(SPLIT_AT_COMMA_PLUS);

			for (int i = 0; i < valueStrs.length; i++) {
				list.add(createValue0(elementType, valueStrs[i]));
			}

			return list;
		} else {
			return createValue0(getType(type), valueStr);
		}
	}

	private static short getType(final String type) {
		if ("String".equals(type)) {
			return STRING_TYPE;
		}

		if ("Version".equals(type)) {
			return VERSION_TYPE;
		}

		if ("Long".equals(type)) {
			return LONG_TYPE;
		}

		if ("Double".equals(type)) {
			return DOUBLE_TYPE;
		}

		return -1;
	}

	private static Object createValue0(final short type, final String valueStr) {
		switch (type) {
		case STRING_TYPE:
			return valueStr;
		case VERSION_TYPE:
			return new Version(valueStr.trim());
		case LONG_TYPE:
			return new Long(valueStr.trim());
		case DOUBLE_TYPE:
			return new Double(valueStr.trim());
		}
		throw new IllegalStateException("invalid type " + type);
	}

	/**
	 * store a file on the storage.
	 * 
	 * @param file
	 *            the file.
	 * @param input
	 *            the input stream.
	 */
	static void storeFile(final File file, final InputStream input) {
		try {
			file.getParentFile().mkdirs();
			final FileOutputStream fos = new FileOutputStream(file);

			final byte[] buffer = new byte[Concierge.CLASSLOADER_BUFFER_SIZE];
			int read;
			while ((read = input.read(buffer, 0,
					Concierge.CLASSLOADER_BUFFER_SIZE)) > -1) {
				fos.write(buffer, 0, read);
			}
			input.close();
			fos.close();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static String unQuote(final String quoted) {
		final String quoted1 = quoted.trim();
		final int len = quoted1.length();
		final int start = quoted1.charAt(0) == '"' ? 1 : 0;
		final int end = quoted1.charAt(quoted1.length() - 1) == '"' ? len - 1
				: len;
		return start == 0 && end == len ? quoted : quoted1
				.substring(start, end);
	}

	static String[] splitString(final String values, final String delimiter)
			throws IllegalArgumentException {
		if (values == null) {
			return new String[0];
		}

		final List<String> tokens = new ArrayList<String>(values.length() / 10);
		int pointer = 0;
		int quotePointer = 0;
		int tokenStart = 0;
		int nextDelimiter;
		while ((nextDelimiter = values.indexOf(delimiter, pointer)) > -1) {
			final int openingQuote = values.indexOf("\"", quotePointer);
			int closingQuote = values.indexOf("\"", openingQuote + 1);
			if (openingQuote > closingQuote) {
				throw new IllegalArgumentException(
						"Missing closing quotation mark.");
			}
			if (openingQuote > -1 && openingQuote < nextDelimiter
					&& closingQuote < nextDelimiter) {
				quotePointer = ++closingQuote;
				continue;
			}
			if (openingQuote < nextDelimiter && nextDelimiter < closingQuote) {
				pointer = ++closingQuote;
				continue;
			}
			// TODO: for performance, fold the trim into the splitting
			tokens.add(values.substring(tokenStart, nextDelimiter).trim());
			pointer = ++nextDelimiter;
			quotePointer = pointer;
			tokenStart = pointer;
		}
		tokens.add(values.substring(tokenStart).trim());
		return tokens.toArray(new String[tokens.size()]);
	}

	/**
	 * check, if the version is in the range of the version range specified by
	 * str
	 * 
	 * @param version
	 *            the Version to compare against the range
	 * @param str
	 *            String, that describes the version range
	 * @return true, if version in range
	 */
	static boolean isVersionInRange(final Version version, String str) {
		// parse range
		if (str == null || str.length() < 1) {
			return version.compareTo(Version.emptyVersion) > -1;
		}

		// remove "
		if (str.startsWith("\"")) {
			str = str.substring(1, str.length());
		}
		if (str.endsWith("\"")) {
			str = str.substring(0, str.length() - 1);
		}

		final String[] bounds = splitString(str, ",");
		if (bounds.length <= 1) {
			// range is lower bound to infinity
			final Version v2 = new Version(str);
			if (version.compareTo(v2) < 0) {
				return false;
			}
		} else {
			// range has lower and upper bound
			final Version lower = new Version(bounds[0].substring(1).trim());
			final Version upper = new Version(bounds[1].substring(0,
					bounds[1].length() - 1).trim());
			// check lower bound
			if (bounds[0].startsWith("[")) {
				if (version.compareTo(lower) < 0) {
					return false;
				}
			} else {
				// assume "("
				if (version.compareTo(lower) <= 0) {
					return false;
				}
			}
			// check upper bound
			if (bounds[1].endsWith("]")) {
				if (version.compareTo(upper) > 0) {
					return false;
				}
			} else {
				// assume ")"
				if (version.compareTo(upper) >= 0) {
					return false;
				}
			}

		}
		return true;
	}

	public static class MultiMap<K, V> implements Map<K, List<V>> {

		private final HashMap<K, List<V>> map;

		private final LinkedHashSet<V> allValues = new LinkedHashSet<V>();

		private final Comparator<V> comp;

		public MultiMap() {
			this.map = new HashMap<K, List<V>>();
			this.comp = null;
		}

		public MultiMap(final int initialSize) {
			this.map = new HashMap<K, List<V>>(initialSize);
			this.comp = null;
		}

		public MultiMap(final MultiMap<K, ? extends V> existing) {
			this();
			insertMap(existing);
		}

		public MultiMap(final Comparator<V> comp) {
			map = new HashMap<K, List<V>>();
			this.comp = comp;
		}

		public void insert(final K key, final V value) {
			List<V> list = map.get(key);
			if (list == null) {
				list = new ArrayList<V>();
				map.put(key, list);
			}
			list.add(value);
			if (comp != null) {
				Collections.sort(list, comp);
			}
			allValues.add(value);
		}

		public void insertEmpty(final K key) {
			List<V> list = map.get(key);
			if (list == null) {
				list = new ArrayList<V>();
				map.put(key, list);
			}
		}

		public void insertUnique(final K key, final V value) {
			List<V> list = map.get(key);
			if (list == null) {
				list = new ArrayList<V>();
				map.put(key, list);
			}
			if (!list.contains(value)) {
				list.add(value);
				if (comp != null) {
					Collections.sort(list, comp);
				}
				allValues.add(value);
			}
		}

		public void insertAll(final K key, final Collection<? extends V> values) {
			List<V> list = map.get(key);
			if (list == null) {
				list = new ArrayList<V>();
				map.put(key, list);
			}
			list.addAll(values);
			if (comp != null) {
				Collections.sort(list, comp);
			}
			allValues.addAll(values);
		}

		public void insertMap(final MultiMap<K, ? extends V> existing) {
			for (final K key : existing.keySet()) {
				final List<? extends V> vals = existing.get(key);
				insertAll(key, vals);
			}
		}

		public List<V> get(final Object key) {
			return map.get(key);
		}

		public int indexOf(final K key, final V value) {
			final List<V> list = get(key);
			return list == null ? -1 : list.indexOf(value);
		}

		public boolean remove(final K key, final V value) {
			final List<V> list = get(key);
			if (list != null) {
				final boolean result = list.remove(value);
				if (result) {
					redoAllValues();
				}
				return result;
			}
			return false;
		}

		public List<V> remove(final Object key) {
			final List<V> values = map.remove(key);
			if (values == null) {
				return null;
			}

			redoAllValues();
			return values;
		}

		public List<V> lookup(final K key) {
			final List<V> result = get(key);
			return result == null ? Collections.<V> emptyList() : result;
		}

		protected void redoAllValues() {
			allValues.clear();
			for (final List<V> valueList : values()) {
				allValues.addAll(valueList);
			}
		}

		public List<V> getAllValues() {
			return new ArrayList<V>(allValues);
		}

		public void removeAll(final K[] keys, final V value) {
			for (int i = 0; i < keys.length; i++) {
				final List<V> list = get(keys[i]);
				if (list != null) {
					list.remove(value);
				}
			}

			redoAllValues();
		}

		public Set<K> keySet() {
			return new KeySet();
		}

		public String toString() {
			return "MultiMap " + map.toString();
		}

		private final class KeySet extends AbstractSet<K> {

			private final Set<K> keySet;

			protected KeySet() {
				keySet = map.keySet();
			}

			public Iterator<K> iterator() {
				final Iterator<K> inner = keySet.iterator();
				return new Iterator<K>() {

					private K element;

					public boolean hasNext() {
						return inner.hasNext();
					}

					public K next() {
						element = inner.next();
						return element;
					}

					public void remove() {
						MultiMap.this.remove(element);
					}

				};
			}

			public int size() {
				return map.size();
			}

			public boolean contains(final Object key) {
				return containsKey(key);
			}

			public boolean remove(final Object key) {
				final boolean result = MultiMap.this.remove(key) != null;

				if (result) {
					redoAllValues();
				}

				return result;
			}

			public void clear() {
				MultiMap.this.clear();
				allValues.clear();
			}
		}

		public int size() {
			return map.size();
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public boolean containsKey(final Object key) {
			return map.containsKey(key);
		}

		public boolean containsValue(final Object value) {
			return allValues.contains(value);
		}

		public List<V> put(final K key, final List<V> value) {
			throw new UnsupportedOperationException("put");
		}

		public void putAll(final Map<? extends K, ? extends List<V>> m) {
			throw new UnsupportedOperationException("putAll");
		}

		public void clear() {
			map.clear();
			allValues.clear();
		}

		public Collection<List<V>> values() {
			return map.values();
		}

		public Set<java.util.Map.Entry<K, List<V>>> entrySet() {
			return map.entrySet();
		}

	}

	/**
	 * get a file from a class name.
	 * 
	 * @param fqc
	 *            the fully qualified class name.
	 * @return the file name.
	 */
	static String classToFile(final String fqc) {
		return fqc.replace('.', '/') + ".class";
	}

	public static String createFilter(final String namespace, final String req,
			final Map<String, Object> attributes) throws BundleException {
		final Object version = attributes.get(Constants.VERSION_ATTRIBUTE);

		if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
			if (version != null
					&& attributes.containsKey(SPECIFICATION_VERSION)) {
				if (!new Version(Utils.unQuote((String) attributes
						.get(SPECIFICATION_VERSION))).equals(version)) {
					throw new BundleException(
							"both version and specification-version are given for the import "
									+ req);
				}
			}
		}

		final StringBuffer buffer = new StringBuffer();
		buffer.append('(');
		buffer.append(namespace);
		buffer.append('=');
		buffer.append(req);
		buffer.append(')');

		if (attributes.size() == 0) {
			return buffer.toString();
		}

		buffer.insert(0, "(&");

		for (final Map.Entry<String, Object> attribute : attributes.entrySet()) {
			final String key = attribute.getKey();
			final Object value = attribute.getValue();

			if (Constants.VERSION_ATTRIBUTE.equals(key)
					|| Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key)) {
				if (value instanceof String) {
					final VersionRange range = new VersionRange(
							Utils.unQuote((String) value));

					if (range.getRight() == null) {
						buffer.append('(');
						buffer.append(key);
						buffer.append(">=");
						buffer.append(range.getLeft());
						buffer.append(')');
					} else {
						boolean open = range.getLeftType() == VersionRange.LEFT_OPEN;
						buffer.append(open ? "(!(" : "(");
						buffer.append(key);
						buffer.append(open ? "<=" : ">=");
						buffer.append(range.getLeft());
						buffer.append(open ? "))" : ")");

						open = range.getRightType() == VersionRange.RIGHT_OPEN;
						buffer.append(open ? "(!(" : "(");
						buffer.append(key);
						buffer.append(open ? ">=" : "<=");
						buffer.append(range.getRight());
						buffer.append(open ? "))" : ")");
					}
				} else {
					buffer.append('(');
					buffer.append(key);
					buffer.append(">=");
					buffer.append(value);
					buffer.append(')');
				}
				continue;
			}

			buffer.append("(");
			buffer.append(key);
			buffer.append("=");
			buffer.append(value);
			buffer.append(")");
		}
		buffer.append(")");

		return buffer.toString();
	}

	static class RemoveOnlyMap<K, V> extends HashMap<K, V> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3743325895136799794L;

		private boolean sealed;

		@Override
		public V put(final K key, final V value) {
			if (sealed) {
				throw new UnsupportedOperationException("put");
			}
			return super.put(key, value);
		}

		public void putAll(final Map<? extends K, ? extends V> m) {
			throw new UnsupportedOperationException("putAll");
		}

		void seal() {
			sealed = true;
		}

	}

	// FIXME: separate concerns... (delta tracking and remove only)
	static class RemoveOnlyList<E> extends ArrayList<E> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2126964539821583131L;

		private final ArrayList<E> removed = new ArrayList<E>();

		public RemoveOnlyList(final Collection<? extends E> result) {
			super(result);
		}

		public boolean add(final Object o) {
			throw new UnsupportedOperationException("add");
		}

		public boolean addAll(final Collection<? extends E> c) {
			throw new UnsupportedOperationException("addAll");
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(final Object o) {
			final boolean modified = super.remove(o);

			if (modified) {
				removed.add((E) o);
			}

			return modified;
		}

		@Override
		public boolean removeAll(final Collection<?> c) {
			boolean modified = false;
			for (final Object o : c) {
				modified |= remove(o);
			}
			return modified;
		}

		@Override
		public boolean retainAll(final Collection<?> c) {
			boolean modified = false;
			for (final E e : this) {
				if (!c.contains(e)) {
					remove(e);
					modified = true;
				}
			}

			return modified;
		}

		public List<E> getRemoved() {
			return removed;
		}

	}

	// FIXME: remove!

	public static void main(final String... args) {

		final String[] res = "foo,\"bar\",\"foo,bar\",test\\\",test"
				.split(SPLIT_AT_COMMA);

		// final String[] res =
		// splitString("foo,\"bar\",\"foo,bar\",test\\\",test", ",");

		System.out.println(Arrays.toString(res));

		for (final String s : res) {
			System.out.println("'" + s + "'");
		}

	}

}
