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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hp.hpl.loom.adapter.AggregationUpdater;
import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.ConnectedItem;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.HostManager;
import com.hp.hpl.loom.adapter.docker.items.RegistryItem;
import com.hp.hpl.loom.adapter.docker.items.RegistryItemAttributes;
import com.hp.hpl.loom.adapter.docker.realworld.Registry;
import com.hp.hpl.loom.exceptions.NoSuchItemTypeException;
import com.hp.hpl.loom.exceptions.NoSuchProviderException;
import com.hp.hpl.loom.model.Aggregation;
import com.hp.hpl.loom.model.CoreItemAttributes.ChangeStatus;
import com.spotify.docker.client.messages.Image;

/***
 * Collects all registries, from all hosts.
 */
public class RegistryItemUpdater extends AggregationUpdater<RegistryItem, RegistryItemAttributes, Registry> {

    private static final String REGISTRY_DESCRIPTION = "Represents an image registry";

    protected DockerDistributedCollector dockerCollector = null;

    private Map<Registry, List<Image>> registriesAndImagesMap;

    // The reverse of the above map. Allows images to quickly find the registries that are
    // associated with them.
    private Map<String, List<Registry>> imageAndRegistriesMap = new HashMap<>();

    /**
     * Constructs a registryItemUpdater.
     *
     * @param aggregation The aggregation this update will update
     * @param adapter The baseAdapter this updater is part of
     * @param DockerDistributedCollector The collector it uses
     *
     * @throws NoSuchItemTypeException Thrown if the ItemType isn't found
     * @throws NoSuchProviderException thrown if adapter is not known
     */
    public RegistryItemUpdater(final Aggregation aggregation, final BaseAdapter adapter,
            final DockerDistributedCollector dockerCollector) throws NoSuchItemTypeException, NoSuchProviderException {
        super(aggregation, adapter, dockerCollector);
        this.dockerCollector = dockerCollector;
    }

    /**
     * Each observed resource should have a way to be identified uniquely within the given adapter's
     * domain and this is what should be returned here. This method is called to create the Item
     * logicalId.
     *
     * @return a unique way to identify a given resource (within the docker adapter). In the case of
     *         registries, is the host address and port.
     */
    @Override
    protected String getItemId(final Registry argRepo) {
        return argRepo.getUID();
    }

    /***
     * This must return a brand new Iterator every collection cycle giving access to all the
     * resources that AggregationUpdater is observing.
     *
     */
    @Override
    protected Iterator<Registry> getResourceIterator() {

        registriesAndImagesMap = HostManager.getInstance().getImageOnEachRegistry();

        // reverse the above map
        Map<String, List<Registry>> underUpdateImageAndRegistriesMap = new HashMap<>();

        for (Map.Entry<Registry, List<Image>> entry : registriesAndImagesMap.entrySet()) {
            // Put each image as key if is does not exist yet
            for (Image image : entry.getValue()) {
                List<Registry> registryList;

                // If the Image was not mentioned yet, the will be no value.
                registryList = underUpdateImageAndRegistriesMap.get(image.id());

                if (registryList == null) {
                    registryList = new ArrayList<Registry>();
                }

                // Add the registry to that image List.
                registryList.add(entry.getKey());
                underUpdateImageAndRegistriesMap.put(image.id(), registryList);
            }
        }

        // update it.
        imageAndRegistriesMap = underUpdateImageAndRegistriesMap;

        List<Registry> registries = registriesAndImagesMap.keySet().stream().collect(Collectors.toList());

        return registries.iterator();
    }

    @Override
    protected Iterator<Registry> getUserResourceIterator(final Collection<Registry> data) {
        return data.iterator();
    }

    /**
     * This method should return an Item only set with its logicalId and ItemType.
     */
    @Override
    protected RegistryItem createEmptyItem(final String logicalId) {
        RegistryItem item = new RegistryItem(logicalId, itemType);
        return item;
    }

    /**
     * This should return a newly created CoreItemAttributes object based on data observed from the
     * resource.
     */
    @Override
    protected RegistryItemAttributes createItemAttributes(final Registry resource) {

        RegistryItemAttributes repAttr = new RegistryItemAttributes();

        repAttr.setName(resource.getName());
        repAttr.setAddress(resource.getAddress());
        repAttr.setPort(resource.getPort());

        repAttr.setItemId(getItemId(resource));
        repAttr.setItemDescription(REGISTRY_DESCRIPTION);
        repAttr.setItemName(getItemId(resource));

        return repAttr;
    }

    /***
     * This method returns a status value encoded as follows:
     *
     * <p>
     * <strong>CoreItemAttributes.Status.UNCHANGED</strong>:there are no changes detected between
     * the previous view (the CoreItemsAttributes argument) and the new one (the Resource argument).
     * The selection of the attributes actually compared is left entirely at the adapter writer’s
     * discretion: for instance, our AggregationUpdater for an OpenStack volume checks the value of
     * the "status" attribute only.
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
     * @param ImageAttributes The Image Item Attributes
     * @param resource The image on docker-java API
     * @return
     */
    @Override
    protected ChangeStatus compareItemAttributesToResource(final RegistryItemAttributes registryAttributes,
            final Registry resource) {

        ChangeStatus status;

        // Image Registries information should not change during execution. They should be
        // considered immutable.
        status = ChangeStatus.CHANGED_IGNORE;

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
     * @param imageItem the image item that Items will be connected to
     * @param resource the docker-java API item, from where the connections will be extracted.
     */
    @Override
    protected void setRelationships(final ConnectedItem registryItem, final Registry resource) {}

    /**
     * @return the imageAndRegistriesMap
     */
    public Map<String, List<Registry>> getImageAndRegistriesMap() {
        return imageAndRegistriesMap;
    }

    /**
     * @param imageAndRegistriesMap the imageAndRegistriesMap to set
     */
    public void setImageAndRegistriesMap(final Map<String, List<Registry>> imageAndRegistriesMap) {
        this.imageAndRegistriesMap = imageAndRegistriesMap;
    }
}
