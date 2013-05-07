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
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.JarModule
 * 
 * The {@link EarModule} implementation for a non J2EE module such as third party libraries. <p/> Such module is not
 * incorporated in the generated <tt>application.xml<tt>
 * but some application servers support it. To include it in the generated
 * deployment descriptor anyway, set the <tt>includeInApplicationXml</tt> boolean flag. <p/> This class deprecates
 * {@link org.apache.maven.plugin.ear.JavaModule}.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class JarModule extends AbstractEarModule {
  
  private boolean includeInApplicationXml = false;


  public JarModule(Artifact a) {
    super(a);
  }

  public JarModule() {
    super();
  }

  public String getType() {
    return "jar"; //$NON-NLS-1$
  }

  void setLibBundleDir(String defaultLibBundleDir) {
    if(defaultLibBundleDir != null && bundleDir == null) {
      this.bundleDir = defaultLibBundleDir;
    }
  }

  public boolean isIncludeInApplicationXml() {
    return includeInApplicationXml;
  }

  public void setIncludeInApplicationXml(boolean includeInApplicationXml) {
    this.includeInApplicationXml = includeInApplicationXml;
  }

  protected void setCustomValues(Xpp3Dom module) {
    Xpp3Dom contextRootDom = new Xpp3Dom("includeInApplicationXml"); //$NON-NLS-1$
    contextRootDom.setValue(Boolean.toString(includeInApplicationXml));
    module.addChild(contextRootDom); 
  }
  
}
