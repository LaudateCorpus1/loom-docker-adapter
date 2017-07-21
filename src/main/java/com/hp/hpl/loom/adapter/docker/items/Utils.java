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

import com.fasterxml.jackson.databind.JsonNode;

/***
 * Conversions and definitions
 */
public final class Utils {
    public static final Long KHERTZ_TO_GIGAHERTZ = new Long(1000000);

    public static final Long BYTE_TO_MEGABYTE = new Long(1024 * 1024);

    public static final Long BYTE_TO_GIGABYTE = new Long(1024 * 1024 * 1024);

    /* cAdvisor information */
    public static final String MAXIMUM_CPU = "2000";
    public static final String MAXIMUM_MEMORY = "10737418240"; // 10 gigabytes

    /**
     * Private constructor as this is a utility class.
     */
    private Utils() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

    public static String getCadvisorAttribute(final JsonNode source, final String name) {
        JsonNode node = source.get(name);
        String response = null;

        if (node != null && !node.isNull()) {
            response = node.toString();
        }

        return response;
    }

    /**
     * @return the cpuInstant
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static Float calculateCpuUsage(final Float currentUsedCpuTime, final Long numberOfCores) {

        // the formulation is described on https://github.com/google/cadvisor/issues/832
        // In this scenario, the interval is one second and only the current usage in one second is
        // taken into account.
        Float usage = new Float(0);
        float usageInCores = 0;

        if (numberOfCores != null && numberOfCores != 0) {
            usageInCores = currentUsedCpuTime / 1000 * 100;
            usage = usageInCores / numberOfCores;
        }
        return usage;
    }

    /***
     * Returns a percentage of total memory used.
     *
     * @param memoryUsed memory used in bytes
     * @return the percentage of total memory being used
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static Float calculateMemoryPercentage(final Float memoryUsed, final Float memoryCapacity) {
        Float percentualValue = new Float(0);

        if (memoryCapacity != null && memoryCapacity != 0) {
            Float totalMemBytes = memoryCapacity * Utils.BYTE_TO_GIGABYTE;
            percentualValue = new Float(memoryUsed * 100 / totalMemBytes);
        }
        return percentualValue;
    }
}
