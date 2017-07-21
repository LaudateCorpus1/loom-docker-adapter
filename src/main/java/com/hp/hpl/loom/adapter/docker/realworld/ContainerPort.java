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

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represent a port. This port always belongs to a container. It can be exposed (i.e. mapped as a
 * host port) or linked (connected) to another container.
 */
public class ContainerPort {

    public static final String NO_INFORMATION = "none";

    // Mandatory fields: Host container information. A host container is the container who exposes
    // that port.
    private String containerID;
    private Long containerPortNumber;
    private String portProtocol;

    // Optional fields: Only populated if a port it mapped into a host port. A container can expose
    // a port without mapping it into a host port.

    /**
     * Ip of the physical/virtual host where this port is going to be mapped
     */
    private String hostID = NO_INFORMATION;
    private Long hostPortNumber = (long) 0;

    /***
     * Interface Ip. I.e: 0.0.0.0
     */
    private String hostInterfaceIp = NO_INFORMATION;

    /**
     * @return the containerID
     */
    public String getContainerID() {
        return containerID;
    }

    /**
     * @param containerID the containerID to set
     */
    public void setContainerID(final String argContainerID) {
        containerID = argContainerID;
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
    public void setContainerPortNumber(final long argContainerPortNumber) {
        containerPortNumber = argContainerPortNumber;
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
     * @return the hostID
     */
    public String getHostID() {
        return hostID;
    }

    /**
     * @param hostID the hostID to set
     */
    public void setHostID(final String argHostID) {
        hostID = argHostID;
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
    public void setHostPortNumber(final long hostPortNumber) {
        this.hostPortNumber = hostPortNumber;
    }

    /**
     * @return the hostInterfaceIp
     */
    public String getHostInterfaceIp() {
        return hostInterfaceIp;
    }

    /**
     * @param hostInterfaceIp the hostInterfaceIp to set
     */
    public void setHostInterfaceIp(final String hostInterfaceIp) {
        this.hostInterfaceIp = hostInterfaceIp;
    }


    /***
     * The hash code for ports is a combination of the following fields: containerId,
     * containerPortNumber, portProtocol, hostID, hostPortNumber, hostInterfaceIp
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(containerID).append(containerPortNumber).append(portProtocol).append(hostID)
                .append(hostPortNumber).append(hostInterfaceIp).toHashCode();
    }

    /***
     * Compares the hashCode of two containerPort items to verify if they are equal.
     *
     * @return true if item are the same
     */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;
        if (this.hashCode() == ((ContainerPort) object).hashCode()) {
            isEqual = true;
        } else {
            isEqual = false;
        }
        return isEqual;
    }
}
