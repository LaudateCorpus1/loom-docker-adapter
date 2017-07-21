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

/***
 * Represents a bind mounted volume in a Docker container.
 */
public class Volume {

    /***
     * Path of the volume on host file system.
     */
    private String path;

    /***
     * Added from the docker API in order to make sure that they are unique across hosts
     */
    private String hostId;

    /***
     * Default Constructor
     *
     * @param path
     * @param hostId
     */
    public Volume(final String path, final String hostId) {
        this.path = path;
        this.hostId = hostId;
    }

    /***
     * getter for Path
     *
     * @return file system path
     */
    public String getPath() {
        return path;
    }

    /***
     * Returns the UID of the host were the file path is contained
     *
     * @return host UID.
     */
    public String getHostId() {
        return hostId;
    }

    @Override
    public String toString() {
        return getPath() + " at " + getHostId();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Volume vol = (Volume) o;

        if (path != null ? !path.equals(vol.path) : vol.path != null) {
            return false;
        }
        if (hostId != null ? !hostId.equals(vol.hostId) : vol.hostId != null) {
            return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (hostId != null ? hostId.hashCode() : 0);
        return result;
    }


}
