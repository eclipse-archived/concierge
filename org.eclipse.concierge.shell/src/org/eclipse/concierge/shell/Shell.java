/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jan S. Rellermeyer, IBM Research - initial API and implementation
 *******************************************************************************/
package org.eclipse.concierge.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.eclipse.concierge.shell.commands.ShellCommandGroup;

/**
 * <p>
 * The shell class. Implements a simple shell for executing framework-related
 * commands.
 * </p>
 * <p>
 * Other bundle can register services of the type
 * <code>org.eclipse.concierge.shell.commands.ShellCommandGroup</code> and
 * dynamically extend the shell by providing their own commands.
 * </p>
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class Shell extends Thread implements ServiceListener {
	/**
	 * is the shell running ?
	 */
	static boolean running;

	/**
	 * the known command groups.
	 */
	private Map<String, ShellCommandGroup> commandGroups = new HashMap<String, ShellCommandGroup>(
			1);

	/**
	 * empty string array.
	 */
	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * the standard out stream.
	 */
	static PrintStream out;

	/**
	 * the standard err stream.
	 */
	static PrintStream err;

	/**
	 * the default commands.
	 */
	private DefaultCommands defaultCommands;

	/**
	 * Create a new shell instance.
	 * 
	 * @param out
	 *            the standard out.
	 * @param err
	 *            the standard err.
	 * @param groups
	 *            all ShellCommandGroup plugins.
	 */
	public Shell(final PrintStream out, final PrintStream err,
			final ShellCommandGroup[] groups) {
		Shell.out = out;
		Shell.err = err;
		defaultCommands = new DefaultCommands();
		Shell.running = true;
		for (int i = 0; i < groups.length; i++) {
			commandGroups.put(groups[i].getGroup(), groups[i]);
		}
		start();
	}

	/**
	 * main thread loop.
	 */
	public void run() {
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));

		try {
			while (running) {
				out.print("\r\nConcierge> ");
				String s = in.readLine();
				if (s == null) {
					running = false;
				}
				if (running) {
					handleCommand(s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace(err);
		}

	}

	/**
	 * handle a command.
	 * 
	 * @param s
	 *            the command string
	 */
	private void handleCommand(final String s) {
		try {
			int pos;
			if (s.equals("")) {
				return;
			}

			final String grcmd = (pos = s.indexOf(" ")) > -1 ? s.substring(0,
					pos).toLowerCase() : s.toLowerCase();
			final String[] args = pos > -1 ? getArgs(s.substring(pos + 1))
					: EMPTY_STRING_ARRAY;
			final String group = (pos = grcmd.indexOf(".")) > -1 ? grcmd
					.substring(0, pos) : "";
			final String command = pos > -1 ? grcmd.substring(pos + 1) : grcmd;

			if (command.equals("help")) {
				out.println(defaultCommands.getHelp());
				ShellCommandGroup[] groups = (ShellCommandGroup[]) commandGroups
						.values().toArray(
								new ShellCommandGroup[commandGroups.size()]);
				for (int i = 0; i < groups.length; i++) {
					out.println(groups[i].getHelp());
				}
				return;
			}

			if (group.equals("")) {
				defaultCommands.handleCommand(command, args);
			} else {
				ShellCommandGroup commandGroup = (ShellCommandGroup) commandGroups
						.get(group);
				if (commandGroup != null) {
					commandGroup.handleCommand(command, args);
				} else {
					out.println("unknown comand group " + group);
				}
			}
		} catch (final Throwable t) {
			t.printStackTrace(err);
		}
	}

	/**
	 * get the arguments of a command.
	 * 
	 * @param args
	 *            the argument string.
	 * @return the arguments.
	 */
	protected String[] getArgs(final String args) {
		final StringTokenizer tokenizer = new StringTokenizer(args, " ");
		final ArrayList<String> result = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken().trim();
			if (token.startsWith("\"")) {
				StringBuffer buffer = new StringBuffer();
				buffer.append(token.substring(1));
				while (!token.endsWith("\"")) {
					if (!tokenizer.hasMoreTokens()) {
						throw new RuntimeException(
								"Expression not well-formed.");
					}
					token = tokenizer.nextToken();
					buffer.append(' ');
					buffer.append(token);
				}
				token = buffer.substring(0, buffer.length() - 1);
			}
			result.add(token);
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * get a bundle.
	 * 
	 * @param bundleIdString
	 *            the bundle id as string.
	 * @return the bundle.
	 */
	protected static Bundle getBundle(final String bundleIdString) {
		int pos;
		final long bundleId = Long
				.parseLong((pos = bundleIdString.indexOf(" ")) > -1 ? bundleIdString
						.substring(0, pos) : bundleIdString);
		final Bundle bundle = ShellActivator.context.getBundle(bundleId);
		if (bundle == null) {
			err.println("Unknown bundle " + bundleId);
		}
		return bundle;
	}

	protected static ServiceReference<?> getServiceRef(
			final String serviceIdString) {
		try {
			final ServiceReference<?>[] ref = ShellActivator.context
					.getServiceReferences((String) null, "("
							+ Constants.SERVICE_ID + "=" + serviceIdString
							+ ")");
			if (ref == null) {
				err.println("Unknown service " + serviceIdString);
			}
			return ref[0];
		} catch (final InvalidSyntaxException e) {
			e.printStackTrace(err);
			return null;
		}
	}

	/**
	 * the default commands.
	 * 
	 * @author Jan S. Rellermeyer
	 */
	protected static final class DefaultCommands implements ShellCommandGroup {

		/**
		 * @return the group identifier. It is the empty string for the default
		 *         command group.
		 * @see org.eclipse.concierge.shell.commands.ShellCommandGroup#getGroup()
		 */
		public String getGroup() {
			return "";
		}

		/**
		 * @return the help page.
		 * @see org.eclipse.concierge.shell.commands.ShellCommandGroup#getHelp()
		 */
		public String getHelp() {
			return "Concierge Shell:\n\tbundles\n\tservices [<bundle>]\n\tinstall <URL of bundle>\n\tstart <bundleId>\n\tstop <bundleId>\n\tupdate <bundleId> [<URL>]\n\tuninstall <bundleId>\n\theaders <bundleId>\n\tstartlevel <bundleId> [startlevel]\n\tproperties <serviceId>\n\tprintenv \n\trestart (framework)\n\texit (framework)\n\tquit (shell)\n";
		}

		/**
		 * handle a command.
		 * 
		 * @param command
		 *            the command.
		 * @param args
		 *            the arguments.
		 * @see org.eclipse.concierge.shell.commands.ShellCommandGroup#handleCommand(java.lang.String,
		 *      java.lang.String[])
		 */
		public void handleCommand(final String command, final String[] args) {
			try {
				if ("bundles".equals(command)) {
					out.println("Bundles:");
					final StringBuffer buffer = new StringBuffer();
					final Bundle[] bundles = ShellActivator.context
							.getBundles();
					for (int i = 0; i < bundles.length; i++) {
						buffer.append("[");
						buffer.append(formatId(bundles[i].getBundleId()));
						buffer.append("] ");
						buffer.append(status(bundles[i].getState()));
						buffer.append(" ");
						buffer.append(nameOrLocation(bundles[i]));
						buffer.append("\r\n");
					}
					out.println(buffer.toString());
					return;
				} else if ("services".equals(command)) {
					out.println("Services:");
					final Bundle[] bundles = ShellActivator.context
							.getBundles();
					final StringBuffer buffer = new StringBuffer();
					if (args.length == 0) {
						for (int i = 0; i < bundles.length; i++) {
							final ServiceReference<?>[] refs = bundles[i]
									.getRegisteredServices();
							if (refs != null && refs.length > 0) {
								buffer.append(bundles[i] + "\n");
								for (int j = 0; j < refs.length; j++) {
									buffer.append("\t[Service ");
									buffer.append(refs[j]
											.getProperty(Constants.SERVICE_ID));
									buffer.append("] ");
									buffer.append(Arrays.asList((Object[]) refs[j]
											.getProperty(Constants.OBJECTCLASS)));
									buffer.append('\n');
								}
							}
						}
						out.println(buffer.toString());
					} else {
						final Bundle bundle = getBundle(args[0]);
						final ServiceReference<?>[] refs = bundle
								.getRegisteredServices();
						buffer.append(bundle + "\n");
						if (refs != null && refs.length > 0) {
							for (int i = 0; i < refs.length; i++) {
								buffer.append("\t[Service ");
								buffer.append(refs[i]
										.getProperty(Constants.SERVICE_ID));
								buffer.append("] ");
								buffer.append(Arrays.asList((Object[]) refs[i]
										.getProperty(Constants.OBJECTCLASS)));
								buffer.append("\n");
							}
						} else {
							buffer.append("\t<none>\n");
						}
						out.println(buffer.toString());
					}
					return;
				} else if ("properties".equals(command)) {
					if (args.length > 0) {
						final ServiceReference<?> ref = getServiceRef(args[0]);
						final String[] keys = ref.getPropertyKeys();
						out.println("Service [" + args[0] + "]:");
						for (int i = 0; i < keys.length; i++) {
							Object value = ref.getProperty(keys[i]);
							if (value.getClass().isArray()) {
								value = Arrays.asList((Object[]) value);
							}
							out.println("\t'" + keys[i] + "' = '" + value + "'");
						}
					} else {
						err.println("Missing argument <serviceId>");
					}
				} else if ("filter".equals(command)) {
					if (args.length == 1) {
						final ServiceReference<?>[] refs = ShellActivator.context
								.getServiceReferences((String) null, args[0]);
						if (refs != null && refs.length > 0) {
							for (int j = 0; j < refs.length; j++) {
								out.println("\t["
										+ refs[j]
												.getProperty(Constants.SERVICE_ID)
										+ "] "
										+ Arrays.asList((Object[]) refs[j]
												.getProperty(Constants.OBJECTCLASS)));
							}
						}
					} else {
						out.println("Usage: filter <filterExpression>");
					}
				} else if ("install".equals(command)) {
					if (args.length > 0) {
						ShellActivator.context.installBundle(args[0]);
					} else {
						err.println("Missing argument <bundleURL>");
					}
					return;
				} else if ("start".equals(command)) {
					final Bundle bundle;
					if (args.length > 0) {
						if ((bundle = getBundle(args[0])) != null) {
							try {
								bundle.start();
							} catch (BundleException be) {
								be.printStackTrace();
								final Throwable t = be.getNestedException();
								if (t != null) {
									System.err.println("Nested exception:");
									t.printStackTrace();
								}
							}
							out.println(bundle.toString() + " started.");
						}
					} else {
						err.println("Missing argument <bundleId>");
					}
					return;
				} else if ("stop".equals(command)) {
					final Bundle bundle;
					if (args.length > 0) {
						if ((bundle = getBundle(args[0])) != null) {
							bundle.stop();
							System.out.println(bundle.toString() + " stopped.");
						}
					} else {
						err.println("Missing argument <bundleId>");
					}
					return;
				} else if ("uninstall".equals(command)) {
					final Bundle bundle;
					if (args.length > 0) {
						if ((bundle = getBundle(args[0])) != null) {
							bundle.uninstall();
							out.println(bundle.toString() + " uninstalled.");
						}
					} else {
						System.err.println("Missing argument <bundleId>");
					}
					return;
				} else if ("update".equals(command)) {
					final Bundle bundle;
					if (args.length == 1) {
						if ((bundle = getBundle(args[0])) != null) {
							bundle.update();
							out.println(bundle.toString() + " updated.");
						}
					} else if (args.length > 1) {
						if ((bundle = getBundle(args[0])) != null) {
							bundle.update(new URL(args[1]).openStream());
							out.println(bundle.toString() + " updated.");
						}
					} else {
						err.println("Missing argument <bundleId>");
					}
					return;
				} else if ("headers".equals(command)) {
					final Bundle bundle;
					if (args.length > 0) {
						if ((bundle = getBundle(args[0])) != null) {
							final Dictionary<String, String> dict = bundle
									.getHeaders();
							final StringBuffer buffer = new StringBuffer();
							buffer.append("Headers for " + bundle + ":\n");
							for (Enumeration<String> en = dict.keys(); en
									.hasMoreElements();) {
								final Object key = en.nextElement();
								buffer.append("\t" + key + " = "
										+ dict.get(key) + "\n");
							}
							out.println(buffer.toString());
						}
					} else {
						err.println("Missing argument <bundleId>");
					}
					return;
				} else if ("startlevel".equals(command)) {
					if (args.length > 0) {
						final Bundle bundle;
						if ((bundle = getBundle(args[0])) != null) {
							final BundleStartLevel bsl = bundle
									.adapt(BundleStartLevel.class);
							if (args.length > 1) {
								bsl.setStartLevel(Integer.parseInt(args[1]));
							}
							final StringBuffer buffer = new StringBuffer();
							buffer.append("Startlevel for " + bundle + ": ");
							buffer.append(bsl.getStartLevel());
							out.println(buffer.toString());
						}
					} else {
						err.println("Missing argument <bundleId>");
					}
					return;
				} else if ("restart".equals(command)) {
					try {
						ShellActivator.context.getBundle(0).update();
					} catch (BundleException e) {
						e.printStackTrace();
					}
				} else if ("quit".equals(command)) {
					try {
						ShellActivator.context.getBundle().stop();
					} catch (BundleException e) {
						e.printStackTrace();
					}
					return;
				} else if ("exit".equals(command)) {
					try {
						ShellActivator.context.getBundle(0).stop();
					} catch (BundleException e) {
						e.printStackTrace();
					}
					running = false;
					return;
				} else if ("printenv".equals(command)) {
					final String[] keys = (String[]) System.getProperties()
							.keySet()
							.toArray(new String[System.getProperties().size()]);
					for (int i = 0; i < keys.length; i++) {
						final String val = System.getProperty(keys[i]);
						out.println(keys[i] + " = " + val);
					}
				} else {
					out.println("unknown command " + command);
				}
			} catch (final BundleException be) {
				be.printStackTrace(err);
			} catch (final NumberFormatException nfe) {
				err.println("Illegal argument " + args[0]);
			} catch (final MalformedURLException e) {
				err.println("Malformed URL " + args[1]);
			} catch (final IOException ioe) {
				ioe.printStackTrace(err);
			} catch (final InvalidSyntaxException e) {
				e.printStackTrace(err);
			}
		}

		/**
		 * Format the bundle id.
		 * 
		 * @param id
		 * @return
		 */
		private String formatId(final long id) {
			return (id < 10) ? " " + id : "" + id;
		}

		private String nameOrLocation(final Bundle b) {
			final Object name = b.getHeaders().get("Bundle-Name");
			return name != null ? name.toString() : b.getLocation();
		}

		/**
		 * get the status.
		 * 
		 * @param state
		 *            the state code.
		 * @return the status message.
		 */
		private String status(final int state) {
			switch (state) {
			case Bundle.ACTIVE:
				return "(active)";
			case Bundle.INSTALLED:
				return "(installed)";
			case Bundle.RESOLVED:
				return ("(resolved)");
			case Bundle.STARTING:
				return ("(starting)   ");
			case Bundle.STOPPING:
				return ("(stopping)");
			case Bundle.UNINSTALLED:
				return ("(uninstalled)");
			}
			return null;
		}
	}


	/**
	 * service listener method.
	 * 
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(final ServiceEvent event) {
		final ServiceReference<?> ref = event.getServiceReference();
		final int type = event.getType();
		if (type == ServiceEvent.REGISTERED) {
			final ShellCommandGroup group = (ShellCommandGroup) ShellActivator.context
					.getService(ref);
			commandGroups.put(group.getGroup(), group);
			return;
		} else if (type == ServiceEvent.UNREGISTERING) {
			final ShellCommandGroup group = (ShellCommandGroup) ShellActivator.context
					.getService(ref);
			commandGroups.remove(group.getGroup());
			return;
		}
	}

}
