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
package com.hp.hpl.loom.adapter.docker.items.actions;

import com.hp.hpl.loom.adapter.docker.items.VolumeItemAttributes;
import com.hp.hpl.loom.model.ActionParameters;

/**
 * Library that contains all the possible actions to be applied on Volumes individual ItemTypes and
 * Base Aggregations
 */
public final class VolumeActions {

    private VolumeActions() {}

    /***
     * Copies recursively the target file or directory to the target mount volume address
     *
     * @param volAttr Attributes of the volume.
     * @param actionParameters actions arguments supplied by the user.
     * @return actionStatus true if no exception, otherwise false.
     */
    public static boolean copyFromLocal(final VolumeItemAttributes volAttr, final ActionParameters actionParameters) {
        /*
         * // Removed for multiple hosts: This action only works on local hosts boolean actionStatus
         * = false;
         *
         * String sourcePath = "";
         *
         * for (ActionParameter parameter : actionParameters) { if
         * (parameter.getId().equals("sourcepath")) { sourcePath = parameter.getValue(); } }
         *
         * if (!sourcePath.isEmpty()) {
         *
         * File sourceDir = new File(sourcePath); File destDir = new File(volAttr.getPathOnHost());
         * try { // // Copy source directory into destination directory // including its child
         * directories and files. When // the destination directory does not exist, it will // be
         * created. This copy process also preserve the // date information of the file. //
         * FileUtils.copyDirectory(sourceDir, destDir); actionStatus = true; } catch (IOException e)
         * { e.printStackTrace(); actionStatus = false; } } return actionStatus;
         */
        return true;
    }
}
