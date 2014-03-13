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

package org.eclipse.concierge.service.log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LogEntry;

/**
 * A lightweight log service implementation. Since this is part of the
 * framework, the framework itself can be configured to use this log for debug
 * messages. This makes it easier to debug bundles on embedded and headless
 * devices.
 * 
 * @author Jan S. Rellermeyer
 */
@SuppressWarnings("rawtypes")
public final class LogServiceImpl implements LogService, LogReaderService {
	/**
	 * the log buffer. Works like a ring buffer. The size can be configured by a
	 * property.
	 */
	private final Vector<LogEntryImpl> logBuffer;

	/**
	 * the list of subscribed listeners.
	 */
	private final List<LogListener> logListeners = new ArrayList<LogListener>(0);

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
	private static final String[] LEVELS = { "NULL", "ERROR", "WARNING",
			"INFO", "DEBUG" };

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
			System.out.println("Logger initialized, loglevel is "
					+ LEVELS[LOG_LEVEL]);
		}
	}

	/**
	 * log an entry.
	 * 
	 * @param entry
	 *            the entry.
	 */
	private void log(final int level, final String message,
			final Throwable throwable, final ServiceReference sref) {
		if (level <= LOG_LEVEL) {
			final LogEntryImpl entry = LogEntryImpl.getEntry(level, message,
					throwable, sref);
			logBuffer.add(entry);
			if (logBuffer.size() > LOG_BUFFER_SIZE) {
				LogEntryImpl.releaseEntry((LogEntryImpl) logBuffer.remove(0));
			}
			for (Iterator listeners = logListeners.iterator(); listeners
					.hasNext();) {
				((LogListener) listeners.next()).logged(entry);
			}
			if (!QUIET) {
				System.out.println(entry);
			}
		}
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
	public void log(int level, String message) {
		log(level, message, null, null);
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
	public void log(int level, String message, Throwable exception) {
		log(level, message, exception, null);
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
	 *      int, java.lang.String)
	 */
	public void log(ServiceReference sr, int level, String message) {
		log(level, message, null, sr);
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
	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		log(level, message, exception, sr);

	}

	/**
	 * Add a log listener.
	 * 
	 * @param listener
	 *            the new listener.
	 * 
	 * @see org.osgi.service.log.LogReaderService#addLogListener(org.osgi.service.log.LogListener)
	 */
	public void addLogListener(LogListener listener) {
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
	public void removeLogListener(LogListener listener) {
		logListeners.remove(listener);
	}

	/**
	 * get the buffered log messages.
	 * 
	 * @return an <code>Enumeration</code> over the buffered log messages.
	 * 
	 * @see org.osgi.service.log.LogReaderService#getLog()
	 */
	public Enumeration getLog() {
		return logBuffer.elements();
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

		private ServiceReference sref;

		private Throwable exception;

		private long time;

		private final static List<LogEntryImpl> entryRecyclingList = new ArrayList<LogEntryImpl>(
				5);

		private final static int THRESHOLD = 5;

		private static LogEntryImpl getEntry(final int level,
				final String message, final Throwable throwable,
				final ServiceReference sref) {
			synchronized (entryRecyclingList) {
				LogEntryImpl entry = entryRecyclingList.isEmpty() ? new LogEntryImpl()
						: (LogEntryImpl) entryRecyclingList.remove(0);
				entry.log(level, message, throwable, sref);
				return entry;
			}
		}

		private static void releaseEntry(final LogEntryImpl entry) {
			synchronized (entryRecyclingList) {
				if (entryRecyclingList.size() < THRESHOLD) {
					entry.log(0, null, null, null);
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
				final Throwable exception, final ServiceReference sref) {
			this.level = level;
			this.message = message;
			this.exception = exception;
			this.sref = sref;
			this.time = System.currentTimeMillis();
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getBundle()
		 */
		public Bundle getBundle() {
			return sref == null ? null : sref.getBundle();
		}

		/**
		 * @see org.osgi.service.log.LogEntry#getServiceReference()
		 */
		public ServiceReference getServiceReference() {
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
			StringBuffer buffer = new StringBuffer("[").append(new Date(time))
					.append("] [").append(LEVELS[level]).append("] ");
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
