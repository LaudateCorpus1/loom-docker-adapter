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

/**
 * This models private registries
 */
public class Registry implements Comparable<Registry> {


    /***
     * By default there is at least a docker hub repository
     */
    public static final String DOCKER_HUB_REGISTRY_NAME = "Docker Hub";
    public static final String DOCKER_HUB_REGISTRY_ADDRESS = "hub.docker.com";
    public static final String DOCKER_HUB_REGISTRY_PORT = "5000";
    public static final String DOCKER_HUB_REGISTRY_UID = DOCKER_HUB_REGISTRY_ADDRESS + ":" + DOCKER_HUB_REGISTRY_PORT;

    /***
     * registry Name
     */
    private String name;

    /***
     * registry address
     */
    private String address;

    /***
     * registry port
     */
    private String port;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    public Registry() {}

    public Registry(final String name, final String address, final String port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public Registry(String registryInfo) {
        this.name = registryInfo.split(":")[0];
        this.port = registryInfo.split(":")[1].split("/")[0];
        this.address = name;
    }

    public String getUID() {
        return address + ":" + port;
    }

    @Override
    public int compareTo(final Registry o) {
        if (getUID().equals(o.getUID())) {
            return 0;
        } else {
            return 1;
        }
    }
}
