/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.internal.modulecore.AddClasspathFoldersParticipant;
import org.eclipse.jst.common.internal.modulecore.AddClasspathLibReferencesParticipant;
import org.eclipse.jst.common.internal.modulecore.AddMappedOutputFoldersParticipant;
import org.eclipse.jst.common.internal.modulecore.IgnoreJavaInSourceFolderParticipant;
import org.eclipse.jst.common.internal.modulecore.SingleRootExportParticipant;
import org.eclipse.jst.j2ee.internal.common.exportmodel.JEEHeirarchyExportParticipant;
import org.eclipse.jst.j2ee.internal.common.exportmodel.JavaEESingleRootCallback;
import org.eclipse.m2e.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.flat.FlatVirtualComponent;
import org.eclipse.wst.common.componentcore.internal.flat.FlatVirtualComponent.FlatComponentTaskModel;
import org.eclipse.wst.common.componentcore.internal.flat.IChildModuleReference;
import org.eclipse.wst.common.componentcore.internal.flat.IFlattenParticipant;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

/**
 * Overlay Virtual Component
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class OverlayVirtualComponent extends VirtualComponent implements
		IOverlayVirtualComponent {

	protected IProject project;

	protected Set<String> exclusionPatterns;
	
	protected Set<String> inclusionPatterns;
	
	protected Set<IVirtualReference> references;
	
	private CompositeVirtualFolder cachedRoot;
	
	private long lastCacheUpdate;
	
	private static int MAX_CACHE = 1000;
	
	public OverlayVirtualComponent(IProject project) {
		super(project, ROOT);
		this.project = project;
	}

	public IVirtualFolder getRootFolder() {
		return getRoot();
	}

	private CompositeVirtualFolder getRoot() {
		if (cachedRoot != null && (System.currentTimeMillis() - lastCacheUpdate) < MAX_CACHE){
			return cachedRoot;
		}
		
		//System.err.println("returning new root "); 
		if (project != null) {
			IVirtualComponent component = ComponentCore.createComponent(project);
			if (component != null) {
				//FlatVirtualComponent will build the project structure from the definition in .component
				FlatVirtualComponent flatVirtualComponent = new FlatVirtualComponent(component, getOptions());
				IResourceFilter filter = new DynamicResourceFilter(getInclusions(), getExclusions()); 
				cachedRoot = new CompositeVirtualFolder(flatVirtualComponent, ROOT, filter);
			}
		}
		lastCacheUpdate = System.currentTimeMillis();
		return cachedRoot;
	}
	
	private FlatComponentTaskModel getOptions() {
		FlatComponentTaskModel options = new FlatComponentTaskModel();
		//Participants produce IFlatResources[]
		//TODO Maybe deal with the inclusion/exclusion stuff on the participant level (using an Adapter or a Callback pattern)
		IFlattenParticipant[] participants = new IFlattenParticipant[] { 
	    	       new SingleRootExportParticipant(new JavaEESingleRootCallback()), 
	    	       new JEEHeirarchyExportParticipant(), 
	    	       new AddClasspathLibReferencesParticipant(), 
	    	       new AddClasspathFoldersParticipant(), 
	    	       new AddMappedOutputFoldersParticipant(),
	    	       new IgnoreJavaInSourceFolderParticipant() 
	    	       };
		options.put(FlatVirtualComponent.PARTICIPANT_LIST, Arrays.asList(participants));
		return options;
	}

	public void setInclusions(Set<String> inclusionPatterns) {
		this.inclusionPatterns = inclusionPatterns;
	}

	public void setExclusions(Set<String> exclusionPatterns) {
		this.exclusionPatterns = exclusionPatterns;
	}

	public Set<String> getExclusions() {
		return exclusionPatterns;
	}

	public Set<String> getInclusions() {
		return inclusionPatterns;
	}
	@Override
	public IVirtualReference[] getReferences(Map<String, Object> paramMap){;
		CompositeVirtualFolder  root = getRoot(); 
		if (root != null) {
			try {
				IVirtualReference[] references = root.getReferences(); 
				return references;
			} catch (Exception e) {
				//TODO handle exception
				e.printStackTrace();
			}
		}
		return new IVirtualReference[0];
	}

	private Set<IVirtualReference> getReferences(IChildModuleReference[] childModules) {
		Set<IVirtualReference> references = new LinkedHashSet<IVirtualReference>(childModules.length);
		for (IChildModuleReference child : childModules){
			references.add(child.getReference());
		}
		return references;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((exclusionPatterns == null) ? 0 : exclusionPatterns
						.hashCode());
		result = prime
				* result
				+ ((inclusionPatterns == null) ? 0 : inclusionPatterns
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OverlayVirtualComponent other = (OverlayVirtualComponent) obj;
		if (!super.equals(obj)) {
			return false;
		}
		if (exclusionPatterns == null) {
			if (other.exclusionPatterns != null)
				return false;
		} else if (!exclusionPatterns.equals(other.exclusionPatterns))
			return false;
		if (inclusionPatterns == null) {
			if (other.inclusionPatterns != null)
				return false;
		} else if (!inclusionPatterns.equals(other.inclusionPatterns))
			return false;
		return true;
	}
}
