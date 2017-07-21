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

/***
 * Model the RegistryItem Attributes
 */
public class RegistryItemAttributes extends CoreItemAttributes {

    public static final String LABEL_REGISTRY_NAME = "Name";
    public static final String LABEL_REGISTRY_ADDRESS = "Address";
    public static final String LABEL_REGISTRY_PORT = "Port";

    /***
     * Registry Name
     */
    @LoomAttribute(key = LABEL_REGISTRY_NAME, supportedOperations = {DefaultOperations.SORT_BY})
    private String name;

    /***
     * Registry Address
     */
    @LoomAttribute(key = LABEL_REGISTRY_ADDRESS, supportedOperations = {DefaultOperations.SORT_BY})
    private String address;

    /***
     * Registry Port Number
     */
    @LoomAttribute(key = LABEL_REGISTRY_PORT, supportedOperations = {DefaultOperations.SORT_BY})
    private String port;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(final String address) {
        this.address = address;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(final String port) {
        this.port = port;
    }


}
