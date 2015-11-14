/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.concierge.shell.commands;

/**
 * Interface for implementing new command groups that can be registered as
 * services in a whiteboard pattern sense and then extend the capabilities of
 * the shell.
 * 
 * @author Jan S. Rellermeyer
 */
public interface ShellCommandGroup {

	/**
	 * get the help page of the command group.
	 * 
	 * @return the help page as continuous string.
	 */
	String getHelp();

	/**
	 * get the group identifier. Should be a single word.
	 * 
	 * @return the group identifier.
	 */
	String getGroup();

	/**
	 * handle a command.
	 * 
	 * @param command
	 *            the command.
	 * @param args
	 *            the arguments.
	 * @throws Exception
	 *             if the command causes some exception.
	 */
	void handleCommand(final String command, final String[] args)
			throws Exception;
}
