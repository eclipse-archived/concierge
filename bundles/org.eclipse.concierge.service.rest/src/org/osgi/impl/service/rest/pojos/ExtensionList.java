/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

package org.osgi.impl.service.rest.pojos;

import java.util.ArrayList;
import org.osgi.framework.ServiceReference;
import org.osgi.impl.service.rest.PojoReflector.RootNode;
import org.osgi.service.rest.RestApiExtension;
import org.osgi.util.tracker.ServiceTracker;
import org.restlet.resource.ServerResource;

/**
 * List of extension pojos.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
@RootNode(name = "extensions")
@SuppressWarnings("serial")
public class ExtensionList extends ArrayList<ExtensionList.ExtensionPojo> {

	public ExtensionList(
			ServiceTracker<RestApiExtension, Class<? extends ServerResource>> tracker) {
		final ServiceReference<RestApiExtension>[] refs = tracker
				.getServiceReferences();
		if (refs != null) {
			for (final ServiceReference<RestApiExtension> ref : refs) {
				add(new ExtensionPojo(
						(String) ref.getProperty(RestApiExtension.NAME),
						(String) ref.getProperty(RestApiExtension.URI_PATH)));
			}
		}
	}

	/**
	 * Pojo for extensions to the REST service.
	 */
	@RootNode(name = "extension")
	public static class ExtensionPojo {

		private String	name;
		private String	path;

		public ExtensionPojo(final String name, final String path) {
			this.name = name;
			this.path = path;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

}
