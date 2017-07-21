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
package com.hp.hpl.loom.adapter.docker.items;


import java.time.Instant;
import java.util.Calendar;

import com.hp.hpl.loom.adapter.NumericAttribute;
import com.hp.hpl.loom.adapter.TimeAttribute;
import com.hp.hpl.loom.adapter.annotations.LoomAttribute;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.CoreItemAttributes;

/***
 * This models the ImageItem Attributes.
 */
public class ImageItemAttributes extends CoreItemAttributes {

    /* Image Labels */
    public static final String LABEL_IMAGE_TAG = "Tag";
    public static final String LABEL_IMAGE_REPOSITORY = "Repository";
    public static final String LABEL_IMAGE_ID = "Image ID";
    public static final String LABEL_IMAGE_CREATION_DATE = "Creation date";
    public static final String LABEL_IMAGE_REAL_SIZE = "Real size";
    public static final String LABEL_IMAGE_VIRTUAL_SIZE = "Virtual size";

    /***
     * Tags attributed to the given Image
     */
    @LoomAttribute(key = LABEL_IMAGE_TAG, supportedOperations = {DefaultOperations.SORT_BY})
    private String tag;

    /***
     * Repository that the image belongs
     */
    @LoomAttribute(key = LABEL_IMAGE_REPOSITORY, supportedOperations = {DefaultOperations.SORT_BY})
    private String repository;

    /***
     * ImageID, as in Docker Image UUID.
     */
    @LoomAttribute(key = LABEL_IMAGE_ID, supportedOperations = {DefaultOperations.SORT_BY})
    private String imageid;

    /***
     * Creation date.
     */
    @LoomAttribute(key = LABEL_IMAGE_CREATION_DATE, supportedOperations = {DefaultOperations.SORT_BY},
            type = TimeAttribute.class, format = "MMM dd, YYYY", shortFormat = "MMM dd", plottable = true)
    private String creationdate;

    /***
     * Real Size (in disk) of an image. Future-friendly: Maximum set to 1 Petabyte.
     */
    @LoomAttribute(key = LABEL_IMAGE_REAL_SIZE, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "Inf", unit = "Bytes")
    private Long realSize;

    /***
     * Virtual Size of an image. Future-friendly: Maximum set to 1 Petabyte.
     */
    @LoomAttribute(key = LABEL_IMAGE_VIRTUAL_SIZE, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "Inf", unit = "Bytes")
    private Long virtualSize;

    // Used in order to search the host database for the correct one, in order to reduce search
    // time.
    private String containingHostUID;

    /**
     * Default constructor.
     */
    public ImageItemAttributes() {}

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(final String tag) {
        this.tag = tag;
    }

    /**
     * @return the repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepository(final String repository) {
        this.repository = repository;
    }

    /**
     * @return the virtualSize
     */
    public Long getVirtualSize() {
        return virtualSize;
    }

    /**
     * @param virtualSize the virtualSize to set
     */
    public void setVirtualSize(final Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    /**
     * @return the imageid
     */
    public String getImageid() {
        return imageid;
    }

    /**
     * @param imageid the imageid to set
     */
    public void setImageid(final String imageid) {
        this.imageid = imageid;
    }

    /**
     * @return the creationdate
     */
    public String getCreationdate() {
        return creationdate;
    }

    /**
     * @param creationdate the creationdate to set
     */
    public void setCreationdate(final long creationdate) {

        Calendar.getInstance().getTimeZone();
        Instant instant = Instant.ofEpochSecond(creationdate);

        this.creationdate = instant.toString();
    }

    /**
     * @return the realSize
     */
    public Long getRealSize() {
        return realSize;
    }

    /**
     * @param realSize the realSize to set
     */
    public void setRealSize(final Long realSize) {
        this.realSize = realSize;
    }

    public String getContainingHostUID() {
        return containingHostUID;
    }

    public void setContainingHostUID(final String containingHostUID) {
        this.containingHostUID = containingHostUID;
    }
}
