package org.eclipse.concierge;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConciergeCollections {

	public static class MultiMap<K, V> implements Map<K, List<V>> {

		protected final HashMap<K, List<V>> map;

		protected final LinkedHashSet<V> allValues = new LinkedHashSet<V>();

		private final Comparator<V> comp;

		boolean dirty = false;

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

		HashMap<K, List<V>> getFlatMap() {
			return map;
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
			if (!dirty) {
				allValues.add(value);
			}
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
				if (!dirty) {
					allValues.add(value);
				}
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
			if (!dirty) {
				allValues.addAll(values);
			}
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

		public boolean remove(final Object key, final Object value) {
			final List<V> list = get(key);
			if (list != null) {
				final boolean result = list.remove(value);
				if (result) {
					dirty = true;
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

			dirty = true;
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
			dirty = false;
		}

		public List<V> getAllValues() {
			if (dirty) {
				redoAllValues();
			}
			return new ArrayList<V>(allValues);
		}

		public void removeAll(final K[] keys, final V value) {
			for (int i = 0; i < keys.length; i++) {
				final List<V> list = get(keys[i]);
				if (list != null) {
					list.remove(value);
				}
			}

			dirty = true;
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
					dirty = true;
				}

				return result;
			}

			public void clear() {
				MultiMap.this.clear();
				allValues.clear();
				dirty = false;
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
			if (dirty) {
				redoAllValues();
			}
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
			dirty = false;
		}

		public Collection<List<V>> values() {
			return map.values();
		}

		public Set<java.util.Map.Entry<K, List<V>>> entrySet() {
			return map.entrySet();
		}

	}

	public static class Tuple<T1, T2> {

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

	static class RemoveOnlyList<E> extends ArrayList<E> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2126964539821583131L;

		public RemoveOnlyList(final Collection<? extends E> result) {
			super(result);
		}

		public boolean add(final Object o) {
			throw new UnsupportedOperationException("add");
		}

		public boolean addAll(final Collection<? extends E> c) {
			throw new UnsupportedOperationException("addAll");
		}

	}

	static class DeltaTrackingRemoveOnlyList<E> extends RemoveOnlyList<E> {

		private final ArrayList<E> removed = new ArrayList<E>();

		/**
		 * 
		 */
		private static final long serialVersionUID = 2467542232248099702L;

		public DeltaTrackingRemoveOnlyList(final Collection<E> result) {
			super(result);
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

}
