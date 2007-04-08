//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2007 Apr 07: Refactor to use setters and an afterPropertiesSet method for
//              configuration and initialization instead of configuration
//              passed to the constructor. - dj@opennms.org
// 2007 Apr 05: Make getPrefabGraphsForResource consider string properties
//              and external values from the OnmsAttribute. - dj@opennms.org
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.dao.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Category;
import org.opennms.core.utils.BundleLists;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.dao.GraphDao;
import org.opennms.netmgt.model.AdhocGraphType;
import org.opennms.netmgt.model.OnmsAttribute;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.netmgt.model.PrefabGraphType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class PropertiesGraphDao implements GraphDao, InitializingBean {
    public static final String DEFAULT_GRAPH_LIST_KEY = "reports";
    
    private Map<String, File> m_prefabConfigs;
    private Map<String, File> m_adhocConfigs;
    
    private Map<String, FileReloadContainer<PrefabGraphType>> m_types =
        new HashMap<String, FileReloadContainer<PrefabGraphType>>();

    private HashMap<String, FileReloadContainer<AdhocGraphType>> m_adhocTypes =
        new HashMap<String, FileReloadContainer<AdhocGraphType>>();
    
    private PrefabGraphTypeCallback m_prefabCallback =
        new PrefabGraphTypeCallback();
    private AdhocGraphTypeCallback m_adhocCallback =
        new AdhocGraphTypeCallback();

    public PropertiesGraphDao() {
    }
    
    private void initPrefab() throws IOException {
        for (Map.Entry<String, File> configEntry : m_prefabConfigs.entrySet()) {
            loadProperties(configEntry.getKey(), configEntry.getValue());
        }
    }
    
    private void initAdhoc() throws IOException {
        for (Map.Entry<String, File> configEntry : m_adhocConfigs.entrySet()) {
            loadAdhocProperties(configEntry.getKey(), configEntry.getValue());
        }
    }

    public PrefabGraphType findByName(String name) {
        return m_types.get(name).getObject();
    }
    
    public AdhocGraphType findAdhocByName(String name) {
        return m_adhocTypes.get(name).getObject();
    }

    public void loadProperties(String type, File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        PrefabGraphType t = createPrefabGraphType(type, in);
        in.close();
        
        m_types.put(t.getName(),
                    new FileReloadContainer<PrefabGraphType>(t, file, m_prefabCallback));
    }
    
    public void loadProperties(String type, InputStream in) throws IOException {
        PrefabGraphType t = createPrefabGraphType(type, in);
        m_types.put(t.getName(), new FileReloadContainer<PrefabGraphType>(t));
    }
    
    private PrefabGraphType createPrefabGraphType(String type, InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);

        PrefabGraphType t = new PrefabGraphType();
        t.setName(type);
        
        t.setCommandPrefix(getProperty(properties, "command.prefix"));
        t.setOutputMimeType(getProperty(properties, "output.mime"));

        t.setDefaultReport(properties.getProperty("default.report", "none"));

        t.setReportMap(getPrefabGraphDefinitions(properties));

        return t;
    }
    
    public void loadAdhocProperties(String type, File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        AdhocGraphType t = createAdhocGraphType(type, in);
        in.close();
        
        m_adhocTypes.put(t.getName(), new FileReloadContainer<AdhocGraphType>(t, file, m_adhocCallback));
    }
    
    public void loadAdhocProperties(String type, InputStream in) throws IOException {
        AdhocGraphType t = createAdhocGraphType(type, in);
        m_adhocTypes.put(t.getName(), new FileReloadContainer<AdhocGraphType>(t));
    }
    
    public AdhocGraphType createAdhocGraphType(String type, InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);
        
        AdhocGraphType t = new AdhocGraphType();
        t.setName(type);
        
        t.setRrdDirectory(new File(getProperty(properties, "command.input.dir")));
        t.setCommandPrefix(getProperty(properties, "command.prefix"));
        t.setOutputMimeType(getProperty(properties, "output.mime"));
        
        t.setTitleTemplate(getProperty(properties, "adhoc.command.title"));
        t.setDataSourceTemplate(getProperty(properties, "adhoc.command.ds"));
        t.setGraphLineTemplate(getProperty(properties, "adhoc.command.graphline"));
        
        return t;
    }

    private Map<String, PrefabGraph> getPrefabGraphDefinitions(Properties properties) {
        Assert.notNull(properties, "properties argument cannot be null");

        String listString = getProperty(properties, DEFAULT_GRAPH_LIST_KEY);

        String[] list = BundleLists.parseBundleList(listString);

        Map<String, PrefabGraph> map = new LinkedHashMap<String, PrefabGraph>();

        for (int i = 0; i < list.length; i++) {
            String key = list[i];
            PrefabGraph graph = makePrefabGraph(key, properties, i);

            map.put(key, graph);
        }

        return map;
    }
    
    private static PrefabGraph makePrefabGraph(String key, Properties props, int order) {
        Assert.notNull(key, "key argument cannot be null");
        Assert.notNull(props, "props argument cannot be null");

        String title = getReportProperty(props, key, "name", true);

        String command = getReportProperty(props, key, "command", true);

        String columnString = getReportProperty(props, key, "columns", true);
        String[] columns = BundleLists.parseBundleList(columnString);

        String externalValuesString = getReportProperty(props, key,
                                                        "externalValues",
                                                        false);
        String[] externalValues;
        if (externalValuesString == null) {
            externalValues = new String[0];
        } else {
            externalValues = BundleLists.parseBundleList(externalValuesString);
        }

        String[] propertiesValues;
        String propertiesValuesString = getReportProperty(props, key,
                                                          "propertiesValues",
                                                          false);
        if (propertiesValuesString == null) {
            propertiesValues = new String[0];
        } else {
            propertiesValues = BundleLists.parseBundleList(propertiesValuesString);
        }

        // can be null
        String[] types;
        String typesString = getReportProperty(props, key,
                                               "type",
                                               false);
        if (typesString == null) {
            types = new String[0];
        } else {
            types = BundleLists.parseBundleList(typesString);
        }

        // can be null
        String description = getReportProperty(props, key, "description", false);


	// TODO: Right now a "width" and "height" property is required
	// in order to get zoom to work properly on non-standard sized
	// graphs. A more elegant solution would be to parse the
	// command string and look for --width and --height and set
	// the following two variables automagically, without having
	// to rely on a config file.

        // can be null
        String graphWidth = getReportProperty(props, key, "width", false);

        // can be null
        String graphHeight = getReportProperty(props, key, "height", false);
        
        return new PrefabGraph(key, title, columns,
                command, externalValues,
                propertiesValues, order, types,
                description, graphWidth, graphHeight);

    }

    private static String getProperty(Properties props, String name) {
        String property = props.getProperty(name);
        if (property == null) {
            throw new DataAccessResourceFailureException("Properties must "
                                                         + "contain \'" + name
                                                         + "\' property");
        }
    
        return property;
    }
    
    private static String getReportProperty(Properties props, String key,
            String suffix, boolean required) {
        String propertyName = "report." + key + "." + suffix;
    
        String property = props.getProperty(propertyName);
        if (property == null && required == true) {
            throw new DataAccessResourceFailureException("Properties for "
                                                         + "report '" + key
                                                         + "' must contain \'"
                                                         + propertyName
                                                         + "\' property");
        }
    
        return property;
    }
    
    private Category log() {
        return ThreadCategory.getInstance();
    }
    
    private class PrefabGraphTypeCallback implements FileReloadCallback<PrefabGraphType> {
        public PrefabGraphType reload(PrefabGraphType object, File file) {
            FileInputStream in;
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                log().error("Could not reload configuration file '"
                            + file.getAbsolutePath()
                            + "' due to FileNotFoundException: " + e,
                            e);
                return null;
            }
            
            PrefabGraphType t;
            try {
                t = createPrefabGraphType(object.getName(), in);
            } catch (IOException e) {
                log().error("Could not reload configuration file '"
                            + file.getAbsolutePath()
                            + "' due to IOException when reading from file: " + e,
                            e);
                return null;
            } catch (DataAccessException e) {
                log().error("Could not reload configuration file '"
                            + file.getAbsolutePath()
                            + "' due to DataAccessException when reading from "
                            + "file: " + e,
                            e);
                return null;
            }
            
            try {
                in.close();
            } catch (IOException e) {
                log().error("Could not reload configuration file '"
                            + file.getAbsolutePath()
                            + "' due to IOException when closing file: " + e,
                            e);
                return null;
            }
            
            return t;
        }
    }
    
    public List<PrefabGraph> getAllPrefabGraphs() {
        List<PrefabGraph> graphs = new ArrayList<PrefabGraph>();
        for (FileReloadContainer<PrefabGraphType> container : m_types.values()) {
            graphs.addAll(container.getObject().getReportMap().values());
        }
        return graphs;
    }
    
    public PrefabGraph getPrefabGraph(String name) {
        for (FileReloadContainer<PrefabGraphType> container : m_types.values()) {
            PrefabGraph graph = container.getObject().getQuery(name);
            if (graph != null) {
                return graph;
            }
        }
        throw new ObjectRetrievalFailureException(PrefabGraph.class, name, "Could not find prefabricated graph report with name '" + name + "'", null);
    }
    
    public PrefabGraph[] getPrefabGraphsForResource(OnmsResource resource) {
        Set<OnmsAttribute> attributes = resource.getAttributes();
        // Check if there are no attributes
        if (attributes.size() == 0) {
            log().debug("returning empty graph list for resource " + resource + " because its attribute list is empty");
            return new PrefabGraph[0];
        }

        Set<String> availableRrdAttributes = resource.getRrdGraphAttributes().keySet();
        Set<String> availableStringAttributes = resource.getStringPropertyAttributes().keySet();
        Set<String> availableExternalAttributes = resource.getExternalValueAttributes().keySet();

        // Check if there are no RRD attributes
        if (availableRrdAttributes.size() == 0) {
            log().debug("returning empty graph list for resource " + resource + " because it has no RRD attributes");
            return new PrefabGraph[0];
        }

        String resourceType = resource.getResourceType().getName();

        List<PrefabGraph> returnList = new ArrayList<PrefabGraph>();
        for (PrefabGraph query : getAllPrefabGraphs()) {
            if (resourceType != null && !query.hasMatchingType(resourceType)) {
                if (log().isDebugEnabled()) {
                    log().debug("skipping " + query.getName() + " because its types \"" + StringUtils.arrayToDelimitedString(query.getTypes(), ", ") + "\" does not match resourceType \"" + resourceType + "\"");
                }
                continue;
            }
            
            if (!verifyAttributesExist(query, "RRD", Arrays.asList(query.getColumns()), availableRrdAttributes)) {
                continue;
            }
            if (!verifyAttributesExist(query, "string property", Arrays.asList(query.getPropertiesValues()), availableStringAttributes)) {
                continue;
            }
            if (!verifyAttributesExist(query, "external value", Arrays.asList(query.getExternalValues()), availableExternalAttributes)) {
                continue;
            }
            
            if (log().isDebugEnabled()) {
                log().debug("adding " + query.getName() + " to query list");
            }
            
            returnList.add(query);
        }

        if (log().isDebugEnabled()) {
            ArrayList<String> nameList = new ArrayList<String>(returnList.size());
            for (PrefabGraph graph : returnList) {
                nameList.add(graph.getName());
            }
            log().debug("found " + nameList.size() + " prefabricated graphs for resource " + resource + ": " + StringUtils.collectionToDelimitedString(nameList, ", "));
        }
        
        return returnList.toArray(new PrefabGraph[returnList.size()]);
    }


    private boolean verifyAttributesExist(PrefabGraph query, String type, List<String> requiredList, Set<String> availableRrdAttributes) {
        if (availableRrdAttributes.containsAll(requiredList)) {
            return true;
        } else {
            if (log().isDebugEnabled()) {
                String name = query.getName();
                log().debug("not adding " + name + " to prefab graph list because the required list of " + type + " attributes (" + StringUtils.collectionToDelimitedString(requiredList, ", ") + ") is not in the list of " + type + " attributes on the resource (" + StringUtils.collectionToDelimitedString(availableRrdAttributes, ", ")+ ")");
            }
            return false;
        }
    }
    
    private class AdhocGraphTypeCallback implements FileReloadCallback<AdhocGraphType> {
        public AdhocGraphType reload(AdhocGraphType object, File file) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                return createAdhocGraphType(object.getName(), in);
            } catch (Exception e) {
                log().error("Could not reload configuration file '"
                            + file.getAbsolutePath()
                            + "' due to: " + e,
                            e);
                return null;
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

    }
    
    public void afterPropertiesSet() throws IOException {
        Assert.notNull(m_prefabConfigs, "property prefabConfigs must be set to a non-null value");
        Assert.notNull(m_adhocConfigs, "property adhocConfigs must be set to a non-null value");
        
        initPrefab();
        initAdhoc();
    }

    public Map<String, File> getAdhocConfigs() {
        return m_adhocConfigs;
    }

    public void setAdhocConfigs(Map<String, File> adhocConfigs) {
        m_adhocConfigs = adhocConfigs;
    }

    public Map<String, File> getPrefabConfigs() {
        return m_prefabConfigs;
    }

    public void setPrefabConfigs(Map<String, File> prefabConfigs) {
        m_prefabConfigs = prefabConfigs;
    }

}
