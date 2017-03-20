/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/

package org.eclipse.concierge;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * The RFC1960 LDAP Filter implementation class.
 * 
 * @author Jan S. Rellermeyer
 */
public final class RFC1960Filter implements Filter {
	/**
	 * AND operator.
	 */
	private static final int AND_OPERATOR = 1;

	/**
	 * OR operator.
	 */
	private static final int OR_OPERATOR = 2;

	/**
	 * NOT operator.
	 */
	private static final int NOT_OPERATOR = 3;

	/**
	 * EQUALS (=) operator.
	 */
	private static final int EQUALS = 0;

	/**
	 * PRESENT (=*) operator.
	 */
	private static final int PRESENT = 1;

	/**
	 * APPROX (=~) operator.
	 */
	private static final int APPROX = 2;

	/**
	 * GREATER (>=) operator.
	 */
	private static final int GREATER = 3;

	/**
	 * LESS (<=) operator.
	 */
	private static final int LESS = 4;

	/**
	 * EQUALS with wildcard
	 */
	private static final int SUBSTRING = 5;

	/**
	 * the string presentations of the operators.
	 */
	protected static final String[] OP = { "=", "=*", "~=", ">=", "<=", "=" };

	/**
	 * the empty "null filter" is generated from null filter strings and matches
	 * everything.
	 */
	private static final Filter NULL_FILTER = new Filter() {
		public final boolean match(final ServiceReference<?> reference) {
			return true;
		}

		public final boolean match(final Dictionary<String, ?> dictionary) {
			return true;
		}

		public final boolean matchCase(final Dictionary<String, ?> dictionary) {
			return true;
		}

		public boolean matches(final Map<String, ?> map) {
			return true;
		}
	};

	// fields

	/**
	 * the operands.
	 */
	private final List<Filter> operands = new ArrayList<Filter>(1);

	/**
	 * the operator.
	 */
	private final int operator;

	/**
	 * create a new filter instance.
	 * 
	 * @param operator
	 *            the operator of the node
	 */
	private RFC1960Filter(final int operator) {
		this.operator = operator;
	}

	private static java.util.WeakHashMap<String, Filter> filterCache = new java.util.WeakHashMap<String, Filter>();
	
	/**
	 * get a filter instance from filter string.
	 * 
	 * @param filterString
	 *            the filter string.
	 * @return a filter instance.
	 * @throws InvalidSyntaxException
	 *             is the string is invalid.
	 */
	public static Filter fromString(final String str)
			throws InvalidSyntaxException {
		if (str == null) {
			return NULL_FILTER;
		}
		final Filter cached = filterCache.get(str);
		if (cached != null) {
			return cached;
		}
		
		final String filterString = str.trim();
		if (filterString.length() == 1) {
			throw new InvalidSyntaxException("Malformed filter", filterString);
		}

		final Stack<Filter> stack = new Stack<Filter>();

		try {
			final int len = filterString.length();

			int last = -1;
			int oper = 0;
			String id = null;
			int comparator = -1;

			final char[] chars = filterString.toCharArray();
			// stack.clear();
			final BitSet esc = new BitSet(chars.length);

			for (int i = 0; i < chars.length; i++) {

				switch (chars[i]) {
				case '\\':
					// escaped character
					if (i < chars.length) {
						if (chars[i + 1] == '(' || chars[i + 1] == ')'
								|| chars[i + 1] == '\\'
								|| chars[i + 1] == '*') {
							esc.set(i);
						}
					}

					i++;
					continue;
				case '(':
					// lookahead ...
					char nextChar = chars[i + 1];
					while (Character.isWhitespace(nextChar)) {
						i++;
						nextChar = chars[i + 1];
					}
					if (nextChar == ')') {
						throw new InvalidSyntaxException("Empty filter",
								filterString);
					}
					// lookahead
					int x = i;
					char nextnextChar = chars[x + 2];
					while (Character.isWhitespace(nextnextChar)) {
						x++;
						nextnextChar = chars[x + 2];
					}
					if (nextChar == '&' && nextnextChar == '(') {
						stack.push(new RFC1960Filter(AND_OPERATOR));
						continue;
					} else if (nextChar == '|' && nextnextChar == '(') {
						stack.push(new RFC1960Filter(OR_OPERATOR));
						continue;
					} else if (nextChar == '!' && nextnextChar == '(') {
						stack.push(new RFC1960Filter(NOT_OPERATOR));
						continue;
					} else {
						if (last == -1) {
							last = i;
						} else {
							throw new InvalidSyntaxException(
									"Surplus left paranthesis at: "
											+ filterString.substring(i),
									filterString);
						}
					}
					continue;
				case ')':
					if (last == -1) {
						final RFC1960Filter filter = (RFC1960Filter) stack
								.pop();
						if (stack.isEmpty()) {
							filterCache.put(str, filter);
							return filter;
						}
						final RFC1960Filter parent = (RFC1960Filter) stack
								.peek();
						if (parent.operator == NOT_OPERATOR
								&& !parent.operands.isEmpty()) {
							throw new InvalidSyntaxException(
									"Unexpected literal: "
											+ filterString.substring(i),
									filterString);
						}
						parent.operands.add(filter);
						if (i == len - 1) {
							throw new InvalidSyntaxException(
									"Missing right paranthesis at the end.",
									filterString);
						}
					} else {
						if (oper == 0) {
							throw new InvalidSyntaxException(
									"Missing operator.", filterString);
						}
						if (stack.isEmpty()) {
							if (i == len - 1) {

								// just a single simple filter
								// String value = filterString.substring(++oper,
								// len - 1);
								String value = value(filterString, ++oper,
										len - 1, esc);
								if (value.equals("*")
										&& comparator == SUBSTRING) {
									comparator = PRESENT;
									value = null;
								}
								final Filter filter = 
								 new RFC1960SimpleFilter(id, comparator,
										value);
								filterCache.put(str, filter);
								return filter;
							} else {
								throw new InvalidSyntaxException(
										"Unexpected literal: "
												+ filterString.substring(i),
										filterString);
							}
						}

						// get the parent from stack
						final RFC1960Filter parent = (RFC1960Filter) stack
								.peek();

						// String value = filterString.substring(++oper, i);
						String value = value(filterString, ++oper, i, esc);
						if (value.equals("*") && comparator == SUBSTRING) {
							comparator = PRESENT;
							value = null;
						}
						// link current element to parent
						parent.operands.add(
								new RFC1960SimpleFilter(id, comparator, value));

						oper = 0;
						last = -1;
						id = null;
						comparator = -1;
					}
					continue;
				case '~':
					if (oper == 0 && chars[i + 1] == '=') {

						id = filterString.substring(last + 1, i).trim();
						comparator = APPROX;
						oper = ++i;
						continue;
					} else {
						throw new InvalidSyntaxException(
								"Unexpected character " + chars[i + 1],
								filterString);
					}
				case '>':
					if (oper == 0 && chars[i + 1] == '=') {
						id = filterString.substring(last + 1, i).trim();
						comparator = GREATER;
						oper = ++i;
						continue;
					} else {
						throw new InvalidSyntaxException(
								"Unexpected character " + chars[i + 1],
								filterString);
					}
				case '<':
					if (oper == 0 && chars[i + 1] == '=') {
						id = filterString.substring(last + 1, i).trim();
						comparator = LESS;
						oper = ++i;
						continue;
					} else {
						throw new InvalidSyntaxException(
								"Unexpected character " + chars[i + 1],
								filterString);
					}
				case '=':
					if (oper == 0) {
						if (last + 1 == i) {
							throw new InvalidSyntaxException(
									"Missing identifier", filterString);
						}
						// could also be a "=*" present production.
						// if this is the case, it is fixed later, because
						// value=* and value=*key would require a lookahead of
						// at
						// least two. (the symbol "=*" alone is ambiguous).
						id = filterString.substring(last + 1, i).trim();
						comparator = EQUALS;
						oper = i;
					}
					continue;
				case '*':
					if (comparator == EQUALS) {
						comparator = SUBSTRING;
					}
					continue;
				}
			}

			final Filter filter = stack.pop();
			filterCache.put(str, filter);
			return filter;
		} catch (final EmptyStackException e) {
			throw new InvalidSyntaxException(
					"Filter expression not well-formed.", filterString);
		}
	}

	private static final String value(final String str, final int start,
			final int end, final BitSet esc) {
		if (esc.isEmpty()) {
			return str.substring(start, end);
		}
		final StringBuilder b = new StringBuilder();

		int i = start;
		int j;
		while ((j = esc.nextSetBit(i)) > -1) {
			b.append(str.substring(i, j));
			i = j + 1;
		}
		b.append(str.substring(i, end));

		return b.toString();
	}

	protected final static Map<String, ?> dict2map(
			final Dictionary<String, ?> dict) {
		if (dict == null) {
			return null;
		}

		if (dict instanceof Hashtable) {
			return (Hashtable<String, ?>) dict;
		}

		// legacy dictionary...
		// don't care about the performance...
		final HashMap<String, Object> map = new HashMap<String, Object>();
		for (final Enumeration<String> e = dict.keys(); e.hasMoreElements();) {
			final String key = e.nextElement();
			map.put(key, dict.get(key));
		}
		return map;
	}

	/**
	 * check if the filter matches a service reference.
	 * 
	 * @param reference
	 *            the service reference.
	 * @return true if the filter matches, false otherwise.
	 * @see org.osgi.framework.Filter#match(org.osgi.framework.ServiceReference)
	 * @category Filter
	 */
	public boolean match(final ServiceReference<?> reference) {
		try {
			return matches(((ServiceReferenceImpl<?>) reference).properties);
		} catch (final ClassCastException ce) {
			// so this was not instance of ServiceReferenceImpl. Someone
			// must have created an own implementation.
			final HashMap<String, Object> dict = new HashMap<String, Object>();
			final String[] keys = reference.getPropertyKeys();
			for (int i = 0; i < keys.length; i++) {
				dict.put(keys[i], reference.getProperty(keys[i]));
			}
			return matches(dict);
		}
	}

	/**
	 * check if the filter matches a dictionary of attributes.
	 * 
	 * @param values
	 *            the attributes.
	 * @return true, if the filter matches, false otherwise.
	 * @see org.osgi.framework.Filter#match(java.util.Dictionary)
	 * @category Filter
	 */
	public boolean match(final Dictionary<String, ?> values) {
		return matches(dict2map(values));
	}

	public boolean matches(final Map<String, ?> map) {
		if (operator == AND_OPERATOR) {
			final Filter[] operandArray = operands
					.toArray(new Filter[operands.size()]);
			for (int i = 0; i < operandArray.length; i++) {
				if (!operandArray[i].matches(map)) {
					return false;
				}
			}
			return true;
		} else if (operator == OR_OPERATOR) {
			final Filter[] operandArray = operands
					.toArray(new Filter[operands.size()]);
			for (int i = 0; i < operandArray.length; i++) {
				if (operandArray[i].matches(map)) {
					return true;
				}
			}
			return false;
		} else if (operator == NOT_OPERATOR) {
			return !operands.get(0).matches(map);
		}
		throw new IllegalStateException("PARSER ERROR");
	}

	/**
	 * check if the filter matches a dictionary of attributes. This method is
	 * case sensitive.
	 * 
	 * @param values
	 *            the attributes.
	 * @return true, if the filter matches, false otherwise.
	 * @see org.osgi.framework.Filter#matchCase(Dictionary)
	 * @category Filter
	 */
	public boolean matchCase(final Dictionary<String, ?> values) {
		if (operator == AND_OPERATOR) {
			final Filter[] operandArray = operands
					.toArray(new Filter[operands.size()]);
			for (int i = 0; i < operandArray.length; i++) {
				if (!operandArray[i].matchCase(values)) {
					return false;
				}
			}
			return true;
		} else if (operator == OR_OPERATOR) {
			final Filter[] operandArray = operands
					.toArray(new Filter[operands.size()]);
			for (int i = 0; i < operandArray.length; i++) {
				if (operandArray[i].matchCase(values)) {
					return true;
				}
			}
			return false;
		} else if (operator == NOT_OPERATOR) {
			return !operands.get(0).matchCase(values);
		}
		throw new IllegalStateException("PARSER ERROR");
	}

	/**
	 * get a string representation of the filter.
	 * 
	 * @return the string.
	 * @category Object
	 */
	public String toString() {
		if (operator == NOT_OPERATOR) {
			return "(!" + operands.get(0) + ")";
		}
		final StringBuffer buffer = new StringBuffer(
				operator == AND_OPERATOR ? "(&" : "(|");
		final Filter[] operandArray = operands
				.toArray(new Filter[operands.size()]);
		for (int i = 0; i < operandArray.length; i++) {
			buffer.append(operandArray[i]);
		}
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * check if the filter equals another object.
	 * 
	 * @param obj
	 *            the other object.
	 * @return true if the object is an instance of RFC1960Filter and the
	 *         filters are equal.
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @category Object
	 */
	public boolean equals(final Object obj) {
		if (obj instanceof RFC1960Filter) {
			final RFC1960Filter filter = (RFC1960Filter) obj;

			if (operands.size() != filter.operands.size()) {
				return false;
			}
			final Filter[] operandArray = operands
					.toArray(new Filter[operands.size()]);
			final Filter[] operandArray2 = filter.operands
					.toArray(new Filter[operands.size()]);
			for (int i = 0; i < operandArray.length; i++) {
				if (!operandArray[i].equals(operandArray2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * get the hash code.
	 * 
	 * @return the hash code.
	 * @category Object
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * check, if a value matches a wildcard expression.
	 * 
	 * @param c1
	 *            the value.
	 * @param p1
	 *            the value index.
	 * @param c2
	 *            the attribute.
	 * @param p2
	 *            the attribute index.
	 * @return integer with the same semantics as compareTo.
	 */
	static int stringCompare(final char[] c1, int p1, final char[] c2, int p2) {
		if (p1 == c1.length) {
			return 0;
		}

		final int l1 = c1.length;
		final int l2 = c2.length;

		while (p1 < l1 && p2 < l2) {
			boolean escaped = false;
			if (c1[p1] == c2[p2]) {
				p1++;
				p2++;
				continue;
			}
			if (c1[p1] == '\\') {
				p1++;
				escaped = true;
			}
			if (c1[p1] == '*' && !escaped) {
				p1++;
				do {
					if (stringCompare(c1, p1, c2, p2) == 0) {
						return 0;
					}
					p2++;
				} while (l2 - p2 > -1);
				return 1;
			} else {
				if (c1[p1] < c2[p2]) {
					return -1;
				} else if (c1[p1] > c2[p2]) {
					return 1;
				}
			}
		}
		if (p2 == l2 && c1[p1 - 1] == c2[p2 - 1]
				&& (p1 == l1 || p1 == l1 - 1 && c1[p1] == '*')) {
			return 0;
		}
		if (p1 > 0 && c1[p1 - 1] == '*' && p1 == l1 && p2 == l2) {
			return 0;
		}
		final int min = l1 < l2 ? l1 : l2;
		return l1 == min ? -1 : 1;
	}

	/**
	 * A simple filter. That is a filter of the form <tt>key operand value</tt>.
	 * A general filter consists of one or more simple filter literals connected
	 * by boolean operators.
	 * 
	 * @author Jan S. Rellermeyer
	 */
	final private static class RFC1960SimpleFilter implements Filter {

		/**
		 * the id.
		 */
		protected final String id;

		/**
		 * the comparator.
		 */
		protected final int comparator;

		/**
		 * the value.
		 */
		protected final String value;

		/**
		 * create a new filter.
		 * 
		 * @param id
		 *            the key
		 * @param comparator
		 *            the comparator
		 */
		protected RFC1960SimpleFilter(final String id, final int comparator,
				final String value) {
			this.id = id;
			this.comparator = comparator;
			this.value = value;
		}

		/**
		 * check, if the filter matches a service reference.
		 * 
		 * @param reference
		 *            the service reference.
		 * @return true, iff it matches.
		 * @see org.osgi.framework.Filter#match(org.osgi.framework.ServiceReference)
		 * @category Filter
		 */
		public boolean match(final ServiceReference<?> reference) {
			try {
				return matches(
						((ServiceReferenceImpl<?>) reference).properties);
			} catch (final ClassCastException e) {
				// so this was not instance of ServiceReferenceImpl. Someone
				// must have created an own implementation.
				final Map<String, Object> dict = new HashMap<String, Object>();
				final String[] keys = reference.getPropertyKeys();
				for (int i = 0; i < keys.length; i++) {
					dict.put(keys[i], reference.getProperty(keys[i]));
				}
				return matches(dict);
			}
		}

		/**
		 * check if the filter matches a dictionary of attributes.
		 * 
		 * @param map
		 *            the attributes.
		 * @return true if the filter matches, false otherwise.
		 * @see org.osgi.framework.Filter#match(java.util.Dictionary)
		 * @category Filter
		 */
		public boolean match(final Dictionary<String, ?> dict) {
			return matches(dict2map(dict), false);
		}

		/**
		 * check if the filter matches a dictionary of attributes. This method
		 * is case sensitive.
		 * 
		 * @param map
		 *            the attributes.
		 * @return true if the filter matches, false otherwise.
		 * @see org.osgi.framework.Filter#matchCase(Dictionary)
		 * @category Filter
		 */
		public boolean matchCase(final Dictionary<String, ?> dict) {
			return matches(dict2map(dict), true);
		}

		public boolean matches(final Map<String, ?> map) {
			return matches(map, false);
		}

		private boolean matches(final Map<String, ?> map,
				final boolean caseSensitive) {
			if (map == null) {
				return false;
			}
			Object temp = null;
			// just by chance, try if the case sensitive matching returns a
			// result.
			temp = map.get(id);

			if (temp == null) {
				if (caseSensitive) {
					return false;
				}

				// no ? Then try lower case.
				temp = map.get(id.toLowerCase());
			}

			if (temp == null) {
				// bad luck, try case insensitive matching of all keys
				for (final String key : map.keySet()) {
					if (key.equalsIgnoreCase(id)) {
						temp = map.get(key);
						break;
					}
				}
			}

			if (temp == null) {
				return false;
			}

			// are we just checking for presence ? Then we are done ...
			if (comparator == PRESENT) {
				return true;
			}

			final Object attr = temp;

			try {
				if (attr instanceof String) {
					return compareString(value, comparator, (String) attr);
				} else if (attr instanceof Number) {
					// all the numbers checkings run a lot faster when compared
					// in a primitive typed way
					return compareNumber(value.trim(), comparator,
							(Number) attr);
				} else if (attr instanceof String[]) {
					final String[] array = (String[]) attr;
					if (array.length == 0) {
						return false;
					}
					final String val = comparator == APPROX
							? stripWhitespaces(value) : value;
					for (int i = 0; i < array.length; i++) {
						if (compareString(val, comparator, array[i])) {
							return true;
						}
					}
					return false;
				} else if (attr instanceof Boolean) {
					return (comparator == EQUALS || comparator == APPROX)
							&& ((Boolean) attr)
									.equals(Boolean.valueOf(value.trim()));
				} else if (attr instanceof Character) {
					final String trimmed = value.trim();
					return trimmed.length() == 1
							? compareTyped(new Character(trimmed.charAt(0)),
									comparator, (Character) attr)
							: trimmed.length() == 0 && Character.isWhitespace(
									((Character) attr).charValue());
				} else if (attr instanceof Collection) {
					final Collection<?> col = (Collection<?>) attr;
					final Object[] obj = col.toArray();
					return compareArray(value, comparator, obj);
				} else if (attr instanceof Object[]) {
					return compareArray(value, comparator, (Object[]) attr);
				} else if (attr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(attr); i++) {
						final Object obj = Array.get(attr, i);
						if (obj instanceof Number
								&& compareNumber(value.trim(), comparator,
										(Number) obj)
								|| obj instanceof Character && compareTyped(
										new Character(value.trim().charAt(0)),
										comparator, (Character) obj)
								|| compareReflective(value, comparator, obj)) {
							return true;
						}
					}
					return false;
				} else {
					return compareReflective(value, comparator, attr);
				}
			} catch (final Throwable t) {
				return false;
			}
		}

		/**
		 * compare a string.
		 * 
		 * @param val
		 *            the filter value.
		 * @param comparator
		 *            the comparator.
		 * @param attr
		 *            the attribute.
		 * @return true, iff matches.
		 */
		private static boolean compareString(final String val,
				final int comparator, final String attr) {
			final String value = comparator == APPROX
					? stripWhitespaces(val).toLowerCase() : val;
			final String attribute = comparator == APPROX
					? stripWhitespaces(attr).toLowerCase() : attr;
			switch (comparator) {
			case APPROX:
			case EQUALS:
			case SUBSTRING:
				return RFC1960Filter.stringCompare(value.toCharArray(), 0,
						attribute.toCharArray(), 0) == 0;
			case GREATER:
				return RFC1960Filter.stringCompare(value.toCharArray(), 0,
						attribute.toCharArray(), 0) <= 0;
			case LESS:
				return RFC1960Filter.stringCompare(value.toCharArray(), 0,
						attribute.toCharArray(), 0) >= 0;
			default:
				throw new IllegalStateException("Found illegal comparator.");
			}
		}

		/**
		 * compare numbers.
		 * 
		 * @param value
		 *            the filter value.
		 * @param comparator
		 *            the comparator.
		 * @param attr
		 *            the number.
		 * @return true, iff matches.
		 */
		private static boolean compareNumber(final String value,
				final int comparator, final Number attr) {
			if (attr instanceof Integer) {
				final int intAttr = ((Integer) attr).intValue();
				final int intValue = Integer.parseInt(value);
				switch (comparator) {
				case GREATER:
					return intAttr >= intValue;
				case LESS:
					return intAttr <= intValue;
				default:
					return intAttr == intValue;
				}
			} else if (attr instanceof Long) {
				final long longAttr = ((Long) attr).longValue();
				final long longValue = Long.parseLong(value);
				switch (comparator) {
				case GREATER:
					return longAttr >= longValue;
				case LESS:
					return longAttr <= longValue;
				default:
					return longAttr == longValue;
				}
			} else if (attr instanceof Byte) {
				final byte byteAttr = ((Byte) attr).byteValue();
				// parse the byte manually since the default implementation does not cache
				int intVal = Integer.parseInt(value);
		        if (intVal < Byte.MIN_VALUE || intVal > Byte.MAX_VALUE)
		            throw new NumberFormatException(
		                "Value out of range. Value:\"" + value);
				switch (comparator) {
				case GREATER:
					return byteAttr >= (byte) intVal;
				case LESS:
					return byteAttr <= (byte) intVal;
				default:
					return byteAttr == (byte) intVal;
				}
			} else if (attr instanceof Short) {
				final short shortAttr = ((Short) attr).shortValue();
				// parse the short manually since the default implementation does not cache
				int intVal = Integer.parseInt(value);
		        if (intVal < Short.MIN_VALUE || intVal > Short.MAX_VALUE)
		            throw new NumberFormatException(
		                "Value out of range. Value:\"" + value);
				switch (comparator) {
				case GREATER:
					return shortAttr >= (short) intVal;
				case LESS:
					return shortAttr <= (short) intVal;
				default:
					return shortAttr == (short) intVal;
				}
			} else if (attr instanceof Double) {
				final double doubleAttr = ((Double) attr).doubleValue();
				final double doubleValue = Double.parseDouble(value);
				switch (comparator) {
				case GREATER:
					return doubleAttr >= doubleValue;
				case LESS:
					return doubleAttr <= doubleValue;
				default:
					return doubleAttr == doubleValue;
				}
			} else if (attr instanceof Float) {
				final float floatAttr = ((Float) attr).floatValue();
				final float floatValue = Float.parseFloat(value);
				switch (comparator) {
				case GREATER:
					return floatAttr >= floatValue;
				case LESS:
					return floatAttr <= floatValue;
				default:
					return floatAttr == floatValue;
				}
			}
			// all other are less frequent and are handled as
			// Comparables or objects.
			return compareReflective(value, comparator, attr);
		}

		/**
		 * compare in a typed way.
		 * 
		 * @param typedVal
		 *            the typed filter value.
		 * @param comparator
		 *            the comparator.
		 * @param attr
		 *            the attribute.
		 * @return true, iff matches.
		 */
		@SuppressWarnings("unchecked")
		private static boolean compareTyped(final Object typedVal,
				final int comparator,
				@SuppressWarnings("rawtypes") final Comparable attr) {
			switch (comparator) {
			case APPROX:
				if (typedVal instanceof Character) {
					return compareString(
							String.valueOf(((Character) typedVal).toString()),
							comparator, ((Character) attr).toString());
				}
			case EQUALS:
				return attr.compareTo(typedVal) == 0;
			case GREATER:
				return attr.compareTo(typedVal) >= 0;
			case LESS:
				return attr.compareTo(typedVal) <= 0;
			default:
				throw new IllegalStateException("Found illegal comparator.");
			}
		}

		/**
		 * compare arrays.
		 * 
		 * @param value
		 *            the filter value.
		 * @param comparator
		 *            the comparator.
		 * @param array
		 *            the array.
		 * @return true, iff matches.
		 */
		private static boolean compareArray(final String value,
				final int comparator, final Object[] array) {
			for (int i = 0; i < array.length; i++) {
				final Object obj = array[i];
				if (obj instanceof String) {
					if (compareString(value.trim(), comparator,
							((String) obj).trim())) {
						return true;
					}
				} else if (obj instanceof Number) {
					if (compareNumber(value.trim(), comparator, (Number) obj)) {
						return true;
					}
				} else {
					if (compareReflective(value, comparator, obj)) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * compare in a generic way by using reflection to create a
		 * corresponding object from the filter values string and compare this
		 * object with the attribute.
		 * 
		 * @param val
		 *            the filter value.
		 * @param comparator
		 *            the comparator.
		 * @param attr
		 *            the attribute.
		 * @return true, iff matches.
		 */
		private static boolean compareReflective(final String val,
				final int comparator, final Object attr) {
			if (comparator == SUBSTRING) {
				return false;
			}

			Object typedVal = null;

			// check if there is a valueOf...
			try {
				final Method m = attr.getClass().getDeclaredMethod("valueOf",
						String.class);
				if (Modifier.isStatic(m.getModifiers()) && attr.getClass()
						.isAssignableFrom(m.getReturnType())) {
					if (!((AccessibleObject) m).isAccessible()) {
						m.setAccessible(true);
					}
					typedVal = m.invoke(null, val);
				}
			} catch (final Exception e) {
				// ignore and move on
			}

			try {
				if (typedVal == null) {
					// check for constructor...
					final Constructor<?> constr = attr.getClass()
							.getConstructor(String.class);
					if (!((AccessibleObject) constr).isAccessible()) {
						constr.setAccessible(true);
					}
					typedVal = constr.newInstance(new Object[] { val });
				}

				return (attr instanceof Comparable) ? compareTyped(typedVal,
						comparator, (Comparable<?>) attr)
						: typedVal.equals(attr);
			} catch (final Exception didNotWork) {
				return false;
			}
		}

		/**
		 * strip whitespaces from a string.
		 * 
		 * @param s
		 *            the string.
		 * @return the stripped string.
		 */
		private static String stripWhitespaces(final String s) {
			return s.replaceAll(" ", "");
		}

		/**
		 * get a string representation of the SimpleFilter.
		 * 
		 * @return the string.
		 * @category Object
		 */
		public String toString() {
			return "(" + id + OP[comparator]
					+ (value == null ? "" : escape(value)) + ")";
		}

		private final static String escape(final String s) {
			final StringBuilder b = new StringBuilder();
			final char[] chars = s.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				switch (chars[i]) {
				case '(':
				case ')':
				case '\\':
					b.append('\\');
				default:
					b.append(chars[i]);
				}
			}
			return b.toString();
		}

		/**
		 * check, if the instance matches another object.
		 * 
		 * @param obj
		 *            the other object.
		 * @return true, iff the other object is an instance of
		 *         RFC1960SimpleFilter and the filter expressions are equal.
		 * @category Object
		 */
		public boolean equals(final Object obj) {
			if (obj instanceof RFC1960SimpleFilter) {
				final RFC1960SimpleFilter filter = (RFC1960SimpleFilter) obj;
				return comparator == filter.comparator && id.equals(filter.id)
						&& value.equals(filter.value);
			}
			return false;
		}

		/**
		 * get the hash code.
		 * 
		 * @return the hash code.
		 * @category Object
		 */
		public int hashCode() {
			return toString().hashCode();
		}
	}

	private static short INSUFFICIENT = 0;
	private static short NECESSARY = 1;
	private static short REQUIRED = 3;

	static List<Capability> filterWithIndex(final Requirement requirement,
			final String filterStr,
			final Concierge.CapabilityRegistry capabilityIndex)
					throws InvalidSyntaxException {
		final Set<String> values = new HashSet<String>();

		final String namespace = requirement.getNamespace();

		final Filter filter = fromString(filterStr);

		final int prefilterResult = prefilter(namespace, filter,
				capabilityIndex, INSUFFICIENT, false, values);

		final List<Capability> candidates;

		if (prefilterResult == REQUIRED) {
			if (values.size() != 1) {
				return Collections.emptyList();
			}
			return capabilityIndex.getByValue(namespace,
					values.iterator().next());
		} else if (prefilterResult == NECESSARY) {
			// FIXME: check
			if (values.size() != 1) {
				candidates = capabilityIndex.getAll(namespace);
			} else {
				candidates = capabilityIndex.getByKey(namespace,
						values.iterator().next());
			}
		} else {
			candidates = capabilityIndex.getAll(namespace);
		}

		if (candidates == null) {
			return Collections.emptyList();
		}

		final ArrayList<Capability> matches = new ArrayList<Capability>();

		for (final Capability cap : candidates) {
			if (filter.matches(cap.getAttributes()) && Concierge
					.matches0(namespace, requirement, cap, filterStr)) {
				matches.add(cap);
			}
		}

		return matches;
	}

	private static int prefilter(final String namespace, final Filter filter,
			final Concierge.CapabilityRegistry capabilities, final int state,
			final boolean inNegation, final Set<String> values) {
		int newState = state;

		if (filter instanceof RFC1960Filter) {
			final RFC1960Filter f = (RFC1960Filter) filter;
			final int operator = f.operator;
			if (f.operands.size() == 1) {
				return prefilter(namespace, f.operands.get(0), capabilities,
						state,
						operator == NOT_OPERATOR ? !inNegation : inNegation,
						values);
			}

			for (final Filter next : f.operands) {
				final int childState = prefilter(namespace, next, capabilities,
						state, inNegation, values);
				if (f.operator == AND_OPERATOR) {
					newState |= childState;
				} else { // if (f.operator == OR_OPERATOR)
					newState &= childState;
				}
				if (newState == INSUFFICIENT) {
					// early termination
					return INSUFFICIENT;
				}
				// as soon as we have more than one clause, REQUIRED degrades to
				// NECESSARY
				newState ^= 2;
			}
			return newState == 3 ? NECESSARY : newState;
		} else if (filter instanceof RFC1960SimpleFilter) {
			final RFC1960SimpleFilter f = (RFC1960SimpleFilter) filter;
			if (namespace.equals(f.id)) {
				if (f.comparator == EQUALS) {
					values.add(f.value);
					return REQUIRED;
				} else if (f.comparator == PRESENT && inNegation) {
					return INSUFFICIENT;
				} else {
					return NECESSARY;
				}
			} else {
				return INSUFFICIENT;
			}
		} else {
			return INSUFFICIENT;
		}
	}

}