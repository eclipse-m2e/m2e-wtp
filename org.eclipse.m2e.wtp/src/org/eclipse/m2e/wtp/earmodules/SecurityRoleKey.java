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


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.SecurityRole
 * 
 * SecurityRoleKey
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author Fred Bricon
 */
public class SecurityRoleKey {
  private String id;
  
  private String roleName;
  
  private String description;
  
  /**
   * @return Returns the id.
   */
  public String getId() {
    return id;
  }

  /**
   * @param id The id to set.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return Returns the roleName.
   */
  public String getRoleName() {
    return roleName;
  }

  /**
   * @param roleName The roleName to set.
   */
  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  /**
   * @return Returns the description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description The description to set.
   */
  public void setDescription(String description) {
    this.description = description;
  }
  
}
