/*******************************************************************************
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class Servlet61WarProjectTest extends AbstractMavenProjectTestCase {
    @Disabled("Re-enable when the test target includes https://github.com/eclipse-jeetools/webtools.javaee/pull/46")
    @Test public void servlet61DescriptorSelects61() throws Exception { checkDescriptor("6.1"); }
    @Test public void servlet60DescriptorRetains60() throws Exception { checkDescriptor("6.0"); }

    private void checkDescriptor(String version) throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("servlet-descriptor-" + version.replace('.', '-'));
        assertFalse(project.exists(), "Test must not replace an existing project");
        project.create(monitor);
        try {
            project.open(monitor);
            String pom = """
                <project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId><artifactId>servlet-descriptor</artifactId><version>1.0</version><packaging>war</packaging>
                <properties><maven.compiler.release>21</maven.compiler.release></properties>
                <dependencies><dependency><groupId>jakarta.servlet</groupId><artifactId>jakarta.servlet-api</artifactId><version>6.1.0</version><scope>provided</scope></dependency></dependencies>
                <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.14.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-war-plugin</artifactId><version>3.4.0</version></plugin></plugins></build></project>
                """;
            project.getFile("pom.xml").create(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)), false, monitor);
            for (String name : new String[]{"src", "src/main", "src/main/java", "src/main/webapp", "src/main/webapp/WEB-INF", "src/test", "src/test/java"}) {
                IFolder folder = project.getFolder(name); folder.create(true, true, monitor);
            }
            String descriptor = "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"" + version + "\"/>";
            project.getFile("src/main/webapp/WEB-INF/web.xml").create(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)), false, monitor);
            var manager = MavenPlugin.getProjectConfigurationManager();
            manager.enableMavenNature(project, new ResolverConfiguration(), monitor);
            manager.updateProjectConfiguration(project, monitor);
            waitForJobsToComplete();
            assertFacet(project, version);
            // The API dependency is 6.1 in both cases: an explicit 6.0 descriptor
            // remains authoritative instead of being raised by classpath inference.
            manager.updateProjectConfiguration(project, monitor);
            waitForJobsToComplete();
            assertFacet(project, version);
            assertNoErrors(project);
        } finally {
            waitForJobsToComplete(); project.delete(true, true, monitor);
        }
    }

    private void assertFacet(IProject project, String expected) throws Exception {
        var faceted = ProjectFacetsManager.create(project); assertNotNull(faceted);
        var facet = faceted.getInstalledVersion(ProjectFacetsManager.getProjectFacet("jst.web"));
        assertNotNull(facet); assertEquals(expected, facet.getVersionString());
    }
}
