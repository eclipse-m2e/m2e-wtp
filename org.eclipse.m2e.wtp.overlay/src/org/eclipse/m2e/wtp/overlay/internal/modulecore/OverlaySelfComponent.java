/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

/**
 * Overlay Self Component
 *
 * @author Fred Bricon
 */
public class OverlaySelfComponent extends OverlayVirtualComponent implements IOverlayVirtualComponent {

	public OverlaySelfComponent(IProject aComponentProject) {
		super(aComponentProject);
	}

	public IVirtualFolder getRootFolder() {
		IVirtualComponent component = ComponentCore.createComponent(getProject());
		IVirtualFolder root =null;
		if (component != null) {
			root = component.getRootFolder();
		}
		return root;
	}
	
	@Override
	public IVirtualReference[] getReferences(Map<String, Object> paramMap){
		return new IVirtualReference[0];
	}
}
