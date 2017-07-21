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

import com.hp.hpl.loom.adapter.annotations.ActionDefinition;
import com.hp.hpl.loom.adapter.annotations.ActionParameter;
import com.hp.hpl.loom.adapter.annotations.ActionRange;
import com.hp.hpl.loom.adapter.annotations.ActionTypes;
import com.hp.hpl.loom.adapter.annotations.ConnectedTo;
import com.hp.hpl.loom.adapter.annotations.ItemTypeInfo;
import com.hp.hpl.loom.adapter.annotations.Root;
import com.hp.hpl.loom.adapter.annotations.Sort;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.ItemType;

@ItemTypeInfo(value = Types.HOST_TYPE_ID, layers = {Types.DOCKER_LAYER}, sorting = {
        // Group by Running OS
        @Sort(operation = DefaultOperations.GROUP_BY,
                fieldOrder = {HostItemAttributes.LABEL_HOST_OS_DISTRIBUTION, HostItemAttributes.LABEL_DOCKER_VERSION,
                        HostItemAttributes.LABEL_KERNEL_VERSION, HostItemAttributes.LABEL_NUMBER_OF_CORES})})

// Relationships definitions:
@Root
@ConnectedTo(toClass = ImageItem.class, type = Relationships.HASLOCAL_TYPE, typeName = Relationships.HASLOCAL_TYPE_NAME)
@ConnectedTo(toClass = ContainerItem.class, type = Relationships.RUNS_TYPE, typeName = Relationships.RUNS_TYPE_NAME)
@ConnectedTo(toClass = VolumeItem.class, type = Relationships.CONTAINS_TYPE,
        typeName = Relationships.CONTAINS_TYPE_NAME)
@ConnectedTo(toClass = PortItem.class, type = Relationships.MAPS_TYPE, typeName = Relationships.MAPS_TYPE_NAME)
@ConnectedTo(toClass = RegistryItem.class, type = Relationships.FETCHES_IMAGES_FROM,
        typeName = Relationships.FETCHES_IMAGES_FROM_NAME)

// Host actions:
@ActionDefinition(id = "createcontainer", name = "Create container", type = ActionTypes.Item, icon = "fa-plus",
        description = "Creates a container with the specified image and command.",
        parameters = {
                @ActionParameter(id = "imagename", name = "Image name:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING),
                @ActionParameter(id = "command", name = "Command to be executed:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})

@ActionDefinition(id = "startallcontainers", name = "Start all container", type = ActionTypes.Item, icon = "fa-play",
        description = "Starts all containers",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "stopsallcontainers", name = "Stops all containers.", type = ActionTypes.Item, icon = "fa-stop",
        description = "stops all running containers.",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "restartsallcontainers", name = "Restarts all containers.", type = ActionTypes.Item,
        icon = "fa-recycle", description = "restarts all running containers.",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "downloadimage", name = "Downloads an image.", type = ActionTypes.Item,
        icon = "fa-cloud-download", description = "Downloads an image from the repository.",
        parameters = {@ActionParameter(id = "imagename", name = "Please provide the image name:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})

@ActionDefinition(id = "deploycadvisor", name = "Deploys cAdvisor", type = ActionTypes.Item, icon = "fa-bar-chart",
        description = "Deploys a cAdvisor container in the host",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "createcontainer", name = "Create container", type = ActionTypes.Thread, icon = "fa-plus",
        description = "Creates a container with the specified image and command.",
        parameters = {
                @ActionParameter(id = "imagename", name = "Image name:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING),
                @ActionParameter(id = "command", name = "Command to be executed:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})

@ActionDefinition(id = "createcontainersperhost", name = "Create containers per host", type = ActionTypes.Thread,
        icon = "fa-play",
        description = "Creates a number of containers with the specified image and command on every host",
        parameters = {
                @ActionParameter(id = "number", name = "Number per host:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING),
                @ActionParameter(id = "imagename", name = "Image name:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING),
                @ActionParameter(id = "command", name = "Command to be executed:",
                        type = com.hp.hpl.loom.model.ActionParameter.Type.STRING)})

@ActionDefinition(id = "terminatecontainers", name = "Terminate containers on all hosts", type = ActionTypes.Thread,
        icon = "fa-bar-chart", description = "Terminate containers (except cAdvisor) on all hosts",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "deploycadvisors", name = "Deploy cAdvisors", type = ActionTypes.Thread, icon = "fa-bar-chart",
        description = "Deploy cAdvisor on all hosts",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

@ActionDefinition(id = "terminatecadvisors", name = "Terminate cAdvisors", type = ActionTypes.Thread,
        icon = "fa-bar-chart", description = "Terminate cAdvisor on all hosts",
        parameters = {@ActionParameter(id = "confirm", name = "Confirmation:",
                type = com.hp.hpl.loom.model.ActionParameter.Type.ENUMERATED,
                ranges = {@ActionRange(id = "no", name = "No"), @ActionRange(id = "yes", name = "Yes")})})

/***
 * HostItem which represents a host running a docker daemon. The Host may be physical of virtual.
 * This host is ready to perform containers operations/deployment. This item is a root in the Loom
 * Graph model.
 */
public class HostItem extends BaseItem<HostItemAttributes> {

    /**
     * Default constructor.
     */
    public HostItem() {
        super();
    }

    /**
     * Constructs a HostItem from a logicalId and itemType.
     *
     * @param logicalId The logicalId
     * @param itemType The itemType
     */
    public HostItem(final String logicalId, final ItemType itemType) {
        super(logicalId, itemType);
    }

    @Override
    public String getQualifiedName() {
        return getCore().getHostAddress();
    };
}
