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

/***
 * ItemTypes defined in the docker-adapter
 */
public final class Types {

    /**
     * Host Type ID.
     */
    public static final String HOST_TYPE_ID = "host";

    /**
     * Image Type ID.
     */
    public static final String IMAGE_TYPE_ID = "image";

    /**
     * Container Type ID.
     */
    public static final String CONTAINER_TYPE_ID = "container";

    /**
     * Volume Type ID.
     */
    public static final String VOLUME_TYPE_ID = "volume";

    /**
     * Port Type ID.
     */
    public static final String PORT_TYPE_ID = "port";

    /***
     * Docker Registry ID
     */
    public static final String REGISTRY_TYPE_ID = "registry";

    /***
     * Docker layer ID
     */
    public static final String DOCKER_LAYER = "docker";

    /**
     * Private constructor as this is a utility class.
     */
    private Types() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }
}
