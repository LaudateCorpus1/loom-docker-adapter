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
import com.hp.hpl.loom.adapter.annotations.ActionRange;
import com.hp.hpl.loom.adapter.annotations.ActionTypes;
import com.hp.hpl.loom.adapter.annotations.ConnectedTo;
import com.hp.hpl.loom.adapter.annotations.ItemTypeInfo;
import com.hp.hpl.loom.adapter.annotations.LoomAttribute;
import com.hp.hpl.loom.adapter.annotations.Sort;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.Fibre;
import com.hp.hpl.loom.model.Item;
import com.hp.hpl.loom.model.ItemType;
import com.hp.hpl.loom.relationships.ConditionalStopInformation;
import com.hp.hpl.loom.relationships.ConnectedRelationships;

@ItemTypeInfo(value = Types.CONTAINER_TYPE_ID, layers = {Types.DOCKER_LAYER},

        sorting = { // Default field sequence, if the user does not specify another visualisation
                    // method:
                @Sort(operation = DefaultOperations.SORT_BY, fieldOrder = {
                        ContainerItemAttributes.LABEL_CONTAINER_COMMAND, ContainerItemAttributes.LABEL_CONTAINER_ID,
                        ContainerItemAttributes.LABEL_CONTAINER_CREATION_DATE,
                        ContainerItemAttributes.LABEL_CONTAINER_NAME, ContainerItemAttributes.LABEL_CONTAINER_LABEL,
                        ContainerItemAttributes.LABEL_CONTAINER_STATUS,
                        ContainerItemAttributes.LABEL_CONTAINER_OVERALL_STATUS,
                        ContainerItemAttributes.LABEL_CONTAINER_BASE_IMAGE_REPOSITORY_NAME,
                        ContainerItem.LABEL_RELATIONSHIP_IMAGE, ContainerItem.LABEL_RELATIONSHIP_HOST,
                        ContainerItem.LABEL_RELATIONSHIP_VOLUME}),

                // Group by Status
                @Sort(operation = DefaultOperations.GROUP_BY,
                        fieldOrder = {ContainerItemAttributes.LABEL_CONTAINER_OVERALL_STATUS,
                                ContainerItem.LABEL_RELATIONSHIP_IMAGE, ContainerItem.LABEL_RELATIONSHIP_HOST,
                                ContainerItem.LABEL_RELATIONSHIP_VOLUME})})

// Relationships:
@ConnectedTo(toClass = ImageItem.class,
        relationshipDetails = @LoomAttribute(key = ContainerItem.LABEL_RELATIONSHIP_IMAGE,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}),
        type = Relationships.ISBASEDON_TYPE, typeName = Relationships.ISBASEDON_TYPE_NAME)

@ConnectedTo(toClass = HostItem.class,
        relationshipDetails = @LoomAttribute(key = ContainerItem.LABEL_RELATIONSHIP_HOST,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}),
        type = Relationships.RUNS_TYPE, typeName = Relationships.RUNS_TYPE_NAME)

@ConnectedTo(toClass = VolumeItem.class,
        relationshipDetails = @LoomAttribute(key = ContainerItem.LABEL_RELATIONSHIP_VOLUME,
                supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}),
        type = Relationships.MOUNTS_TYPE, typeName = Relationships.MOUNTS_TYPE_NAME)

@ConnectedTo(toClass = PortItem.class, type = Relationships.EXPOSES_TYPE, typeName = Relationships.EXPOSES_TYPE_NAME)
@ConnectedTo(toClass = ContainerItem.class, type = Relationships.LINKS_TYPE, typeName = Relationships.LINKS_TYPE_NAME)

// Single Item Actions:
@ActionDefinition(id = "start", name = "Start container", type = ActionTypes.Item, icon = "fa-play",
        description = "Starts the container",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "stop", name = "Stop", type = ActionTypes.Item, icon = "fa-stop",
        description = "Stops the container",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "restart", name = "Restart", type = ActionTypes.Item, icon = "fa-recycle",
        description = "Restarts the container",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

/*
 * @ActionDefinition(id = "storelogs", name = "Store Container Logs", type = ActionTypes.Item, icon
 * = "fa-file-text-o", description = "Stores the container logs to file", parameters =
 * {@ActionParameter(id = "path", name = "Provide logfile path:", type =
 * com.hp.hpl.loom.model.ActionParameter.Type.STRING)})
 */

/*
 * @ActionDefinition(id = "storeinspection", name = "Store Container Inspection", type =
 * ActionTypes.Item, icon = "fa-file-code-o", description =
 * "Stores the container inspection to file", parameters = {@ActionParameter(id = "path", name =
 * "Provide logfile path:", type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})
 */

@ActionDefinition(id = "removecontainer", name = "Remove container", type = ActionTypes.Item, icon = "fa-trash",
        description = "Removes the container. Should the container is running, stops it and then removes.",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})


// Aggregations Actions:
@ActionDefinition(id = "start", name = "Start container", type = ActionTypes.Aggregation, icon = "fa-play",
        description = "Starts the containers with the specified command.",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "stop", name = "Stop", type = ActionTypes.Aggregation, icon = "fa-stop",
        description = "Stops the containers",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "restart", name = "Restart", type = ActionTypes.Aggregation, icon = "fa-recycle",
        description = "Restarts the containers",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "removecontainer", name = "Remove container", type = ActionTypes.Aggregation, icon = "fa-trash",
        description = "Removes the containers. Should the containers are running, stops them and then removes.",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

/***
 * ContainerItem represents a docker container.
 */
public class ContainerItem extends BaseItem<ContainerItemAttributes> {
    static final String LABEL_RELATIONSHIP_VOLUME = "mounts volume";
    static final String LABEL_RELATIONSHIP_HOST = "runs on host ";
    static final String LABEL_RELATIONSHIP_IMAGE = "is based on image";

    /**
     * Default constructor.
     *
     * @param itemType The item type
     */
    public ContainerItem(final ItemType itemType) {
        super(null, itemType);
    }

    /**
     * Constructs a ContainerItem using the provided logicalId and itemType.
     *
     * @param logicalId The logical id
     * @param itemType The itemType
     */
    public ContainerItem(final String logicalId, final ItemType itemType) {
        super(logicalId, itemType);
    }

    /***
     * Only acts like root if the traversal comes from Stree
     */
    @Override
    public void reportStopTraversalRules() {

        List<BiFunction<ConnectedRelationships, ConditionalStopInformation, Boolean>> stopRules =
                Fibre.STOP_TRAVERSAL_RULES_BY_CLASS.get(this.getClass());

        if (stopRules == null) {
            stopRules = new ArrayList<>(); // Add rule to behave like root if it was reached by
                                           // STree


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
            stopRules.add(stopFromSTree);
            stopRules.add(ConnectedRelationships.VISIT_LAYER_ONLY_ONCE);
            Fibre.STOP_TRAVERSAL_RULES_BY_CLASS.put(this.getClass(), stopRules);
        }
    }


}
