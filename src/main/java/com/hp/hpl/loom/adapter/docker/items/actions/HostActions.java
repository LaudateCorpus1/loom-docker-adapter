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
package com.hp.hpl.loom.adapter.docker.items.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StopWatch;

import com.hp.hpl.loom.adapter.docker.distributed.realworld.Host;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.HostManager;
import com.hp.hpl.loom.adapter.docker.items.HostItem;
import com.hp.hpl.loom.adapter.docker.items.HostItemAttributes;
import com.hp.hpl.loom.model.ActionParameter;
import com.hp.hpl.loom.model.ActionParameters;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.DockerClient.RemoveContainerParam;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.DockerRequestException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;

/**
 * Library that contains all the possible actions to be applied on Host individual ItemTypes and
 * Base Aggregations
 */
public final class HostActions {
    private static final int ACTION_MAX_THREADS = 50;
    private static final String IMAGE_GOOGLE_CADVISOR_LATEST = "google/cadvisor:latest";
    private static final int WAIT_LOCK_TIME_MS = 1000;
    private static final Log LOG = LogFactory.getLog(HostActions.class);

    private HostActions() {}

    /***
     * Downloads an image from the Docker repository to the Host
     *
     * @param hostAttributes
     * @param actionParameters
     * @return true if success, otherwise false
     */
    public static boolean downloadImage(final HostItem host, final HostItemAttributes hostAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;
        for (ActionParameter parameter : actionParameters) {
            if (!parameter.getValue().isEmpty()) {
                String imageName = parameter.getValue();

                DockerClient dClient =
                        HostManager.getInstance().locateHostByUID(hostAttributes.getItemId()).getDockerClient();

                // Checks if the image is local, otherwise downloads the image.
                List<Image> imageList = new ArrayList<Image>();

                try {
                    imageList = dClient.listImages();
                } catch (DockerException | InterruptedException e) {
                    LOG.error("Could not list images from host '" + dClient.getHost() + "'", e);
                }

                Image goalImage = null;

                // Search on all images
                searchLoop: for (Image image : imageList) {
                    // Search on all images repositories
                    for (String repoTag : image.repoTags()) {
                        if (repoTag.toLowerCase().contains(imageName)) {
                            // found image locally, stop the search
                            goalImage = image;
                            break searchLoop;
                        }
                    }
                }

                /* Image not found, download required */
                if (goalImage == null) {

                    try {
                        dClient.pull(imageName);
                    } catch (DockerException | InterruptedException e) {
                        LOG.error("Could not download image '" + imageName + "' on host '" + dClient.getHost() + "'",
                                e);
                    }

                    actionStatus = true;
                }
            }
        }

        return actionStatus;
    }

    /***
     * Restarts all containers in a given Host.
     *
     * @param hostAttributes
     * @param actionParameters
     * @return true if success, otherwise false
     */
    public static boolean restartAllContainers(final HostItem host, final HostItemAttributes hostAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;

        for (ActionParameter parameter : actionParameters) {
            if (!parameter.getValue().isEmpty()) {
                if (parameter.getValue().toLowerCase().equals("yes")) {

                    DockerClient dockerClient =
                            HostManager.getInstance().locateHostByUID(hostAttributes.getItemId()).getDockerClient();

                    // In this scenario, there is only sense to restart containers that are already
                    // running
                    try {
                        List<Container> containerList =
                                dockerClient.listContainers(ListContainersParam.allContainers(true));

                        for (Container container : containerList) {
                            try {
                                dockerClient.restartContainer(container.id());
                            } catch (DockerRequestException e) {
                                LOG.error("Could not restart container " + container.id() + " on host '"
                                        + dockerClient.getHost() + "'", e);
                            }
                        }
                    } catch (InterruptedException | DockerException e) {
                        LOG.error("Could not list containers on host '" + dockerClient.getHost() + "'", e);
                    }

                    actionStatus = true;
                }
            }
        }

        return actionStatus;
    }

    /***
     * Stops all containers in a given Host.
     *
     * @param hostAttributes
     * @param actionParameters
     * @return true if success, otherwise false
     */
    public static boolean stopAllContainers(final HostItem host, final HostItemAttributes hostAttributes,
            final ActionParameters actionParameters) {
        boolean actionStatus = false;

        for (ActionParameter parameter : actionParameters) {
            if (!parameter.getValue().isEmpty()) {
                if (parameter.getValue().toLowerCase().equals("yes")) {

                    // In this scenario, there is only sense to stop containers that are already
                    // running
                    DockerClient dClient =
                            HostManager.getInstance().locateHostByUID(hostAttributes.getItemId()).getDockerClient();

                    try {
                        List<Container> containerList = dClient.listContainers();

                        for (Container container : containerList) {
                            try {
                                dClient.stopContainer(container.id(), 2);
                            } catch (DockerRequestException e) {
                                LOG.error("Could not stop container " + container.id() + " on host '"
                                        + dClient.getHost() + "'", e);
                            }
                        }
                        actionStatus = true;
                    } catch (DockerException | InterruptedException e) {
                        LOG.error("Could not list containers on host '" + dClient.getHost() + "'", e);
                    }
                }
            }
        }

        return actionStatus;
    }

    /***
     * Starts all containers in a given Host
     *
     * @param hostAttributes
     * @param actionParameters
     * @return true if success, otherwise false
     */
    public static boolean startAllContainers(final HostItem host, final HostItemAttributes hostAttributes,
            final ActionParameters actionParameters) {
        boolean actionStatus = false;

        for (ActionParameter parameter : actionParameters) {
            if (parameter.getValue().toLowerCase().equals("yes")) {

                hostAttributes.getHostDockerDaemonAddress();

                DockerClient dClient =
                        HostManager.getInstance().locateHostByUID(hostAttributes.getItemId()).getDockerClient();

                try {
                    List<Container> containerList = dClient.listContainers(ListContainersParam.allContainers(true));

                    for (Container container : containerList) {
                        // Cannot start a container that is already running
                        if (!container.status().toLowerCase().contains("up")) {
                            try {
                                dClient.startContainer(container.id());
                            } catch (DockerRequestException e) {
                                LOG.error("Could not start container " + container.id() + " on host '"
                                        + dClient.getHost() + "'", e);
                            }
                        }
                    }
                } catch (DockerException | InterruptedException e) {
                    LOG.error("Could not list containers on host '" + dClient.getHost() + "'", e);
                }

                actionStatus = true;
            }
        }

        return actionStatus;
    }

    /***
     * Run a container from a selected Image. Should the designated image is not found in the Host,
     * it will download the image and then start a container from it.
     *
     * @param hostAttributes
     * @param actionParameters
     * @return
     */
    public static boolean createContainerWithHostItem(final HostItem host, final HostItemAttributes hostAttributes,
            final ActionParameters actionParameters) {
        boolean actionStatus = false;
        String imageName = "";

        // If the command is empty, the container will start with the default command, described in
        // the image or /bin/sh
        String command = "";

        for (ActionParameter parameter : actionParameters) {
            if (parameter.getId().equals("imagename")) {
                imageName = parameter.getValue();
            } else if (parameter.getId().equals("command")) {
                command = parameter.getValue();
            }
        }

        if (!imageName.isEmpty()) {

            // Need to become final to be used in the callback
            final String execCommand = command;
            final String imageUsed = imageName;
            final Host executedHost = HostManager.getInstance().locateHostByUID(hostAttributes.getItemId());

            actionStatus = createContainerWithHost(execCommand, imageUsed, executedHost);
        }

        return actionStatus;
    }

    public static boolean createContainerWithHost(final String execCommand, final String imageUsed,
            final Host executedHost) {

        boolean actionStatus = false;

        DockerClient dClient = executedHost.getDockerClient();

        // Checks if the image is local, otherwise downloads the image.
        List<Image> imageList = new ArrayList<Image>();
        try {
            imageList = dClient.listImages();
        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not list images on host '" + dClient.getHost() + "'", e);
        }

        Image goalImage = null;

        // Search on all images
        searchLoop: for (Image image : imageList) {
            // Search on all images repositories
            for (String repoTag : image.repoTags()) {
                if (repoTag.toLowerCase().contains(imageUsed.toLowerCase())) {
                    // found image locally, stop the search
                    goalImage = image;
                    break searchLoop;
                }
            }
        }

        if (goalImage == null) {
            ProgressHandler imageDownloadCallback = new ProgressHandler() {
                @Override
                public void progress(final ProgressMessage message) throws DockerException {
                    if (message.status() != null && message.status().contains("Downloaded newer image")) {
                        createContainerWithImageAvailable(execCommand, imageUsed, executedHost, dClient);
                    }
                }
            };

            try {
                dClient.pull(imageUsed, imageDownloadCallback);
                actionStatus = true;
            } catch (DockerException | InterruptedException e) {
                LOG.error("Could not download image '" + imageUsed + "' on host '" + dClient.getHost() + "'", e);
            }
        } else {
            actionStatus = createContainerWithImageAvailable(execCommand, imageUsed, executedHost, dClient);
        }

        return actionStatus;
    }


    /***
     * Once the image is already available on the host, creates the container.
     *
     * @param execCommand
     * @param imageUsed
     * @param executedHost
     * @param dClient
     */
    private static boolean createContainerWithImageAvailable(final String execCommand, final String imageUsed,
            final Host executedHost, final DockerClient dClient) {
        ContainerConfig config = ContainerConfig.builder().image(imageUsed).cmd(execCommand.split(" ")).build();
        ContainerCreation container;
        boolean success = false;

        while (!executedHost.getUpdateListOrCreateContainerLock().tryLock()) {
            // wait until the lock is released
            try {
                Thread.sleep(WAIT_LOCK_TIME_MS);
                // executedHost.getUpdateListOrCreateContainerLock().wait(WAIT_LOCK_TIME_MS);
            } catch (InterruptedException e) {
                LOG.warn("Couldn't obtain lock - retrying");
            }
        }

        // Lock acquired
        try {
            LOG.info("Creating container on host '" + dClient.getHost() + "' using image " + imageUsed);

            container = dClient.createContainer(config);
            dClient.startContainer(container.id());
            executedHost.updateContainerInspectionCache(container.id());
            success = true;
        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not launch container on host '" + dClient.getHost() + "' using command: " + execCommand,
                    e);
        } finally {
            executedHost.getUpdateListOrCreateContainerLock().unlock();
        }

        return success;
    }

    /***
     * Deploy a cAdvisor image on this container
     *
     * @param hostAttributes
     * @param actionParameters
     * @return
     */
    public static boolean deployCAdvisor(final HostItem host, final HostItemAttributes hostAttributes,
            final ActionParameters actionParameters) {
        boolean actionStatus = false;

        for (ActionParameter parameter : actionParameters) {
            if (parameter.getValue().toLowerCase().equals("yes")) {
                Host physicalHost = HostManager.getInstance().locateHostByUID(hostAttributes.getItemId());

                deployCAdvisor(physicalHost);
                actionStatus = true;
                break;
            }
        }

        return actionStatus;
    }

    private static boolean deployCAdvisor(final Host physicalHost) {
        final String cAdvisorImageName = new String(IMAGE_GOOGLE_CADVISOR_LATEST);
        final String volume1 = "/:/rootfs:ro";
        final String volume2 = "/var/run:/var/run:rw";
        final String volume3 = "/sys:/sys:ro";
        final String volume4 = "/var/lib/docker/:/var/lib/docker:ro";
        boolean actionStatus = true;

        // if there is already a cAdvisor running on the host, do not launch another one
        if (!physicalHost.hasCAdvisorRunning()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deploying cAdvisor on host '" + physicalHost.getDaemonIp() + "'");
            }
            DockerClient dClient = physicalHost.getDockerClient();

            // Checks if the image is local, otherwise downloads the image.
            List<Image> imageList = new ArrayList<Image>();
            try {
                imageList = dClient.listImages();
            } catch (DockerException | InterruptedException e) {
                LOG.error("Could not list images on host '" + dClient.getHost() + "'", e);
                actionStatus = false;
            }

            Image goalImage = null;

            // Search on all images
            searchLoop: for (Image image : imageList) {
                // Search on all images repositories
                for (String repoTag : image.repoTags()) {
                    if (repoTag.toLowerCase().contains(cAdvisorImageName.toLowerCase())) {
                        // found image locally, stop the search
                        goalImage = image;
                        break searchLoop;
                    }
                }
            }

            /* Image not found, download required */
            if (goalImage == null) {
                ProgressHandler imageDownloadCallback = new ProgressHandler() {
                    @Override
                    public void progress(final ProgressMessage message) throws DockerException {
                        if (message.status() != null && message.status().contains("Downloaded newer image")) {
                            createCAdvisorContainer(physicalHost, cAdvisorImageName, volume1, volume2, volume3, volume4,
                                    dClient);
                        }
                    }
                };

                try {
                    dClient.pull(cAdvisorImageName, imageDownloadCallback);
                } catch (DockerException | InterruptedException e) {
                    LOG.error(
                            "Could not download image '" + cAdvisorImageName + "' on host '" + dClient.getHost() + "'",
                            e);
                    actionStatus = false;
                }
            } else {
                createCAdvisorContainer(physicalHost, cAdvisorImageName, volume1, volume2, volume3, volume4, dClient);
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("cAdvisor already running on host '" + physicalHost.getDaemonIp() + "'");
        }

        return actionStatus;
    }

    /***
     * Deploys cAdvisor on all hosts. If it is already running it skips the host. If it hits an
     * error, it continues onto the next host.
     *
     * @param actionParameters
     * @return
     */
    public static boolean deployCAdvisorContainersThread(final ActionParameters actionParameters) {
        ActionParameter parameter = actionParameters.get(0);
        boolean actionStatus = true;

        if (!parameter.getValue().isEmpty() && parameter.getValue().toLowerCase().equals("yes")) {
            LOG.info("Deploying cAdvisor on all Hosts");

            try {
                List<Host> hostList = HostManager.getInstance().getHostList();
                int numThreads = hostList.size() > ACTION_MAX_THREADS ? ACTION_MAX_THREADS : hostList.size();
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                CompletionService<Boolean> compService = new ExecutorCompletionService<>(executor);

                for (Host host : hostList) {
                    compService.submit(new DeployCAdvisorAction(host));
                }

                for (int i = 0; i < hostList.size(); ++i) {
                    Future<Boolean> future = compService.take();

                    if (!future.get()) {
                        actionStatus = false;
                    }
                }

                executor.shutdown();
            } catch (InterruptedException e) {
                LOG.error("Deploy cAdvisor interrupted", e);
            } catch (ExecutionException e) {
                LOG.error("Failed to complete cAdvisor deployment", e);
            }
        } else {
            actionStatus = false;
        }

        return actionStatus;
    }

    /***
     * Terminates cAdvisor on all hosts. If it hits an error, it continues onto the next host.
     *
     * @param actionParameters
     * @return
     */
    public static boolean terminateCAdvisorContainersThread(final ActionParameters actionParameters) {
        ActionParameter parameter = actionParameters.get(0);
        boolean actionStatus = true;

        if (!parameter.getValue().isEmpty() && parameter.getValue().toLowerCase().equals("yes")) {
            LOG.info("Terminating cAdvisor on all Hosts");

            try {
                List<Host> hostList = HostManager.getInstance().getHostList();
                int numThreads = hostList.size() > ACTION_MAX_THREADS ? ACTION_MAX_THREADS : hostList.size();
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                CompletionService<Boolean> compService = new ExecutorCompletionService<>(executor);

                for (Host host : hostList) {
                    compService.submit(new TerminateCAdvisorAction(host));
                }

                for (int i = 0; i < hostList.size(); ++i) {
                    Future<Boolean> future = compService.take();

                    if (!future.get()) {
                        actionStatus = false;
                    }
                }

                executor.shutdown();
            } catch (InterruptedException e) {
                LOG.error("Terminate cAdvisor interrupted", e);
            } catch (ExecutionException e) {
                LOG.error("Failed to complete cAdvisor termination", e);
            }
        } else {
            actionStatus = false;
        }

        return actionStatus;
    }

    /***
     * Creates a new cAdvisorImage. This method is called once the image finishes downloading, or,
     * if the image is already local, straight away.
     *
     * @param host
     * @param cAdvisorImageName
     * @param volume1
     * @param volume2
     * @param volume3
     * @param volume4
     * @param dClient
     */
    private static void createCAdvisorContainer(final Host host, final String cAdvisorImageName, final String volume1,
            final String volume2, final String volume3, final String volume4, final DockerClient dClient) {

        final List<String> bindList = new ArrayList<String>();
        bindList.add(volume1);
        bindList.add(volume2);
        bindList.add(volume3);
        bindList.add(volume4);

        Map<String, List<PortBinding>> bindedPorts = new HashMap<String, List<PortBinding>>();
        final String port = "8080/tcp";
        final String hostIP = "";
        final String hostPort = "8080";

        PortBinding binding = PortBinding.of(hostIP, hostPort);
        List<PortBinding> bindingList = new ArrayList<PortBinding>();
        bindingList.add(binding);

        bindedPorts.put(port, bindingList);

        final HostConfig mountHostConfig = HostConfig.builder().portBindings(bindedPorts).binds(bindList).build();

        final ContainerConfig config =
                ContainerConfig.builder().hostConfig(mountHostConfig).image(cAdvisorImageName).build();
        // TODO
        // TEST

        ContainerCreation cAdvisorContainer;
        try {
            cAdvisorContainer = dClient.createContainer(config);
            dClient.startContainer(cAdvisorContainer.id());
            host.updateContainerInspectionCache(cAdvisorContainer.id());
        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not start container on host '" + dClient.getHost() + "'", e);
        }
    }

    /***
     * Chooses the Host based on hosts with less containers that have the image local. Should no
     * image is found, spins the container on the host with less containers.
     *
     * @param actionParameters
     * @return
     */
    public static boolean createContainerThread(final ActionParameters actionParameters) {

        // Note: currently there are no way to distinguish between group by threads. Thus
        // this action can happen in any host item.
        boolean actionStatus = false;

        String imageName = "";
        /*
         * if the command is empty, it will create a container if the default image file command or
         * /bin/bash
         */
        String command = "";
        List<Host> localImagesHost = new ArrayList<Host>();
        Host chosenHost;

        for (ActionParameter parameter : actionParameters) {
            if (parameter.getId().equals("imagename")) {
                imageName = parameter.getValue();
            } else if (parameter.getId().equals("command")) {
                command = parameter.getValue();
            }
        }

        if (!imageName.isEmpty()) {

            // In order to be used in the comparison, it needs to be final:
            final String imageNameFinal = imageName;

            List<Host> hostList = HostManager.getInstance().getHostList();

            // Find hosts with that image local
            localImagesHost = hostList.stream()
                    .filter(host -> host.getLocalImages().stream().anyMatch(
                            image -> new String(image.repoTags().get(0).replaceAll("<", "").replaceAll(">", ""))
                                    .contains(imageNameFinal)))
                    .collect(Collectors.toList());

            // No hosts have that image local
            if (localImagesHost.isEmpty()) {
                localImagesHost = hostList;
            }

            // Deploy the container in the host with less containers.
            Collections.sort(localImagesHost,
                    (h1, h2) -> Integer.compare(h1.getLocalContainers().size(), h2.getLocalContainers().size()));

            chosenHost = localImagesHost.get(0);

            if (chosenHost != null) {
                actionStatus = createContainerWithHost(command, imageName, chosenHost);
            }
        }

        return actionStatus;
    }

    /***
     * Chooses the Host based on hosts with less containers that have the image local. Should no
     * image is found, spins the container on the host with less containers.
     *
     * @param actionParameters
     * @return
     */
    public static boolean createContainersPerHostThread(final ActionParameters actionParameters) {
        boolean actionStatus = false;
        String imageName = "";
        /*
         * if the command is empty, it will create a container if the default image file command or
         * /bin/bash
         */
        String command = "";
        int number = 0;

        for (ActionParameter parameter : actionParameters) {
            if (parameter.getId().equals("imagename")) {
                imageName = parameter.getValue();
            } else if (parameter.getId().equals("command")) {
                command = parameter.getValue();
            } else if (parameter.getId().equals("number")) {
                number = Integer.valueOf(parameter.getValue());
            }
        }

        LOG.info("Creating " + number + " containers per host using " + imageName + " running command '" + command
                + "'");

        if (!imageName.isEmpty()) {
            StopWatch stopWatch = new StopWatch("Create Containers Per Host");

            stopWatch.start("Create all");

            try {
                List<Host> hostList = HostManager.getInstance().getHostList();
                int numThreads = hostList.size() > ACTION_MAX_THREADS ? ACTION_MAX_THREADS : hostList.size();
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                CompletionService<Boolean> compService = new ExecutorCompletionService<>(executor);

                for (Host host : hostList) {
                    compService.submit(new CreateContainerAction(number, command, imageName, host));
                }

                for (int i = 0; i < hostList.size(); ++i) {
                    Future<Boolean> future = compService.take();

                    if (!future.get()) {
                        actionStatus = false;
                    }
                }

                executor.shutdown();
            } catch (InterruptedException e) {
                LOG.error("Container creation interrupted", e);
            } catch (ExecutionException e) {
                LOG.error("Failed to complete container creation", e);
            }

            stopWatch.stop();
            LOG.info(stopWatch.prettyPrint());
        }

        return actionStatus;

    }

    /***
     * Terminates all containers (except cAdvisor) on all hosts. If it hits an error, it continues
     * onto the next host.
     *
     * @param actionParameters
     * @return
     */
    public static boolean terminateContainersThread(final ActionParameters actionParameters) {
        ActionParameter parameter = actionParameters.get(0);
        boolean actionStatus = true;

        if (!parameter.getValue().isEmpty() && parameter.getValue().toLowerCase().equals("yes")) {
            LOG.info("Terminating containers on all Hosts");

            StopWatch stopWatch = new StopWatch("Terminate Containers Per Host");

            stopWatch.start("Terminate all");

            try {
                List<Host> hostList = HostManager.getInstance().getHostList();
                int numThreads = hostList.size() > ACTION_MAX_THREADS ? ACTION_MAX_THREADS : hostList.size();
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                CompletionService<Boolean> compService = new ExecutorCompletionService<>(executor);

                for (Host host : hostList) {
                    compService.submit(new TerminateContainerAction(host));
                }

                for (int i = 0; i < hostList.size(); ++i) {
                    Future<Boolean> future = compService.take();

                    if (!future.get()) {
                        actionStatus = false;
                    }
                }

                executor.shutdown();
            } catch (InterruptedException e) {
                LOG.error("Container creation interrupted", e);
            } catch (ExecutionException e) {
                LOG.error("Failed to complete container termination", e);
            }

            stopWatch.stop();
            LOG.info(stopWatch.prettyPrint());
        } else {
            actionStatus = false;
        }

        return actionStatus;
    }

    private static final class CreateContainerAction implements Callable<Boolean> {
        private int numContainers;
        private String command;
        private String imageName;
        private Host host;

        CreateContainerAction(final int numContainers, final String command, final String imageName, final Host host) {
            this.numContainers = numContainers;
            this.command = command;
            this.imageName = imageName;
            this.host = host;
        }

        @Override
        public Boolean call() throws Exception {
            boolean result = true;

            for (int i = 0; i < numContainers; ++i) {
                if (!createContainerWithHost(command, imageName, host)) {
                    result = false;
                }
            }

            return result;
        }
    }

    private static final class TerminateContainerAction implements Callable<Boolean> {
        private Host host;

        TerminateContainerAction(final Host host) {
            this.host = host;
        }

        @Override
        public Boolean call() throws Exception {
            boolean result = true;
            DockerClient dockerClient = host.getDockerClient();

            try {
                List<Container> containerList = dockerClient.listContainers(ListContainersParam.allContainers(true));

                for (Container container : containerList) {
                    if (!container.image().equals(IMAGE_GOOGLE_CADVISOR_LATEST)) {
                        try {
                            LOG.info("Terminating container " + container.id() + " on host '" + dockerClient.getHost()
                                    + "'");

                            dockerClient.stopContainer(container.id(), 2);
                            dockerClient.removeContainer(container.id(), RemoveContainerParam.removeVolumes(),
                                    RemoveContainerParam.forceKill());
                        } catch (DockerRequestException e) {
                            LOG.error("Could not terminate container " + container.id() + " on host '"
                                    + dockerClient.getHost() + "'", e);
                            result = false;
                        }
                    }
                }
            } catch (InterruptedException | DockerException e) {
                LOG.error("Could not list containers on host '" + dockerClient.getHost() + "'", e);
            }

            return result;
        }
    }

    private static final class DeployCAdvisorAction implements Callable<Boolean> {
        private Host host;

        DeployCAdvisorAction(final Host host) {
            this.host = host;
        }

        @Override
        public Boolean call() throws Exception {
            return deployCAdvisor(host);
        }
    }

    private static final class TerminateCAdvisorAction implements Callable<Boolean> {
        private Host host;

        TerminateCAdvisorAction(final Host host) {
            this.host = host;
        }

        @Override
        public Boolean call() throws Exception {
            DockerClient dockerClient = host.getDockerClient();
            boolean result = true;

            try {
                List<Container> containerList = dockerClient.listContainers(ListContainersParam.allContainers(true));

                for (Container container : containerList) {
                    if (container.image().equals(IMAGE_GOOGLE_CADVISOR_LATEST)) {
                        try {
                            LOG.info("Terminating container " + container.id() + " on host '" + dockerClient.getHost()
                                    + "'");

                            dockerClient.stopContainer(container.id(), 2);
                            dockerClient.removeContainer(container.id(), RemoveContainerParam.removeVolumes(),
                                    RemoveContainerParam.forceKill());
                        } catch (DockerRequestException e) {
                            LOG.error("Could not terminate container " + container.id() + " on host '"
                                    + dockerClient.getHost() + "'", e);
                            result = false;
                        }
                    }
                }
            } catch (InterruptedException | DockerException e) {
                LOG.error("Could not list containers on host '" + dockerClient.getHost() + "'", e);
            }

            return result;
        }
    }
}
