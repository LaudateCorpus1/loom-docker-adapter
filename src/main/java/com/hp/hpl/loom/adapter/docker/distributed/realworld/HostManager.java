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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.docker.items.ContainerItem;
import com.hp.hpl.loom.adapter.docker.items.ImageItem;
import com.hp.hpl.loom.adapter.docker.items.PortItem;
import com.hp.hpl.loom.adapter.docker.items.Types;
import com.hp.hpl.loom.adapter.docker.items.VolumeItem;
import com.hp.hpl.loom.adapter.docker.realworld.ContainerPort;
import com.hp.hpl.loom.adapter.docker.realworld.Registry;
import com.hp.hpl.loom.model.Item;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Image;

/***
 * Singleton class. Manages the hosts running Docker daemons. All hosts running daemons are listed
 * inside hostFile property, on docker.properties file
 */
public final class HostManager {
    private static final Log LOG = LogFactory.getLog(HostManager.class);

    // since there is only one HostManager, the objects of this class should be a singleton.
    private static HostManager instance = null;

    // Hash map used to increase string search. It is used in threads while the hosts are being
    // initialised.
    private Map<String, Host> hostMap = new ConcurrentHashMap<String, Host>();

    // Image Private registries
    private ConcurrentMap<Registry, List<Image>> imageOnEachRegistry = new ConcurrentHashMap<Registry, List<Image>>();

    /***
     * Thread pool used for Registry retrieves.
     */
    private ExecutorService registryExtractPool;

    private HostManager(final BaseAdapter adapter) {

        registryExtractPool = Executors.newCachedThreadPool();

        ExecutorService startManagerThread = Executors.newSingleThreadExecutor();

        /*
         * The Threads prevents that hosts that are being initialised try to invoke the HostManager
         * Singleton and get a NULL pointer, since it did not finish the constructor before being
         * invoked.
         */
        startManagerThread.submit(() -> {
            hostMap = retrieveAndInitializeHostList(adapter);
            // Locate registries
            locateAndAddPrivateRegistries(adapter);
        });
    }

    private HostManager() {}

    /***
     * Returns an instance of the HostManager Class.
     *
     * @return HostManager singleton object
     */
    public static HostManager getInstance(final BaseAdapter adapter) {
        synchronized (HostManager.class) {
            if (instance == null) {
                instance = new HostManager(adapter);
            }

            return instance;
        }
    }

    /***
     * Returns an instance of the HostManager Class. This should be only called after a call of
     * getInstance(final BaseAdapter adapter), otherwise a null point will be returned.
     *
     * @return HostManager singleton object
     */
    public static HostManager getInstance() {
        if (instance == null) {
            LOG.error("Failed to find HostManager", new RuntimeException("Host manager instance is null"));
        }
        return instance;
    }

    /***
     * Generates a Map with all hosts available in the system.
     *
     * @param adapter
     * @return
     */
    @SuppressWarnings({"checkstyle:emptyblock", "checkstyle:avoidnestedblocks"})
    private Map<String, Host> retrieveAndInitializeHostList(final BaseAdapter adapter) {
        Map<String, Host> hosts = new ConcurrentHashMap<String, Host>();

        Object hostFileName = adapter.getAdapterConfig().getPropertiesConfiguration().getProperty("hostFile");

        if (!StringUtils.isEmpty(hostFileName)) {
            String fileName = (String) hostFileName;

            // get information from JSON file
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;

            try {
                rootNode = mapper.readTree(new File(fileName));
                JsonNode jsonHosts = rootNode.get("Hosts");

                ExecutorService es = Executors.newCachedThreadPool();

                StopWatch stopWatch = new StopWatch("Initialize Host List");

                stopWatch.start("Initialize Host List");

                for (JsonNode jsonHost : jsonHosts) {

                    // Initialise Host on a thread
                    es.execute(() -> {
                        try {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Attempting connection to host '" + jsonHost + "'");
                            }

                            Host extractedHost = new Host(jsonHost, adapter);

                            hosts.put(extractedHost.getUID(), extractedHost);
                        } catch (Exception e) {
                            LOG.error("Error connecting to host '" + jsonHost.toString() + "'", e);

                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Removing host '" + jsonHost.toString() + "' from Host Pool");
                            }
                        }
                    });
                }

                es.shutdown();

                if (waitAllHostInitializations(adapter)) {
                    // Waits forever until all hosts initialise:
                    es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                }

                stopWatch.stop();

                if (LogManager.getRootLogger().isDebugEnabled()) {
                    LogManager.getRootLogger().debug("Docker adaptor: Hosts initialisation sucessfull!");
                }

                LOG.info(stopWatch.prettyPrint());
            } catch (JsonProcessingException e) {
                // JSON structure error.
                LOG.error("Couldn't process JSON response", e);
            } catch (IOException e) {
                // I/O error.
                LOG.error("Couldn't process JSON response", e);
            } catch (InterruptedException e) {
                // Wait interrupted
                LOG.error("Failed to retrieve all Host information - wait interrupted", e);
            }
        }

        return hosts;
    }

    /***
     * Using the hostLocalId, search in the host map for the given host
     *
     * @param hostLocalId
     * @return host, null if the host was not found.
     */
    public Host locateHostByUID(final String hostLocalId) {
        // Extract host UID from Host LocalID
        String[] parts = hostLocalId.split("http");
        // http was removed in the previous split.
        String hostUID = new String("http") + parts[parts.length - 1];

        return hostMap.get(hostUID);
    }

    /***
     * Using the host name, search in the host map for the given host
     *
     * @param hostLocalId
     * @return host, null if the host was not found.
     */
    public Host locateHostByNameSimilarity(final String hostName) {
        String locatedHost = null;

        try {
            locatedHost = hostMap.keySet().stream()
                    .filter(iterHost -> iterHost.toLowerCase().contains(hostName.toLowerCase())).findFirst().get();
        } catch (NoSuchElementException e) {
            LOG.error("Couldn't find host similar to '" + hostName + "' in set of " + hostMap.size() + " hosts", e);
        }

        if (locatedHost != null) {
            return hostMap.get(locatedHost);
        } else {
            return null;
        }
    }

    /***
     * Using the the ContainerItem relationships, search in the host map for the given host
     *
     * @param ContainerItem that has relationships to a host.
     * @return host, null if the host was not found.
     */
    public Host locateHost(final ContainerItem container) {
        List<Item> connectedItems = new ArrayList<Item>(container.getAllConnectedItems());

        for (Item item : connectedItems) {
            if (item.getItemType().getLocalId().equals(Types.HOST_TYPE_ID)) {
                return hostMap.get(item.getName());
            }
        }

        // No host found.
        return null;
    }

    /***
     * Using the the ImageItem relationships, search in the hosts maps, for all hosts that have
     * local a given image.
     *
     * @param ItemItem that has relationships to a host.
     * @return host, null if the host was not found.
     */
    public List<Host> locateHosts(final ImageItem image) {
        List<Item> connectedItems = new ArrayList<Item>(image.getAllConnectedItems());

        List<Host> localHostList = new ArrayList<Host>();

        for (Item item : connectedItems) {
            if (item.getItemType().getLocalId().equals(Types.HOST_TYPE_ID)) {
                localHostList.add(hostMap.get(item.getName()));
            }
        }

        return localHostList;
    }

    /***
     * Using the the PortItem relationships, search in the host map for the given host
     *
     * @param PortItem that has relationships to a host.
     * @return host, null if the host was not found.
     */
    public Host locateHost(final PortItem port) {
        String hostUID = port.getCore().getContainingHostUID();
        Host host = hostMap.get(hostUID);
        return host;
    }

    /***
     * Using the the VolumeItem relationships, search in the host map for the given host
     *
     * @param VolumeItem that has relationships to a host.
     * @return host, null if the host was not found.
     */
    public Host locateHost(final VolumeItem volume) {
        String hostUID = volume.getCore().getContainingHostUID();
        Host host = hostMap.get(hostUID);
        return host;
    }



    /***
     * Generates a list of hosts, from the original Map.
     *
     * @return list of hosts
     */
    public List<Host> getHostList() {
        List<Host> hostList = new ArrayList<Host>(hostMap.values());
        return hostList;
    }

    /***
     * Generates a List of containers available in all hosts
     *
     * @return Container List
     */
    public List<Container> getAllContainers() {
        List<Container> containerList = new ArrayList<Container>();
        if (hostMap != null && !hostMap.isEmpty()) {
            for (Host host : hostMap.values()) {
                containerList.addAll(host.getLocalContainers());
            }
        }

        return containerList;
    }

    /***
     * Generates a List of images available in all hosts
     *
     * @return list of images
     */
    public List<Image> getAllImages() {
        // Map is used in order to make sure that the images will only be referenced once.
        HashMap<String, Image> imageMap = new HashMap<String, Image>();

        if (hostMap != null && !hostMap.isEmpty()) {
            for (Host host : hostMap.values()) {
                host.getLocalImages().forEach(image -> {
                    if (imageMap.get(image.id()) == null) {
                        imageMap.put(image.id(), image);
                    }
                });
            }
        }

        return imageMap.values().stream().collect(Collectors.toList());
    }

    /***
     * Generates a list of ports available in all hosts
     *
     * @return list of ports
     */
    public List<ContainerPort> getAllPorts() {
        List<ContainerPort> portList = new ArrayList<ContainerPort>();
        if (hostMap != null && !hostMap.isEmpty()) {
            for (Host host : hostMap.values()) {
                portList.addAll(host.getAllPorts());
            }
        }
        return portList;
    }

    /***
     * List of volumes available in all host
     *
     * @return list of volumes
     */
    public List<Volume> getAllVolumes() {
        List<Volume> volumeList = new ArrayList<Volume>();
        if (hostMap != null && !hostMap.isEmpty()) {
            for (Host host : hostMap.values()) {
                volumeList.addAll(host.getLocalVolumes());
            }
        }
        return volumeList;
    }

    /***
     * Every single time that hostUpdater.getIterator() is called, the host database is also
     * updated. This is a coherence feature: since the host updater runs before any other updater,
     * the entities created by the other updaters will already have been referenced by the HostType
     * items on setRelationships method (if the case). Furthermore, this makes sure that the
     * complete Container and Image database of a given host is updated on every update cycle . In
     * other words, acts as a synchronous barrier.
     *
     * <p>
     * Calls <strong>refreshContainerDatabase()</strong> and <strong>refreshImageDatabase()</strong>
     * on each host
     */
    public void refreshHostsInformation() {
        StopWatch stopWatch = new StopWatch("Refresh");
        stopWatch.start("Refresh Host Content");

        getHostList().forEach(host -> {
            try {
                host.refreshHostContent(stopWatch);
            } catch (Exception e) {
                LOG.error("Could not refresh host '" + host.getUID() + "'", e);
            }
        });

        stopWatch.stop();
        LOG.info(stopWatch.prettyPrint());
    }

    /***
     * For some applications and examples, it may be desirable to wait all the hosts to be ready
     * until information is provided to Loom. This is configurable on the docker.properties file
     * with the property waitAllHostInitializations. If true, Loom waits all hosts to be initialised
     * (or aborted) to proceed. If not defined, the default value is false.
     *
     * @return if the initialisation of all hosts should be waited or not.
     */
    private boolean waitAllHostInitializations(final BaseAdapter adapter) {
        Object waitHosts =
                adapter.getAdapterConfig().getPropertiesConfiguration().getProperty("waitAllHostInitializations");

        boolean waitEnabled = false;

        if (!StringUtils.isEmpty(waitHosts)) {
            waitEnabled = Boolean.parseBoolean((String) waitHosts);
        }
        return waitEnabled;
    }

    private void locateAndAddPrivateRegistries(final BaseAdapter adapter) {

        // By default there is always the docker Registry.
        Registry dockerRepo = new Registry(Registry.DOCKER_HUB_REGISTRY_NAME, Registry.DOCKER_HUB_REGISTRY_ADDRESS,
                Registry.DOCKER_HUB_REGISTRY_PORT);

        imageOnEachRegistry.put(dockerRepo, new ArrayList<Image>());

        // extract other registries from the inputfile.
        // Load hostFile
        Object hostFileName = adapter.getAdapterConfig().getPropertiesConfiguration().getProperty("hostFile");

        if (!StringUtils.isEmpty(hostFileName)) {
            String fileName = (String) hostFileName;

            // get information from JSON file
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;

            // Read list of private registries
            try {
                rootNode = mapper.readTree(new File(fileName));

                JsonNode jsonregistries = rootNode.get("PrivateRegistries");

                // private registries are optional, so it can return null
                if (jsonregistries != null) {
                    for (JsonNode jsonRepos : jsonregistries) {

                        // Launch a thread per Registry to retrieve information.
                        registryExtractPool.execute(() -> this.extractRegistryInformation(jsonRepos));
                    }
                }
            } catch (JsonProcessingException e) {
                // JSON structure error.
                LOG.error("Couldn't process JSON response", e);
            } catch (IOException e) {
                // I/O error.
                LOG.error("Couldn't process JSON response", e);
            }
        }
    }

    /***
     * Extracts the Registry information
     *
     * @param jsonRepos
     */
    private void extractRegistryInformation(final JsonNode jsonRepos) {
        // Load Registry information:

        // extract Registry name, address and port:
        String repoName = jsonRepos.get("name").textValue();
        String repoAddress = jsonRepos.get("address").textValue();
        String repoPort = jsonRepos.get("port").textValue();

        // Only register the Registry if no information is missing.
        if (repoName != null && !repoName.isEmpty() && repoAddress != null && !repoAddress.isEmpty() && repoPort != null
                && !repoPort.isEmpty()) {

            Registry extractedRegistry = new Registry(repoName, repoAddress, repoPort);

            List<Image> imageList = extractRegistryImages(jsonRepos);

            // Append to extracted registries List:
            imageOnEachRegistry.putIfAbsent(extractedRegistry, imageList);
        }
    }


    /***
     * Register new registry based on image information
     *
     * @param registryInfo the registry that the image belongs to
     * @param regImage the image that reported the registry
     * @return the registry it just registered.
     */
    public Registry registerRegistry(final String registryInfo, final Image regImage) {
        List<Image> imageList = null;
        Registry returnedRegistry;

        final Registry reportedRegistry = new Registry(registryInfo);

        // Search if a registry with the same name has already been reported.
        Optional<Registry> registry = imageOnEachRegistry.keySet().stream()
                .filter(reg -> reg.getUID().equals(reportedRegistry.getUID())).findFirst();

        if (registry.isPresent()) {
            returnedRegistry = registry.get();
            imageList = imageOnEachRegistry.get(returnedRegistry);
        } else {
            returnedRegistry = reportedRegistry;
            imageList = new ArrayList<>();
        }

        imageList.add(regImage);

        imageOnEachRegistry.put(returnedRegistry, imageList);

        return returnedRegistry;
    }

    @SuppressWarnings("checkstyle:todocomment")
    private List<Image> extractRegistryImages(final JsonNode jsonRepos) {
        List<Image> imageList = new ArrayList<Image>();

        // TODO: fetch image list from private registry

        return imageList;
    }

    /**
     * @return the imageOnEachRegistry
     */
    public ConcurrentMap<Registry, List<Image>> getImageOnEachRegistry() {
        return imageOnEachRegistry;
    }
}
