/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import java.text.Collator;

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
//    if(e1 instanceof WebResourcesNode) {
//      if(e2 instanceof IResource) {
//        return 1;
//      }
//    } else if(e2 instanceof WebResourcesNode) {
//      if(e2 instanceof IResource) {
//        return -1;
//      }
//    }
    return super.compare(viewer, e1, e2);
  }
  
}
