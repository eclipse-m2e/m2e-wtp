/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;

/**
 * ComponentModule Utility class
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class ComponentModuleUtil {

	private ComponentModuleUtil() {
	}

	public static IVirtualComponent getOrCreateComponent(IProject project, IProgressMonitor monitor) {
		IVirtualComponent component = ComponentCore.createComponent(project, true);
		if (component != null) {
			StructureEdit core = null;
			try {
					core = StructureEdit.getStructureEditForRead(project);
					if(core != null) {
						//For some reason, we're facing a lot of issues with
						// component.setReferences failing with an NPE because of
						// the underlying core.getComponent() being null but not being Null-Checked

						if (core.getComponent() == null){
							//We could try to fix the missing workbench module but we're already
							//drowning in a sea of hacks to workaround WTP quirks

							//core.createWorkbenchModule(project.getName());
							//core.saveIfNecessary(monitor);
							return null;
						}
					}
			} finally {
				if(core != null) {
					core.dispose();
				}
			}
		}
		return component;
	}

}
