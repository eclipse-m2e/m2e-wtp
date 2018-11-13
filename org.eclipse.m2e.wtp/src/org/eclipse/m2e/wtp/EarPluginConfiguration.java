/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.wtp.earmodules.ArtifactTypeMappingService;
import org.eclipse.m2e.wtp.earmodules.EarModule;
import org.eclipse.m2e.wtp.earmodules.EarModuleFactory;
import org.eclipse.m2e.wtp.earmodules.EarPluginException;
import org.eclipse.m2e.wtp.earmodules.SecurityRoleKey;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.namemapping.AbstractFileNameMapping;
import org.eclipse.m2e.wtp.namemapping.FileNameMapping;
import org.eclipse.m2e.wtp.namemapping.FileNameMappingFactory;
import org.eclipse.m2e.wtp.namemapping.PatternBasedFileNameMapping;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * maven-ear-plugin configuration model.
 * 
 * @see http://maven.apache.org/plugins/maven-ear-plugin/
 * @see http://maven.apache.org/plugins/maven-ear-plugin/modules.html
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class EarPluginConfiguration extends AbstractFilteringSupportMavenPlugin implements IMavenPackageFilter {

  private static final Logger LOG = LoggerFactory.getLogger(EarPluginConfiguration.class);

  //Careful : This has a different meaning from the default library directory (/lib)
  private static final String EAR_DEFAULT_BUNDLE_DIR = "/";  //$NON-NLS-1$

  private static final String EAR_DEFAULT_CONTENT_DIR = "src/main/application"; // J2EEConstants.EAR_DEFAULT_LIB_DIR //$NON-NLS-1$

  // Default EAR version produced by the maven-ear-plugin
  private static final IProjectFacetVersion DEFAULT_EAR_FACET = IJ2EEFacetConstants.ENTERPRISE_APPLICATION_13;

  private final MavenProject mavenProject;

  /**
   * directory where jars will be deployed.
   */
  private String libDirectory;

  // private String contentDirectory;

  // XXX see if Lazy loading / caching the different factories and services is relevant.
  private ArtifactTypeMappingService typeMappingService;

  private Set<EarModule>  earModules;
  
  private boolean supportsUseBaseVersion = false;
  
  public EarPluginConfiguration(MavenProject mavenProject) {
    if(JEEPackaging.EAR != JEEPackaging.getValue(mavenProject.getPackaging())) {
      throw new IllegalArgumentException(Messages.EarPluginConfiguration_Project_Must_Have_ear_Packaging);
    }

    this.mavenProject = mavenProject;
    Plugin plugin = getPlugin();
    try {
    	VersionRange ear_2_9 = VersionRange.createFromVersionSpec("[2.9,)"); //$NON-NLS-1$
    	if(ear_2_9.containsVersion(new DefaultArtifactVersion(plugin.getVersion()))) {
    		supportsUseBaseVersion = true;
    	}
    } catch(Exception ex) {
        //Can't happen
    }
    
    setConfiguration((Xpp3Dom)plugin.getConfiguration());
  }

  public Plugin getPlugin() {
    return mavenProject.getPlugin("org.apache.maven.plugins:maven-ear-plugin"); //$NON-NLS-1$
  }


  /**
   * Gets an IProjectFacetVersion version from maven-ear-plugin configuration.
   * 
   * @return the facet version of the project, Maven defaults to (Java EE) 1.3
   */
  public IProjectFacetVersion getEarFacetVersion() {
    Xpp3Dom config = getConfiguration();
    if(config == null) {
      return DEFAULT_EAR_FACET;
    }

    Xpp3Dom domVersion = config.getChild("version"); //$NON-NLS-1$
    if(domVersion != null) {
      String sVersion = domVersion.getValue();
      try {
        double version = Double.parseDouble(sVersion); // transforms version 5 to 5.0
        sVersion = Double.toString(version);
        try {
          return IJ2EEFacetConstants.ENTERPRISE_APPLICATION_FACET.getVersion(sVersion);
        } catch (Exception e) {
          //If Ear Version > 5.0 and WTP < 3.2, downgrade to Ear facet 5.0
          LOG.warn(e.getMessage());
          if (version > 5.0){
            return IJ2EEFacetConstants.ENTERPRISE_APPLICATION_FACET.getVersion("5.0"); //$NON-NLS-1$
          }
        }
        } catch(NumberFormatException nfe) {
        LOG.error(NLS.bind(Messages.EarPluginConfiguration_Error_Reading_EAR_Version,sVersion), nfe);
        return DEFAULT_EAR_FACET;
      }
    }
    return DEFAULT_EAR_FACET;
  }

  /**
   * Gets the ear content directory of the project from pom.xml configuration.
   * 
   *  @return the contents of the earSourceDirectory element. If earSourceDirectory is not specified
   *          in pom.xml, the default value src/main/application is returned. 
   */
  public String getEarContentDirectory(IProject project) {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom contentDirDom = config.getChild("earSourceDirectory"); //$NON-NLS-1$
      if(contentDirDom != null && contentDirDom.getValue() != null) {
        String contentDir = contentDirDom.getValue().trim();
        
        //MNGECLIPSE-1600 fixed absolute earSourceDirectory
        if(project != null) {
          IPath projectLocationPath = project.getLocation();
          if(projectLocationPath != null) {
            String projectLocation = projectLocationPath.toOSString();
            if(contentDir.startsWith(projectLocation)) {
              return contentDir.substring(projectLocation.length());
            }
          }
        }
        contentDir = (contentDir.length() == 0) ? EAR_DEFAULT_CONTENT_DIR : contentDir;
        return contentDir;
      }
    }

    return EAR_DEFAULT_CONTENT_DIR;
  }

  /**
   * Return the default bundle directory, where jars will be deployed.
   */
  public String getDefaultBundleDirectory() {
    if(libDirectory == null) {
      Xpp3Dom config = getConfiguration();
      if(config != null) {
        Xpp3Dom libDom = config.getChild("defaultLibBundleDir"); //$NON-NLS-1$
        if(libDom != null && libDom.getValue() != null) {
          String libDir = libDom.getValue().trim();
          libDirectory = (libDir.length() == 0) ? EAR_DEFAULT_BUNDLE_DIR : libDir;
        }
      }
      libDirectory = (libDirectory  == null)?EAR_DEFAULT_BUNDLE_DIR:libDirectory;
    }
    return libDirectory;
  }

  
  /**
   * Reads maven-ear-plugin configuration to build a set of EarModule.
   * 
   * @see org.apache.maven.plugin.ear.AbstractEarMojo
   * @return an unmodifiable set of EarModule
   */
  public Set<EarModule> getEarModules() throws EarPluginException {
    if (earModules == null) {
      //Lazy load modules
      earModules = collectEarModules();
      
      //Remove excluded artifacts 
      Iterator<EarModule> modulesIterator = earModules.iterator();
      while (modulesIterator.hasNext())
      {
        EarModule module = modulesIterator.next();
        if (module.isExcluded())
        {
          modulesIterator.remove();  
        }
      }

      earModules = Collections.unmodifiableSet(earModules);
    }
    return earModules;
  }

  public Set<EarModule> getAllEarModules() throws EarPluginException {
     return Collections.unmodifiableSet(collectEarModules());
  }

  private Set<EarModule> collectEarModules() throws EarPluginException {
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    if(artifacts == null || artifacts.isEmpty()) {
      return Collections.<EarModule> emptySet();
    }

    Set<EarModule> earModules = new LinkedHashSet<>(artifacts.size());
    String defaultBundleDir = getDefaultBundleDirectory();
    IProjectFacetVersion javaEEVersion = getEarFacetVersion();
    EarModuleFactory earModuleFactory = EarModuleFactory.createEarModuleFactory(getArtifactTypeMappingService(),
        getFileNameMapping(), getMainArtifactId(), artifacts);

    //Resolve Ear modules from plugin config
    earModules.addAll(getEarModulesFromConfig(earModuleFactory, defaultBundleDir, javaEEVersion)); 

    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

    //next, add remaining modules from maven project dependencies
    for(Artifact artifact : artifacts) {

      // If the artifact's type is POM, ignore and continue
      // since it's used for transitive deps only.
      if("pom".equals(artifact.getType())) { //$NON-NLS-1$
        continue;
      }

      boolean isIncludedInApplicationXml = isIncludeLibInApplicationXml();
      // Artifact is not yet registered and it has neither test, nor a
      // provided scope, nor is it optional
      if(!isArtifactRegistered(artifact, earModules) && filter.include(artifact) && !artifact.isOptional()) {
        EarModule module = earModuleFactory.newEarModule(artifact, defaultBundleDir, javaEEVersion, isIncludedInApplicationXml);
        if(module != null) {
          earModules.add(module);
        }
      }
    }
    return earModules;
  }

  private String getMainArtifactId() {
    // TODO read xml config
    return "none"; //$NON-NLS-1$
  }

  private ArtifactTypeMappingService getArtifactTypeMappingService() throws EarPluginException {
    if(typeMappingService == null) {
      Xpp3Dom config = getConfiguration();
      Xpp3Dom artifactTypeMappingConfig = null;
      if (config != null) {
        artifactTypeMappingConfig = config.getChild("artifactTypeMappings"); //$NON-NLS-1$
      }
      typeMappingService = new ArtifactTypeMappingService(artifactTypeMappingConfig);
    }
    return typeMappingService;
  }

  private FileNameMapping getFileNameMapping() {

    Xpp3Dom config = getConfiguration();
    FileNameMapping mapping = null;
    boolean useBaseVersion = !supportsUseBaseVersion;
    if(config != null) {
        Xpp3Dom outputFileNameMappingDom = config.getChild("outputFileNameMapping"); //$NON-NLS-1$
        if (outputFileNameMappingDom != null) {
            String pattern = outputFileNameMappingDom.getValue().trim();
            mapping = new PatternBasedFileNameMapping(pattern);
        } else {
            Xpp3Dom fileNameMappingDom = config.getChild("fileNameMapping"); //$NON-NLS-1$
            if(fileNameMappingDom != null) {
              String fileNameMappingName = fileNameMappingDom.getValue().trim();
              mapping = FileNameMappingFactory.getFileNameMapping(fileNameMappingName);
            }
            if (supportsUseBaseVersion) {// for ear-plugin >= 2.9
                useBaseVersion = DomUtils.getBooleanChildValue(config, "useBaseVersion", false); //$NON-NLS-1$
            }
        }
    }
    if (mapping == null) {
    	mapping = FileNameMappingFactory.getDefaultFileNameMapping(); 
    }
    if (mapping instanceof AbstractFileNameMapping) {
    	((AbstractFileNameMapping)mapping).setUseBaseVersion(useBaseVersion);
    }
    return mapping;
  }

  /**
   * Return a set of ear modules defined in maven-ear-plugin configuration.
   * 
   * @param earModuleFactory
   */
  private Set<EarModule> getEarModulesFromConfig(EarModuleFactory earModuleFactory, String defaultBundleDir, IProjectFacetVersion javaEEVersion) throws EarPluginException {
    Set<EarModule> earModules = new LinkedHashSet<>();
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return earModules;
    }
    Xpp3Dom modulesNode = configuration.getChild("modules"); //$NON-NLS-1$

    if(modulesNode == null) {
      return earModules;
    }

    Xpp3Dom[] domModules = modulesNode.getChildren();
    if(domModules == null || domModules.length == 0) {
      return earModules;
    }
    
    boolean isIncludedInApplicationXml = isIncludeLibInApplicationXml();
    for(Xpp3Dom domModule : domModules) {
      EarModule earModule = earModuleFactory.newEarModule(domModule, defaultBundleDir, javaEEVersion, isIncludedInApplicationXml);
      if(earModule != null) {
        earModules.add(earModule);
      }
    }
    
    return earModules;
  }

  private static boolean isArtifactRegistered(Artifact a, Set<EarModule> modules) {
    for(EarModule module : modules) {
      if(module.getArtifact().equals(a)) {
        return true;
      }
    }
    return false;
  }

  public boolean isGenerateApplicationXml()  {
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return true;
    }
    Xpp3Dom generateApplicationXmlNode = configuration.getChild("generateApplicationXml"); //$NON-NLS-1$
    return (generateApplicationXmlNode == null) || Boolean.parseBoolean(generateApplicationXmlNode.getValue());
  }
  
  public Set<SecurityRoleKey>  getSecurityRoleKeys() {
    Set<SecurityRoleKey> securityRoles = new HashSet<>();
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return securityRoles;
    }
    Xpp3Dom securityNode = configuration.getChild("security"); //$NON-NLS-1$

    if(securityNode == null) {
      return securityRoles;
    }

    Xpp3Dom[] secRoles = securityNode.getChildren("security-role"); //$NON-NLS-1$
    if(secRoles == null || secRoles.length == 0) {
      return securityRoles;
    }
    
    for(Xpp3Dom domSecRole : secRoles) {
      String id = domSecRole.getAttribute("id"); //$NON-NLS-1$
      String description = DomUtils.getChildValue(domSecRole, "description"); //$NON-NLS-1$
      String roleName = DomUtils.getChildValue(domSecRole, "role-name"); //$NON-NLS-1$
      if (roleName != null)
      {
        SecurityRoleKey srk = new SecurityRoleKey();
        srk.setId(id);
        srk.setRoleName(roleName);
        srk.setDescription(description);
        securityRoles.add(srk);
      }
    }
    
    return securityRoles;
  }

  @Override
  protected String getFilteringAttribute() {
    return "filtering"; //$NON-NLS-1$
  }

  public boolean isIncludeLibInApplicationXml() {
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return false;
    }
    
    boolean isIncluded = DomUtils.getBooleanChildValue(configuration, "includeLibInApplicationXml"); //$NON-NLS-1$
    return isIncluded;
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
    return DomUtils.getPatternsAsArray(getConfiguration(),"earSourceExcludes"); //$NON-NLS-1$
  }

  @Override
  public String[] getSourceIncludes() {
    return DomUtils.getPatternsAsArray(getConfiguration(),"earSourceIncludes"); //$NON-NLS-1$
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
    return "earSourceIncludes"; //$NON-NLS-1$
  }
  
  public String getFinalName() {
    String finalName = DomUtils.getChildValue(getConfiguration(), "finalName"); //$NON-NLS-1$
    if (StringUtils.isEmpty(finalName)) {
      finalName = mavenProject.getBuild().getFinalName(); 
    }
    return finalName;
  }
}
