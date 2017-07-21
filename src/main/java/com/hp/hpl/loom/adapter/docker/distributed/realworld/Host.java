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
package com.hp.hpl.loom.adapter.docker.distributed.realworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.docker.realworld.ContainerPort;
import com.jcraft.jsch.JSchException;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerMount;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Info;
import com.spotify.docker.client.messages.PortBinding;

/***
 * Represents a Host. In the distributed docker application, there are multiple hosts.
 */
public class Host {
    private static final Log LOG = LogFactory.getLog(Host.class);
    private static final int DOCKER_CONNECTION_POOL_SIZE = 1000;
    private static final long DEFAULT_READ_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);


    /***
     * IP for Host running the docker daemon, as provided in the hosts.json file.
     */
    private String daemonIp;

    /***
     * Listening Port for Host running the docker daemon, as provided in the hosts.json file.
     */
    private String daemonListeningPort;

    /***
     * Complete Docker address of host running the docker daemon.
     * <p>
     * Has the format: http[s]://[host with docker daemon]:[daemon listening port]
     */
    private String dockerAddress;

    private DockerClient dockerClient;

    /* OS Distribution, used when SSH is enabled. */
    private String osDistribution;
    private boolean osInformationAvailable = false;

    // Client used to retrieve cAdvisor information
    private Client jerseyClient = null;

    // Information updated on every single update cycle
    private JsonNode machinePhysicalInformation;
    private JsonNode machineStatistics;
    private Map<String, JsonNode> containerStatisticsMap;

    // cAvisor that monitors that host
    private Container cAdvisorContainer = null;

    // Lists contained by the host and refreshed on every hostUpdater iteration
    private List<Container> containerList;

    // Map to verify quickly if a given image is local on the host
    private Map<String, Image> imageMap;

    // Both volume and port are hash maps in order allow quick updates on it, should a container get
    // removed preventing iterating on the whole list.

    // The key for the portMap is the containerId, since no port exists with a container.
    private Map<String, List<ContainerPort>> portMap;

    /*
     * The representation of volumes is a bit tricky. Because on docker model, volumes are
     * independent entities. This means that they do not require containers to exist. In the current
     * docker API, however, one can only detect a volume by inspecting Containers and noticing that
     * they are mounting certain volumes. Volumes have a n to n relationship with containers, thus a
     * map would not be a good solution for it. In the HashMap, the key is the Volume and the value
     * is a list of containers that mount that volume.
     */
    private Map<Volume, List<String>> volumeMap;

    // HashMap to reduce container inspection requests. Works as a cache.
    // The developer must take care to update this cache on every container addition or deletion.
    private Map<String, ContainerInfo> inspectionMap;

    // Container OS, due to the NFS system, has some delay to provide container information. This
    // would cause container inspection to no be able to correctly update, since the containercreate
    // would not have returned yet. This variable prevents any containerList update if a create
    // container action happened and did not finish yet.
    private Lock updateListOrCreateContainerLock = new ReentrantLock();

    @SuppressWarnings("checkstyle:redundantthrows")
    protected Host(final JsonNode hostInfo, final BaseAdapter adapter) throws Exception {
        daemonIp = retrieveDaemonIp(hostInfo);
        daemonListeningPort = retrieveDaemonListeningPort(hostInfo);
        dockerAddress = setDockerAddress(hostInfo);

        // If enabled, retrieves Host OS information through SSH.
        retrieveIntraInformation(hostInfo);

        // Generate a docker daemon and get info.
        dockerClient = DefaultDockerClient.builder().connectionPoolSize(DOCKER_CONNECTION_POOL_SIZE).uri(dockerAddress)
                .build();

        StopWatch stopWatch = null;

        if (LOG.isTraceEnabled()) {
            stopWatch = new StopWatch("Initial Refresh");
        }

        try {
            // Note: Optimisation - find a way to solve conflict that makes two requests on the
            // first cycle
            refreshHostContent(stopWatch);

            /*
             * Generates a cache of containers inspection on the local host, this reduces the number
             * of Get requests of the docker API.
             */

            if (LOG.isTraceEnabled()) {
                stopWatch.start("Initial Update Container Inspection Cache");
            }

            updateAllContainerInspectionCache();

            if (LOG.isTraceEnabled()) {
                stopWatch.stop();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (LOG.isTraceEnabled()) {
                LOG.trace(stopWatch.prettyPrint());
            }
        }
    }

    /***
     * Retrieves Host OS information.
     *
     * @param hostInfo
     * @throws JSchException
     */
    private void retrieveIntraInformation(final JsonNode hostInfo) throws JSchException {
        DockerClient disposableDockerClient =
                DefaultDockerClient.builder().connectionPoolSize(DOCKER_CONNECTION_POOL_SIZE)
                        .readTimeoutMillis(DEFAULT_READ_TIMEOUT_MILLIS).uri(dockerAddress).build();

        Info clientInformation = null;
        try {
            clientInformation = disposableDockerClient.info();
        } catch (DockerException | InterruptedException e) {
            LOG.warn("Could not retrieve docker daemon information from host '" + disposableDockerClient.getHost()
                    + "'");
        }

        if (clientInformation != null) {
            String operatingSystemInfo = clientInformation.operatingSystem();

            osDistribution = operatingSystemInfo.split(" ")[0];
            osInformationAvailable = true;
        }
    }

    /***
     * Updates the host containers, images and cAvisors databases.
     *
     * @throws Exception
     */
    public void refreshHostContent(final StopWatch stopWatch) throws Exception {
        if (LOG.isTraceEnabled()) {
            stopWatch.start("Refresh Container Database");
        }

        verifyLockAndRefreshContainerDatabase();

        if (LOG.isTraceEnabled()) {
            stopWatch.stop();

            stopWatch.start("Refresh Image Database");
        }

        refreshImageDatabase();

        if (LOG.isTraceEnabled()) {
            stopWatch.stop();

            stopWatch.start("Refresh CAdvisors");
        }

        refreshCAdvisors();

        if (LOG.isTraceEnabled()) {
            stopWatch.stop();
        }
    }

    /***
     * Looks for a running cAdvisor container on the host
     */
    private void refreshCAdvisors() {

        // look for a new one every single iteration.
        cAdvisorContainer = null;

        for (Container container : containerList) {
            if (container.image().toLowerCase().contains("cadvisor")) {
                // cAdvisor container found.
                if (container.status().toLowerCase().contains("up")) {
                    // and the container is currently monitoring the host.
                    cAdvisorContainer = container;

                    // initialises the jerseyClient on the first call:
                    if (jerseyClient == null) {
                        jerseyClient = ClientBuilder.newClient();
                    }

                    updateCAdvisorHostInformations();
                    updateCAdvisorContainersInformation();
                    break;
                }
            }
        }
    }

    /***
     * Retrieves all the Host information from cAdvisor
     */
    private void updateCAdvisorHostInformations() {
        String uri = "http://" + getDaemonIp() + ":8080/api/v2.0/summary";

        try {
            // Retrieve cAdvisor Statistics information through REST API
            WebTarget resource = jerseyClient.target(uri);
            Response statisticsResponse = resource.request(MediaType.APPLICATION_JSON).get();

            // Retrieve cAdvisor machine information through REST API
            resource = jerseyClient.target("http://" + getDaemonIp() + ":8080/api/v2.0/machine");
            Response machineInfoResponse = resource.request(MediaType.APPLICATION_JSON).get();

            if (statisticsResponse.getStatus() == Response.Status.OK.getStatusCode()
                    && machineInfoResponse.getStatus() == Response.Status.OK.getStatusCode()) {

                String statResponse = statisticsResponse.readEntity(String.class);
                String machResponse = machineInfoResponse.readEntity(String.class);

                ObjectMapper mapper = new ObjectMapper();
                try {
                    machineStatistics = mapper.readTree(statResponse);
                    machinePhysicalInformation = mapper.readTree(machResponse);
                } catch (Exception e) {
                    LOG.error("Failed to process information from " + uri, e);
                }
            }
        } catch (Exception e) {
            /*
             * Note: Sometimes a runtime ConnectionRefused Exception can be thrown here. This is
             * possibly because the cAdvisor container is not ready yet to get connection here. On
             * the next iteration cycle it will behave as intended.
             */
            LOG.warn("cAdvisor connection to " + uri + " refused - has it just been launched?");
        }
    }

    /***
     * Updates all containers usage information available. The containers retrieve this from a
     * cache. The information of all containers is updated on every single adaptor update cycle, in
     * just one GET request.
     */
    private void updateCAdvisorContainersInformation() {
        String uri = "http://" + getDaemonIp() + ":8080/api/v2.0/summary/docker/?recursive=true";

        containerStatisticsMap = new HashMap<String, JsonNode>();

        try {
            WebTarget resource = jerseyClient.target(uri);
            Response containerStatistics = resource.request(MediaType.APPLICATION_JSON).get();

            if (containerStatistics.getStatus() == Response.Status.OK.getStatusCode()) {
                String containerStat = containerStatistics.readEntity(String.class);
                ObjectMapper mapper = new ObjectMapper();

                try {
                    JsonNode root = mapper.readTree(containerStat);

                    // The return is not an Iterable:
                    Iterator<Map.Entry<String, JsonNode>> itr = root.fields();

                    while (itr.hasNext()) {

                        Map.Entry<String, JsonNode> child = itr.next();

                        /*
                         * The first element is an overall usage of docker. Thus there is no
                         * interest on it.
                         */

                        // Select only containers, not the daemon or the application.
                        if (!child.getKey().equals("/docker-daemon/docker") && !child.getKey().equals("/docker")) {

                            // get container name:
                            String[] subStrings = child.getKey().split("/");
                            String containerUID = subStrings[subStrings.length - 1];

                            // get Container statistics:
                            JsonNode containerStats = child.getValue();

                            containerStatisticsMap.put(containerUID, containerStats);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process response to " + uri, e);
                }
            }
        } catch (Exception e1) {
            /*
             * Note: Sometimes a runtime ConnectionRefused Exception can be thrown here. This is
             * possibly because the cAdvisor container is not ready yet to get connection here. On
             * the next iteration cycle it will behave as intended.
             */
            LOG.warn("cAdvisor connection to " + uri + " refused - has is just been launched?");
        }
    }

    /***
     * Returns true if there is a cAdvisor running, otherwise returns false
     *
     * @return true if has cAdvisor running, otherwise false.
     */
    public boolean hasCAdvisorRunning() {
        if (getcAdvisorContainer() != null) {
            return true;
        }

        return false;
    }

    /**
     * @return the dockerAddress
     */
    public String getDockerAddress() {
        return dockerAddress;
    }

    /**
     * @return the daemonListeningPort
     */
    public String getDaemonListeningPort() {
        return daemonListeningPort;
    }


    /***
     * @return the IP/URL address of the daemon
     */
    public String getDaemonIp() {
        return daemonIp;
    }

    /***
     * The UID of the docker daemon host is the complete docker address
     *
     * @return UID
     */
    public String getUID() {
        return getDockerAddress();
    }

    /***
     * Retrieves the daemon IP address configuration value (from the key dockerDaemonAddress)
     * contained in the file dockerlocal.properties
     *
     * @param adapter
     * @return listening host with a docker daemon
     */
    private String retrieveDaemonIp(final JsonNode hostInfo) {

        String retrievedDaemonIp = hostInfo.get("address").textValue();

        if (retrievedDaemonIp == null || retrievedDaemonIp.isEmpty()) {
            retrievedDaemonIp = "localhost";
        }

        return retrievedDaemonIp;
    }

    /***
     * Retrieves the daemon listening port configuration value (from the key dockerDaemonPort)
     *
     * @param adapter
     * @return listening port
     */
    private String retrieveDaemonListeningPort(final JsonNode hostInfo) {
        String retrivedDaemonListeningPort = hostInfo.get("port").textValue();

        if (retrivedDaemonListeningPort == null || retrivedDaemonListeningPort.isEmpty()) {
            retrivedDaemonListeningPort = "2375";
        }

        return retrivedDaemonListeningPort;
    }

    /***
     * The docker address is composed as: "http[s]://[host with docker daemon]:[daemon listening
     * port]
     *
     * @return docker address
     */
    private String setDockerAddress(final JsonNode hostInfo) {

        // Note: in order configure correctly docker on TLS, use the following guide:
        // - https://docs.docker.com/articles/https/

        // Checks if TLS verification is required:
        boolean tlsRequired = hostInfo.get("tlsRequired").asBoolean();

        if (!tlsRequired) {
            return "http://" + daemonIp + ":" + daemonListeningPort;
        } else {
            return "https://" + daemonIp + ":" + daemonListeningPort;
        }
    }

    /***
     * Retrieves a list of local images. The list is updated every single HostItemUpdater cycle.
     *
     * @return List of local images
     */
    public List<Image> getLocalImages() {

        if (imageMap == null) {
            imageMap = new HashMap<String, Image>();

            // Request to the daemon a new image list
            try {
                refreshImageDatabase();
            } catch (Exception e) {
                LOG.error("Image refresh failed", e);
            }
        }

        // convert the map to list:
        List<Image> imageList = imageMap.values().stream().collect(Collectors.toList());
        return imageList;
    }

    /***
     * Retrieves the image list from the docker daemon
     *
     * @throws Exception
     */
    public void refreshImageDatabase() throws Exception {

        if (imageMap != null) {
            // Clears any value that was before in the map:
            imageMap.clear();
        } else {
            imageMap = new HashMap<String, Image>();
        }

        try {
            List<Image> imageList = dockerClient.listImages();

            // Puts the list in the map.
            imageList.forEach((image) -> imageMap.put(image.id(), image));
        } catch (DockerException | InterruptedException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not list images");
            }

            throw e;
        }
    }

    /***
     * Connects to the docker daemon and retrieves a list of local containers, including stopped
     * ones. Due to the adapter update cycle, there may be a race condition caused by a
     * createContainer command (e.g. create container action).
     *
     * @return List of local containers
     */
    public List<Container> getLocalContainers() {
        if (containerList == null) {
            containerList = new ArrayList<Container>();

            // Request to the daemon a new Container list
            try {
                verifyLockAndRefreshContainerDatabase();
            } catch (Exception e) {
                LOG.error("Failed to retrieve list of containers", e);
            }
        }

        return containerList;
    }

    /***
     * If an update happens while the createContainer method was called (by an action on the host),
     * it causes an exception to be thrown, since no containerinspection information will be
     * retrieved during container update cycle, although the container existence is detected. The
     * method makes sure that the old container list will be returned if a createmethod has not
     * returned yet. When the create method returns, it will then update the container list.
     *
     * @throws Exception
     */
    public void verifyLockAndRefreshContainerDatabase() throws Exception {
        if (updateListOrCreateContainerLock.tryLock()) {
            // Lock is not being used.
            try {
                refreshContainerDatabase();
            } catch (Exception e) {
                LOG.error("Failed to update the container database", e);
            } finally {
                updateListOrCreateContainerLock.unlock();
            }
        }
    }

    /***
     * Retrieves the container List from the Docker Daemon.
     *
     * @throws Exception
     */
    public void refreshContainerDatabase() throws Exception {
        try {
            final HashMap<String, Container> containerListBeforeUpdate = new HashMap<String, Container>();

            // Compare deltas for inspection cache
            boolean compareDeltas = false;

            if (containerList != null) {

                // On the first iteration of the updater, there is no delta do be compared.
                compareDeltas = true;

                /*
                 * Generates a Hash Map with keys being the containerId strings. This makes the
                 * comparison less expensive than comparing Containers instances.
                 */
                containerList.forEach((container) -> containerListBeforeUpdate.put(container.id(), container));
            }

            containerList = dockerClient.listContainers(ListContainersParam.allContainers(true));

            if (compareDeltas) {
                HashMap<String, Container> containerListAfterUpdate = new HashMap<String, Container>();
                containerList.forEach((container) -> containerListAfterUpdate.put(container.id(), container));

                /*
                 * One needs to find containers that were added and deleted between updates, in
                 * order to keep the cache coherent. They can be removed by command line. Thus, no
                 * cache coherence call would have been made. Indexing the container data structure
                 * per container id makes the search more light weight.
                 */

                // Clone containerListBeforeUpdate and containerListAfterUpdate
                HashMap<String, Container> removedContainers =
                        new HashMap<String, Container>(containerListBeforeUpdate);

                HashMap<String, Container> addedContainers = new HashMap<String, Container>(containerListAfterUpdate);

                // Get Containers that were deleted:
                containerListAfterUpdate.forEach((containerId, container) -> removedContainers.remove(containerId));

                // Get Containers that were added:
                containerListBeforeUpdate.forEach((containerId, container) -> addedContainers.remove(containerId));

                // Remove deleted from the inspectionMap cache
                removedContainers.forEach((containerId, container) -> removeContainerInspectionFromCache(containerId));

                // Add new containers to inspectionMap cache
                addedContainers.forEach((containerId, container) -> updateContainerInspectionCache(containerId));

            }

        } catch (DockerException | InterruptedException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not list containers.");
            }

            throw e;
        }
    }

    /***
     * Returns the inspection JSON of a container, from the inspection cache.
     *
     * @param container to be inspected
     * @return json information
     */
    public ContainerInfo inspectContainer(final Container container) {
        return inspectionMap.get(container.id());
    }

    /***
     * returns the inspection JSON of a container, from the inspection cache.
     *
     * @param id of the container of be inspected
     * @return json information
     */
    public ContainerInfo inspectContainer(final String containerId) {
        return inspectionMap.get(containerId);
    }

    /***
     * Request information of all the containers in a single host. This call is very <b>Network
     * Intensive</b>. Thus must be avoided on all costs. Every single container is a http request.
     *
     * Due to Docker API limitations, the Host actually holds all the inspection on a memory cache.
     * Refreshing this cache is really expensive, because every single container inspection is a
     * separate GET request - as defined on docker API.
     *
     * @param container
     */
    public void updateAllContainerInspectionCache() {
        if (containerList != null) {
            for (Container container : containerList) {
                updateContainerInspectionCache(container);
            }
        }
    }

    /***
     * Updates the information of only one container. <b>This method must be called after every
     * single container <i>creation</i></b>.
     *
     * Due to Docker API limitations, the Host actually holds all the inspection on a memory cache.
     * Refreshing this cache is really expensive, because every single container inspection is a
     * separate GET request - as defined on docker API.
     *
     * <strong>Note:</strong> This method can differentiate an addition from a deletion.
     *
     * @param Container object
     */
    public void updateContainerInspectionCache(final Container container) {
        updateContainerInspectionCache(container.id());
    }

    /***
     * Updates the information of only one container. <b>This method must be called after every
     * single container <i>creation</i></b>.
     *
     * Due to Docker API limitations, the Host actually holds all the inspection on a memory cache.
     * Refreshing this cache is really expensive, because every single container inspection is a
     * separate GET request - as defined on docker API.
     *
     * @param container id
     */
    public void updateContainerInspectionCache(final String containerId) {
        if (inspectionMap == null) {
            inspectionMap = new HashMap<String, ContainerInfo>();
        }

        // Update the container information
        try {
            ContainerInfo contentOnUpdate = dockerClient.inspectContainer(containerId);
            inspectionMap.put(containerId, contentOnUpdate);

            // The Volumes and Ports may have changed, update
            updateVolumeInformationFromContainer(containerId);
            updatePortsFromContainer(containerId);

        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not inpect the container " + containerId, e);
        }
    }

    /***
     * Removes the information of only one container. <b>This method must be called after every
     * single container <i>deletion</i></b>
     *
     * Due to Docker API limitations, the Host actually holds all the inspection on a memory cache.
     * Refreshing this cache is really expensive, because every single container inspection is a
     * separate GET request - as defined on docker API.
     *
     * @param container id
     */
    public void removeContainerInspectionFromCache(final String containerId) {

        if (inspectionMap == null) {
            inspectionMap = new HashMap<String, ContainerInfo>();
        } else {
            // removes the inspection from the cache.
            inspectionMap.remove(containerId);
        }

        // The Volumes and Ports may have changed, update
        updatePortsFromContainer(containerId);
        updateVolumeInformationFromContainer(containerId);
    }

    /***
     * Returns all Volumes mounted by Containers running in the docker daemon host. Is updated on
     * every single inspection cache update.
     *
     * @return @return list of volumes on the local host
     */
    public List<Volume> getLocalVolumes() {
        if (volumeMap == null) {
            refreshVolumesList();
        }

        // return the map as a List
        List<Volume> listOfVolumes = new ArrayList<Volume>(volumeMap.keySet());
        return listOfVolumes;
    }

    private void refreshVolumesList() {
        volumeMap = new HashMap<Volume, List<String>>();

        if (containerList != null) {
            for (Container container : containerList) {
                updateVolumeInformationFromContainer(container.id());
            }
        }
    }

    private void updateVolumeInformationFromContainer(final String containerId) {
        if (volumeMap == null) {
            volumeMap = new HashMap<Volume, List<String>>();
        }

        ContainerInfo information = null;
        boolean removedContainer = false;

        // get all volumes on a given container

        /*
         * if the container information was removed from the inpectionMap, than the container was
         * removed.
         */

        information = inspectionMap.get(containerId);
        if (information == null) {
            // remove all references of that container from volume map.
            // Note: This may be slow if there are several volumes.
            removedContainer = true;

            for (Map.Entry<Volume, List<String>> entry : volumeMap.entrySet()) {
                List<String> listOfContainersWithVolume = entry.getValue();

                if (listOfContainersWithVolume.contains(containerId)) {
                    listOfContainersWithVolume.remove(containerId);
                }
            }
        }

        // If the container was not removed, update information
        if (!removedContainer) {

            List<ContainerMount> mountList = information.mounts();

            // Verifies is the container mounts any volume
            if (mountList != null && !mountList.isEmpty()) {
                for (ContainerMount mount : mountList) {
                    String pathOnHost = mount.source();

                    Volume volume = new Volume(pathOnHost, dockerAddress);

                    List<String> containersThatMountThisVolume = null;
                    containersThatMountThisVolume = volumeMap.get(volume);

                    if (containersThatMountThisVolume == null) {
                        // First time a container references that volume
                        containersThatMountThisVolume = new ArrayList<String>();
                    }

                    // Add container that mounts volume to list.
                    containersThatMountThisVolume.add(containerId);
                    volumeMap.put(volume, containersThatMountThisVolume);
                }
            }
        }
    }

    /***
     * Returns all Ports exposed by Containers running in the docker daemon host. Is updated on
     * every single inspection cache update.
     *
     * @return list containing all containers exposed ports.
     */
    public List<ContainerPort> getAllPorts() {
        if (portMap == null) {
            refreshPortList();
        }

        // returns the map as a list:
        List<ContainerPort> retPortList = new ArrayList<ContainerPort>();
        for (List<ContainerPort> listPortsOnContainer : portMap.values()) {
            retPortList.addAll(listPortsOnContainer);
        }

        return retPortList;
    }

    private void refreshPortList() {
        if (portMap == null) {
            portMap = new HashMap<String, List<ContainerPort>>();
        }

        for (Container container : containerList) {
            updatePortsFromContainer(container.id());
        }
    }

    private void updatePortsFromContainer(final String containerId) {
        if (portMap == null) {
            portMap = new HashMap<String, List<ContainerPort>>();
        }

        ContainerInfo inspection = null;
        inspection = inspectionMap.get(containerId);

        if (inspection == null) {
            /*
             * If the container that had the port does not exist anymore, remove all ports that he
             * exposed
             */
            portMap.remove(containerId);
        } else {
            /*
             * In the case inspection is null, the container got removed from the host. In this
             * scenario, Loom will compare the iterators and remove it.
             */

            Map<String, List<PortBinding>> ports = inspection.networkSettings().ports();

            if (ports != null) {

                for (Map.Entry<String, List<PortBinding>> exposedPort : ports.entrySet()) {
                    ContainerPort containerPort = new ContainerPort();

                    String containerPortInfo = exposedPort.getKey();
                    String[] containerParts = containerPortInfo.split("/");

                    String containerPortNumber = containerParts[0];
                    String containerPortProtocol = containerParts[1];

                    containerPort.setContainerID(inspection.id());
                    containerPort.setContainerPortNumber(Integer.parseInt(containerPortNumber));
                    containerPort.setPortProtocol(containerPortProtocol);

                    if (exposedPort.getValue() != null) {
                        List<PortBinding> portBindingList = exposedPort.getValue();
                        String interfaceIP = portBindingList.get(0).hostIp();
                        String hostPort = portBindingList.get(0).hostPort();

                        containerPort.setHostInterfaceIp(interfaceIP);
                        containerPort.setHostPortNumber(Integer.parseInt(hostPort));
                        containerPort.setHostID(this.getUID());
                    }

                    // Check if there is any other port associated to that container before.
                    List<ContainerPort> portsOnGivenContainer = null;
                    portsOnGivenContainer = portMap.get(containerId);

                    if (portsOnGivenContainer == null) {
                        portsOnGivenContainer = new ArrayList<ContainerPort>();
                    }

                    // Adds only that port once in a container
                    if (!portsOnGivenContainer.contains(containerPort)) {
                        portsOnGivenContainer.add(containerPort);
                    }

                    portMap.put(containerId, portsOnGivenContainer);
                }
            }
        }
    }

    /***
     * Search the local host for a container with the given name or ID
     *
     * @param targetContainerIdentifier
     * @param sourceContainerName A container can contain several names, the first one identifies
     *        the container, and the other ones identifies links between containers in the format
     *        /[Container that created the link]/[Target Container].
     * @return the container, NULL if no match is found.
     */
    public Container getContainerByNameOrId(final String targetContainerIdentifier, final String sourceContainerName) {
        Container targetContainer = null;

        String targetContainerName = targetContainerIdentifier.split(":")[0];

        search: for (Container container : containerList) {

            // Check if the ID Match:
            if (container.id().equals(targetContainerName)) {

                break search;
            }

            // Check if any of the container names matches
            List<String> containerNames = container.names();

            if ((containerNames != null) && (!containerNames.isEmpty())) {
                for (String name : containerNames) {
                    // Container Found
                    if (name.equals(sourceContainerName + targetContainerName)) {
                        targetContainer = container;
                        break search;
                    }
                }
            }
        }

        return targetContainer;
    }

    /**
     * @return the dockerClient
     */
    public DockerClient getDockerClient() {
        return dockerClient;
    }

    /**
     * @return the cAdvisorContainer
     */
    public Container getcAdvisorContainer() {
        return cAdvisorContainer;
    }

    /**
     * @return the jerseyClient
     */
    public Client getJerseyClient() {
        return jerseyClient;
    }

    /**
     * @return the machinePhysicialInformation
     */
    public JsonNode getMachinePhysicalInformation() {
        return machinePhysicalInformation;
    }

    /**
     * @return the machineStatistics
     */
    public JsonNode getMachineStatistics() {
        return machineStatistics;
    }

    /**
     * @return the containerStatisticsMap
     */
    public Map<String, JsonNode> getContainerStatisticsMap() {
        return containerStatisticsMap;
    }

    /***
     * Searches the host image list, by image id.
     *
     * Verifies if the given image is local on the given host
     *
     * @param baseImageId
     * @return true if image is local on given host, otherwise false.
     */
    public boolean hasImage(final String baseImageId) {
        boolean hasLocal = false;
        if (imageMap.get(baseImageId) != null) {
            hasLocal = true;
        }
        return hasLocal;
    }

    /**
     * @return the hasOSInformation
     */
    public boolean isOSInformationAvailable() {
        return osInformationAvailable;
    }

    /**
     * @return the osDistribution
     */
    public String getOsDistribution() {
        return osDistribution;
    }

    /**
     * @return the updateListOrCreateContainerLock
     */
    public Lock getUpdateListOrCreateContainerLock() {
        return updateListOrCreateContainerLock;
    }

    /***
     * This methods is used in order to locate what containers mount a given volume.
     *
     *
     * @return the volumeMap
     */
    public Map<Volume, List<String>> getVolumeMap() {
        return volumeMap;
    }
}
