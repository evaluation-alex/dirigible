/*******************************************************************************
 * Copyright (c) 2017 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 *******************************************************************************/

package org.eclipse.dirigible.air.registry;

import javax.servlet.annotation.WebServlet;

/**
 * Wrapper for CommandRegistryServlet
 */
@WebServlet({ "/services/registry-command/*", "/services/scripting/command/*" })
public class CommandRegistryServlet extends org.eclipse.dirigible.runtime.registry.CommandRegistryServlet {
	private static final long serialVersionUID = 1L;
}