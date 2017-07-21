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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.loom.adapter.docker.distributed.realworld.Host;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.HostManager;
import com.hp.hpl.loom.adapter.docker.items.ImageItem;
import com.hp.hpl.loom.adapter.docker.items.ImageItemAttributes;
import com.hp.hpl.loom.model.ActionParameter;
import com.hp.hpl.loom.model.ActionParameters;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;

/**
 * Library that contains all the possible actions to be applied on Images individual ItemTypes and
 * Base Aggregations
 */
public final class ImageActions {

    private static final Log LOG = LogFactory.getLog(ImageActions.class);

    private ImageActions() {}

    static final int MAX_NUMBER_OF_CREATED_CONTAINERS = 20;
    static final int MIN_NUMBER_OF_CREATED_CONTAINERS = 0;

    /***
     * Starts a container, with the chosen command, from the selected Image. It always deploys in
     * the host with less containers. The user may choose the number fo containers to be deployed.
     * Should an incorrect value be entered, the default value will be assumed. The default value is
     * one container.
     *
     * @param image
     * @param imgAttributes
     * @param actionParameters
     * @return true if success, otherwise false
     */
    public static boolean launchContainer(final ImageItem image, final ImageItemAttributes imgAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;

        String imageName = imgAttributes.getImageid();
        /*
         * if the command is empty, it will create a container if the default image file command or
         * /bin/bash
         */
        String command = "";
        int numberOfContainers = 1;

        for (ActionParameter parameter : actionParameters) {
            if (parameter.getId().equals("containercommand")) {
                command = parameter.getValue();
            }

            if (parameter.getId().equals("numberofcontainers")) {
                boolean validNumber = true;
                try {
                    numberOfContainers = Integer.parseInt(parameter.getValue());
                    // check if the value is in an acceptable range.
                    if (numberOfContainers <= MIN_NUMBER_OF_CREATED_CONTAINERS
                            || numberOfContainers > MAX_NUMBER_OF_CREATED_CONTAINERS) {
                        validNumber = false;
                    }
                } catch (NumberFormatException e) {
                    validNumber = false;
                    // TODO this should really be an action error and return at this point
                    LOG.warn("Couldn't determine number of containers from '" + parameter.getValue()
                            + "', defaulting to 1");
                }

                if (!validNumber) {
                    numberOfContainers = 1;
                }
            }
        }

        /**
         * Choose on which host should it run the container. The current metric is only the host
         * that runs less containers
         */
        int minContainerOnHost = Integer.MAX_VALUE;
        Host designatedHost = null;

        for (Host currentHost : HostManager.getInstance().locateHosts(image)) {
            if (currentHost.getLocalContainers().size() < minContainerOnHost) {
                designatedHost = currentHost;
                minContainerOnHost = currentHost.getLocalContainers().size();
            }
        }

        DockerClient dClient = designatedHost.getDockerClient();

        final ContainerConfig config = ContainerConfig.builder().image(imageName).cmd(command.split(" "))
                .attachStdin(true).attachStdout(true).build();

        for (int i = 0; i < numberOfContainers; i++) {
            ContainerCreation container;
            try {
                container = dClient.createContainer(config);
                dClient.startContainer(container.id());

                // Updates the inspection cache:
                designatedHost.updateContainerInspectionCache(container.id());

                actionStatus = true;
            } catch (DockerException | InterruptedException e) {
                LOG.error("Couldn't create container based on image " + imgAttributes.getImageid() + " on host "
                        + dClient.getHost());
            }

            actionStatus = false;
        }

        return actionStatus;
    }

    /***
     * Deletes the select image from the Host.
     *
     * @param image
     * @param imgAttributes
     * @param actionParameters
     * @return true if success, otherwise false
     */
    /*
     * public static boolean deleteImage(final ImageItem image, final ImageItemAttributes
     * imgAttributes, final ActionParameters actionParameters) { boolean actionStatus = false; for
     * (ActionParameter parameter : actionParameters) { if (!parameter.getValue().isEmpty()) { if
     * (parameter.getValue().toLowerCase().equals("yes")) { DockerClient dClient =
     * HostManager.getInstance().locateHost(image).getDockerClient(); try {
     * dClient.removeImage(imgAttributes.getImageid()); actionStatus = true; } catch
     * (DockerException | InterruptedException e) { if (LogManager.getRootLogger().isDebugEnabled())
     * { LogManager.getRootLogger() .debug("Could not remove the image: " +
     * imgAttributes.getImageid()); } e.printStackTrace(); actionStatus = false; } } } } return
     * actionStatus; }
     */
}
