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

import com.hp.hpl.loom.adapter.annotations.LoomAttribute;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.CoreItemAttributes;

/**
 * Models the VolumeItem Attributes
 */
public class VolumeItemAttributes extends CoreItemAttributes {
    public static final String LABEL_VOLUME_PATH_ON_HOST = "Path on host";
    public static final String LABEL_VOLUME_HOST_ID = "Host ID";

    /**
     * Path, in the host machine, where a given volume is contained.
     */
    @LoomAttribute(key = LABEL_VOLUME_PATH_ON_HOST,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY})
    private String pathOnHost;

    /**
     * Id of the host that contains the volume
     */
    @LoomAttribute(key = LABEL_VOLUME_HOST_ID, supportedOperations = {DefaultOperations.SORT_BY})
    private String hostId;

    // Used in order to search the host database for the correct one, in order to reduce search
    // time.
    private String containingHostUID;

    /**
     * @return Path, in the host machine, where a given volume is contained.
     */
    public String getPathOnHost() {
        return pathOnHost;
    }

    /**
     * @param pathOnHost the pathOnHost to set
     */
    public void setPathOnHost(final String pathOnHost) {
        this.pathOnHost = pathOnHost;
    }

    /**
     * @return the containingHostUID
     */
    public String getContainingHostUID() {
        return containingHostUID;
    }

    /**
     * @param containingHostUID the containingHostUID to set
     */
    public void setContainingHostUID(final String containingHostUID) {
        this.containingHostUID = containingHostUID;
    }

    /**
     * @return the hostId
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * @param hostId the hostId to set
     */
    public void setHostId(final String hostId) {
        this.hostId = hostId;
    }

}
