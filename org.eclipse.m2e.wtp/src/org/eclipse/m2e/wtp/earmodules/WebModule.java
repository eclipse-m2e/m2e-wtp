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
import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.WebModule
 * 
 * The {@link EarModule} implementation for a Web application module.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class WebModule extends AbstractEarModule {
  
  private String contextRoot;

  public WebModule() {
    super();
  }

  public WebModule(Artifact a) {
    super(a);
  }

  /**
   * Returns the context root to use for the web module. <p/> Note that this might return <tt>null</tt> till the
   * artifact has been resolved.
   * 
   * @return the context root
   */
  public String getContextRoot() {
    // Context root has not been customized - using default
    if(contextRoot == null) {
      contextRoot = getDefaultContextRoot(getArtifact());
    }
    return contextRoot;
  }

  public String getType() {
    return "war";
  }

  /**
   * Generates a default context root for the given artifact, based on the <tt>artifactId</tt>.
   * 
   * @param a the artifact
   * @return a context root for the artifact
   */
  private static String getDefaultContextRoot(Artifact a) {
    if(a == null) {
      throw new NullPointerException("Artifact could not be null.");
    }
    return "/" + a.getArtifactId();
  }

  public void setContextRoot(String contextRoot) {
    this.contextRoot = contextRoot;
  }
  
  protected void setCustomValues(Xpp3Dom module) {
    Xpp3Dom contextRootDom = new Xpp3Dom("contextRoot");
    contextRootDom.setValue(getContextRoot());
    module.addChild(contextRootDom);
  }

  protected String getModuleType() {
    return "webModule";
  }
}
