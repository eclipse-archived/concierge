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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.concierge.ConciergeCollections.ParseResult;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;

public final class Utils {

	private static String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Pattern LIST_TYPE_PATTERN = Pattern
			.compile("List\\s*<\\s*([^\\s]*)\\s*>");

	@SuppressWarnings("deprecation")
	private static final String SPECIFICATION_VERSION = Constants.PACKAGE_SPECIFICATION_VERSION;

	public static String[] splitString(final String values, final char delimiter) {
		return splitString(values, delimiter, Integer.MAX_VALUE);
	}

	static String[] splitString(final String values, final char delimiter,
			final int limit) {
		if (values == null || values.length() == 0) {
			return EMPTY_STRING_ARRAY;
		}

		final List<String> tokens = new ArrayList<String>(values.length() / 10);

		final char[] chars = values.toCharArray();

		final int len = chars.length;
		int openingQuote = -1;
		int pointer = 0;
		int curr = 0;
		int matches = 0;

		// skip trailing whitespaces
		while (Character.isWhitespace(chars[curr])) {
			curr++;
		}

		pointer = curr;

		do {
			if (chars[curr] == '\\') {
				curr += 2;
				continue;
			} else if (chars[curr] == '"') {
				if (openingQuote < 0) {
					openingQuote = curr;
				} else {
					openingQuote = -1;
				}

				curr++;
				continue;
			} else if (chars[curr] == delimiter && openingQuote < 0) {
				matches++;
				if (matches > limit) {
					break;
				}

				// scan back to skip whitepspaces
				int endPointer = curr - 1;
				while (endPointer > 0
						&& Character.isWhitespace(chars[endPointer])) {
					endPointer--;
				}

				// copy from pointer to current - 1
				final int count = endPointer - pointer + 1;
				if (count > 0) {
					tokens.add(new String(chars, pointer, count));
				}

				curr++;

				// scan forward to skip whitespaces
				while (curr < len && Character.isWhitespace(chars[curr])) {
					curr++;
				}

				pointer = curr;
				continue;
			}

			curr++;
		} while (curr < len);

		if (openingQuote > -1) {
			throw new IllegalArgumentException(
					"Unmatched quotation mark at position " + openingQuote);
		}

		// scan back to skip whitepspaces
		int endPointer = len - 1;
		while (endPointer > 0 && Character.isWhitespace(chars[endPointer])) {
			endPointer--;
		}

		final int count = endPointer - pointer + 1;
		if (count > 0) {
			tokens.add(new String(chars, pointer, count));
		}

		return tokens.toArray(new String[tokens.size()]);
	}

	public static ParseResult parseLiterals(final String[] literals,
			final int start) throws BundleException {
		final HashMap<String, String> directives = new HashMap<String, String>();
		final HashMap<String, Object> attributes = new HashMap<String, Object>();

		for (int i = start; i < literals.length; i++) {

			final String[] parts = splitString(literals[i], '=', 1);
			final String name = parts[0].trim();
			final int e = name.length() - 1;
			if (name.charAt(e) == ':') {
				// directive
				final String directive = name.substring(0, e).trim();

				if (directives.containsKey(directive)) {
					throw new BundleException("Duplicate directive '"
							+ directive + "'");
				}

				directives.put(directive, unQuote(parts[1].trim()));
			} else {
				// attribute

				if (attributes.containsKey(name)) {
					throw new BundleException("Duplicate attribute " + name);
				}

				final String[] nameParts = splitString(name, ':');
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
		return new ParseResult(directives, attributes);
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

			final String[] valueStrs = splitString(valueStr, ',');

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

	// TODO: fold into splitString?
	public static String unQuote(final String quoted) {
		final String quoted1 = quoted.trim();
		final int len = quoted1.length();
		final int start = quoted1.charAt(0) == '"' ? 1 : 0;
		final int end = quoted1.charAt(quoted1.length() - 1) == '"' ? len - 1
				: len;
		return start == 0 && end == len ? quoted : quoted1
				.substring(start, end);
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
				} else {
					attributes.remove(SPECIFICATION_VERSION);
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

}