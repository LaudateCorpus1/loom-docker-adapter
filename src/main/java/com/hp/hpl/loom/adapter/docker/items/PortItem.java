/*******************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development LP Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.hp.hpl.loom.adapter.docker.items;

import com.hp.hpl.loom.adapter.annotations.ConnectedTo;
import com.hp.hpl.loom.adapter.annotations.ItemTypeInfo;
import com.hp.hpl.loom.adapter.annotations.LoomAttribute;
import com.hp.hpl.loom.adapter.annotations.Sort;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.ItemType;

@ItemTypeInfo(value = Types.PORT_TYPE_ID, layers = {Types.DOCKER_LAYER}, sorting = {
        // Default field sequence, if the user does not specify another visualisation
        // method:
        @Sort(operation = DefaultOperations.SORT_BY,
                fieldOrder = {PortItemAttributes.LABEL_PORT_CONTAINER_ID,
                        PortItemAttributes.LABEL_PORT_CONTAINER_PORT_NUMBER, PortItemAttributes.LABEL_PORT_PROTOCOL,
                        PortItemAttributes.LABEL_PORT_HOST_UID, PortItemAttributes.LABEL_PORT_HOST_PORT_NUMBER,
                        PortItemAttributes.LABEL_PORT_INTERFACE_ADDRESS, PortItem.LABEL_RELATIONSHIP_CONTAINER,
                        PortItem.LABEL_RELATIONSHIP_HOST}),
        // Default sorting method, if the user does not specify another visualisation
        // method:
        @Sort(operation = DefaultOperations.GROUP_BY,
                fieldOrder = {PortItem.LABEL_RELATIONSHIP_CONTAINER, PortItem.LABEL_RELATIONSHIP_HOST,
                        PortItemAttributes.LABEL_PORT_CONTAINER_PORT_NUMBER,
                        PortItemAttributes.LABEL_PORT_HOST_PORT_NUMBER, PortItemAttributes.LABEL_PORT_PROTOCOL})})

// Relationships definitions:
// A container exposes a port. This port may be used by another container of by the host.
@ConnectedTo(toClass = ContainerItem.class, type = Relationships.EXPOSES_TYPE,
        typeName = Relationships.EXPOSES_TYPE_NAME,
        relationshipDetails = @LoomAttribute(key = PortItem.LABEL_RELATIONSHIP_CONTAINER,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}))

/*
 * When a port is translated into a host port, it is mapped into a host port. Mapping a port into a
 * host is optional.
 */
@ConnectedTo(toClass = HostItem.class, type = Relationships.MAPS_TYPE, typeName = Relationships.MAPS_TYPE_NAME,
        relationshipDetails = @LoomAttribute(key = PortItem.LABEL_RELATIONSHIP_HOST,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}))

/***
 * Port Item represent a docker port used by a container. It can be exposed on the host containing
 * that container.
 */

public class PortItem extends BaseItem<PortItemAttributes> {
    static final String LABEL_RELATIONSHIP_CONTAINER = "Container";
    static final String LABEL_RELATIONSHIP_HOST = "Host";

    /**
     * Default constructor.
     *
     * @param itemType The item type
     */
    public PortItem(final ItemType itemType) {
        super(null, itemType);
    }

    /**
     * Constructs a ContainerItem using the provided logicalId and itemType.
     *
     * @param logicalId The logical id
     * @param itemType The itemType
     */
    public PortItem(final String logicalId, final ItemType itemType) {
        super(logicalId, itemType);
    }
}
