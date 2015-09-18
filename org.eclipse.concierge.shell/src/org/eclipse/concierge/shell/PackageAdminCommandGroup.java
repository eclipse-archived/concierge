package org.eclipse.concierge.shell;

import org.eclipse.concierge.shell.commands.ShellCommandGroup;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * the package admin shell commands.
 * 
 * @author Jan S. Rellermeyer
 */
@SuppressWarnings("deprecation")
final class PackageAdminCommandGroup implements ShellCommandGroup {
	/**
	 * the package admin instance.
	 */
	private PackageAdmin pkgAdmin;

	/**
	 * create a new command group.
	 * 
	 * @param obj
	 *            the package admin instance.
	 */
	PackageAdminCommandGroup(final Object obj) {
		this.pkgAdmin = (PackageAdmin) obj;
	}

	/**
	 * get the group identifier.
	 * 
	 * @return the group identifier.
	 * @see org.eclipse.concierge.shell.commands.ShellCommandGroup#getGroup()
	 */
	public String getGroup() {
		return "package";
	}

	/**
	 * get the help page.
	 * 
	 * @return the help page.
	 * @see org.eclipse.concierge.shell.commands.ShellCommandGroup#getHelp()
	 */
	public String getHelp() {
		return "\tpackage.{\n\t\tpackages [<bundleID>]\n\t\trefresh [bundleID]\n\t}";
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
			final String cmd = command.intern();
			if (cmd == "packages") {
				final Bundle bundle;
				if (args.length > 0) {
					if ((bundle = Shell.getBundle(args[0])) == null) {
						return;
					}
				} else {
					bundle = null;
				}

				Shell.out.println("Packages:");
				ExportedPackage[] packages = pkgAdmin.getExportedPackages(bundle);
				if (packages == null) {
					Shell.out.println(
							"Package " + Shell.getBundle(args[0]).getBundleId() + " has no exported packages.");
				} else {
					for (int i = 0; i < packages.length; i++) {
						Shell.out.println(packages[i]);
					}
				}
				return;
			} else if (cmd == "refresh") {
				final Bundle[] bundles;
				if (args.length > 0) {
					final Bundle bundle;
					if ((bundle = Shell.getBundle(args[0])) == null) {
						return;
					}
					bundles = new Bundle[] { bundle };
				} else {
					bundles = null;
				}
				pkgAdmin.refreshPackages(bundles);
			} else {
				Shell.err.println("Unknown command package." + cmd);
			}
		} catch (final NumberFormatException nfe) {
			Shell.err.println("Illegal argument " + args[0]);
		}
	}
}
