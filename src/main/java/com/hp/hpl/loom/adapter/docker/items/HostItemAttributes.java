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
 * Attributes of the HostItem.
 */
public class HostItemAttributes extends CoreItemAttributes {

    /* Host Labels */
    public static final String LABEL_HOST_ADDRESS = "Host address";
    public static final String LABEL_HOST_PORT = "Host port";
    public static final String LABEL_HOST_DOCKER_DAEMON_ADDRESS = "Docker daemon address";
    public static final String LABEL_HOST_OS_DISTRIBUTION = "Operational system distribution";

    /* Loom key labels */
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
    public static final String LABEL_DOCKER_VERSION = "Docker Version";
    public static final String LABEL_KERNEL_VERSION = "Kernel Version";

    /***
     * The address of a host running a docker daemon
     */
    @LoomAttribute(key = LABEL_HOST_ADDRESS, supportedOperations = {DefaultOperations.SORT_BY})
    private String hostAddress;

    /***
     * The port of a host running a docker daemon
     */
    @LoomAttribute(key = LABEL_HOST_PORT, supportedOperations = {DefaultOperations.SORT_BY})
    private String hostPort;

    /***
     * The complete docker daemon address, in the format http://[docker daemon host]:[daemon
     * listening port]
     */
    @LoomAttribute(key = LABEL_HOST_DOCKER_DAEMON_ADDRESS, supportedOperations = {DefaultOperations.SORT_BY})
    private String hostDockerDaemonAddress;

    /***
     * OS running on host, only retrieved during startup. The process is done through an RPC call.
     */
    @LoomAttribute(key = LABEL_HOST_OS_DISTRIBUTION,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY})
    private String runningOS = "no information";

    /* CAdvisor attributes */
    @LoomAttribute(key = LABEL_NUMBER_OF_CORES,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = false,
            type = NumericAttribute.class, min = "0", max = "1024", unit = "cores", ignoreUpdate = true)
    private Long numberOfCores = null;

    @LoomAttribute(key = LABEL_DOCKER_VERSION,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = false)
    private String dockerVersion = null;

    @LoomAttribute(key = LABEL_KERNEL_VERSION,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = false)
    private String kernelVersion = null;

    @LoomAttribute(key = LABEL_MEMORY_PRESENT_USAGE, supportedOperations = {DefaultOperations.SORT_BY},
            plottable = true, type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float memoryInstant = null;

    @LoomAttribute(key = LABEL_CPU_PRESENT_USAGE, supportedOperations = {DefaultOperations.SORT_BY}, plottable = true,
            type = NumericAttribute.class, min = "0", max = "100", unit = "%", ignoreUpdate = true)
    private Float cpuInstant = null;

    // Setting them to null will prevent to be displayed in hosts without cAdvisor.
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

    @LoomAttribute(key = LABEL_CPU_FREQUENCY,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = false,
            type = NumericAttribute.class, min = "0", max = "10737418240", unit = "GHz", ignoreUpdate = true)
    private Float cpuFrequency = null;

    @LoomAttribute(key = LABEL_TOTAL_FS_CAPACITY,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = false,
            type = NumericAttribute.class, min = "0", max = "1125899906842624", unit = "GB", ignoreUpdate = true)
    private Float fileSystemCapacity = null;

    @LoomAttribute(key = LABEL_TOTAL_MEM_CAPACITY,
            supportedOperations = {DefaultOperations.SORT_BY, DefaultOperations.GROUP_BY}, plottable = false,
            type = NumericAttribute.class, min = "0", max = "1125899906842624", unit = "GB", ignoreUpdate = true)
    private Float memoryCapacity = null;

    /**
     * Default constructor.
     */
    public HostItemAttributes() {}

    /**
     * @return the hostPort
     */
    public String getHostPort() {
        return hostPort;
    }

    /**
     * @param hostPort the hostPort to set
     */
    public void setHostPort(final String hostPort) {
        this.hostPort = hostPort;
    }

    /**
     * @return the hostAddress
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param host The host address to set
     */
    public void setHostAddress(final String host) {
        hostAddress = host;
    }

    /**
     * @return the hostDockerDaemonAddress
     */
    public String getHostDockerDaemonAddress() {
        return hostDockerDaemonAddress;
    }

    /**
     * @param hostDockerDaemonAddress the hostDockerDaemonAddress to set
     */
    public void setHostDockerDaemonAddress(final String hostDockerDaemonAddress) {
        this.hostDockerDaemonAddress = hostDockerDaemonAddress;
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

    /**
     * @return the cpuFrequency
     */
    public Float getCpuFrequency() {
        // returns in GigaHertz.
        return cpuFrequency;
    }

    /**
     * @return the fileSystemCapacity
     */
    public Float getFileSystemCapacity() {
        return fileSystemCapacity;
    }

    /**
     * @return the memoryCapacity
     */
    public Float getMemoryCapacity() {
        return memoryCapacity;
    }

    /**
     * @return the numberOfCores
     */
    public Long getNumberOfCores() {
        return numberOfCores;
    }

    public String getDockerVersion() {
        return dockerVersion;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    /***
     * Updates the machine statistics of the host. Requires cAdvisor container running on the host
     *
     * @param argDayCpuMax
     * @param argDayCpuMean
     * @param argCpuInstant
     * @param argDayMemoryMax
     * @param argDayMemoryMean
     * @param argMemoryInstant
     */
    public void updateStatistics(final String argDayCpuMax, final String argDayCpuMean, final String argCpuInstant,
            final String argDayMemoryMax, final String argDayMemoryMean, final String argMemoryInstant) {
        dayCpuMax = Utils.calculateCpuUsage(new Float(argDayCpuMax), numberOfCores);
        dayCpuMean = Utils.calculateCpuUsage(new Float(argDayCpuMean), numberOfCores);
        cpuInstant = Utils.calculateCpuUsage(new Float(argCpuInstant), numberOfCores);
        dayMemoryMax = Utils.calculateMemoryPercentage(new Float(argDayMemoryMax), memoryCapacity);
        dayMemoryMean = Utils.calculateMemoryPercentage(new Float(argDayMemoryMean), memoryCapacity);
        memoryInstant = Utils.calculateMemoryPercentage(new Float(argMemoryInstant), memoryCapacity);

    }

    /***
     * Updates the information about the machine running the host. Requires cAdvisor container
     * running on that host.
     *
     * @param argCpuFrequency
     * @param argFileSystemCapacity
     * @param argMemoryCapacity
     * @param argNumberOfCores
     * @param argDockerVersion
     * @param argKernelVersion
     */
    public void updateMachineInfo(final String argCpuFrequency, final String argFileSystemCapacity,
            final String argMemoryCapacity, final String argNumberOfCores, final String argDockerVersion,
            final String argKernelVersion) {
        cpuFrequency = new Float(argCpuFrequency) / Utils.KHERTZ_TO_GIGAHERTZ;
        fileSystemCapacity = new Float(argFileSystemCapacity) / Utils.BYTE_TO_GIGABYTE;
        memoryCapacity = new Float(argMemoryCapacity) / Utils.BYTE_TO_GIGABYTE;
        numberOfCores = new Long(argNumberOfCores);
        dockerVersion = argDockerVersion;
        kernelVersion = argKernelVersion;
    }

    /**
     * @return the runningOS
     */
    public String getRunningOS() {
        return runningOS;
    }

    /**
     * @param runningOS the runningOS to set
     */
    public void setRunningOS(final String runningOS) {
        this.runningOS = runningOS;
    }
}
