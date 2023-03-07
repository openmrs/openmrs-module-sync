/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple utility bean the represents a field of a SyncItem during ingest
 */
public class SyncItemFields {

    private final List<String> fields = new ArrayList();
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, String> types = new HashMap<>();

    public SyncItemFields(NodeList nodes) {
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                String fieldName = n.getNodeName();
                fields.add(fieldName);
                values.put(fieldName, n.getTextContent());
                if (n.getAttributes() != null) {
                    Node typeNode = n.getAttributes().getNamedItem("type");
                    if (typeNode != null) {
                        types.put(fieldName, typeNode.getNodeValue());
                    }
                }
            }
        }
    }

    public String toString() {
        return fields.stream().map(f -> f + " = " + values.get(f) + " (type = " + types.get(f) + ")").collect(Collectors.joining(", "));
    }

    public String getValue(String fieldName) {
        return values.get(fieldName);
    }

    public String getType(String fieldName) {
        return types.get(fieldName);
    }

    public List<String> getFields() {
        return fields;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public Map<String, String> getTypes() {
        return types;
    }
}
