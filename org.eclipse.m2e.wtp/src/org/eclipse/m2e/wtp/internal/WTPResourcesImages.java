/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Eugene Kuleshov
 */
public class WTPResourcesImages {

  private static final Logger LOG = LoggerFactory.getLogger(WTPResourcesImages.class);

  // descriptors
  public static final ImageDescriptor WEB_RESOURCES = create("web-resources.gif"); //$NON-NLS-1$
  
  public static final ImageDescriptor APP_RESOURCES = create("ear-resources.gif"); //$NON-NLS-1$

  private static ImageDescriptor create(String key) {
    try {
      ImageRegistry imageRegistry = getImageRegistry();
      if(imageRegistry != null) {
        ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(key);
        if(imageDescriptor==null) {
          imageDescriptor = createDescriptor(key);
          imageRegistry.put(key, imageDescriptor);
        }
        return imageDescriptor;
      }
    } catch(Exception ex) {
      LOG.error(NLS.bind(Messages.WTPResourcesImages_Error_Creating_ImageDescriptor,key), ex);
    }
    return null;
  }

  private static ImageRegistry getImageRegistry() {
    MavenWtpPlugin plugin = MavenWtpPlugin.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(MavenWtpPlugin.ID, "icons/" + image); //$NON-NLS-1$
  }

}
