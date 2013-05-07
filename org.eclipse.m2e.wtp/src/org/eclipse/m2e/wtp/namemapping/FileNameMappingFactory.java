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

/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.output.FileNameMapping 
 * 
 * Provides access to {@link FileNameMapping} implementations.
 * <p/>
 * Two basic implementations are provided by default:
 * <ul>
 * <li>standard: the default implementation</li>
 * <li>full: an implementation that maps to a 'full' file name, i.e. containing the groupId</li>
 * </ul>
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: FileNameMappingFactory.java 992847 2010-09-05 18:16:55Z snicoll $
 */
public final class FileNameMappingFactory
{
    static final String STANDARD_FILE_NAME_MAPPING = "standard"; //$NON-NLS-1$

    static final String FULL_FILE_NAME_MAPPING = "full"; //$NON-NLS-1$

    static final String NO_VERSION_FILE_NAME_MAPPING = "no-version"; //$NON-NLS-1$

    static final String NO_VERSION_FOR_EJB_NAME_MAPPING = "no-version-for-ejb"; //$NON-NLS-1$


    private FileNameMappingFactory()
    {
    }

    public static FileNameMapping getDefaultFileNameMapping()
    {
        return new StandardFileNameMapping();
    }

    /**
     * Returns the file name mapping implementation based on a logical name
     * of a fully qualified name of the class.
     *
     * @param nameOrClass a name of the fqn of the implementation
     * @return the file name mapping implementation
     * @throws IllegalStateException if the implementation is not found
     */
    public static FileNameMapping getFileNameMapping( final String nameOrClass )
        throws IllegalStateException
    {
        if ( STANDARD_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return getDefaultFileNameMapping();
        }
        if ( FULL_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new FullFileNameMapping();
        }
        if ( NO_VERSION_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new NoVersionFileNameMapping();
        }
        if ( NO_VERSION_FOR_EJB_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new EjbNoVersionFileNameMapping();
        }
        try
        {
            final Class<? extends FileNameMapping> c = Class.forName( nameOrClass ).asSubclass(FileNameMapping.class);
            return c.newInstance();
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException(
                "File name mapping implementation[" + nameOrClass + "] was not found " + e.getMessage() ); //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch ( InstantiationException e )
        {
            throw new IllegalStateException( "Could not instantiate file name mapping implementation[" + nameOrClass + //$NON-NLS-1$
                                                 "] make sure it has a default public constructor" ); //$NON-NLS-1$
        }
        catch ( IllegalAccessException e )
        {
            throw new IllegalStateException( "Could not access file name mapping implementation[" + nameOrClass + //$NON-NLS-1$
                                                 "] make sure it has a default public constructor" ); //$NON-NLS-1$
        }
        catch ( ClassCastException e )
        {
            throw new IllegalStateException(
                "Specified class[" + nameOrClass + "] does not implement[" + FileNameMapping.class.getName() + "]" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
