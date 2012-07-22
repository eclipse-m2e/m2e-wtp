/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.filtering;

import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.IPath;

/**
 * ResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public interface ResourceFilteringConfiguration {

  /**
   * @return the target folder in which filtered resources should be generated
   */
  IPath getTargetFolder();
  
  /**
   * @return the list of resources to filter / copy
   */
  List<Xpp3Dom> getResources();
  
  /**
   * Filters (property files) to include during the interpolation of the pom.xml.
   * @return the list of Filters  
   */
  List<String> getFilters();

  /**
   * Expression preceded with this String won't be interpolated \${foo} will be replaced with ${foo}
   * @return the escape String
   */
  String getEscapeString();

  /**
   * @return A list of file extensions that should not be filtered if filtering is enabled.
   */
  List<Xpp3Dom> getNonfilteredExtensions();
}
