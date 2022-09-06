/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.DependencyContext;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.internal.Messages;

/**
 * Helper for {@link MavenSession} manipulations.
 * 
 * @provisional This class has been added as part of a work in progress. It is
 * not guaranteed to work or remain the same in future releases. For more
 * information contact
 * <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class MavenSessionHelper {

	private final IMavenProjectFacade facade;

	private MavenProject project;

	private Set<Artifact> artifacts;

	private Set<Artifact> dependencyArtifacts;

	public MavenSessionHelper(IMavenProjectFacade facade) {
		if (facade == null) {
			throw new IllegalArgumentException(Messages.Error_Maven_Project_Cant_Be_Null);
		}
		this.facade = facade;
	}

	public void ensureDependenciesAreResolved(String pluginId, String goal, IProgressMonitor monitor)
			throws CoreException {
		project = facade.getMavenProject(monitor);
		MavenExecutionPlan executionPlan = MavenPlugin.getMaven().calculateExecutionPlan(project,
				Collections.singletonList(goal), true, monitor);

		MojoExecution execution = getExecution(executionPlan, pluginId);
		IMavenExecutionContext context = facade.createExecutionContext();
		context.execute(project, (ctx, pm) -> {
			ensureDependenciesAreResolved(ctx, execution, monitor);
			return null;
		}, monitor);
	}

	private void ensureDependenciesAreResolved(IMavenExecutionContext ctx, MojoExecution execution,
			IProgressMonitor monitor) {
		MavenSession session = ctx.getSession();
		try {
			artifacts = project.getArtifacts();
			dependencyArtifacts = project.getDependencyArtifacts();
			MojoExecutor mojoExecutor = ctx.getComponentLookup().lookup(MojoExecutor.class);
			DependencyContext dependencyContext = mojoExecutor.newDependencyContext(session, List.of(execution));
			mojoExecutor.ensureDependenciesAreResolved(execution.getMojoDescriptor(), session, dependencyContext);
		} catch (Exception ex) {
			dispose();
		}
	}

	public void dispose() {
		if (project != null) {
			project.setArtifactFilter(null);
			project.setResolvedArtifacts(null);
			project.setArtifacts(artifacts);
			project.setDependencyArtifacts(dependencyArtifacts);
		}
	}

	public static MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId) throws CoreException {
		if (executionPlan == null)
			return null;
		for (MojoExecution execution : executionPlan.getMojoExecutions()) {
			if (artifactId.equals(execution.getArtifactId())) {
				return execution;
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public MavenProject getMavenProject() {
		return project;
	}
}
