/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectNature;
import org.junit.jupiter.api.Test;

public class DummyWarProjectTest extends AbstractMavenProjectTestCase {
	
	@Test
	public void testConfigureBasicWar() throws Exception {
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject("basicWar");
		p.create(null);
		p.open(null);
		IFile pom = p.getFile("pom.xml");
		pom.create(new ByteArrayInputStream("""
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.example</groupId>
	<artifactId>test</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<packaging>war</packaging>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.10.1</version>
				<configuration>
					<source>1.8</source>
					<target>1.8</target>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-war-plugin</artifactId>
				<version>3.3.2</version>
			</plugin>
		</plugins>
	</build>

	<dependencies>
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>javax.servlet-api</artifactId>
			<version>3.1.0</version>
			<scope>provided</scope>
		</dependency>
	</dependencies>
</project>
				""".getBytes()), false, null);
		// copied from EnableNatureAction
		ResolverConfiguration configuration = new ResolverConfiguration();
		configuration.setResolveWorkspaceProjects(true);
		IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
		configurationManager.enableMavenNature(p, configuration, monitor);
		configurationManager.updateProjectConfiguration(p, monitor);
		waitForJobsToComplete();

		assertTrue(Arrays.asList(p.getDescription().getNatureIds()).contains(FacetedProjectNature.NATURE_ID));
	}

}
