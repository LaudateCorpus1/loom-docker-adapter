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
package com.hp.hpl.loom.adapter.docker.realworld;
/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.messages.Image;

import jersey.repackaged.com.google.common.base.MoreObjects;

/**
 * This class was created because originally it was impossible to populate fields Images from
 * ImageInformation, due to the lack of setters.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class RegistryImage extends Image {

    @JsonProperty("Created")
    private String created;
    @JsonProperty("Id")
    private String id;
    @JsonProperty("ParentId")
    private String parentId;
    @JsonProperty("RepoTags")
    private ImmutableList<String> repoTags;
    @Nullable
    @JsonProperty("RepoDigests")
    private ImmutableList<String> repoDigests;
    @JsonProperty("Size")
    private Long size;
    @JsonProperty("VirtualSize")
    private Long virtualSize;
    @Nullable
    @JsonProperty("Labels")
    private ImmutableMap<String, String> labels;

    @Override
    public String created() {
        return created;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String parentId() {
        return parentId;
    }

    @Override
    public ImmutableList<String> repoTags() {
        return repoTags;
    }

    @Override
    public ImmutableList<String> repoDigests() {
        return repoDigests;
    }

    @Override
    public Long size() {
        return size;
    }

    @Override
    public Long virtualSize() {
        return virtualSize;
    }

    @Override
    public ImmutableMap<String, String> labels() {
        return labels;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Image that = (Image) o;

        return Objects.equal(created, that.created()) && Objects.equal(id, that.id())
                && Objects.equal(parentId, that.parentId()) && Objects.equal(repoTags, that.repoTags())
                && Objects.equal(size, that.size()) && Objects.equal(virtualSize, that.virtualSize());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(created, id, parentId, repoTags, size, virtualSize);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("created", created).add("id", id).add("parentId", parentId)
                .add("repoTags", repoTags).add("size", size).add("virtualSize", virtualSize).toString();
    }

    /**
     * @param created the created to set
     */
    public void setCreated(final String created) {
        this.created = created;
    }

    /**
     * @param id the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    /**
     * @param repoTags the repoTags to set
     */
    public void setRepoTags(final ImmutableList<String> repoTags) {
        this.repoTags = repoTags;
    }

    /**
     * @param size the size to set
     */
    public void setSize(final Long size) {
        this.size = size;
    }

    /**
     * @param virtualSize the virtualSize to set
     */
    public void setVirtualSize(final Long virtualSize) {
        this.virtualSize = virtualSize;
    }
}
