/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * Represents a {@link IVirtualFolder} that can be filtered out from some resources.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 * 
 */
public interface IFilteredVirtualFolder extends IVirtualFolder {

	void setFilter(IResourceFilter filter);
	
	IResourceFilter getFilter();
}
