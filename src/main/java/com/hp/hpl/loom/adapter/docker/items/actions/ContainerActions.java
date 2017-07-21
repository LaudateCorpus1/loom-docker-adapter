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
import com.hp.hpl.loom.adapter.docker.items.ContainerItem;
import com.hp.hpl.loom.adapter.docker.items.ContainerItemAttributes;
import com.hp.hpl.loom.model.ActionParameter;
import com.hp.hpl.loom.model.ActionParameters;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.RemoveContainerParam;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * Library that contains all the possible actions to be applied on Container individual ItemTypes
 * and Base Aggregations
 */
public final class ContainerActions {

    private static final Log LOG = LogFactory.getLog(ContainerActions.class);

    /***
     * Private constructor to avoid instantiation of an object of this class
     */
    private ContainerActions() {}

    /***
     * Stores the result of a container inspection to a given file on the host file system. The host
     * is the machine that contains the given container.
     *
     * @param container
     * @param containerAttributes
     * @param actionParameters
     * @return true is success, otherwise false.
     */

    // public static boolean storeInspection(final ContainerItem container, final
    // ContainerItemAttributes containerAttributes, final ActionParameters actionParameters) {
    //
    // boolean actionStatus = false;
    //
    // for (ActionParameter parameter : actionParameters) {
    //
    // if (parameter.getId().equals("path")) {
    //
    // final String inspectionStorePath = parameter.getValue();
    //
    // if ((inspectionStorePath != null) && (!inspectionStorePath.isEmpty())) {
    //
    // ContainerInfo inspection = HostManager.getInstance().locateHost(container)
    // .inspectContainer(containerAttributes.getItemId());

    // Store to file: File outputFile = new File(inspectionStorePath);

    // ObjectMapper mapper = new ObjectMapper();

    // print pretty: mapper.enable(SerializationFeature.INDENT_OUTPUT);

    // try { mapper.writeValue(outputFile, inspection); } catch (IOException e) { if
    // (LOG.isDebugEnabled()) { LOG.debug("Could not inspect container " +
    // containerAttributes.getItemId()); } e.printStackTrace(); }
    //
    // actionStatus = true; } } } return actionStatus; }


    /***
     * Stops the selected container
     *
     * @param container
     * @param containerAttributes
     * @param actionParameters
     * @return true if success, otherwise false.
     */
    public static boolean stop(final ContainerItem container, final ContainerItemAttributes containerAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;
        for (ActionParameter parameter : actionParameters) {
            if (parameter.getValue().toLowerCase().equals("yes")) {

                try {
                    DockerClient dockerClient = HostManager.getInstance().locateHost(container).getDockerClient();

                    dockerClient.stopContainer(containerAttributes.getContainerId(), new Integer(2));

                } catch (NullPointerException | DockerException | InterruptedException e) {
                    LOG.error("Could not stop container " + containerAttributes.getItemId(), e);
                }

                actionStatus = true;
            }
        }
        return actionStatus;
    }

    /***
     * Starts the selected container
     *
     * @param container
     * @param containerAttributes
     * @param actionParameters
     * @return true if success, otherwise false.
     */
    public static boolean start(final ContainerItem container, final ContainerItemAttributes containerAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;
        for (ActionParameter parameter : actionParameters) {
            if (parameter.getValue().toLowerCase().equals("yes")) {

                // Cannot start a container that is already running.
                if (!containerAttributes.getStatus().toLowerCase().contains("up")) {

                    DockerClient dockerClient = HostManager.getInstance().locateHost(container).getDockerClient();

                    try {
                        dockerClient.startContainer(containerAttributes.getContainerId());
                    } catch (DockerException | InterruptedException e) {
                        LOG.error("Could not start container " + containerAttributes.getItemId(), e);
                    }
                }

                actionStatus = true;
            }
        }
        return actionStatus;
    }

    /***
     * Restarts the selected container.
     *
     * @param container
     * @param containerAttributes
     * @param actionParameters
     * @return true if success, otherwise false.
     */
    public static boolean restart(final ContainerItem container, final ContainerItemAttributes containerAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;
        for (ActionParameter parameter : actionParameters) {
            if (parameter.getValue().toLowerCase().equals("yes")) {
                DockerClient dockerClient = HostManager.getInstance().locateHost(container).getDockerClient();

                try {
                    dockerClient.restartContainer(containerAttributes.getContainerId());
                } catch (DockerException | InterruptedException e) {
                    LOG.error("Could not restart container " + containerAttributes.getItemId(), e);
                }

                actionStatus = true;
            }
        }

        return actionStatus;
    }

    /***
     * Stores the containers logs in the given file, contained in the Host file system. The Host is
     * the machine that contains a given container.
     *
     * @param container
     * @param containerAttributes
     * @param actionParameters
     * @return true is success, otherwise false.
     */

    // @SuppressWarnings("checkstyle:linelength") public static boolean storelogs(final Provider
    // provider, final ContainerItem container, final ContainerItemAttributes containerAttributes,
    // final ActionParameters actionParameters) { boolean actionStatus = false;
    //
    // for (ActionParameter parameter : actionParameters) {
    //
    // if (parameter.getId().equals("path")) {
    //
    // final String logPath = parameter.getValue();
    //
    // if ((logPath != null) && !logPath.isEmpty()) {
    // container.getFirstConnectedItemWithRelationshipName(
    // RelationshipUtil.getRelationshipNameBetweenLocalTypeIdsWithRelType(provider.getProviderType
    // (), Types.HOST_TYPE_ID, Types.CONTAINER_TYPE_ID, "runs"));
    //
    // DockerClient dockerclient =
    // HostManager.getInstance().locateHost(container).getDockerClient();
    //
    // File outputFile = new File(logPath);
    //
    // LogStream containerLog; try { containerLog =
    // dockerclient.logs(containerAttributes.getItemId(), LogsParameter.TIMESTAMPS); OutputStream
    // outputStream = new FileOutputStream(outputFile); containerLog.attach(outputStream,
    // outputStream);
    //
    // actionStatus = true; } catch (DockerException | InterruptedException | IOException e) { if
    // (LOG.isDebugEnabled()) { LOG.debug("Problem reading logs" + containerAttributes.getItemId());
    // } e.printStackTrace(); }
    //
    // } } } return actionStatus; }


    /***
     * Removes the container. In the case the container is running, first it stops the container and
     * then removes it.
     *
     * @param container
     * @param containerAttributes
     * @param actionParameters
     * @return true if container was successfully removed.
     */
    public static boolean remove(final ContainerItem container, final ContainerItemAttributes containerAttributes,
            final ActionParameters actionParameters) {

        boolean actionStatus = false;
        for (ActionParameter parameter : actionParameters) {
            if (parameter.getValue().toLowerCase().equals("yes")) {

                try {
                    Host localHost = HostManager.getInstance().locateHost(container);
                    DockerClient dockerClient = localHost.getDockerClient();

                    dockerClient.removeContainer(containerAttributes.getContainerId(),
                            RemoveContainerParam.removeVolumes(), RemoveContainerParam.forceKill());

                    // Removes the item from the host cache
                    localHost.removeContainerInspectionFromCache(containerAttributes.getContainerId());

                } catch (NullPointerException | DockerException | InterruptedException e) {
                    LOG.error("Could not remove container " + containerAttributes.getItemId(), e);
                }

                actionStatus = true;
            }
        }

        return actionStatus;
    }
}
