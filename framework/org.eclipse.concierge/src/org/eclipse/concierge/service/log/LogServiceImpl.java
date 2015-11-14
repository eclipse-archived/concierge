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

package org.eclipse.concierge.service.log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * A lightweight log service implementation. Since this is part of the
 * framework, the framework itself can be configured to use this log for debug
 * messages. This makes it easier to debug bundles on embedded and headless
 * devices.
 *
 * @author Jan S. Rellermeyer
 */
public final class LogServiceImpl implements LogReaderService {
	/**
	 * the log buffer. Works like a ring buffer. The size can be configured by a
	 * property.
	 */
	private final Vector<LogEntryImpl> logBuffer;

	/**
	 * the list of subscribed listeners.
	 */
	private final List<LogListener> logListeners = new ArrayList<LogListener>(
			0);

	/**
	 * the size.
	 */
	private final int LOG_BUFFER_SIZE;

	/**
	 * the log level.
	 */
	private final int LOG_LEVEL;

	/**
	 * do not log to screen ?
	 */
	private final boolean QUIET;

	/**
	 * the constants for the log levels.
	 */
	protected static final String[] LEVELS = { "NULL", "ERROR", "WARNING",
			"INFO", "DEBUG" };

	protected static final String getLevelString(final int level) {
		if (level < 0 || level > 4) {
			return Integer.toString(level);
		}
		return LEVELS[level];
	}

	public final ServiceFactory<LogService> factory = new LogServiceFactory();

	public LogServiceImpl(final int buffersize, final int loglevel,
			final boolean quiet) {
		LOG_BUFFER_SIZE = buffersize;
		if (loglevel < 0) {
			LOG_LEVEL = 0;
		} else if (loglevel > 4) {
			LOG_LEVEL = 4;
		} else {
			LOG_LEVEL = loglevel;
		}
		QUIET = quiet;
		logBuffer = new Vector<LogEntryImpl>(LOG_BUFFER_SIZE);
		if (!QUIET) {
			System.out.println(
					"Logger initialized, loglevel is " + LEVELS[LOG_LEVEL]);
		}
	}

	/**
	 * log an entry.
	 *
	 * @param entry
	 *            the entry.
	 */
	protected void log(final int level, final String message,
			final Throwable throwable, final ServiceReference<?> sref,
			final Bundle bundle) {
		if (level <= LOG_LEVEL) {
			final LogEntryImpl entry = LogEntryImpl.getEntry(level, message,
					throwable, sref, bundle);
			logBuffer.add(entry);
			if (logBuffer.size() > LOG_BUFFER_SIZE) {
				LogEntryImpl.releaseEntry(logBuffer.remove(0));
			}
			for (final Iterator<LogListener> listeners = logListeners
					.iterator(); listeners.hasNext();) {
				listeners.next().logged(entry);
			}
			if (!QUIET) {
				System.out.println(entry);
			}
		}
	}

	/**
	 * Add a log listener.
	 *
	 * @param listener
	 *            the new listener.
	 *
	 * @see org.osgi.service.log.LogReaderService#addLogListener(org.osgi.service.log.LogListener)
	 */
	public void addLogListener(final LogListener listener) {
		logListeners.add(listener);
	}

	/**
	 * remove a log listener.
	 *
	 * @param listener
	 *            the listener.
	 *
	 * @see org.osgi.service.log.LogReaderService#removeLogListener(org.osgi.service.log.LogListener)
	 */
	public void removeLogListener(final LogListener listener) {
		logListeners.remove(listener);
	}

	/**
	 * get the buffered log messages.
	 *
	 * @return an <code>Enumeration</code> over the buffered log messages.
	 *
	 * @see org.osgi.service.log.LogReaderService#getLog()
	 */
	public Enumeration<? extends LogEntry> getLog() {
		return logBuffer.elements();
	}

	/**
	 * The service factory producing per-bundle instances of the log service
	 * facet.
	 *
	 * @author Jan S. Rellermeyer
	 *
	 */
	final class LogServiceFactory implements ServiceFactory<LogService> {
		public LogService getService(final Bundle bundle,
				final ServiceRegistration<LogService> registration) {
			return new LogServiceInstance(bundle);
		}

		public void ungetService(final Bundle bundle,
				final ServiceRegistration<LogService> registration,
				final LogService service) {
			// nop
		}
	}

	/**
	 * A bundle-specific instance of the log service facet.
	 *
	 * @author Jan S. Rellermeyer
	 *
	 */
	@SuppressWarnings("rawtypes")
	private final class LogServiceInstance implements LogService {

		private final Bundle bundle;

		protected LogServiceInstance(final Bundle bundle) {
			this.bundle = bundle;
		}

		/**
		 * Log a message.
		 *
		 * @param level
		 *            the level.
		 * @param message
		 *            the message.
		 * @see org.osgi.service.log.LogService#log(int, java.lang.String)
		 */
		public void log(final int level, final String message) {
			LogServiceImpl.this.log(level, message, null, null, bundle);
		}

		/**
		 * Log a message.
		 *
		 * @param level
		 *            the level.
		 * @param message
		 *            the message.
		 * @param exception
		 *            an exception.
		 *
		 * @see org.osgi.service.log.LogService#log(int, java.lang.String,
		 *      java.lang.Throwable)
		 */
		public void log(final int level, final String message,
				final Throwable exception) {
			LogServiceImpl.this.log(level, message, exception, null, bundle);
		}

		/**
		 * Log a message.
		 *
		 * @param sr
		 *            the service reference.
		 * @param level
		 *            the level.
		 * @param message
		 *            the message.
		 * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference,
		 *      int, java.lang.String)
		 */
		public void log(final ServiceReference sr, final int level,
				final String message) {
			LogServiceImpl.this.log(level, message, null, sr, bundle);
		}

		/**
		 * Log a message.
		 *
		 * @param sr
		 *            the service reference.
		 * @param level
		 *            the level.
		 * @param message
		 *            the message.
		 *
		 * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference,
		 *      int, java.lang.String, java.lang.Throwable)
		 */
		public void log(final ServiceReference sr, final int level,
				final String message, final Throwable exception) {
			LogServiceImpl.this.log(level, message, exception, sr, bundle);
		}

	}

	/**
	 * A log entry.
	 *
	 * @author Jan S. Rellermeyer
	 *
	 */
	final static class LogEntryImpl implements LogEntry {
		private int level;

		private String message;

		private ServiceReference<?> sref;

		private Throwable exception;

		private Bundle bundle;

		private long time;

		private final static List<LogEntryImpl> entryRecyclingList = new ArrayList<LogEntryImpl>(
				5);

		private final static int THRESHOLD = 5;

		protected static LogEntryImpl getEntry(final int level,
				final String message, final Throwable throwable,
				final ServiceReference<?> sref, final Bundle bundle) {
			synchronized (entryRecyclingList) {
				final LogEntryImpl entry = entryRecyclingList.isEmpty()
						? new LogEntryImpl()
						: (LogEntryImpl) entryRecyclingList.remove(0);
				entry.log(level, message, throwable, sref, bundle);
				return entry;
			}
		}

		protected static void releaseEntry(final LogEntryImpl entry) {
			synchronized (entryRecyclingList) {
				if (entryRecyclingList.size() < THRESHOLD) {
					entry.log(0, null, null, null, null);
					entryRecyclingList.add(entry);
				}
			}
		}

		/**
		 *
		 */
		private LogEntryImpl() {
		}

		/**
		 * @param sref
		 * @param level
		 * @param message
		 * @param exception
		 */
		private void log(final int level, final String message,
				final Throwable exception, final ServiceReference<?> sref,
				final Bundle bundle) {
			this.level = level;
			this.message = message;
			this.exception = exception;
			this.sref = sref;
			this.bundle = bundle;
			this.time = System.currentTimeMillis();
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getBundle()
		 */
		public Bundle getBundle() {
			return bundle;
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getServiceReference()
		 */
		public ServiceReference<?> getServiceReference() {
			return sref;
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getLevel()
		 */
		public int getLevel() {
			return level;
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getMessage()
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getException()
		 */
		public Throwable getException() {
			return exception;
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getTime()
		 */
		public long getTime() {
			return time;
		}

		/**
		 *
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			final StringBuffer buffer = new StringBuffer("[")
					.append(new Date(time)).append("] [")
					.append(getLevelString(level)).append("] ");
			if (sref != null) {
				buffer.append("Bundle: ");
				buffer.append(sref.getBundle());
				buffer.append(" ");
				buffer.append("ServiceReference: ");
				buffer.append(sref);
				buffer.append(" ");
			}
			buffer.append(message);
			if (exception != null) {
				buffer.append("\n\tException: ");
				buffer.append(exception.getMessage());
			}
			return buffer.toString();
		}

	}

}
