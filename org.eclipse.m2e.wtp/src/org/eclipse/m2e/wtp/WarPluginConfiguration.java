/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.plugin.war.overlay.InvalidOverlayConfigurationException;
import org.apache.maven.plugin.war.overlay.OverlayManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.wtp.internal.StringUtils;
import org.eclipse.m2e.wtp.namemapping.FileNameMapping;
import org.eclipse.m2e.wtp.namemapping.PatternBasedFileNameMapping;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;


/**
 *  maven-war-plugin configuration model.
 *  
 * @see <a href="http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html">http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html</a>
 *  
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class WarPluginConfiguration extends AbstractFilteringSupportMavenPlugin implements IMavenPackageFilter {
  private static final String WAR_SOURCE_FOLDER = "/src/main/webapp";

  private static final String WAR_PACKAGING = "war";

  private static final String WEB_XML = "WEB-INF/web.xml";

  private IProject project;
  
  private MavenProject mavenProject;

  public WarPluginConfiguration(MavenProject mavenProject, IProject project) {
    this.project = project;
    this.mavenProject = mavenProject;
    Plugin plugin = getPlugin();
    setConfiguration((Xpp3Dom)plugin.getConfiguration());
  }

  public Plugin getPlugin() {
    return mavenProject.getPlugin("org.apache.maven.plugins:maven-war-plugin");
  }

  static boolean isWarProject(MavenProject mavenProject) {
    return WAR_PACKAGING.equals(mavenProject.getPackaging());
  }

  public Xpp3Dom[] getWebResources() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom webResources = config.getChild("webResources");
      if (webResources != null && webResources.getChildCount() > 0)
      {
        int count = webResources.getChildCount();  
        Xpp3Dom[] resources = new Xpp3Dom[count];
        for (int i= 0; i< count ; i++) {
          //MECLIPSEWTP-97 support old maven-war-plugin configurations which used <webResource> 
          // instead of <resource>
          Xpp3Dom webResource = new Xpp3Dom(webResources.getChild(i),"resource"); 
          
          //MECLIPSEWTP-152 : Web resource processing fails when targetPath has a leading /
          Xpp3Dom targetPath = webResource.getChild("targetPath");
          if(targetPath != null && targetPath.getValue().startsWith("/")) {
            targetPath.setValue(targetPath.getValue().substring(1));
          }

          resources[i] = webResource;
        }
        return resources;
      }
    }
    return new Xpp3Dom[0];
  }

  public String getWarSourceDirectory() {
    Xpp3Dom dom = getConfiguration();
    if(dom == null) {
      return WAR_SOURCE_FOLDER;
    }

    Xpp3Dom[] warSourceDirectory = dom.getChildren("warSourceDirectory");
    if(warSourceDirectory != null && warSourceDirectory.length > 0) {
      // first one wins
      String dir = warSourceDirectory[0].getValue();
      //MNGECLIPSE-1600 fixed absolute warSourceDirectory thanks to Snjezana Peco's patch
      if(project != null) {
        return WTPProjectsUtil.tryProjectRelativePath(project, dir).toOSString();
      }
      return dir;
    }

    return WAR_SOURCE_FOLDER;
  }

  public String[] getPackagingExcludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"packagingExcludes");
  }

  public String[] getPackagingIncludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"packagingIncludes");
  }

  public String[] getSourceExcludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"warSourceExcludes");
  }

  public String[] getSourceIncludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"warSourceIncludes");
  }

  public boolean isAddManifestClasspath() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom arch = config.getChild("archive");
      if(arch != null) {
        Xpp3Dom manifest = arch.getChild("manifest");
        if(manifest != null) {
          Xpp3Dom addToClp = manifest.getChild("addClasspath");
          if(addToClp != null) {
            return Boolean.valueOf(addToClp.getValue());
          }
        }
      }
  }
    return false;
  }

  public String getManifestClasspathPrefix() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom arch = config.getChild("archive");
      if(arch != null) {
        Xpp3Dom manifest = arch.getChild("manifest");
        if(manifest != null) {
          Xpp3Dom prefix = manifest.getChild("classpathPrefix");
          if(prefix != null && !StringUtils.nullOrEmpty(prefix.getValue())) {
            String rawPrefix = prefix.getValue().trim();
            if (!rawPrefix.endsWith("/")){
              rawPrefix += "/";
            }
            return rawPrefix;
          }
        }
      }
    }
    return null;
  }

  public IProjectFacetVersion getWebFacetVersion(IProject project) {
    IFile webXml;
    String customWebXml = getCustomWebXml(project);
    if (customWebXml == null) {
      webXml = project.getFolder(getWarSourceDirectory()).getFile(WEB_XML);
    } else {
      webXml = project.getFile(customWebXml);
    }

    if(webXml.isAccessible()) {
      try {
        InputStream is = webXml.getContents();
        try {
          JavaEEQuickPeek jqp = new JavaEEQuickPeek(is);
          switch(jqp.getVersion()) {
            case J2EEVersionConstants.WEB_2_2_ID:
              return WebFacetUtils.WEB_22;
            case J2EEVersionConstants.WEB_2_3_ID:
              return WebFacetUtils.WEB_23;
            case J2EEVersionConstants.WEB_2_4_ID:
              return WebFacetUtils.WEB_24;
            case J2EEVersionConstants.WEB_2_5_ID:
              return WebFacetUtils.WEB_25;
            //MNGECLIPSE-1978  
            case J2EEVersionConstants.WEB_3_0_ID:
              return WebFacetUtils.WEB_30;
          }
        } finally {
          is.close();
        }
      } catch(IOException ex) {
        // expected
      } catch(CoreException ex) {
        // expected
      }
    }
   
    //MNGECLIPSE-1978 If no web.xml found and the project depends on some java EE 6 jar, then set web facet to 3.0
    if (WTPProjectsUtil.hasInClassPath(project, "javax.servlet.annotation.WebServlet")) {
      return WebFacetUtils.WEB_30;
    }
    
    //MNGECLIPSE-984 web.xml is optional for 2.5 Web Projects
    return WTPProjectsUtil.DEFAULT_WEB_FACET;
    //We don't want to prevent the project creation when the java compiler level is < 5, we coud try that instead :
    //IProjectFacetVersion javaFv = JavaFacetUtils.compilerLevelToFacet(JavaFacetUtils.getCompilerLevel(project));
    //return (JavaFacetUtils.JAVA_50.compareTo(javaFv) > 0)?WebFacetUtils.WEB_24:WebFacetUtils.WEB_25; 
  }
  /**
   * Get the custom location of web.xml, as set in &lt;webXml&gt;.
   * @return the custom location of web.xml or null if &lt;webXml&gt; is not set
   */
  public String getCustomWebXml(IProject project) {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom webXmlDom = config.getChild("webXml");
      if(webXmlDom != null && webXmlDom.getValue() != null) {
        String webXmlFile = webXmlDom.getValue().trim();
        webXmlFile = ProjectUtils.getRelativePath(project, webXmlFile);
        return webXmlFile;
      }
    }
    return null;
  }

  /**
   * @return
   * @throws CoreException 
   */
  public List<Overlay> getOverlays() throws CoreException {
    Overlay currentProjectOverlay = Overlay.createInstance();
    currentProjectOverlay.setArtifact(mavenProject.getArtifact());
    OverlayManager overlayManager = null;
    List<Overlay> overlays = null;
    try {
      overlayManager = new OverlayManager(getConfiguredOverlays(), 
                                                         mavenProject, 
                                                         getDependentWarIncludes(),
                                                         getDependentWarExcludes(), 
                                                         currentProjectOverlay);
      overlays = overlayManager.getOverlays();
    } catch(InvalidOverlayConfigurationException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, ex.getMessage(),ex));
    }
    
    return overlays;
  }
  
  public String getDependentWarIncludes() {
    return DomUtils.getChildValue(getConfiguration(), "dependentWarIncludes", "**/**");
  }

  public String getDependentWarExcludes() {
    return DomUtils.getChildValue(getConfiguration(), "dependentWarExcludes", "META-INF/MANIFEST.MF");
  }

  public List<Overlay> getConfiguredOverlays() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom overlaysNode = config.getChild("overlays");
      if (overlaysNode != null && overlaysNode.getChildCount() > 0) {
        List<Overlay> overlays = new ArrayList<Overlay>(overlaysNode.getChildCount());
        for (Xpp3Dom overlayNode : overlaysNode.getChildren("overlay")) {
          overlays.add(parseOverlay(overlayNode));
        }
        return overlays;
      }
    }
    return Collections.emptyList();
  }
  
  /**
   * @param overlayNode
   * @return
   */
  private Overlay parseOverlay(Xpp3Dom overlayNode) {
    String artifactId = DomUtils.getChildValue(overlayNode, "artifactId");
    String groupId = DomUtils.getChildValue(overlayNode, "groupId");
    String[] exclusions = DomUtils.getChildrenAsStringArray(overlayNode.getChild("excludes"), "exclude");
    String[] inclusions = DomUtils.getChildrenAsStringArray(overlayNode.getChild("includes"), "include");
    String classifier = DomUtils.getChildValue(overlayNode, "classifier");
    boolean filtered = DomUtils.getBooleanChildValue(overlayNode, "filtered");
    boolean skip = DomUtils.getBooleanChildValue(overlayNode, "skip");
    String type = DomUtils.getChildValue(overlayNode, "type", "war");
    String targetPath = DomUtils.getChildValue(overlayNode, "targetPath", "/");

    Overlay overlay = new Overlay();
    overlay.setArtifactId(artifactId);
    overlay.setGroupId(groupId);
    overlay.setClassifier(classifier);
    if (exclusions== null || exclusions.length ==0) {
      overlay.setExcludes(getDependentWarExcludes());
    } else {
      overlay.setExcludes(exclusions);
    }
    if (inclusions== null || inclusions.length ==0) {
      overlay.setIncludes(getDependentWarIncludes());
    } else {
      overlay.setIncludes(inclusions);
    }
    overlay.setFiltered(filtered);
    overlay.setSkip(skip);
    overlay.setTargetPath(targetPath);
    overlay.setType(type);
    
    return overlay;
  }

  public FileNameMapping getFileNameMapping() {
    Xpp3Dom config = getConfiguration();
    String expression = null;
    if(config != null) {
      expression = DomUtils.getChildValue(config, "outputFileNameMapping");
    }
    return new PatternBasedFileNameMapping(expression);
  }
  
  protected String getFilteringAttribute() {
    return "filteringDeploymentDescriptors";
  }
  
  public String getWarName() {
    Xpp3Dom config = getConfiguration();
    String warName = null;
    if (config != null) {
      warName = DomUtils.getChildValue(config, "warName");
    }
    if (StringUtils.nullOrEmpty(warName)) {
      warName = mavenProject.getBuild().getFinalName();
    }
    return warName;
  }

  public SourceLocation getSourceLocation() {
    Plugin plugin = getPlugin();
    if (plugin == null) {
      return null;
    }
    return SourceLocationHelper.findLocation(plugin, "configuration");
  }

  public String getSourceIncludeParameterName() {
    return "warSourceIncludes";
  }

  
}
