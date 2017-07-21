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

import java.util.Collection;
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
import com.hp.hpl.loom.adapter.ConnectedItem;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.Host;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.HostManager;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.Volume;
import com.hp.hpl.loom.adapter.docker.items.HostItem;
import com.hp.hpl.loom.adapter.docker.items.HostItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.Relationships;
import com.hp.hpl.loom.adapter.docker.items.Types;
import com.hp.hpl.loom.adapter.docker.items.Utils;
import com.hp.hpl.loom.adapter.docker.realworld.Registry;
import com.hp.hpl.loom.exceptions.NoSuchItemTypeException;
import com.hp.hpl.loom.exceptions.NoSuchProviderException;
import com.hp.hpl.loom.model.Aggregation;
import com.hp.hpl.loom.model.CoreItemAttributes.ChangeStatus;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Image;

/***
 * Collects all hosts available in the system.
 */
public class HostItemUpdater extends AggregationUpdater<HostItem, HostItemAttributes, Host> {
    private static final Log LOG = LogFactory.getLog(HostItemUpdater.class);

    private static final String HOST_DESCRIPTION = "Host running a docker daemon";

    protected DockerDistributedCollector dockerDistributedCollector = null;

    private Set<String> compromisedHostsOnLastIteration = new HashSet<String>();


    /**
     * Constructs a HostItemUpdater.
     *
     * @param aggregation The aggregation this update will update
     * @param adapter The baseAdapter this updater is part of
     * @param DockerDistributedCollector The collector it uses
     *
     * @throws NoSuchItemTypeException Thrown if the ItemType isn't found
     * @throws NoSuchProviderException thrown if adapter is not known
     */
    public HostItemUpdater(final Aggregation aggregation, final BaseAdapter adapter,
            final DockerDistributedCollector dockerCollector) throws NoSuchItemTypeException, NoSuchProviderException {
        super(aggregation, adapter, dockerCollector);
        dockerDistributedCollector = dockerCollector;
    }

    /**
     * Each observed resource should have a way to be identified uniquely within the given adapter’s
     * domain and this is what should be returned here. This method is called to create the Item
     * logicalId.
     *
     * @return a unique way to identify a given resource (within the docker adapter). In the case of
     *         hosts, it is the [host address]:[daemon listening port]
     */
    @Override
    protected String getItemId(final Host argHost) {
        return argHost.getUID();
    }

    /***
     * This must return a brand new Iterator every collection cycle giving access to all the
     * resources that AggregationUpdater is observing.
     *
     */
    @Override
    protected Iterator<Host> getResourceIterator() {

        // Retrieve HostManager singleton
        // Make sure that it updates the content every x seconds on the property file.
        HostManager manager = HostManager.getInstance(adapter);

        // Make the manager update all Containers and Images databases
        manager.refreshHostsInformation();

        List<Host> hosts = manager.getHostList();

        return hosts.iterator();
    }

    @Override
    protected Iterator<Host> getUserResourceIterator(final Collection<Host> data) {
        return data.iterator();
    }

    /**
     * This method should return an Item only set with its logicalId and ItemType.
     */
    @Override
    protected HostItem createEmptyItem(final String logicalId) {
        HostItem item = new HostItem(logicalId, itemType);
        return item;
    }

    /**
     * This should return a newly created CoreItemAttributes object based on data observed from the
     * resource.
     */
    @Override
    protected HostItemAttributes createItemAttributes(final Host resource) {

        HostItemAttributes hostAttr = new HostItemAttributes();

        hostAttr.setHostAddress(resource.getDaemonIp());
        hostAttr.setHostPort(resource.getDaemonListeningPort());
        hostAttr.setHostDockerDaemonAddress(resource.getDockerAddress());

        hostAttr.setItemId(resource.getDockerAddress());
        hostAttr.setItemName(resource.getDockerAddress());
        hostAttr.setItemDescription(HOST_DESCRIPTION);

        if (resource.isOSInformationAvailable()) {
            hostAttr.setRunningOS(resource.getOsDistribution());
        }

        // Where there is a cAdvisor running on the host, retrieve the host statistics.
        if (resource.hasCAdvisorRunning()) {
            retrieveCAdvisorHostInformation(resource, hostAttr);
        }

        return hostAttr;
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
     * @param hostattributes The Host Item Attributes
     * @param resource The physical host
     * @return
     */
    @Override
    protected ChangeStatus compareItemAttributesToResource(final HostItemAttributes hostattributes,
            final Host resource) {

        ChangeStatus status;

        if (hostattributes.getHostDockerDaemonAddress().equals(resource.getDockerAddress())) {

            // since the docker daemon is composed of the other attributes, one can just evaluate
            // it.
            status = ChangeStatus.CHANGED_IGNORE;

            // update just cadvisor
            retrieveCAdvisorHostInformation(resource, hostattributes);
        } else {
            status = ChangeStatus.CHANGED_UPDATE;
        }

        return status;
    }

    /***
     * If the given resource is connected to another resource, then this method must set the itemId
     * of the connected Resource for a given relationship using the method
     * adapterItem.setRelationship(ItemTypeLocalId, connectedItemId) where ItemTypeLocalId is used
     * by the helper classes to name the relationship and derive the logicalId of the Item matching
     * the connected resource. ConnectedItem is an interface implemented by AdapterItem exposing the
     * few methods that should be used within the context of this method.
     *
     * @param hostItem the host item that Items will be connected to
     * @param resource the real world host item, from where the connections will be extracted.
     */
    @Override
    @SuppressWarnings("checkstyle:linelength")
    protected void setRelationships(final ConnectedItem hostItem, final Host resource) {

        // All images local on the host
        List<Image> imgList = resource.getLocalImages();

        if (!imgList.isEmpty()) {
            for (Image image : imgList) {
                hostItem.setRelationshipWithType(adapter.getProvider(), Types.IMAGE_TYPE_ID, image.id(),
                        Relationships.HASLOCAL_TYPE);
            }
        }

        // All containers local on the host
        List<Container> containerList = resource.getLocalContainers();
        if (!containerList.isEmpty()) {
            for (Container container : containerList) {
                hostItem.setRelationshipWithType(adapter.getProvider(), Types.CONTAINER_TYPE_ID, container.id(),
                        Relationships.RUNS_TYPE);
            }
        }

        // All volume contained in the host
        List<Volume> volumeList = resource.getLocalVolumes();
        if (!volumeList.isEmpty()) {
            for (Volume volume : volumeList) {
                // Every volume is identified by its hash
                hostItem.setRelationshipWithType(adapter.getProvider(), Types.VOLUME_TYPE_ID,
                        Integer.toString(volume.hashCode()), Relationships.CONTAINS_TYPE);
            }
        }

        // All Registries where this host can fetch images:
        Set<Registry> relatedRegistries = new HashSet<>();

        // By default. all hosts can fetch docker hub.
        hostItem.setRelationshipWithType(adapter.getProvider(), Types.REGISTRY_TYPE_ID,
                Registry.DOCKER_HUB_REGISTRY_UID, Relationships.AVAILABLE_AT_TYPE);

        Map<String, List<Registry>> imageToRegMap =
                dockerDistributedCollector.getRegistryItemUpdater().getImageAndRegistriesMap();

        for (Image image : resource.getLocalImages()) {

            List<Registry> registriesWithImages = imageToRegMap.get(image.id());

            if (registriesWithImages != null) {
                for (Registry reg : registriesWithImages) {
                    if (!reg.getUID().toLowerCase().contains(Registry.DOCKER_HUB_REGISTRY_UID)) {
                        relatedRegistries.add(reg);
                    }
                }
            }
        }

        for (Registry reg : relatedRegistries) {
            hostItem.setRelationshipWithType(adapter.getProvider(), Types.REGISTRY_TYPE_ID, reg.getUID(),
                    Relationships.FETCHES_IMAGES_FROM);
        }
    }

    /***
     * Uses the cAdvisor RESTful interface to retrieve host information
     *
     * @param host
     * @param attr
     */
    public void retrieveCAdvisorHostInformation(final Host host, final HostItemAttributes attr) {
        JsonNode machineStatisticsResponse = host.getMachineStatistics();
        JsonNode machinePhysicalInfoRespose = host.getMachinePhysicalInformation();

        /*
         * It way take a few refresh cycles for the cAdvisor container start providing responses. In
         * this meanwhile the response may be null.
         */
        if (machinePhysicalInfoRespose != null) {
            populateMachineInfoFromRestResponse(attr, machinePhysicalInfoRespose);
        }
        if (machineStatisticsResponse != null) {
            populateStatisticsFromRestResponse(attr, machineStatisticsResponse);
        }
    }

    /***
     * Given a cAdvisor, retrieves the Host Machine information from it.
     *
     * @param attr
     * @param response
     */
    private void populateMachineInfoFromRestResponse(final HostItemAttributes attr, final JsonNode response) {
        String cpuFrequency = Utils.getCadvisorAttribute(response, "cpu_frequency_khz");

        String fileSystemCapacity = Utils.getCadvisorAttribute(response.get("filesystems").get(0), "capacity");
        String memoryCapacity = Utils.getCadvisorAttribute(response, "memory_capacity");
        String numberOfCores = Utils.getCadvisorAttribute(response, "num_cores");
        String dockerVersion = Utils.getCadvisorAttribute(response, "docker_version");
        String kernelVersion = Utils.getCadvisorAttribute(response, "kernel_version");

        attr.updateMachineInfo(cpuFrequency, fileSystemCapacity, memoryCapacity, numberOfCores, dockerVersion,
                kernelVersion);
    }



    /***
     * Given a cAdvisor, retrieves the host usage information from it.
     *
     * @param attr
     * @param response
     */
    private void populateStatisticsFromRestResponse(final HostItemAttributes attr, final JsonNode response) {
        String dayCpuMax = Utils.getCadvisorAttribute(response.get("/").get("day_usage").get("cpu"), "max");
        String dayCpuMean = Utils.getCadvisorAttribute(response.get("/").get("day_usage").get("cpu"), "mean");
        String cpuInstant = Utils.getCadvisorAttribute(response.get("/").get("latest_usage"), "cpu");

        String dayMemoryMax = Utils.getCadvisorAttribute(response.get("/").get("day_usage").get("memory"), "max");
        String dayMemoryMean = Utils.getCadvisorAttribute(response.get("/").get("day_usage").get("memory"), "mean");
        String memoryInstant = Utils.getCadvisorAttribute(response.get("/").get("latest_usage"), "memory");

        attr.updateStatistics(dayCpuMax, dayCpuMean, cpuInstant, dayMemoryMax, dayMemoryMean, memoryInstant);
    }
}
