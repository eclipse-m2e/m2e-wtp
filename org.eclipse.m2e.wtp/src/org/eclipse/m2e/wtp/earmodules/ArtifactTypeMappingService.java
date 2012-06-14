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

package org.eclipse.m2e.wtp.earmodules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * This class was derived from maven-ear-plugin's org.apache.maven.plugin.ear.util.ArtifactTypeMappingService
 * 
 * Allows to map custom artifact type to standard type.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class ArtifactTypeMappingService {
  static final String ARTIFACT_TYPE_MAPPING_ELEMENT = "artifactTypeMapping";

  static final String TYPE_ATTRIBUTE = "type";

  static final String MAPPING_ATTRIBUTE = "mapping";

  public final static List<String> standardArtifactTypes = new ArrayList<String>(10);

  static {
    standardArtifactTypes.add("jar");
    standardArtifactTypes.add("ejb");
    standardArtifactTypes.add("ejb3");
    standardArtifactTypes.add("par");
    standardArtifactTypes.add("ejb-client");
    standardArtifactTypes.add("rar");
    standardArtifactTypes.add("war");
    standardArtifactTypes.add("sar");
    standardArtifactTypes.add("wsr");
    standardArtifactTypes.add("har");
    standardArtifactTypes.add("app-client");    
  }

  // A standard type to a list of customType
  private Map<String, List<String>> typeMappings;

  // The user-defined mapping for direct access
  private Map<String, String> customMappings;

  public ArtifactTypeMappingService(Xpp3Dom plexusConfiguration) throws EarPluginException {
    // Initializes the typeMappings with default values
    init();

    // No user defined configuration
    if(plexusConfiguration == null) {
      return;
    }

    // Inject users configuration
    final Xpp3Dom[] artifactTypeMappings = plexusConfiguration.getChildren(ARTIFACT_TYPE_MAPPING_ELEMENT);

    for(Xpp3Dom artifactTypeMapping : artifactTypeMappings) {
      final String customType = artifactTypeMapping.getAttribute(TYPE_ATTRIBUTE);
      final String mapping = artifactTypeMapping.getAttribute(MAPPING_ATTRIBUTE);

      if(customType == null) {
        throw new EarPluginException("Invalid artifact type mapping, type attribute should be set.");
      } else if(mapping == null) {
        throw new EarPluginException("Invalid artifact type mapping, mapping attribute should be set.");
      } else if(!isStandardArtifactType(mapping)) {
        throw new EarPluginException("Invalid artifact type mapping, mapping[" + mapping
            + "] must be a standard Ear artifact type[" + getStandardArtifactTypes() + "]");
      } else if(customMappings.containsKey(customType)) {
        throw new EarPluginException("Invalid artifact type mapping, type[" + customType + "] is already registered.");
      } else {
        // Add the custom mapping
        customMappings.put(customType, mapping);

        // Register the custom mapping to its standard type
        List<String> typeMapping = typeMappings.get(mapping);
        typeMapping.add(customType);
      }
    }
  }

  /**
   * Specify whether the <tt>customType</tt> could be mapped to the <tt>standardType</tt>.
   * 
   * @param standardType the standard type (ejb, jar, war, ...)
   * @param customType a user-defined type
   * @return true if the customType could be mapped to the standard type
   */
  public boolean isMappedToType(final String standardType, final String customType) {
    if(!isStandardArtifactType(standardType)) {
      throw new IllegalStateException("Artifact type[" + standardType + "] is not a standard Ear artifact type["
          + getStandardArtifactTypes() + "]");
    }
    final List<String> typeMappings = this.typeMappings.get(standardType);
    return typeMappings.contains(customType);

  }

  /**
   * Returns the standard type for the specified <tt>type</tt>. If the specified type is already a standard type, the
   * orignal type is returned.
   * 
   * @param type a type
   * @return the standard type (ejb, jar, war, ...) for this type
   */
  public String getStandardType(final String type) throws UnknownArtifactTypeException {
    if(type == null) {
      throw new IllegalStateException("custom type could not be null.");
    } else if(getStandardArtifactTypes().contains(type)) {
      return type;
    } else if(!customMappings.containsKey(type)) {
      throw new UnknownArtifactTypeException("Unknown artifact type[" + type + "]");
    } else {
      return customMappings.get(type);
    }
  }

  private void init() {
    this.typeMappings = new HashMap<String, List<String>>();
    this.customMappings = new HashMap<String, String>();

    // Initialize the mapping with the standard artifact types
    for(String type : getStandardArtifactTypes()) {
      List<String> typeMapping = new ArrayList<String>();
      typeMapping.add(type);
      this.typeMappings.put(type, typeMapping);
    }
  }

  /**
   * Returns a list of standard artifact types.
   * 
   * @return the standard artifact types
   */
  public static List<String> getStandardArtifactTypes() {
    return standardArtifactTypes;
  }

  /**
   * Specify whether the specified type is standard artifact type.
   * 
   * @param type the type to check
   * @return true if the specified type is a standard artifact type
   */
  public static boolean isStandardArtifactType(final String type) {
    return standardArtifactTypes.contains(type);
  }

}
