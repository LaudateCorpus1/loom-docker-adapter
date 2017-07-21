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

public final class Relationships {

    public static final String MOUNTS_TYPE = "mounts";
    public static final String MOUNTS_TYPE_NAME = "mounts";

    public static final String RUNS_TYPE = "runs";
    public static final String RUNS_TYPE_NAME = "runs";

    public static final String ISBASEDON_TYPE = "isbasedon";
    public static final String ISBASEDON_TYPE_NAME = "is based on";

    public static final String EXPOSES_TYPE = "exposes";
    public static final String EXPOSES_TYPE_NAME = "exposes";

    public static final String LINKS_TYPE = "links";
    public static final String LINKS_TYPE_NAME = "links";

    public static final String CONTAINS_TYPE = "contains";
    public static final String CONTAINS_TYPE_NAME = "contains";

    public static final String MAPS_TYPE = "maps";
    public static final String MAPS_TYPE_NAME = "maps";

    public static final String HASLOCAL_TYPE = "haslocal";
    public static final String HASLOCAL_TYPE_NAME = "has local";

    public static final String AVAILABLE_AT_TYPE = "availableat";
    public static final String AVAILABLE_AT_TYPE_NAME = "available at";

    public static final String FETCHES_IMAGES_FROM = "fetchesimagesfrom";
    public static final String FETCHES_IMAGES_FROM_NAME = "fetches images from";

    private Relationships() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }
}
