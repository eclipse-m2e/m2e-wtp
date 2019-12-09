/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
