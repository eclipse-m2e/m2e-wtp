/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * WTPResourcesLabelProvider
 *
 * @author Eugene Kuleshov
 */
public class WTPResourcesLabelProvider extends WorkbenchLabelProvider implements ICommonLabelProvider {

  public void init(ICommonContentExtensionSite config) {
  }

  public void restoreState(IMemento memento) {
  }

  public void saveState(IMemento memento) {
  }

  public String getDescription(Object element) {
    if(element instanceof WTPResourcesNode) {
      return ((WTPResourcesNode)element).getLabel();
    }
    return null;
  }
}
