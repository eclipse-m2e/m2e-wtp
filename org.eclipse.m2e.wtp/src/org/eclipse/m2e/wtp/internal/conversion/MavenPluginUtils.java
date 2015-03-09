/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.SearchExpression;
import org.eclipse.m2e.core.internal.index.SourcedSearchExpression;
import org.eclipse.m2e.jdt.internal.JavaProjectConversionParticipant;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link Plugin} manipulations.
 *
 * @author Fred Bricon
 */
public class MavenPluginUtils {

  private static final Logger log = LoggerFactory.getLogger(MavenPlugin.class);
  
  private static final String CONFIGURATION_KEY = "configuration"; //$NON-NLS-1$

  private MavenPluginUtils() {
  }
  
  private static Xpp3Dom getOrCreateConfiguration(Plugin plugin) {
    Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
    if (configuration == null) {
      configuration = new Xpp3Dom(CONFIGURATION_KEY);
      plugin.setConfiguration(configuration);
    }
    return configuration;
  }
  
  public static void configure(Plugin plugin, String key, String value) {
    if (plugin == null) {
      return;
    }
    Xpp3Dom configuration = getOrCreateConfiguration(plugin);
    Xpp3Dom keyDom = configuration.getChild(key);
    if (keyDom == null) {
      keyDom = new Xpp3Dom(key);
      configuration.addChild(keyDom);
    }  
    keyDom.setValue(value);
  }
  

  /**
   * Returns the highest, non-snapshot plugin version between the given reference version and the versions found in the
   * Nexus indexes.
   * 
   * This code was copied from {@link JavaProjectConversionParticipant}
   */
  @SuppressWarnings("restriction")
  public static String getMostRecentPluginVersion(String groupId, String artifactId, String referenceVersion) {
      Assert.isNotNull(groupId, Messages.MavenPluginUtils_GroupId_Cant_Be_Null);
      Assert.isNotNull(artifactId, Messages.MavenPluginUtils_ArtifactId_Cant_Be_Null);
      String version = referenceVersion;
      String partialKey = artifactId + " : " + groupId; //$NON-NLS-1$
      try {
        IIndex index = MavenPlugin.getIndexManager().getAllIndexes();
        SearchExpression a = new SourcedSearchExpression(artifactId);

        //For some reason, an exact search using : 
        //ISearchEngine searchEngine  = M2EUIPluginActivator.getDefault().getSearchEngine(null)
        //searchEngine.findVersions(groupId, artifactId, searchExpression, packaging)
        //
        //doesn't yield the expected results (the latest versions are not returned), so we rely on a fuzzier search
        //and refine the results.
        Map<String, IndexedArtifact> values = index.search(a, IIndex.SEARCH_PLUGIN);
        if(!values.isEmpty()) {
          SortedSet<ComparableVersion> versions = new TreeSet<ComparableVersion>();
          ComparableVersion referenceComparableVersion = referenceVersion == null ? null : new ComparableVersion(
              referenceVersion);

          for(Map.Entry<String, IndexedArtifact> e : values.entrySet()) {
            if(!(e.getKey().endsWith(partialKey))) {
              continue;
            }
            for(IndexedArtifactFile f : e.getValue().getFiles()) {
              if(groupId.equals(f.group) && artifactId.equals(f.artifact) && !f.version.contains("-SNAPSHOT")) { //$NON-NLS-1$
                ComparableVersion v = new ComparableVersion(f.version);
                if(referenceComparableVersion == null || v.compareTo(referenceComparableVersion) > 0) {
                  versions.add(v);
                }
              }
            }
            if(!versions.isEmpty()) {
              List<String> sorted = new ArrayList<>(versions.size());
              for(ComparableVersion v : versions) {
                sorted.add(v.toString());
              }
              Collections.reverse(sorted);
              version = sorted.iterator().next();
            }
          }
        }
      } catch(CoreException e) {
        log.error(NLS.bind(Messages.MavenPluginUtils_Error_Cant_Retrieve_Latest_Plugin_Version, partialKey), e);
      }
      return version;
    }
}
