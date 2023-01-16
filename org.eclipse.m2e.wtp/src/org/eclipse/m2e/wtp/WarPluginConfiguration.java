/*******************************************************************************
 * Copyright (c) 2008-2023 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
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
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.StringUtils;
import org.eclipse.m2e.wtp.namemapping.FileNameMapping;
import org.eclipse.m2e.wtp.namemapping.PatternBasedFileNameMapping;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

  private static final Logger LOG = LoggerFactory.getLogger(WarPluginConfiguration.class);
  
  private static final String WAR_SOURCE_FOLDER = "/src/main/webapp"; //$NON-NLS-1$

  private static final String WAR_PACKAGING = "war"; //$NON-NLS-1$

  private static final String WEB_XML = "WEB-INF/web.xml"; //$NON-NLS-1$

  private static final int WEB_3_1_ID = 31;
  private static final String WEB_3_1_TEXT = "3.1"; //$NON-NLS-1$

  private static final int WEB_4_0_ID = 40;
  private static final String WEB_4_0_TEXT = "4.0"; //$NON-NLS-1$

  private static final int WEB_5_0_ID = 50;
  private static final String WEB_5_0_TEXT = "5.0"; //$NON-NLS-1$

  private static final int WEB_6_0_ID = 60;
  private static final String WEB_6_0_TEXT = "6.0"; //$NON-NLS-1$

  private static final String FAIL_ON_MISSING_WEB_XML = "failOnMissingWebXml";
  
  
  //Keep backward compat with WTP < Kepler by having our own constants
  private static final IProjectFacetVersion WEB_31 = WebFacetUtils.WEB_FACET.hasVersion(WEB_3_1_TEXT)?
                                                              WebFacetUtils.WEB_FACET.getVersion(WEB_3_1_TEXT)
                                                             :WebFacetUtils.WEB_30;

                                                              
  private static final IProjectFacetVersion WEB_40 = WebFacetUtils.WEB_FACET.hasVersion(WEB_4_0_TEXT)?
													          WebFacetUtils.WEB_FACET.getVersion(WEB_4_0_TEXT)
													         :WEB_31;
          

  private static final IProjectFacetVersion WEB_50 = WebFacetUtils.WEB_FACET.hasVersion(WEB_5_0_TEXT)?
	          WebFacetUtils.WEB_FACET.getVersion(WEB_5_0_TEXT)
	         :WEB_40;

  private static final IProjectFacetVersion WEB_60 = WebFacetUtils.WEB_FACET.hasVersion(WEB_6_0_TEXT)?
	          WebFacetUtils.WEB_FACET.getVersion(WEB_6_0_TEXT)
	         :WEB_50;

  private boolean defaultFailOnMissingWebXml = true; 
  
  private IProject project;
  
  private MavenProject mavenProject;

  /** {@code true} if {@code maven-war-plugin} version is &ge; 3.0.0 */
  private boolean isVersion3OrGreater = false;

  public WarPluginConfiguration(MavenProject mavenProject, IProject project) {
    this.project = project;
    this.mavenProject = mavenProject;
    Plugin plugin = getPlugin();
    if (plugin != null) {
    	try {
        	VersionRange war_3_0_0 = VersionRange.createFromVersionSpec("[3.0.0,)"); //$NON-NLS-1$
        	if(war_3_0_0.containsVersion(new DefaultArtifactVersion(plugin.getVersion()))) {
        		isVersion3OrGreater = true;
        		defaultFailOnMissingWebXml = false;
        	}
        } catch(Exception ex) {
            //Can't happen
        }
    	
    	setConfiguration((Xpp3Dom)plugin.getConfiguration());
    }
  }

  public Plugin getPlugin() {
    return mavenProject.getPlugin("org.apache.maven.plugins:maven-war-plugin"); //$NON-NLS-1$
  }

  static boolean isWarProject(MavenProject mavenProject) {
    return WAR_PACKAGING.equals(mavenProject.getPackaging());
  }

  public Xpp3Dom[] getWebResources() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom webResources = config.getChild("webResources"); //$NON-NLS-1$
      if (webResources != null && webResources.getChildCount() > 0)
      {
        int count = webResources.getChildCount();  
        Xpp3Dom[] resources = new Xpp3Dom[count];
        for (int i= 0; i< count ; i++) {
          //MECLIPSEWTP-97 support old maven-war-plugin configurations which used <webResource> 
          // instead of <resource>
          Xpp3Dom webResource = new Xpp3Dom(webResources.getChild(i),"resource");  //$NON-NLS-1$
          
          //MECLIPSEWTP-152 : Web resource processing fails when targetPath has a leading /
          Xpp3Dom targetPath = webResource.getChild("targetPath"); //$NON-NLS-1$
          if(targetPath != null && targetPath.getValue() != null && targetPath.getValue().startsWith("/")) { //$NON-NLS-1$
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

    Xpp3Dom[] warSourceDirectory = dom.getChildren("warSourceDirectory"); //$NON-NLS-1$
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

  @Override
public String[] getPackagingExcludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"packagingExcludes"); //$NON-NLS-1$
  }

  @Override
public String[] getPackagingIncludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"packagingIncludes"); //$NON-NLS-1$
  }

  @Override
public String[] getSourceExcludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"warSourceExcludes"); //$NON-NLS-1$
  }

  @Override
public String[] getSourceIncludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"warSourceIncludes"); //$NON-NLS-1$
  }

  public boolean isAddManifestClasspath() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom arch = config.getChild("archive"); //$NON-NLS-1$
      if(arch != null) {
        Xpp3Dom manifest = arch.getChild("manifest"); //$NON-NLS-1$
        if(manifest != null) {
          Xpp3Dom addToClp = manifest.getChild("addClasspath"); //$NON-NLS-1$
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
      Xpp3Dom arch = config.getChild("archive"); //$NON-NLS-1$
      if(arch != null) {
        Xpp3Dom manifest = arch.getChild("manifest"); //$NON-NLS-1$
        if(manifest != null) {
          Xpp3Dom prefix = manifest.getChild("classpathPrefix"); //$NON-NLS-1$
          if(prefix != null && !StringUtils.nullOrEmpty(prefix.getValue())) {
            String rawPrefix = prefix.getValue().trim();
            if (!rawPrefix.endsWith("/")){ //$NON-NLS-1$
              rawPrefix += "/"; //$NON-NLS-1$
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

	if (webXml.isAccessible()) {
	// web.xml was found, see to what version of the grammar it refers
	    try (InputStream is = webXml.getContents()){
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
	        case J2EEVersionConstants.WEB_3_0_ID:
	          return WebFacetUtils.WEB_30;
	        case WEB_3_1_ID:
	          return WEB_31;
	        case WEB_4_0_ID:
	          return WEB_40;
	        case WEB_5_0_ID:
	          return WEB_50;
	        case WEB_6_0_ID:
	          return WEB_60;
	      }
	    } catch(IOException | CoreException ex) {
        // expected
	    }
    }

    //If no web.xml found and the project depends on Servlet 6.0 API, then set web facet to 6.0
    if (WTPProjectsUtil.hasInClassPath(project, "jakarta.servlet.ServletConnection")) { //$NON-NLS-1$
        return WEB_60;
    }

    //If no web.xml found and the project depends on Servlet 5.0 API, then set web facet to 5.0
    if (WTPProjectsUtil.hasInClassPath(project, "jakarta.servlet.GenericFilter")) { //$NON-NLS-1$
        return WEB_50;
    }

    //If no web.xml found and the project depends on Servlet 4.0 API, then set web facet to 4.0
    if (WTPProjectsUtil.hasInClassPath(project, "javax.servlet.http.HttpServletMapping")) { //$NON-NLS-1$
      return WEB_40;
    }
    
    //If no web.xml found and the project depends on Servlet 3.1 API, then set web facet to 3.1
    if (WTPProjectsUtil.hasInClassPath(project, "javax.servlet.http.WebConnection")) { //$NON-NLS-1$
      return WEB_31;
    }
    //MNGECLIPSE-1978 If no web.xml found and the project depends on Servlet 3 API, then set web facet to 3.0
    if (WTPProjectsUtil.hasInClassPath(project, "javax.servlet.annotation.WebServlet")) { //$NON-NLS-1$
      return WebFacetUtils.WEB_30;
    }
    
    //If no web.xml found, don't change existing facet version
    try {
      IFacetedProject fProject = ProjectFacetsManager.create(project);
      if (fProject != null && fProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
        return fProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET);
      }
    } catch (Exception e) {
      LOG.warn(NLS.bind(Messages.Error_Reading_Project_Facet, project.getName()), e); 
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
      Xpp3Dom webXmlDom = config.getChild("webXml"); //$NON-NLS-1$
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
    return DomUtils.getChildValue(getConfiguration(), "dependentWarIncludes", "**/**"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public String getDependentWarExcludes() {
    return DomUtils.getChildValue(getConfiguration(), "dependentWarExcludes", "META-INF/MANIFEST.MF"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public List<Overlay> getConfiguredOverlays() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom overlaysNode = config.getChild("overlays"); //$NON-NLS-1$
      if (overlaysNode != null && overlaysNode.getChildCount() > 0) {
        List<Overlay> overlays = new ArrayList<>(overlaysNode.getChildCount());
        for (Xpp3Dom overlayNode : overlaysNode.getChildren("overlay")) { //$NON-NLS-1$
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
    String artifactId = DomUtils.getChildValue(overlayNode, "artifactId"); //$NON-NLS-1$
    String groupId = DomUtils.getChildValue(overlayNode, "groupId"); //$NON-NLS-1$
    String[] exclusions = DomUtils.getChildrenAsStringArray(overlayNode.getChild("excludes"), "exclude"); //$NON-NLS-1$ //$NON-NLS-2$
    String[] inclusions = DomUtils.getChildrenAsStringArray(overlayNode.getChild("includes"), "include"); //$NON-NLS-1$ //$NON-NLS-2$
    String classifier = DomUtils.getChildValue(overlayNode, "classifier"); //$NON-NLS-1$
    boolean filtered = DomUtils.getBooleanChildValue(overlayNode, "filtered"); //$NON-NLS-1$
    boolean skip = DomUtils.getBooleanChildValue(overlayNode, "skip"); //$NON-NLS-1$
    String type = DomUtils.getChildValue(overlayNode, "type", "war"); //$NON-NLS-1$ //$NON-NLS-2$
    String targetPath = DomUtils.getChildValue(overlayNode, "targetPath", "/"); //$NON-NLS-1$ //$NON-NLS-2$

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
      expression = DomUtils.getChildValue(config, "outputFileNameMapping"); //$NON-NLS-1$
    }
    return new PatternBasedFileNameMapping(expression);
  }
  
  @Override
  protected String getFilteringAttribute() {
    return "filteringDeploymentDescriptors"; //$NON-NLS-1$
  }

  public boolean isFilteringDeploymentDescriptorsEnabled() {
    Xpp3Dom config = getConfiguration();
    boolean filteringDeploymentDescriptors = false;
    String filter = null;
    if (config != null) {
      filter = DomUtils.getChildValue(config, getFilteringAttribute());
    }
    if (filter == null && !isVersion3OrGreater) {
      filter = mavenProject.getProperties().getProperty("maven.war." + getFilteringAttribute()); //$NON-NLS-1$
    }
    if (filter != null) {
      filteringDeploymentDescriptors = Boolean.parseBoolean(filter);
    }
    return filteringDeploymentDescriptors;
  }

  public String getWarName() {
    Xpp3Dom config = getConfiguration();
    String warName = null;
    if (config != null) {
      warName = DomUtils.getChildValue(config, "warName"); //$NON-NLS-1$
    }
    if (StringUtils.nullOrEmpty(warName)) {
      warName = mavenProject.getBuild().getFinalName();
    }
    return warName;
  }

  @Override
  public SourceLocation getSourceLocation() {
    Plugin plugin = getPlugin();
    if (plugin == null) {
      return null;
    }
    return SourceLocationHelper.findLocation(plugin, "configuration"); //$NON-NLS-1$
  }

  @Override
  public String getSourceIncludeParameterName() {
    return "warSourceIncludes"; //$NON-NLS-1$
  }

  
  public boolean isFailOnMissingWebXml() {
    Xpp3Dom config = getConfiguration();
    boolean failOnMissingWebXml = defaultFailOnMissingWebXml;
    String fail = null;
    if (config != null) {
      fail = DomUtils.getChildValue(config, FAIL_ON_MISSING_WEB_XML); //$NON-NLS-1$
    }
    if (fail == null) {
      fail = mavenProject.getProperties().getProperty(FAIL_ON_MISSING_WEB_XML); //$NON-NLS-1$
    }
    if (fail != null) {
      failOnMissingWebXml = Boolean.parseBoolean(fail);
    }
    return failOnMissingWebXml;
  }
}
