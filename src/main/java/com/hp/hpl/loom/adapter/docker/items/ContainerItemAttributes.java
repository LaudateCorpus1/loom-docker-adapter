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
 * Models the ContainerItem Attributes
 */
public class ContainerItemAttributes extends CoreItemAttributes {

    // Loom key labels
    public static final String LABEL_CONTAINER_COMMAND = "Command";
    public static final String LABEL_CONTAINER_CREATION_DATE = "Creation date";
    public static final String LABEL_CONTAINER_ID = "Container ID";
    public static final String LABEL_CONTAINER_NAME = "Name";
    public static final String LABEL_CONTAINER_LABEL = "Label";
    public static final String LABEL_CONTAINER_STATUS = "Status";
    public static final String LABEL_CONTAINER_OVERALL_STATUS = "Overall status";
    public static final String LABEL_CONTAINER_BASE_IMAGE_REPOSITORY_NAME = "Base image repository name";
    public static final String LABEL_CONTAINER_BASE_IMAGE_ID = "Base image ID";

    // CAdvisor labels
    public static final String LABEL_DAY_CPU_MAXIMUM = "Day CPU Maximum";
    public static final String LABEL_DAY_CPU_MEAN = "Day CPU Mean";
    public static final String LABEL_CPU_PRESENT_USAGE = "Present CPU Usage";

    public static final String LABEL_DAY_MEMORY_MAXIMUM = "Day Memory Maximum";
    public static final String LABEL_DAY_MEMORY_MEAN = "Day Memory Mean";
    public static final String LABEL_MEMORY_PRESENT_USAGE = "Present Memory Usage";

    public static final String LABEL_CPU_FREQUENCY = "CPU Frequency";
    public static final String LABEL_TOTAL_FS_CAPACITY = "Total Filesystem Capacity";
    public static final String LABEL_TOTAL_MEM_CAPACITY = "Memory Capacity";
    public static final String LABEL_NUMBER_OF_CORES = "Number of Cores";

    /***
     * Command running on the container.
     */
    @LoomAttribute(key = LABEL_CONTAINER_COMMAND, supportedOperations = {DefaultOperations.SORT_BY})
    private String command = null;

    /***
     * Creation date, on unix timestamp format.
     */
    @LoomAttribute(key = LABEL_CONTAINER_CREATION_DATE, supportedOperations = {DefaultOperations.SORT_BY},
            type = TimeAttribute.class, format = "MMM dd, YYYY", shortFormat = "MMM dd", plottable = true)
    private String creationdate = null;

    /***
     * Container ID, as represented on Docker.
     */
    @LoomAttribute(key = LABEL_CONTAINER_ID, supportedOperations = {DefaultOperations.SORT_BY})
    private String containerId = null;

    /***
     * Container Name on docker system, like busy_turin
     */
    @LoomAttribute(key = LABEL_CONTAINER_NAME, supportedOperations = {DefaultOperations.SORT_BY})
    private String name = null;

    @LoomAttribute(key = LABEL_CONTAINER_LABEL, supportedOperations = {DefaultOperations.SORT_BY})
    private String label = null;

    /***
     * Container status, i.e.: Up for 39 seconds. Needs to be set to ignore, since the time changes
     * on every query.
     */
    @LoomAttribute(key = LABEL_CONTAINER_STATUS, supportedOperations = {DefaultOperations.SORT_BY}, ignoreUpdate = true)
    private String status = null;

    /***
     * Container overallStatus, can be "up" or "stopped"
     */
    @LoomAttribute(key = LABEL_CONTAINER_OVERALL_STATUS,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY})
    private String overallStatus = null;

    /***
     * Repository name of the Image where this container comes from
     */
    @LoomAttribute(key = LABEL_CONTAINER_BASE_IMAGE_REPOSITORY_NAME, supportedOperations = {DefaultOperations.SORT_BY})
    private String baseImageRepositoryName = null;

    /***
     * Image ID where this container comes from
     */
    @LoomAttribute(key = LABEL_CONTAINER_BASE_IMAGE_ID, supportedOperations = {DefaultOperations.SORT_BY})
    private String baseImageID = null;

    @LoomAttribute(key = LABEL_CPU_PRESENT_USAGE, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float cpuInstant = null;

    @LoomAttribute(key = LABEL_MEMORY_PRESENT_USAGE, supportedOperations = {DefaultOperations.SORT_BY},
            plottable = true, type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float memoryInstant = null;

    @LoomAttribute(key = LABEL_DAY_CPU_MAXIMUM, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float dayCpuMax = null;

    @LoomAttribute(key = LABEL_DAY_CPU_MEAN, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float dayCpuMean = null;

    @LoomAttribute(key = LABEL_DAY_MEMORY_MAXIMUM, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float dayMemoryMax = null;

    @LoomAttribute(key = LABEL_DAY_MEMORY_MEAN, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float dayMemoryMean = null;

    // Used in order to search the host database for the correct one, in order to reduce search
    // time.
    private String containingHostUID = null;

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(final String command) {
        this.command = command;
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
    public void setCreationdate(final Long creationdate) {

        Calendar.getInstance().getTimeZone();
        Instant instant = Instant.ofEpochSecond(creationdate);

        this.creationdate = instant.toString();
    }

    /**
     * @return the containerId
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * @param containerId the containerId to set
     */
    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

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
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * @return the baseImageRepositoryName
     */
    public String getBaseImageRepositoryName() {
        return baseImageRepositoryName;
    }

    /**
     * @param baseImage the baseImageRepositoryName to set
     */
    public void setBaseImageRepositoryName(final String baseImage) {
        baseImageRepositoryName = baseImage;
    }

    /**
     * @return the baseImageID
     */
    public String getBaseImageID() {
        return baseImageID;
    }

    /**
     * @param baseImageID the baseImageID to set
     */
    public void setBaseImageID(final String baseImageID) {
        this.baseImageID = baseImageID;
    }

    /**
     * @return the overallStatus
     */
    public String getOverallStatus() {
        return overallStatus;
    }

    /**
     * @param overallStatus the overallStatus to set
     */
    public void setOverallStatus(final String overallStatus) {
        this.overallStatus = overallStatus;
    }

    /***
     * Retrieves the UID of the container.
     *
     * @return
     */
    public String getContainingHostUID() {
        return containingHostUID;
    }

    /**
     * @param setContainingHostUID the setContainingHostUID to set
     */
    public void setContainingHostUID(final String containingHostUID) {
        this.containingHostUID = containingHostUID;
    }

    /**
     * @return the dayCpuMax
     */
    public Float getDayCpuMax() {
        return dayCpuMax;
    }

    /**
     * @return the dayCpuMean
     */
    public Float getDayCpuMean() {
        return dayCpuMean;
    }

    /**
     * @return the cpuInstant
     */
    public Float getCpuInstant() {
        return cpuInstant;
    }

    /**
     * @return the dayMemoryMax
     */
    public Float getDayMemoryMax() {
        return dayMemoryMax;
    }

    /**
     * @return the dayMemoryMean
     */
    public Float getDayMemoryMean() {
        return dayMemoryMean;
    }

    /**
     * @return the memoryInstant
     */
    public Float getMemoryInstant() {
        return memoryInstant;
    }

    /***
     * Updates the container statistics. Requires cAdvisor container running on the host
     *
     * @param argDayCpuMax
     * @param argDayCpuMean
     * @param argCpuInstant
     * @param argDayMemoryMax
     * @param argDayMemoryMean
     * @param argMemoryInstant
     */
    public void updateStatistics(final HostItemAttributes hostAttributes, final String argDayCpuMax,
            final String argDayCpuMean, final String argCpuInstant, final String argDayMemoryMax,
            final String argDayMemoryMean, final String argMemoryInstant) {
        dayCpuMax = Utils.calculateCpuUsage(new Float(argDayCpuMax), hostAttributes.getNumberOfCores());
        dayCpuMean = Utils.calculateCpuUsage(new Float(argDayCpuMean), hostAttributes.getNumberOfCores());
        cpuInstant = Utils.calculateCpuUsage(new Float(argCpuInstant), hostAttributes.getNumberOfCores());
        dayMemoryMax = Utils.calculateMemoryPercentage(new Float(argDayMemoryMax), hostAttributes.getMemoryCapacity());
        dayMemoryMean =
                Utils.calculateMemoryPercentage(new Float(argDayMemoryMean), hostAttributes.getMemoryCapacity());
        memoryInstant =
                Utils.calculateMemoryPercentage(new Float(argMemoryInstant), hostAttributes.getMemoryCapacity());
        // dayCpuMax = new Float(argDayCpuMax);
        // dayCpuMean = new Float(argDayCpuMean);
        // cpuInstant = new Float(argCpuInstant);
        // dayMemoryMax = new Float(argDayMemoryMax) / Utils.BYTE_TO_MEGABYTE;
        // dayMemoryMean = new Float(argDayMemoryMean) / Utils.BYTE_TO_MEGABYTE;
        // memoryInstant = new Float(argMemoryInstant) / Utils.BYTE_TO_MEGABYTE;
    }
}
