/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.eclipse.m2e.wtp.earmodules;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;


/** 
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.AbstractEarModule 
 * 
 * A base implementation of an {@link EarModule}.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractEarModule implements EarModule {

  private String uri;

  private Artifact artifact;

  // Those are set by the configuration

  private String groupId;

  private String artifactId;

  private String classifier;

  protected String bundleDir;

  protected String bundleFileName;

  protected boolean excluded;

  //Unusable by WTP so far
  protected Boolean unpack = null;

  //Unusable by WTP so far
  protected String altDeploymentDescriptor;

  /**
   * Empty constructor to be used when the module is built based on the configuration.
   */
  public AbstractEarModule() {
  }

  /**
   * Creates an ear module from the artifact.
   * 
   * @param a the artifact
   */
  public AbstractEarModule(Artifact a) {
    setArtifact(a);
  }
  
  public Artifact getArtifact() {
    return artifact;
  }

  void setArtifact(Artifact a) {
    this.artifact = a;
    this.groupId = a.getGroupId();
    this.artifactId = a.getArtifactId();
    this.classifier = a.getClassifier();
  }

  public String getUri() {
    if(uri == null) {
      String bd = getBundleDir();
      if(StringUtils.isEmpty(bd) || "/".equals(bd)) {
        uri = getBundleFileName();
      } else {
        if(!bd.endsWith("/")) {
          bd = bd + "/";
        } 
        if (bd.startsWith("/")) {
          bd = bd.substring(1);
        }
        uri =  bd + getBundleFileName();
      }
    }
    return uri;
  }

  /**
   * Returns the artifact's groupId.
   * 
   * @return the group Id
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Returns the artifact's Id.
   * 
   * @return the artifact Id
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Returns the artifact's classifier.
   * 
   * @return the artifact classifier
   */
  public String getClassifier() {
    return classifier;
  }

  /**
   * Returns the bundle directory. If null, the module is bundled in the root of the EAR.
   * 
   * @return the custom bundle directory
   */
  public String getBundleDir() {
    if(bundleDir != null) {
      bundleDir = cleanBundleDir(bundleDir);
    }
    return bundleDir;
  }

  /**
   * Returns the bundle file name. If null, the artifact's file name is returned.
   * 
   * @return the bundle file name
   */
  public String getBundleFileName() {
    return bundleFileName;
  }

  /**
   * The alt-dd element specifies an optional URI to the post-assembly version of the deployment descriptor file for a
   * particular Java EE module. The URI must specify the full pathname of the deployment descriptor file relative to the
   * application's root directory.
   * 
   * @return the alternative deployment descriptor for this module
   * @since JavaEE 5
   */
  public String getAltDeploymentDescriptor() {
    return altDeploymentDescriptor;
  }

  /**
   * Specify whether this module should be excluded or not.
   * 
   * @return true if this module should be skipped, false otherwise
   */
  public boolean isExcluded() {
    return excluded;
  }

  public Boolean shouldUnpack() {
    //Unusable by WTP so far
    return unpack;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getType()).append(":").append(groupId).append(":").append(artifactId);
    if(classifier != null) {
      sb.append(":").append(classifier);
    }
    if(artifact != null) {
      sb.append(":").append(artifact.getVersion());
    }
    return sb.toString();
  }

  /**
   * Cleans the bundle directory so that it might be used properly.
   * 
   * @param bundleDir the bundle directory to clean
   * @return the cleaned bundle directory
   */
  static String cleanBundleDir(String bundleDir) {
    if(bundleDir == null) {
      return bundleDir;
    }

    // Using slashes
    bundleDir = bundleDir.replace('\\', '/');

    //WTP needs the bundle dir to start with a '/'
    if(!bundleDir.startsWith("/")) {
      bundleDir = "/" + bundleDir ;
    }

    return bundleDir;
  }

  void setBundleDir(String bundleDir) {
    //Ignore bundleDir if uri has already been set
    if (StringUtils.isBlank(uri)){
      this.bundleDir = bundleDir;
    } 
  }

  void setBundleFileName(String fileName) {
    //Ignore bundleFileName if uri has already been set
    if (StringUtils.isBlank(uri)){
      this.bundleFileName = fileName;
    } 
  }

  /**
   * Setting an URI overrides any bundleDir or bundleFileName set on a module.
   * @param uri 
   */
  void setUri(String uri) {
    this.uri = uri;
    resolveDeploymentInfo();
  }


  /**
   * Parses the module's uri to compute the bundleDir and bundleFileName.
   */
  private void resolveDeploymentInfo() {
    if (StringUtils.isNotBlank(uri)) {
      int lastSlash = uri.lastIndexOf('/');
      if (lastSlash == uri.length()-1){
        throw new IllegalArgumentException("module uri ("+uri+") : can not end with a / ");
      }
      if (lastSlash>0) {
        bundleDir = uri.substring(0,lastSlash);
        bundleFileName = uri.substring(lastSlash+1);
      } else {
        bundleFileName = uri.replace("/","");
      }
      bundleDir = cleanBundleDir(bundleDir);

      if (StringUtils.isBlank(bundleFileName)){
        throw new IllegalArgumentException("module uri must contain a file name ");
      }
    }
  }

  void setExcluded(boolean excluded) {
    this.excluded = excluded;
  }

  void setAltDeploymentDescriptor(String altDeploymentDescriptor) {
    //Unusable by WTP so far
    this.altDeploymentDescriptor = altDeploymentDescriptor;
  }
  
  void setShouldUnpack(Boolean unpack) {
    this.unpack = unpack;
  }
  
  public Xpp3Dom getAsDom() {
    Xpp3Dom moduleDom = new Xpp3Dom(getModuleType());
    Xpp3Dom groupIdDom = new Xpp3Dom("groupId");
    groupIdDom.setValue(getGroupId());  
    moduleDom.addChild(groupIdDom);
    Xpp3Dom artifactIdDom = new Xpp3Dom("artifactId");
    artifactIdDom.setValue(getArtifactId());  
    moduleDom.addChild(artifactIdDom);
    Xpp3Dom uriDom = new Xpp3Dom("uri");
    uriDom.setValue(getUri());  
    moduleDom.addChild(uriDom);
    Xpp3Dom altDdDom = new Xpp3Dom("altDeploymentDescriptor");
    altDdDom.setValue(getAltDeploymentDescriptor());  
    moduleDom.addChild(altDdDom);
    Xpp3Dom excludedDom = new Xpp3Dom("excluded");
    excludedDom.setValue(Boolean.toString(isExcluded()));  
    moduleDom.addChild(excludedDom);
    setCustomValues(moduleDom);
    return moduleDom;
  }

  /**
   * @return
   */
  protected String getModuleType() {
    return "jarModule";
  }
  
  protected void setCustomValues(Xpp3Dom module) {
  }
  
  
}
