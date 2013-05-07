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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.wtp.internal.Messages;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.EarPluginException
 * 
 * The base exception of the EAR plugin.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author Fred Bricon
 */
public class EarPluginException extends CoreException {
  private static final long serialVersionUID = -819727447130647982L;

  private static final String DEFAULT_MESSAGE = Messages.EarPluginException_Default_Message;

  public EarPluginException() {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, DEFAULT_MESSAGE));
  }

  public EarPluginException(String message) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, message));
  }

  public EarPluginException(Throwable cause) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, DEFAULT_MESSAGE, cause));
  }

  public EarPluginException(String message, Throwable cause) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, message, cause));
  }
}
