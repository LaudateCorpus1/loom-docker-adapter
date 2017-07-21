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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.loom.adapter.AggregationUpdater;
import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.ConnectedItem;
import com.hp.hpl.loom.adapter.docker.distributed.realworld.HostManager;
import com.hp.hpl.loom.adapter.docker.items.ImageItem;
import com.hp.hpl.loom.adapter.docker.items.ImageItemAttributes;
import com.hp.hpl.loom.adapter.docker.items.Relationships;
import com.hp.hpl.loom.adapter.docker.items.Types;
import com.hp.hpl.loom.adapter.docker.realworld.Registry;
import com.hp.hpl.loom.exceptions.NoSuchItemTypeException;
import com.hp.hpl.loom.exceptions.NoSuchProviderException;
import com.hp.hpl.loom.model.Aggregation;
import com.hp.hpl.loom.model.CoreItemAttributes.ChangeStatus;
import com.spotify.docker.client.messages.Image;

/***
 * Collects all images, from all hosts
 */
public class ImageItemUpdater extends AggregationUpdater<ImageItem, ImageItemAttributes, Image> {
    private static final Log LOG = LogFactory.getLog(ImageItemUpdater.class);
    private static final String IMAGE_DESCRIPTION = "Represents a container image";

    protected DockerDistributedCollector dockerCollector = null;

    private Set<String> tamperedImagesOnLastIteration = new HashSet<String>();

    /**
     * Constructs a ImageItemUpdater.
     *
     * @param aggregation The aggregation this update will update
     * @param adapter The baseAdapter this updater is part of
     * @param DockerDistributedCollector The collector it uses
     *
     * @throws NoSuchItemTypeException Thrown if the ItemType isn't found
     * @throws NoSuchProviderException thrown if adapter is not known
     */
    public ImageItemUpdater(final Aggregation aggregation, final BaseAdapter adapter,
            final DockerDistributedCollector dockerCollector) throws NoSuchItemTypeException, NoSuchProviderException {
        super(aggregation, adapter, dockerCollector);
        this.dockerCollector = dockerCollector;
    }

    /**
     * Each observed resource should have a way to be identified uniquely within the given adapter’s
     * domain and this is what should be returned here. This method is called to create the Item
     * logicalId.
     *
     * @return a unique way to identify a given resource (within the docker adapter). In the case of
     *         images, it is the docker image UID
     */
    @Override
    protected String getItemId(final Image argImage) {
        return argImage.id();
    }

    /***
     * This must return a brand new Iterator every collection cycle giving access to all the
     * resources that AggregationUpdater is observing.
     *
     */
    @Override
    protected Iterator<Image> getResourceIterator() {
        // Get all docker daemon images:
        final List<Image> dockerDaemonImageList = HostManager.getInstance(adapter).getAllImages();

        // Get all private registry images:
        final List<Image> registryImageList = new ArrayList<Image>();

        // Merge all images in the List of Image Lists to a single List (registryImageList)
        for (List<Image> registryImages : HostManager.getInstance().getImageOnEachRegistry().values()) {
            registryImageList.addAll(registryImages);
        }

        // Remove duplicate Images contained on multiple registries, based on imageid
        Map<String, Image> noDuplicatesMap = new HashMap<>();

        registryImageList.forEach(image -> noDuplicatesMap.put(image.id(), image));

        // Merge with private repo images. If a key collision happens, give preference to the
        // private repository information. Otherwise add to the map. The key is the image id.
        dockerDaemonImageList.forEach(image -> {
            if (noDuplicatesMap.get(image.id()) == null) {
                noDuplicatesMap.put(image.id(), image);
            }
        });

        List<Image> allAvailableImages = new ArrayList<Image>();

        // convert map values to List
        allAvailableImages = noDuplicatesMap.values().stream().collect(Collectors.toList());

        return allAvailableImages.iterator();

    }

    @Override
    protected Iterator<Image> getUserResourceIterator(final Collection<Image> data) {
        return data.iterator();
    }

    /**
     * This method should return an Item only set with its logicalId and ItemType.
     */
    @Override
    protected ImageItem createEmptyItem(final String logicalId) {
        ImageItem item = new ImageItem(logicalId, itemType);
        return item;
    }

    /**
     * This should return a newly created CoreItemAttributes object based on data observed from the
     * resource.
     */
    @Override
    protected ImageItemAttributes createItemAttributes(final Image resource) {

        ImageItemAttributes imageAttr = new ImageItemAttributes();

        imageAttr.setImageid(resource.id());
        imageAttr.setRealSize(resource.size());
        imageAttr.setVirtualSize(resource.virtualSize());
        imageAttr.setCreationdate(Integer.parseInt(resource.created()));

        String repoTags = (resource.repoTags().get(0)).replaceAll("<", "").replaceAll(">", "");

        int separator = repoTags.lastIndexOf(":");
        String repository = repoTags.substring(0, separator);
        String tag = repoTags.substring(separator + 1);

        imageAttr.setRepository(repository);
        imageAttr.setTag(tag);

        // Define the id of an instance of ImageType.l
        imageAttr.setItemId(getItemId(resource));
        imageAttr.setItemDescription(IMAGE_DESCRIPTION);
        imageAttr.setItemName(imageAttr.getRepository() + ":" + imageAttr.getTag());

        return imageAttr;
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
     * @param ImageAttributes The Image Item Attributes
     * @param resource The image on docker-java API
     * @return
     */
    @Override
    protected ChangeStatus compareItemAttributesToResource(final ImageItemAttributes imageAttributes,
            final Image resource) {

        ChangeStatus status;

        // if (imageAttributes.getImageid().equals(resource.id()))
        // {
        // The ID an image on the docker API is an image UUID
        status = ChangeStatus.CHANGED_UPDATE;
        // } else {
        // status = ChangeStatus.CHANGED_IGNORE;
        // }

        if (dockerCollector.getRegistryItemUpdater().getImageAndRegistriesMap() != null) {
            // If the image is mentioned to be available in a private registry, update relation:
            if (dockerCollector.getRegistryItemUpdater().getImageAndRegistriesMap().get(resource.id()) != null) {
                status = ChangeStatus.CHANGED_UPDATE;
            }

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
     * @param imageItem the image item that Items will be connected to
     * @param resource the docker-java API item, from where the connections will be extracted.
     */
    @Override
    protected void setRelationships(final ConnectedItem imageItem, final Image resource) {

        // Note: The host that has local this image relationship is defined in the
        // HostItemUpdater.

        // Locate registries that contain the image. If no registry is found, just assume it is a
        // dockerhub image:
        ImageItem image = (ImageItem) imageItem;
        String registryName = image.getCore().getItemName();

        Pattern privateRegistry = Pattern.compile("^.+:\\d{2,4}");

        if (privateRegistry.matcher(registryName).find()) {
            Registry registeredRegistry = HostManager.getInstance().registerRegistry(registryName, resource);
            imageItem.setRelationshipWithType(adapter.getProvider(), Types.REGISTRY_TYPE_ID,
                    registeredRegistry.getUID(), Relationships.AVAILABLE_AT_TYPE);
        } else {
            // Assume that the image comes from docker hub
            imageItem.setRelationshipWithType(adapter.getProvider(), Types.REGISTRY_TYPE_ID,
                    Registry.DOCKER_HUB_REGISTRY_UID, Relationships.AVAILABLE_AT_TYPE);
        }
    }
}
