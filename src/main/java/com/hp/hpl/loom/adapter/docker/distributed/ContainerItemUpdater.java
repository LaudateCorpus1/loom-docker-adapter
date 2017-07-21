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
package com.hp.hpl.loom.adapter.docker.distributed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.loom.adapter.AggregationUpdater;
import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.BaseItemCollector;
import com.hp.hpl.loom.adapter.ConnectedItem;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.Host;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.HostManager;
import com.hp.hpl.loom.adapter.docker.items.ContainerItem;
import com.hp.hpl.loom.adapter.docker.items.ContainerItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.HostItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.Relationships;
import com.hp.hpl.loom.adapter.docker.items.Types;
import com.hp.hpl.loom.adapter.docker.items.Utils;
import com.hp.hpl.loom.exceptions.NoSuchItemTypeException;
import com.hp.hpl.loom.exceptions.NoSuchProviderException;
import com.hp.hpl.loom.model.Aggregation;
import com.hp.hpl.loom.model.CoreItemAttributes.ChangeStatus;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;


/***
 * ContainerTypeUpdates, used to create Containers
 */
public class ContainerItemUpdater extends AggregationUpdater<ContainerItem, ContainerItemAttributes, Container> {
    private static final Log LOG = LogFactory.getLog(ContainerItemUpdater.class);

    private static final int CONTAINER_ID_TRUNCATE_END = 6;

    private static final int CONTAINER_ID_TRUNCATE_START = 0;

    private static final String CONTAINER_DESCRIPTION = "Represents a docker container";

    private static final String GHOST_CONTAINER_ALERT = "Ghost Container: The image that it refers does not exist";

    private static final int GHOST_CONTAINER_ALERT_LEVEL = 5;

    private Set<String> tamperedContainersOnLastIteration = new HashSet<String>();

    protected DockerDistributedCollector dockerDistributedCollector = null;

    /**
     * Constructs a ContainerItemUpdater.
     *
     * @param aggregation The aggregation this update will update
     * @param adapter The baseAdapter this updater is part of
     * @param DockerDistributedCollector The collector it uses
     *
     * @throws NoSuchItemTypeException Thrown if the ItemType isn't found
     * @throws NoSuchProviderException thrown if adapter is not known
     */
    public ContainerItemUpdater(final Aggregation aggregation, final BaseAdapter adapter,
            final DockerDistributedCollector dockerDistributedCollector)
            throws NoSuchItemTypeException, NoSuchProviderException {
        super(aggregation, adapter, dockerDistributedCollector);
        this.dockerDistributedCollector = dockerDistributedCollector;
    }

    /**
     * Each observed resource should have a way to be identified uniquely within the given adapter’s
     * domain and this is what should be returned here. This method is called to create the Item
     * logicalId.
     *
     * @return a unique way to identify a given resource (within the docker adapter). In the case of
     *         containers, it is the docker containers UID
     */
    @Override
    protected String getItemId(final Container argContainer) {
        return argContainer.id();
    }

    /***
     * This must return a brand new Iterator every collection cycle giving access to all the
     * resources that AggregationUpdater is observing.
     *
     */
    @Override
    protected Iterator<Container> getResourceIterator() {
        List<Container> containerList = HostManager.getInstance(adapter).getAllContainers();

        return containerList.iterator();
    }

    /**
     * This method should return an Item only set with its logicalId and ItemType.
     */
    @Override
    protected ContainerItem createEmptyItem(final String logicalId) {
        ContainerItem item = new ContainerItem(logicalId, itemType);
        return item;
    }

    /**
     * This should return a newly created CoreItemAttributes object based on data observed from the
     * resource.
     */
    @Override
    protected ContainerItemAttributes createItemAttributes(final Container resource) {

        ContainerItemAttributes containerAttr = new ContainerItemAttributes();

        containerAttr.setContainerId(resource.id());

        if ((resource.names() != null) && (!resource.names().isEmpty())) {
            containerAttr.setLabel(resource.names().toString());
        }
        containerAttr.setCommand(resource.command());
        containerAttr.setCreationdate(resource.created());

        if ((resource.names() != null) && (resource.names().size() != 0)) {
            String completeName = "{";

            for (String name : resource.names()) {
                completeName = completeName.concat("[" + name + "]");
            }
            completeName = completeName.concat("}");
            containerAttr.setName(completeName);
        } else {
            containerAttr.setName("");
        }

        containerAttr.setStatus(resource.status());

        containerAttr.setOverallStatus(analyseOverallStatus(resource.status()));

        containerAttr.setBaseImageRepositoryName(resource.image());

        // Define the id of an instance of Container Type.
        containerAttr.setItemId(getItemId(resource));
        containerAttr.setItemDescription(CONTAINER_DESCRIPTION);
        containerAttr.setItemName(
                containerAttr.getContainerId().substring(CONTAINER_ID_TRUNCATE_START, CONTAINER_ID_TRUNCATE_END));


        // Search for previously declared relationships to this item:
        // Since the HostItemUpdater runs before the ContainerItemUpdater, the HostItem will declare
        // a relationship the container before it exists.
        // This prevents containerID namespace crash between different hosts (this may happen in the
        // case of large number of containers)

        HashMap<String, String> declaredRelations =
                ((BaseItemCollector) itemCollector).getRelationshipsDiscoveredOnCurrentUpdateCycle().get(
                        dockerDistributedCollector.getLogicalId(Types.CONTAINER_TYPE_ID, containerAttr.getItemId()));

        String associatedHost = new String("");

        for (Map.Entry<String, String> entry : declaredRelations.entrySet()) {
            if (entry.getKey().contains("runs")) { // Host Relationship found. associatedHost =
                associatedHost = entry.getValue();
            }
        }

        Host localHost = HostManager.getInstance(adapter).locateHostByUID(associatedHost);

        /*
         * update container statistics, if cAdvisor is present on host. Requires to find the local
         * host, that is the reason why the update on the attributes had to be done here and not in
         * the proper section.
         */
        if (localHost.hasCAdvisorRunning()) {

            // If the container is running. Stopped container have no statistics
            if (containerAttr.getOverallStatus().toLowerCase().equals("up")) {
                JsonNode containerStats = localHost.getContainerStatisticsMap().get(resource.id());
                // statistics found
                if (containerStats != null) {
                    HostItemAttributes hostAttributes = (HostItemAttributes) itemCollector
                            .getAdapterItem(Types.HOST_TYPE_ID, associatedHost).getCore();
                    populateStatisticsFromRestResponse(hostAttributes, containerAttr, containerStats);
                }
            }
        }


        return containerAttr;
    }

    /***
     * Generates an overall status from a status.
     *
     * @param status
     * @return overall status
     */
    private String analyseOverallStatus(final String status) {
        String overallStatus = "";
        if (status.toLowerCase().contains("up")) {
            overallStatus = "Up";
        } else {
            overallStatus = "Stopped";
        }
        return overallStatus;
    }

    /***
     * This method returns a status value encoded as follows:
     *
     * <p>
     * <strong>CoreItemAttributes.Status.UNCHANGED</strong>:there are no changes detected between
     * the previous view (the CoreItemsAttributes argument) and the new one (the Resource argument).
     * The selection of the attributes actually compared is left entirely at the adapter writer’s
     * discretion: for instance, our AggregationUpdater for an OpenStack volume checks the value of
     * the status attribute only.
     * <p>
     * <strong>CoreItemAttributes.Status.CHANGED_IGNORE</strong>: some attributes have changed but
     * those changes do not impact any queries or relationships, i.e. the current aggregation cache
     * that Loom has built is still valid. This is in particular targeted at the update of
     * metrics-like attributes or any fast changing ones.
     * <p>
     * <strong>CoreItemAttributes.Status.CHANGED_UPDATE</strong>: the attributes that have changed
     * have an impact of queries, derived attributes or relationships. This means that Loom should
     * mark the related GroundedAggregation as dirty and invalidate any cached DerivedAggregations
     *
     * @param ContainerAttributes The Container Item Attributes
     * @param resource The container on docker-java API
     * @return
     */
    @Override
    protected ChangeStatus compareItemAttributesToResource(final ContainerItemAttributes containerAttributes,
            final Container resource) {

        ChangeStatus status;

        // If the ID or the Status of the container changed, update
        if ((!containerAttributes.getContainerId().equals(resource.id()))
                || (!containerAttributes.getOverallStatus().equals(analyseOverallStatus(resource.status())))) {
            status = ChangeStatus.CHANGED_UPDATE;
        } else {
            status = ChangeStatus.CHANGED_IGNORE;
        }

        return status;
    }

    /***
     * If the given resource is connected to another resource, then this method must set the itemId
     * of the connected Resource for a given relationship using the method
     * adapterItem.setRelationship(ItemTypeId, connectedItemId) where ItemTypeId is used by the
     * helper classes to name the relationship and derive the logicalId of the Item matching the
     * connected resource. ConnectedItem is an interface implemented by AdapterItem exposing the few
     * methods that should be used within the context of this method. *
     *
     * @param containerItem the container item that Items will be connected to
     * @param resource the docker-java API item, from where the connections will be extracted.
     */
    @Override
    @SuppressWarnings("checkstyle:linelength")
    protected void setRelationships(final ConnectedItem containerItem, final Container resource) {

        // Search for previously declared relationships to this item:
        // Since the HostItemUpdater runs before the ContainerItemUpdater, the HostItem will declare
        // a relationship the container before it exists.
        // This prevents containerID namespace clash between different hosts (this may happen in the
        // case of large number of containers)

        HashMap<String, String> declaredRelations = ((BaseItemCollector) itemCollector)
                .getRelationshipsDiscoveredOnCurrentUpdateCycle().get(containerItem.getLogicalId());

        String associatedHost = new String("");

        for (Map.Entry<String, String> entry : declaredRelations.entrySet()) {
            if (entry.getKey().contains("runs")) {
                // Host Relationship found.
                associatedHost = entry.getValue();
            }
        }

        Host localHost = HostManager.getInstance(adapter).locateHostByUID(associatedHost);

        // Image which that container is based on:
        String baseImageId = resource.imageId();

        /*
         * Verifies if that image actually exists in the local host. Due to a docker daemon bug,
         * there can be "ghost-containers" - e.g. containers without an image. This is due to an
         * error when the user removes an image using the -f parameters, without removing the
         * containers associated to it. This containers only exist in the host filesystem, without
         * the user being able to run, start or stop it. Should this be the case, throw an alert.
         */
        if (localHost.hasImage(baseImageId)) {
            containerItem.setRelationshipWithType(adapter.getProvider(), Types.IMAGE_TYPE_ID, baseImageId,
                    Relationships.ISBASEDON_TYPE);
        } else {
            // Generate an alert. The user should remove that container.
            ((ContainerItem) containerItem).setAlertLevel(GHOST_CONTAINER_ALERT_LEVEL);
            ((ContainerItem) containerItem).setAlertDescription(GHOST_CONTAINER_ALERT);
        }

        // Search for the container information on the Map
        ContainerInfo inspectionResponse = null;
        inspectionResponse = localHost.inspectContainer(resource);

        if (inspectionResponse != null) {

            // Linked containers:
            List<String> links = inspectionResponse.hostConfig().links();
            if ((links != null) && (links.size() != 0)) {
                for (String link : links) {
                    String targetContainerNameOrID = link;

                    Container foundContainer =
                            localHost.getContainerByNameOrId(targetContainerNameOrID, resource.names().get(0));
                    if (foundContainer != null) {
                        containerItem.setRelationshipWithType(adapter.getProvider(), Types.CONTAINER_TYPE_ID,
                                foundContainer.id(), Relationships.LINKS_TYPE);
                    }
                }
            }
        }

        /*
         * // Docker Demo hack ): // Connect container to Registry. for (Entry<Registry,
         * List<Image>> entry : HostManager.getInstance(adapter).getImageOnEachRegistry()
         * .entrySet()) { // if (entry.getValue().stream().anyMatch(image -> image.id() ==
         * resource.imageId())) { containerItem.setRelationshipWithType(adapter.getProvider(),
         * Types.REGISTRY_TYPE_ID, entry.getKey().getUID(), Relationships.DOCKER_DEMO_COMES_FROM);
         * // } }
         */
    }

    /***
     * Given a cAdvisor, retrieves the container usage information from it.
     *
     * @param attr
     * @param response
     */
    private void populateStatisticsFromRestResponse(final HostItemAttributes hostAttributes,
            final ContainerItemAttributes attr, final JsonNode response) {
        String dayCpuMax = Utils.getCadvisorAttribute(response.get("day_usage").get("cpu"), "max");
        String dayCpuMean = Utils.getCadvisorAttribute(response.get("day_usage").get("cpu"), "mean");
        String cpuInstant = Utils.getCadvisorAttribute(response.get("latest_usage"), "cpu");

        String dayMemoryMax = Utils.getCadvisorAttribute(response.get("day_usage").get("memory"), "max");
        String dayMemoryMean = Utils.getCadvisorAttribute(response.get("day_usage").get("memory"), "mean");
        String memoryInstant = Utils.getCadvisorAttribute(response.get("latest_usage"), "memory");

        attr.updateStatistics(hostAttributes, dayCpuMax, dayCpuMean, cpuInstant, dayMemoryMax, dayMemoryMean,
                memoryInstant);
    }
}
