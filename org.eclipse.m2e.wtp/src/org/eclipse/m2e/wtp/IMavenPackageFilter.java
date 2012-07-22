/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import org.eclipse.m2e.core.internal.markers.SourceLocation;

/**
 * Represents Maven packaging filter settings.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public interface IMavenPackageFilter {

  /**
   * @return exclusion patterns applied during packaging
   */
  public String[] getPackagingExcludes();

  /**
   * @return inclusion patterns applied during packaging
   */
  public String[] getPackagingIncludes();

  /**
   * @return exclusion patterns applied on sources
   */
  public String[] getSourceExcludes();

  /**
   * @return exclusion patterns applied on sources
   */
  public String[] getSourceIncludes();
  
  
  /**
   * @return the source location of the maven plugin
   */
  public SourceLocation getSourceLocation();

  /**
   * @return the name of the *SourceInclude parameter
   */
  public String getSourceIncludeParameterName();
  
}
