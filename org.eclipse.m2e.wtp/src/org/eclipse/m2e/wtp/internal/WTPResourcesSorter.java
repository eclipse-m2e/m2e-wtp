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

import java.text.Collator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * WTPResourcesSorter
 *
 * @author Eugene Kuleshov
 */
public class WTPResourcesSorter extends ViewerSorter {

  public WTPResourcesSorter() {
  }

  public WTPResourcesSorter(Collator collator) {
    super(collator);
  }

  @Override
  public int category(Object element) {
    if(element instanceof WTPResourcesNode) {
      return 0;
    }
    return 1;
  }
  
  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    if(e1 instanceof WTPResourcesNode && e2 instanceof IResource) {
        return -1;
    } else if(e2 instanceof WTPResourcesNode && e1 instanceof IResource) {
        return 1;
    }
	if (e1 instanceof IFolder && e2 instanceof IFile) {
		return -1;
	} else if (e2 instanceof IFolder && e1 instanceof IFile) {
		return 1;
	}
    
    return super.compare(viewer, e1, e2);
  }
  
}
