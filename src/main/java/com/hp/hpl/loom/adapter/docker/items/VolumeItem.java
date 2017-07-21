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

@ItemTypeInfo(value = Types.VOLUME_TYPE_ID, layers = {Types.DOCKER_LAYER}, sorting = {
        // Default field sequence, if the user does not specify another visualisation
        // method:
        @Sort(operation = DefaultOperations.SORT_BY,
                fieldOrder = {VolumeItemAttributes.LABEL_VOLUME_PATH_ON_HOST,
                        VolumeItemAttributes.LABEL_VOLUME_HOST_ID}),

        @Sort(operation = DefaultOperations.GROUP_BY, fieldOrder = {VolumeItemAttributes.LABEL_VOLUME_PATH_ON_HOST,
                VolumeItem.LABEL_RELATIONSHIP_CONTAINER, VolumeItem.LABEL_RELATIONSHIP_HOST})})

// Relationships definitions
@ConnectedTo(toClass = ContainerItem.class, type = Relationships.MOUNTS_TYPE, typeName = Relationships.MOUNTS_TYPE_NAME,
        relationshipDetails = @LoomAttribute(key = VolumeItem.LABEL_RELATIONSHIP_CONTAINER,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}))

@ConnectedTo(toClass = HostItem.class, type = Relationships.CONTAINS_TYPE, typeName = Relationships.CONTAINS_TYPE_NAME,
        relationshipDetails = @LoomAttribute(key = VolumeItem.LABEL_RELATIONSHIP_HOST,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}))

/**
 * Represent a docker volume
 */
public class VolumeItem extends BaseItem<VolumeItemAttributes> {
    static final String LABEL_RELATIONSHIP_CONTAINER = "Container";
    static final String LABEL_RELATIONSHIP_HOST = "Host";

    /**
     * Default constructor.
     *
     * @param itemType The item type
     */
    public VolumeItem(final ItemType itemType) {
        super(null, itemType);
    }

    /**
     * Constructs a ContainerItem using the provided logicalId and itemType.
     *
     * @param logicalId The logical id
     * @param itemType The itemType
     */
    public VolumeItem(final String logicalId, final ItemType itemType) {
        super(logicalId, itemType);
    }
}
