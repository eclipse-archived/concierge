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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Jan S. Rellermeyer
 */
final class ServiceReferenceImpl<S> implements ServiceReference<S> {
	/**
	 * the framework
	 */
	private final Concierge framework;

	/**
	 * the bundle object.
	 */
	Bundle bundle;

	/**
	 * the service object.
	 */
	private S service;

	/**
	 * the service properties.
	 */
	final Hashtable<String, Object> properties;

	/**
	 * the bundles that are using the service.
	 */
	final Map<Bundle, Integer> useCounters = new HashMap<Bundle, Integer>(0);

	/**
	 * cached service objects if the registered service is a service factory.
	 */
	private HashMap<Bundle, S> cachedServices = null;

	/**
	 * the registration.
	 */
	ServiceRegistration<S> registration;

	/**
	 * the next service id.
	 */
	private static long nextServiceID = 0;

	private final boolean isServiceFactory;

	/**
	 * these service properties must not be overwritten by property updates.
	 */
	private final static HashSet<String> forbidden;
	static {
		forbidden = new HashSet<String>(2);
		forbidden.add(Constants.SERVICE_ID.toLowerCase());
		forbidden.add(Constants.OBJECTCLASS.toLowerCase());
	}

	/**
	 * create a new service reference implementation instance.
	 * 
	 * @param bundle
	 *            the bundle.
	 * @param service
	 *            the service object.
	 * @param props
	 *            the service properties.
	 * @param clazzes
	 *            the interface classes that the service is registered under.
	 * @throws ClassNotFoundException
	 */
	ServiceReferenceImpl(final Concierge framework, final Bundle bundle,
			final S service, final Dictionary<String, ?> props,
			final String[] clazzes) {
		if (service instanceof ServiceFactory) {
			isServiceFactory = true;
		} else {
			isServiceFactory = false;
			checkService(service, clazzes);
		}

		this.framework = framework;
		this.bundle = bundle;
		this.service = service;
		this.properties = new Hashtable<String, Object>(props == null ? 2
				: props.size() + 2);
		if (props != null) {
			for (final Enumeration<String> keys = props.keys(); keys
					.hasMoreElements();) {
				final String key = keys.nextElement();
				properties.put(key, props.get(key));
			}
		}
		properties.put(Constants.OBJECTCLASS, clazzes);
		properties.put(Constants.SERVICE_ID, new Long(++nextServiceID));
		final Integer ranking = props == null ? null : (Integer) props
				.get(Constants.SERVICE_RANKING);
		properties.put(Constants.SERVICE_RANKING,
				ranking == null ? new Integer(0) : ranking);
		this.registration = new ServiceRegistrationImpl();
	}

	private void checkService(final Object service, final String[] clazzes)
			throws IllegalArgumentException {
		if (service == null) {
			throw new IllegalArgumentException("ServiceFactory produced /null/");
		}
		for (int i = 0; i < clazzes.length; i++) {
			try {
				final Class<?> current = Class.forName(clazzes[i], false,
						service.getClass().getClassLoader());
				if (!current.isInstance(service)) {
					throw new IllegalArgumentException("Service "
							+ service.getClass().getName()
							+ " does not implement the interface " + clazzes[i]);
				}
			} catch (final ClassNotFoundException e) {
				throw new IllegalArgumentException("Interface " + clazzes[i]
						+ " implemented by service "
						+ service.getClass().getName() + " cannot be located: "
						+ e.getMessage());
			}
		}
	}

	void invalidate() {
		service = null;
		useCounters.clear();
		bundle = null;
		registration = null;
		if (cachedServices != null) {
			cachedServices = null;
		}
		// this causes Knopflerfish's test case 55 to fail
		// final String[] keys = getPropertyKeys();
		// for (int i=0; i<keys.length; i++) {
		// properties.remove(keys[i]);
		// }
	}

	/**
	 * get the bundle that has registered the service.
	 * 
	 * @return the bundle object.
	 * @see org.osgi.framework.ServiceReference#getBundle()
	 * @category ServiceReference
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * get a property.
	 * 
	 * @param key
	 *            the key.
	 * @return the value or null, if the entry does not exist.
	 * @see org.osgi.framework.ServiceReference#getProperty(java.lang.String)
	 * @category ServiceReference
	 */
	public Object getProperty(final String key) {
		// first, try the original case
		Object result = properties.get(key);
		if (result != null) {
			return result;
		}

		// then, try the lower case variant
		result = properties.get(key.toLowerCase());
		if (result != null) {
			return result;
		}

		// bad luck, try case insensitive matching of the keys
		for (final Enumeration<String> keys = properties.keys(); keys
				.hasMoreElements();) {
			final String k = keys.nextElement();
			if (k.equalsIgnoreCase(key)) {
				result = properties.get(k);
				break;
			}
		}
		return result;
	}

	/**
	 * get all property keys.
	 * 
	 * @return the array of all keys.
	 * @see org.osgi.framework.ServiceReference#getPropertyKeys()
	 * @category ServiceReference
	 */
	public String[] getPropertyKeys() {
		return properties.keySet().toArray(new String[properties.size()]);
	}

	/**
	 * get the using bundles.
	 * 
	 * @return the array of all bundles.
	 * @see org.osgi.framework.ServiceReference#getUsingBundles()
	 * @category ServiceReference
	 */
	public Bundle[] getUsingBundles() {
		synchronized (useCounters) {
			if (useCounters.isEmpty()) {
				return null;
			}
			return useCounters.keySet().toArray(new Bundle[useCounters.size()]);
		}
	}

	// FIXME: concurrency???
	private boolean marker = false;

	/**
	 * get the service object. If the service is a service factory, a cached
	 * value might be returned.
	 * 
	 * @param theBundle
	 *            the requesting bundle.
	 * @return the service object.
	 */
	S getService(final Bundle theBundle) {
		if (service == null || marker) {
			return null;
		}

		synchronized (useCounters) {
			if (isServiceFactory) {
				if (cachedServices == null) {
					cachedServices = new HashMap<Bundle, S>(1);
				}
				final S cachedService = cachedServices.get(theBundle);
				if (cachedService != null) {
					incrementCounter(theBundle);
					return cachedService;
				}
				@SuppressWarnings("unchecked")
				final ServiceFactory<S> factory = (ServiceFactory<S>) service;
				final S factoredService;
				try {
					incrementCounter(theBundle);
					marker = true;
					factoredService = factory.getService(theBundle,
							registration);
					marker = false;
					checkService(factoredService,
							(String[]) properties.get(Constants.OBJECTCLASS));
					// catch failed check and exceptions thrown in factory
				} catch (final IllegalArgumentException iae) {
					decrementCounter(theBundle);
					framework.notifyFrameworkListeners(FrameworkEvent.ERROR,
							bundle, new ServiceException(
									"Invalid service object",
									ServiceException.FACTORY_ERROR));
					return null;
				} catch (final Throwable t) {
					decrementCounter(theBundle);
					framework.notifyFrameworkListeners(FrameworkEvent.ERROR,
							bundle, new ServiceException(
									"Exception while factoring the service",
									ServiceException.FACTORY_EXCEPTION, t));
					return null;
				}
				cachedServices.put(theBundle, factoredService);

				return factoredService;

			}

			incrementCounter(theBundle);

			return service;
		}
	}

	private void incrementCounter(final Bundle theBundle) {
		Integer counter = useCounters.get(theBundle);
		if (counter == null) {
			counter = new Integer(1);
		} else {
			counter = new Integer(counter.intValue() + 1);
		}
		useCounters.put(theBundle, counter);
	}

	private void decrementCounter(final Bundle theBundle) {
		Integer counter = useCounters.get(theBundle);
		final int newValue = counter.intValue() - 1;
		if (newValue == 0) {
			counter = null;
		} else {
			counter = new Integer(newValue);
		}
		useCounters.put(theBundle, counter);
	}

	/**
	 * unget the service.
	 * 
	 * @param theBundle
	 *            the using bundle.
	 * @return <tt>false</tt> if the context bundle's use count for the service
	 *         is zero or if the service has been unregistered; <tt>true</tt>
	 *         otherwise.
	 */
	@SuppressWarnings("unchecked")
	boolean ungetService(final Bundle theBundle) {
		synchronized (useCounters) {
			if (service == null) {
				return false;
			}
			Integer counter = useCounters.get(theBundle);
			if (counter == null) {
				return false;
			}
			if (counter.intValue() == 1) {
				if (isServiceFactory) {
					try {
						((ServiceFactory<S>) service).ungetService(theBundle,
								registration, cachedServices.get(theBundle));
						// catch exceptions thrown in factory
					} catch (final Throwable t) {
						framework.notifyFrameworkListeners(
								FrameworkEvent.ERROR, bundle, t);
					}
					useCounters.remove(theBundle);
					cachedServices.remove(theBundle);
				}
				return true;
			} else {
				counter = new Integer(counter.intValue() - 1);
				useCounters.put(theBundle, counter);
				return true;
			}
		}
	}

	/**
	 * get a string representation of the service reference implementation.
	 * 
	 * @return the string.
	 * @category Object
	 */
	public String toString() {
		return "ServiceReference{" + service + "}";
	}

	/**
	 * compare service references.
	 * 
	 * @param reference
	 *            , the reference to compare to
	 * @return integer , return value < 0 if this < reference return value = 0
	 *         if this = reference return value > 0 if tis > reference
	 * @see org.osgi.framework.ServiceReference#compareTo(Object)
	 * @category ServiceReference FIXME: ugly code!!!
	 */
	public int compareTo(final Object reference) {
		if (!(reference instanceof ServiceReferenceImpl)
				|| ((ServiceReferenceImpl<?>) reference).framework != framework) {
			throw new IllegalArgumentException(
					"ServiceReference was not created by the same framework instance");
		}
		final ServiceReferenceImpl<?> other = (ServiceReferenceImpl<?>) reference;
		final int comparedServiceIds = ((Long) properties
				.get(Constants.SERVICE_ID)).compareTo((Long) other.properties
				.get(Constants.SERVICE_ID));
		if (comparedServiceIds == 0) {
			return 0;
		}
		final int res = ((Integer) properties.get(Constants.SERVICE_RANKING))
				.compareTo((Integer) other.properties
						.get(Constants.SERVICE_RANKING));
		if (res < 0) {
			return -1;
		} else if (res > 0) {
			return 1;
		}
		if (comparedServiceIds < 0) {
			return 1;
		} else {
			return -1;
		}

	}

	/**
	 * test if bundle and class have same source
	 * 
	 * @param theBundle
	 *            the bundle
	 * @param className
	 *            the class name
	 * @return true if bundle and class have same source
	 * @see org.osgi.framework.ServiceReference#isAssignableTo(Bundle, String)
	 * @category ServiceReference
	 */
	public boolean isAssignableTo(final Bundle theBundle, final String className) {
		// if the bundle is the one that registered the service, we are done
		if (theBundle == bundle || bundle == framework
				|| theBundle == framework) {
			return true;
		}

		final BundleImpl otherBundle = (BundleImpl) theBundle;
		final BundleImpl ourBundle = (BundleImpl) bundle;

		try {
			return otherBundle.loadClass(className) == ourBundle
					.loadClass(className);
		} catch (final ClassNotFoundException e) {
			return true;
		}
	}

	/**
	 * The service registration. It is a private inner class since this entity
	 * is just once returned to the registrar and never retrieved again. It is
	 * more an additional facet of the service than a separate entity.
	 * 
	 * @author Jan S. Rellermeyer
	 */
	private final class ServiceRegistrationImpl implements
			ServiceRegistration<S> {

		/**
		 * get the service reference.
		 * 
		 * @return the service reference.
		 * @see org.osgi.framework.ServiceRegistration#getReference()
		 * @category ServiceRegistration
		 */
		public ServiceReference<S> getReference() {
			if (service == null) {
				throw new IllegalStateException(
						"Service has already been uninstalled");
			}
			return ServiceReferenceImpl.this;
		}

		/**
		 * set some new service properties.
		 * 
		 * @param newProps
		 *            the new service properties.
		 * @see org.osgi.framework.ServiceRegistration#setProperties(java.util.Dictionary)
		 * @category ServiceRegistration
		 */
		public void setProperties(final Dictionary<String, ?> newProps) {
			/*
			 * The values for service.id and objectClass must not be overwritten
			 */
			if (service == null) {
				throw new IllegalStateException(
						"Service has already been uninstalled");
			}

			final Hashtable<String, Object> oldProps = new Hashtable<String, Object>(
					properties);

			final HashMap<String, String> cases = new HashMap<String, String>(
					properties.size());
			for (final Enumeration<String> keys = properties.keys(); keys
					.hasMoreElements();) {
				final String key = keys.nextElement();
				final String lower = key.toLowerCase();
				if (cases.containsKey(lower)) {
					throw new IllegalArgumentException(
							"Properties contain the same key in different case variants");
				}
				cases.put(lower, key);
			}
			for (final Enumeration<String> keys = newProps.keys(); keys
					.hasMoreElements();) {
				final String key = keys.nextElement();
				final Object value = newProps.get(key);
				final String lower = key.toLowerCase();

				if (!forbidden.contains(lower)) {
					final Object existing = cases.get(lower);
					if (existing != null) {
						if (existing.equals(key)) {
							properties.remove(existing);
						} else {
							throw new IllegalArgumentException(
									"Properties already exists in a different case variant");
						}
					}
					properties.put(key, value);
				}
			}

			framework.notifyServiceListeners(ServiceEvent.MODIFIED,
					ServiceReferenceImpl.this, oldProps);
		}

		/**
		 * unregister the service.
		 * 
		 * @see org.osgi.framework.ServiceRegistration#unregister()
		 * @category ServiceRegistration
		 */
		public void unregister() {
			if (service == null) {
				throw new IllegalStateException(
						"Service has already been uninstalled");
			}

			framework.unregisterService(ServiceReferenceImpl.this);
			service = null;
		}
	}

	boolean isAssignableTo(final AbstractBundle otherBundle,
			final String[] clazzes) {
		// TODO: simplify or cache results.
		for (int j = 0; j < clazzes.length; j++) {
			if (!isAssignableTo(otherBundle, clazzes[j])) {
				return false;
			}
		}
		return true;
	}
}
