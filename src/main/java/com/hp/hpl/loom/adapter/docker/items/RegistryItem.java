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
import com.hp.hpl.loom.adapter.annotations.Root;
import com.hp.hpl.loom.adapter.annotations.Sort;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.ItemType;

/**
 * RegistryItem represents a docker registry.
 *
 */
@ItemTypeInfo(value = Types.REGISTRY_TYPE_ID, layers = {Types.DOCKER_LAYER},

        sorting = {
                // Default field sequence, if the user does not specify another visualisation
                // method:
                @Sort(operation = DefaultOperations.SORT_BY, fieldOrder = {RegistryItemAttributes.LABEL_REGISTRY_NAME,
                        RegistryItemAttributes.LABEL_REGISTRY_ADDRESS, RegistryItemAttributes.LABEL_REGISTRY_PORT})})

// Relationships definitions:
@ConnectedTo(toClass = ImageItem.class,
        relationshipDetails = @LoomAttribute(key = Relationships.AVAILABLE_AT_TYPE,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}),
        type = Relationships.AVAILABLE_AT_TYPE, typeName = Relationships.AVAILABLE_AT_TYPE_NAME)

@Root

/***
 * RepositotyItem represents an image registry.
 */
public class RegistryItem extends BaseItem<RegistryItemAttributes> {

    /**
     * Default constructor.
     *
     * @param registry Type The item type
     */
    public RegistryItem(final ItemType itemType) {
        super(null, itemType);
    }

    /**
     * Constructs a registryItem using the provided logicalId and itemType.
     *
     * @param logicalId The logical id
     * @param itemType The itemType
     */
    public RegistryItem(final String logicalId, final ItemType itemType) {
        super(logicalId, itemType);
    }
}

