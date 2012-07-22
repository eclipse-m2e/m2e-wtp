/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.overlay.modulecore;

import java.util.Set;

import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;

/**
 * Represents an Overlay virtual component. This component's resources can be filtered out depending
 * on its inclusion and exclusion patterns. 
 *
 * @author Fred Bricon
 */
public interface IOverlayVirtualComponent extends IVirtualComponent{

	void setInclusions(Set<String> inclusionPatterns);
	
	void setExclusions(Set<String> inclusionPatterns);
	
	Set<String> getExclusions();

	Set<String> getInclusions();

}
