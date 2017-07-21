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


import com.hp.hpl.loom.adapter.NumericAttribute;
import com.hp.hpl.loom.adapter.annotations.LoomAttribute;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.model.CoreItemAttributes;

/***
 * Model the PortItem Attributes
 */
public class PortItemAttributes extends CoreItemAttributes {

    public static final String LABEL_PORT_CONTAINER_ID = "Container ID";
    public static final String LABEL_PORT_CONTAINER_PORT_NUMBER = "Container port number";
    public static final String LABEL_PORT_PROTOCOL = "Port protocol";
    public static final String LABEL_PORT_HOST_UID = "Host UID";
    public static final String LABEL_PORT_HOST_PORT_NUMBER = "Host port number";
    public static final String LABEL_PORT_INTERFACE_ADDRESS = "Interface address";

    /***
     * Container ID in the deployment
     */
    @LoomAttribute(key = LABEL_PORT_CONTAINER_ID, supportedOperations = {DefaultOperations.SORT_BY})
    private String containerID;

    /***
     * The port number that is open in a given container. Setting the maximum to "Inf" means that
     * the plotted output will be useful since most ports are very low on the scale up to 65535.
     */
    @LoomAttribute(key = LABEL_PORT_CONTAINER_PORT_NUMBER,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "Inf")
    private Long containerPortNumber;

    /***
     * Port Protocol. I.e. UDP/TCP; UDP; TCP
     */
    @LoomAttribute(key = LABEL_PORT_PROTOCOL,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY})
    private String portProtocol;

    /**
     * UID of the physical/virtual host where this port is going to be mapped
     */
    @LoomAttribute(key = LABEL_PORT_HOST_UID, supportedOperations = {DefaultOperations.SORT_BY})
    private String hostUID;

    /***
     * The Host port that the container port is mapped to. Setting the maximum to "Inf" means that
     * the plotted output will be useful since most ports are very low on the scale up to 65535.
     */
    @LoomAttribute(key = LABEL_PORT_HOST_PORT_NUMBER,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "Inf")
    private Long hostPortNumber;

    /***
     * The Host network interface address
     */
    @LoomAttribute(key = LABEL_PORT_INTERFACE_ADDRESS, supportedOperations = {DefaultOperations.SORT_BY})
    private String interfaceAddress;

    // Used in order to search the host database for the correct one, in order to reduce search
    // time.
    private String containingHostUID;

    /**
     * @return the containerID
     */
    public String getContainerID() {
        return containerID;
    }

    /**
     * @param containerID the containerID to set
     */
    public void setContainerID(final String containerID) {
        this.containerID = containerID;
    }

    /**
     * @return the containerPortNumber
     */
    public Long getContainerPortNumber() {
        return containerPortNumber;
    }

    /**
     * @param containerPortNumber the containerPortNumber to set
     */
    public void setContainerPortNumber(final Long containerPortNumber) {
        this.containerPortNumber = containerPortNumber;
    }

    /**
     * @return the portProtocol
     */
    public String getPortProtocol() {
        return portProtocol;
    }

    /**
     * @param portProtocol the portProtocol to set
     */
    public void setPortProtocol(final String portProtocol) {
        this.portProtocol = portProtocol;
    }

    /**
     * @return the hostUID
     */
    public String getHostUID() {
        return hostUID;
    }

    /**
     * @param hostUID the hostUID to set
     */
    public void setHostUID(final String hostUID) {
        this.hostUID = hostUID;
    }

    /**
     * @return the hostPortNumber
     */
    public Long getHostPortNumber() {
        return hostPortNumber;
    }

    /**
     * @param hostPortNumber the hostPortNumber to set
     */
    public void setHostPortNumber(final Long hostPortNumber) {
        this.hostPortNumber = hostPortNumber;
    }

    /**
     * @return the interfaceAddress
     */
    public String getInterfaceAddress() {
        return interfaceAddress;
    }

    /**
     * @param interfaceAddress the interfaceAddress to set
     */
    public void setInterfaceAddress(final String interfaceAddress) {
        this.interfaceAddress = interfaceAddress;
    }

    public String getContainingHostUID() {
        return containingHostUID;
    }

    public void setContainingHostUID(final String containingHostUID) {
        this.containingHostUID = containingHostUID;
    }

}
