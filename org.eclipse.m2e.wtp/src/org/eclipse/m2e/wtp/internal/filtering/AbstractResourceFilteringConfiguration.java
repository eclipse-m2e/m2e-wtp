/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.filtering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.AbstractFilteringSupportMavenPlugin;

/**
 * Base class for resource filtering configuration
 *
 * @author Fred Bricon
 */
public abstract class AbstractResourceFilteringConfiguration implements ResourceFilteringConfiguration {

  protected IMavenProjectFacade mavenProjectFacade;
  protected AbstractFilteringSupportMavenPlugin pluginConfiguration;
  
  public AbstractResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
    this.mavenProjectFacade = mavenProjectFacade;
  }
  

  @Override
public List<String> getFilters() {
    List<String> filters = new ArrayList<>(mavenProjectFacade.getMavenProject().getFilters());
    if (pluginConfiguration != null) {
      filters.addAll(pluginConfiguration.getFilters());
    }
    return filters;
  }

  @Override
public String getEscapeString() {
    if (pluginConfiguration == null) {
      return null;
    }
    return pluginConfiguration.getEscapeString();
  }

  @Override
public List<Xpp3Dom> getNonfilteredExtensions() {
    if (pluginConfiguration == null) {
      return Collections.emptyList();
    }
    Xpp3Dom[] domext = pluginConfiguration.getNonfilteredExtensions();
    if(domext == null || domext.length == 0){
      return Collections.emptyList();
    }
    return Arrays.asList(domext);
  }
  
}
