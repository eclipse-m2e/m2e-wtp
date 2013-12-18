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
package org.eclipse.m2e.wtp.namemapping;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.eclipse.m2e.wtp.ArtifactHelper;



/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.output.AbstractFileNameMapping 
 * 
 * A base class used to generate the standard name of an
 * artifact instead of relying on the (potentially) wrong
 * file name provided by {@link org.apache.maven.artifact.Artifact#getFile()}.
 *
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public abstract class AbstractFileNameMapping
    implements FileNameMapping
{


    private boolean useBaseVersion;

	/**
     * Generates a standard file name for the specified {@link Artifact}.
     * <p/>
     * Returns something like <tt>artifactId-version[-classifier].extension</tt>
     * if <tt>addVersion</tt> is true. Otherwise it generates something
     * like <tt>artifactId[-classifier].extension</tt>
     *
     * @param a          the artifact to generate a filename from
     * @param addVersion whether the version should be added
     * @return the filename, with a standard format
     */
    protected String generateFileName( final Artifact a, boolean addVersion )
    {
    	ArtifactHandler artifactHandler = a.getArtifactHandler();
    	ArtifactHelper.fixArtifactHandler(artifactHandler);
        String extension = artifactHandler.getExtension();
        final StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( a.getArtifactId() );
        if ( addVersion )
        {
            buffer.append( '-' );
            if (useBaseVersion) {
            	buffer.append( a.getBaseVersion() );
            } else {
            	buffer.append( a.getVersion() );
            }
            
        }
        if ( a.hasClassifier() )
        {
            buffer.append( '-' ).append( a.getClassifier() );
        }
        if ( extension != null && extension.length() > 0 )
        {
            buffer.append( '.' ).append( extension );
        }

        return buffer.toString();
    }
    
    /**
     * Introduced in maven-ear-plugin 2.9 : when using a fileNameMapping with versions, either use the 
     * <code>baseVersion</code> or the <code>version</code>. When the artifact is a SNAPSHOT, version 
     * will always return a value with a -SNAPSHOT postfix instead of the possible timestamped value.
     * 
     * @since 1.1.0
     */
    public void setUseBaseVersion(boolean useBaseVersion) {
		this.useBaseVersion = useBaseVersion;
    }

    /**
     * @since 1.1.0
     */
    public boolean isUseBaseVersion() {
    	return useBaseVersion;
    }
    
}
