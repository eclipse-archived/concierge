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

import java.util.HashMap;

public class Tuple<T1, T2> {

	private final T1 former;
	private final T2 latter;

	public Tuple(final T1 former, final T2 latter) {
		this.former = former;
		this.latter = latter;
	}

	public T1 getFormer() {
		return former;
	}

	public T2 getLatter() {
		return latter;
	}

	@Override
	public String toString() {
		return "<" + former + ", " + latter + ">";
	}

	public static class ParseResult extends
			Tuple<HashMap<String, String>, HashMap<String, Object>> {

		public ParseResult(final HashMap<String, String> directives,
				final HashMap<String, Object> attributes) {
			super(directives, attributes);
		}

		public HashMap<String, String> getDirectives() {
			return getFormer();
		}

		public HashMap<String, Object> getAttributes() {
			return getLatter();
		}
	}
}
