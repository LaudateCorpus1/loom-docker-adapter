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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.loom.adapter.AggregationUpdater;
import com.hp.hpl.loom.adapter.AggregationUpdaterBasedItemCollector;
import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.docker.items.ContainerItem;
import com.hp.hpl.loom.adapter.docker.items.ContainerItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.HostItem;
import com.hp.hpl.loom.adapter.docker.items.HostItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.ImageItem;
import com.hp.hpl.loom.adapter.docker.items.ImageItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.Types;
import com.hp.hpl.loom.adapter.docker.items.VolumeItem;
import com.hp.hpl.loom.adapter.docker.items.VolumeItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.actions.ContainerActions;
import com.hp.hpl.loom.adapter.docker.items.actions.HostActions;
import com.hp.hpl.loom.adapter.docker.items.actions.ImageActions;
import com.hp.hpl.loom.adapter.docker.items.actions.VolumeActions;
import com.hp.hpl.loom.exceptions.InvalidActionSpecificationException;
import com.hp.hpl.loom.exceptions.NoSuchItemTypeException;
import com.hp.hpl.loom.exceptions.NoSuchProviderException;
import com.hp.hpl.loom.manager.adapter.AdapterManager;
import com.hp.hpl.loom.model.Action;
import com.hp.hpl.loom.model.ActionParameters;
import com.hp.hpl.loom.model.ActionResult;
import com.hp.hpl.loom.model.Aggregation;
import com.hp.hpl.loom.model.Credentials;
import com.hp.hpl.loom.model.Item;
import com.hp.hpl.loom.model.Session;

@SuppressWarnings("unchecked")
/***
 * Provides a Collector for the Docker Distributed Adapter. The Docker Distributed Adaptor creates
 * only one collector object. The collector may have several ItemType Updater instance, that are
 * ItemType specific.
 */
public class DockerDistributedCollector extends AggregationUpdaterBasedItemCollector {
    private ContainerItemUpdater containerItemUpdater;

    private boolean global = false;
    private ImageItemUpdater imageItemUpdater; // created for the demo. Delete later.

    private RegistryItemUpdater registryItemUpdater;

    /**
     * Constructor it takes a client session, adapter and adapter Manager to register back with.
     *
     * @param session - Client session
     * @param adapter - base adapter (the docker adapter)
     * @param adapterManager adapterManager to register ourselves with
     */
    public DockerDistributedCollector(final Session session, final BaseAdapter adapter,
            final AdapterManager adapterManager) {
        super(session, adapter, adapterManager);
    }

    /***
     * A specific subclass creates its own flavours of AggregationUpdaters by implementing the
     * factory method getAggregationUpdater(aggregation) to create the appropriate updater per
     * ItemType. A helper method called aggregationMatchesItemType(aggregation, itemTypeLocalId)
     * returning a boolean is provided to simplify selecting the right AggregationUpdater flavour.
     *
     * @throws No ItemType found for the given aggregation
     * @return DockerLocal adaptor special AggregationUpdater
     */
    @Override
    protected AggregationUpdater<?, ?, ?> getAggregationUpdater(final Aggregation aggregation)
            throws NoSuchProviderException, NoSuchItemTypeException {

        if (aggregationMatchesItemType(aggregation, Types.HOST_TYPE_ID)) {
            return new HostItemUpdater(aggregation, adapter, this);
        }
        if (aggregationMatchesItemType(aggregation, Types.REGISTRY_TYPE_ID)) {
            if (registryItemUpdater == null) {
                registryItemUpdater = new RegistryItemUpdater(aggregation, adapter, this);
            }
            return registryItemUpdater;
        }
        if (aggregationMatchesItemType(aggregation, Types.IMAGE_TYPE_ID)) {
            if (imageItemUpdater == null) {
                imageItemUpdater = new ImageItemUpdater(aggregation, adapter, this);
            }
            return imageItemUpdater;
        }
        if (aggregationMatchesItemType(aggregation, Types.CONTAINER_TYPE_ID)) {
            if (containerItemUpdater == null) {
                containerItemUpdater = new ContainerItemUpdater(aggregation, adapter, this);
            }
            return containerItemUpdater;
        }
        if (aggregationMatchesItemType(aggregation, Types.VOLUME_TYPE_ID)) {
            return new VolumeItemUpdater(aggregation, adapter, this);
        }
        if (aggregationMatchesItemType(aggregation, Types.PORT_TYPE_ID)) {
            return new PortItemUpdater(aggregation, adapter, this);
        }

        throw new NoSuchItemTypeException(aggregation.getTypeId());
    }

    /***
     * Each ItemType localId listed here leads to the creation and registration of the related
     * grounded aggregation.
     *
     * @ return list with Ground Aggregation ItemTypes
     */
    @Override
    protected Collection<String> getUpdateItemTypeIdList() {
        List<String> list = new ArrayList<String>();
        list.add(Types.HOST_TYPE_ID);
        list.add(Types.REGISTRY_TYPE_ID);
        list.add(Types.IMAGE_TYPE_ID);
        list.add(Types.CONTAINER_TYPE_ID);
        list.add(Types.VOLUME_TYPE_ID);
        list.add(Types.PORT_TYPE_ID);

        return list;
    }

    /***
     * Each ItemType localId listed here leads to the creation of an Aggregation, if it hasn’t been
     * created yet by belonging to the list returned by getUpdateItemTypeIdList(). This aggregation
     * is purely for internal purpose and is not registered with the Adapter Manager. Each one of
     * those ItemTypes leads to the creation of an AggregationUpdater object: the list returned by
     * this method is used to call in turn (on a single thread of execution) each
     * AggregationUpdater. In some cases data collection order matters, therefore this method should
     * be implemented so that an iterator will be order preserving.
     *
     * @return list of aggregation ItemTypes
     *
     */
    @Override
    protected Collection<String> getCollectionItemTypeIdList() {
        List<String> list = new ArrayList<String>();

        // For distributed implementation, all the hosts must be collected before, in order to find
        // all images.
        list.add(Types.HOST_TYPE_ID);
        list.add(Types.REGISTRY_TYPE_ID);
        list.add(Types.IMAGE_TYPE_ID);
        list.add(Types.CONTAINER_TYPE_ID);
        list.add(Types.VOLUME_TYPE_ID);
        list.add(Types.PORT_TYPE_ID);

        return list;
    }

    @Override
    /***
     * Actions are classified by ItemType and by collection size.
     */
    protected ActionResult doAction(final Action action, final String itemTypeId, final Collection<Item> items)
            throws InvalidActionSpecificationException {

        boolean actionResult = false;

        // Judge actions depending on item type:
        if (itemTypeId.equals(Types.CONTAINER_TYPE_ID)) {
            // Action for Items
            if (items.size() == 1) {
                actionResult = singleContainerActions(action, items);

            } else if (items.size() > 1) {
                // Action for Aggregations
                actionResult = multipleContainerActions(action, items);
            }

        } else if (itemTypeId.equals(Types.HOST_TYPE_ID)) {
            // Action for Items
            // No Host aggregations on this version of the adapter.
            if (items == null) {
                actionResult = hostThreadActions(action, items);
            } else {
                if (items.size() == 1) {
                    actionResult = singleHostActions(action, items);
                }
            }
        } else if (itemTypeId.equals(Types.IMAGE_TYPE_ID)) {
            // Action for Items
            if (items.size() == 1) {
                actionResult = singleImageActions(action, items);

            } else if (items.size() > 1) {
                // Action for Aggregations
                actionResult = multipleImageActions(action, items);
            }
        } else if (itemTypeId.equals(Types.VOLUME_TYPE_ID)) {
            // Action for Items
            if (items.size() == 1) {
                actionResult = singleVolumeActions(action, items);

            } else if (items.size() > 1) {
                // Action for Aggregations
                actionResult = multipleVolumeActions(action, items);
            }
        }

        if (actionResult) {

            return new ActionResult(ActionResult.Status.completed);

        } else {
            return new ActionResult(ActionResult.Status.aborted);
        }

    }


    /***
     * Actions that can be applied only to a single Container Item.
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean singleContainerActions(final Action action, final Collection<Item> items) {

        boolean actionStatus = false;

        ContainerItem container = (ContainerItem) items.toArray()[0];
        ContainerItemAttributes containerAttributes = container.getCore();

        ActionParameters actionParameters = action.getParams();

        // Action: Start
        if (action.getId().equals("start")) {

            actionStatus = ContainerActions.start(container, containerAttributes, actionParameters);
        }
        // Action: Stop
        if (action.getId().equals("stop")) {
            actionStatus = ContainerActions.stop(container, containerAttributes, actionParameters);
        }
        // Action: Restart
        if (action.getId().equals("restart")) {

            actionStatus = ContainerActions.restart(container, containerAttributes, actionParameters);
        }

        // Action: Remove
        if (action.getId().equals("removecontainer")) {
            actionStatus = ContainerActions.remove(container, containerAttributes, actionParameters);
        }

        return actionStatus;
    }

    /***
     * Actions that can be applied on more than one container.
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean multipleContainerActions(final Action action, final Collection<Item> items) {

        boolean actionStatus = false;

        ActionParameters actionParameters = action.getParams();

        for (Item itemObject : items) {
            ContainerItem container = (ContainerItem) itemObject;

            ContainerItemAttributes containerAttributes = container.getCore();

            // Action: Start
            if (action.getId().equals("start")) {

                actionStatus = ContainerActions.start(container, containerAttributes, actionParameters);
            } else if (action.getId().equals("stop")) {

                actionStatus = ContainerActions.stop(container, containerAttributes, actionParameters);
            } else if (action.getId().equals("restart")) {
                actionStatus = ContainerActions.restart(container, containerAttributes, actionParameters);
            } else if (action.getId().equals("removecontainer")) {
                actionStatus = ContainerActions.remove(container, containerAttributes, actionParameters);
            }
        }

        return actionStatus;
    }

    /***
     * Actions that be applied to a single host
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean singleHostActions(final Action action, final Collection<Item> items) {

        boolean actionStatus = false;

        HostItem host = (HostItem) items.toArray()[0];
        HostItemAttributes hostAttributes = host.getCore();

        ActionParameters actionParameters = action.getParams();

        // Action: createcontainer
        if (action.getId().equals("createcontainer")) {
            actionStatus = HostActions.createContainerWithHostItem(host, hostAttributes, actionParameters);

        }

        // Action: Start
        if (action.getId().equals("startallcontainers")) {

            actionStatus = HostActions.startAllContainers(host, hostAttributes, actionParameters);
        }
        // Action: stopallcontainers
        if (action.getId().equals("stopsallcontainers")) {

            actionStatus = HostActions.stopAllContainers(host, hostAttributes, actionParameters);
        }

        // Action: restartallcontainers
        if (action.getId().equals("restartsallcontainers")) {
            actionStatus = HostActions.restartAllContainers(host, hostAttributes, actionParameters);
        }

        // Action: downloadimage
        if (action.getId().equals("downloadimage")) {
            actionStatus = HostActions.downloadImage(host, hostAttributes, actionParameters);
        }

        // Action: deploycadvisor
        if (action.getId().equals("deploycadvisor")) {
            actionStatus = HostActions.deployCAdvisor(host, hostAttributes, actionParameters);
        }

        return actionStatus;
    }

    /***
     * Actions that be applied to host thread
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean hostThreadActions(final Action action, final Collection<Item> items) {
        boolean actionStatus = false;

        ActionParameters actionParameters = action.getParams();

        if (action.getId().equals("createcontainer")) {
            actionStatus = HostActions.createContainerThread(actionParameters);
        } else if (action.getId().equals("deploycadvisors")) {
            actionStatus = HostActions.deployCAdvisorContainersThread(actionParameters);
        } else if (action.getId().equals("terminatecadvisors")) {
            actionStatus = HostActions.terminateCAdvisorContainersThread(actionParameters);
        } else if (action.getId().equals("createcontainersperhost")) {
            actionStatus = HostActions.createContainersPerHostThread(actionParameters);
        } else if (action.getId().equals("terminatecontainers")) {
            actionStatus = HostActions.terminateContainersThread(actionParameters);
        }

        return actionStatus;
    }

    /***
     * Actions that may be applied only in a single ImageType Item.
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean singleImageActions(final Action action, final Collection<Item> items) {
        boolean actionStatus = false;

        ImageItem image = (ImageItem) items.toArray()[0];
        ImageItemAttributes imgAttributes = image.getCore();

        ActionParameters actionParameters = action.getParams();

        // Action: deleteimage
        /*
         * if (action.getId().equals("deleteimage")) { actionStatus =
         * ImageActions.deleteImage(image, imgAttributes, actionParameters); }
         */
        // Action launchcontainer
        if (action.getId().equals("launchcontainer")) {

            actionStatus = ImageActions.launchContainer(image, imgAttributes, actionParameters);
        }
        return actionStatus;
    }

    /***
     * Actions that may be applied on several ImageType items.
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean multipleImageActions(final Action action, final Collection<Item> items) {
        boolean actionStatus = false;

        ActionParameters actionParameters = action.getParams();

        for (Item itemObject : items) {
            ImageItem image = (ImageItem) itemObject;
            ImageItemAttributes imgAttributes = image.getCore();

            // Action: deleteimage
            /*
             * if (action.getId().equals("deleteimage")) { actionStatus =
             * ImageActions.deleteImage(image, imgAttributes, actionParameters); }
             */
            // Action launchcontainer
            if (action.getId().equals("launchcontainer")) {

                actionStatus = ImageActions.launchContainer(image, imgAttributes, actionParameters);
            }
        }
        return actionStatus;
    }

    /***
     * Actions that can be applied on a single VolumeType Item.
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean singleVolumeActions(final Action action, final Collection<Item> items) {
        boolean actionStatus = false;

        VolumeItem volume = (VolumeItem) items.toArray()[0];
        VolumeItemAttributes volAttr = volume.getCore();

        ActionParameters actionParameters = action.getParams();

        // Action: copyfromlocal
        if (action.getId().equals("copyfromlocal")) {
            actionStatus = VolumeActions.copyFromLocal(volAttr, actionParameters);
        }

        return actionStatus;
    }

    /***
     * Actions that can be applied on multiple VolumeType Items.
     *
     * @param action
     * @param items
     * @return true if success, otherwise false
     */
    private boolean multipleVolumeActions(final Action action, final Collection<Item> items) {
        boolean actionStatus = false;

        action.getParams();

        for (Item itemObject : items) {
            VolumeItem volume = (VolumeItem) itemObject;
            volume.getCore();
        }

        return actionStatus;
    }

    /**
     * @return the containerItemUpdater
     */
    public ContainerItemUpdater getContainerItemUpdater() {
        return containerItemUpdater;
    }

    @Override
    public void setCredentials(final Credentials credentials) {
        if (credentials.getUsername().equals("admin")) {
            global = true;
        } else {
            global = false;
        }
    }

    @Override
    public boolean isGlobal() {
        return global;
    }

    /**
     * @return the imageItemUpdater
     */
    public ImageItemUpdater getImageItemUpdater() {
        return imageItemUpdater;
    }

    /**
     * @return the registryItemUpdater
     */
    public RegistryItemUpdater getRegistryItemUpdater() {
        return registryItemUpdater;
    }
}
