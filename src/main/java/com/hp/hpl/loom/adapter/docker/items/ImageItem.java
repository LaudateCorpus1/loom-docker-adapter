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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.hp.hpl.loom.adapter.annotations.ActionDefinition;
import com.hp.hpl.loom.adapter.annotations.ActionParameter;
import com.hp.hpl.loom.adapter.annotations.ActionTypes;
import com.hp.hpl.loom.adapter.annotations.ConnectedTo;
import com.hp.hpl.loom.adapter.annotations.ItemTypeInfo;
import com.hp.hpl.loom.adapter.annotations.LoomAttribute;
import com.hp.hpl.loom.adapter.annotations.Sort;
import com.hp.hpl.loom.adapter.docker.distributed.DockerDistributedAdapter;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.Fibre;
import com.hp.hpl.loom.model.Item;
import com.hp.hpl.loom.model.ItemType;
import com.hp.hpl.loom.relationships.ConditionalStopInformation;
import com.hp.hpl.loom.relationships.ConnectedRelationships;

/**
 * ImageItem represents a docker image.
 *
 */
@ItemTypeInfo(value = Types.IMAGE_TYPE_ID, layers = {Types.DOCKER_LAYER}, sorting = {
        // Default field sequence, if the user does not specify another visualisation
        // method:
        @Sort(operation = DefaultOperations.SORT_BY,
                fieldOrder = {ImageItemAttributes.LABEL_IMAGE_TAG, ImageItemAttributes.LABEL_IMAGE_REPOSITORY,
                        ImageItemAttributes.LABEL_IMAGE_ID, ImageItemAttributes.LABEL_IMAGE_CREATION_DATE,
                        ImageItemAttributes.LABEL_IMAGE_REAL_SIZE, ImageItemAttributes.LABEL_IMAGE_VIRTUAL_SIZE}),

        // Default sorting method, if the user does not specify another visualisation
        // method:
        @Sort(operation = DefaultOperations.GROUP_BY, fieldOrder = {ImageItemAttributes.LABEL_IMAGE_REPOSITORY})},
        supportedAdditionalOperations = {DockerDistributedAdapter.TODO_OPERATION})

// Relationships definitions:
@ConnectedTo(toClass = HostItem.class,
        relationshipDetails = @LoomAttribute(key = ImageItem.LABEL_RELATIONSHIP_HOST,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}),
        type = Relationships.HASLOCAL_TYPE, typeName = Relationships.HASLOCAL_TYPE_NAME)

@ConnectedTo(toClass = ContainerItem.class, type = Relationships.ISBASEDON_TYPE,
        typeName = Relationships.ISBASEDON_TYPE_NAME)

// Single Item Actions:
/*
 * @ActionDefinition(id = "deleteimage", name = "Delete Image", type = Fibre.Type.Item, icon =
 * "fa-ban", description = "Deletes the highlighted image", parameters = {@ActionParameter(id =
 * "confirm", name = "Confirmation:", type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
 * ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})
 */

@ActionDefinition(id = "launchcontainer", name = "Launch Container", type = ActionTypes.Item, icon = "fa-play",
        description = "launches a container based on this image",
        parameters = {
                @ActionParameter(id = "containercommand", name = "Container Command:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING),
                @ActionParameter(id = "numberofcontainers", name = "Number of Containers:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})

// Aggregation Actions:
/*
 * @ActionDefinition(id = "deleteimage", name = "Delete Image", type = Fibre.Type.Aggregation, icon
 * = "fa-ban", description = "Deletes the highlighted image", parameters = {@ActionParameter(id =
 * "confirm", name = "Confirmation:", type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
 * ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})
 */

@ActionDefinition(id = "launchcontainer", name = "Launch Container", type = ActionTypes.Aggregation, icon = "fa-play",
        description = "launches a container based on this image",
        parameters = {
                @ActionParameter(id = "containercommand", name = "Container Command:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING),
                @ActionParameter(id = "numberofcontainers", name = "Number of Containers:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})

/***
 * ImageItem represents a docker image, this item is a root in the Loom Graph Model.
 */
public class ImageItem extends BaseItem<ImageItemAttributes> {

    static final String LABEL_RELATIONSHIP_HOST = "local on host";

    /**
     * Default constructor.
     *
     * @param itemType The item type
     */
    public ImageItem(final ItemType itemType) {
        super(null, itemType);
    }

    /**
     * Constructs a ImageItem using the provided logicalId and itemType.
     *
     * @param logicalId The logical id
     * @param itemType The itemType
     */
    public ImageItem(final String logicalId, final ItemType itemType) {
        super(logicalId, itemType);
    }

    /***
     * Only acts like root if the traversal comes from host or Stree
     */
    @Override
    public void reportStopTraversalRules() {

        List<BiFunction<ConnectedRelationships, ConditionalStopInformation, Boolean>> stopRules =
                Fibre.STOP_TRAVERSAL_RULES_BY_CLASS.get(this.getClass());

        if (stopRules == null) {
            stopRules = new ArrayList<>(); // Add rule to behave like root if it was reached by Host

            BiFunction<ConnectedRelationships, ConditionalStopInformation, Boolean> stopFromHost =
                    (relation, stopCondition) -> {

                        Boolean returnResult;

                        String fromItemType =
                                stopCondition.getSequenceOfTraversedItemsUntilHere().get(0).getItemType().getId();
                        if ((fromItemType != null) && fromItemType.toLowerCase().contains("host")) {
                            returnResult = true;
                        } else {
                            returnResult = false;
                        }

                        return returnResult;
                    };

            BiFunction<ConnectedRelationships, ConditionalStopInformation, Boolean> stopFromSTree =
                    (relation, stopCondition) -> {

                        Boolean returnResult = false;
                        List<Item> path = stopCondition.getSequenceOfTraversedItemsUntilHere();

                        if (path.size() >= 2) {

                            String fromItemType = path.get(path.size() - 2).getItemType().getId();
                            if ((fromItemType != null) && fromItemType.toLowerCase().contains("stree")) {
                                returnResult = true;
                            }
                        }

                        return returnResult;
                    };
            stopRules.add(stopFromHost);
            stopRules.add(stopFromSTree);
            stopRules.add(ConnectedRelationships.VISIT_LAYER_ONLY_ONCE);
            Fibre.STOP_TRAVERSAL_RULES_BY_CLASS.put(this.getClass(), stopRules);
        }
    }
}
