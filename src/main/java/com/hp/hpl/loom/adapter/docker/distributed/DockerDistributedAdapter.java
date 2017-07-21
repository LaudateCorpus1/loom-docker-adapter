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
package com.hp.hpl.loom.adapter.docker.distributed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.loom.adapter.BaseAdapter;
import com.hp.hpl.loom.adapter.ItemCollector;
import com.hp.hpl.loom.adapter.docker.items.ContainerItem;
import com.hp.hpl.loom.adapter.docker.items.HostItem;
import com.hp.hpl.loom.adapter.docker.items.ImageItem;
import com.hp.hpl.loom.adapter.docker.items.PortItem;
import com.hp.hpl.loom.adapter.docker.items.RegistryItem;
import com.hp.hpl.loom.adapter.docker.items.Types;
import com.hp.hpl.loom.adapter.docker.items.VolumeItem;
import com.hp.hpl.loom.manager.query.DefaultOperations;
import com.hp.hpl.loom.manager.query.OperationContext;
import com.hp.hpl.loom.manager.query.OperationErrorCode;
import com.hp.hpl.loom.manager.query.PipeLink;
import com.hp.hpl.loom.manager.query.QuadFunction;
import com.hp.hpl.loom.manager.query.QuadFunctionMeta;
import com.hp.hpl.loom.manager.query.QueryOperation;
import com.hp.hpl.loom.manager.query.utils.LoomQueryUtils;
import com.hp.hpl.loom.manager.query.utils.StatUtils;
import com.hp.hpl.loom.manager.query.utils.SupportedStats;
import com.hp.hpl.loom.model.Credentials;
import com.hp.hpl.loom.model.Fibre;
import com.hp.hpl.loom.model.ItemType;
import com.hp.hpl.loom.model.Provider;
import com.hp.hpl.loom.model.SeparableItem;
import com.hp.hpl.loom.model.Session;
import com.hp.hpl.loom.tapestry.Operation;
import com.hp.hpl.loom.tapestry.PatternDefinition;

/***
 * Main Implementation Class for DockerDistributedAdapter. Extends the {@link BaseAdapter} to
 * provide data on the docker distributed hosts. Types are then registered via getItemTypes and
 * getAnnotatedItemsClasses.
 */
public class DockerDistributedAdapter extends BaseAdapter {

    public static final String TODO_OPERATION = "TODO Operation";

    public static final String ALL_INFO = "All types";

    public static final String CONTAINERS_PATTERN = "Containers";
    public static final String CONTAINERS_FULL_PATTERN = "Containers (Full)";

    /**
     * Pattern for the HostItem type.
     */
    public static final String HOST_PATTERN = "Host";

    /**
     * Pattern for the ImageItem type.
     */
    public static final String IMAGE_PATTERN = "Image";

    /**
     * Pattern for the ContainerItem type.
     */
    public static final String CONTAINER_PATTERN = "Container";

    /**
     * Pattern for the VolumeItem type.
     */
    public static final String VOLUME_PATTERN = "Volume";

    /**
     * Pattern for the PortItem type.
     */
    public static final String PORT_PATTERN = "Port";

    /**
     * Pattern for the RegistryItem type.
     */
    public static final String REGISTRY_PATTERN = "Registry";

    /***
     * Maximum number of fibres.
     */
    static final int MAX_FIBERS = 40;

    /***
     * Retrieves the ItemTypes explicitly created by the adapter
     *
     * @return types the Item Types created
     */
    @Override
    public Collection<ItemType> getItemTypes() {
        Collection<ItemType> types = new ArrayList<ItemType>();
        return types;
    }

    /***
     * Retrieves the ItemTypes created using annotations by the adapter
     *
     * @return types the Item Types created
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<Class> getAnnotatedItemsClasses() {
        Collection<Class> types = new ArrayList<Class>();
        types.add(HostItem.class);
        types.add(RegistryItem.class);
        types.add(ImageItem.class);
        types.add(ContainerItem.class);
        types.add(VolumeItem.class);
        types.add(PortItem.class);

        return types;
    }

    /***
     * Defining pattern definitions is a way for the Adapter writer to suggest to the client ways of
     * grouping Loom threads so that it can build meaningful tapestries. PatternDefinitions are
     * built by specifying a list of ThreadDefinition objects which in turn include each a
     * QueryDefinition object. QueryDefinition objects specify which aggregation they use as input
     * and which operations are associated with the query. All those objects are in package
     * com.hp.hpl.loom.tapestry. A QueryDefinition must only contain a single input aggregation. Our
     * current Weaver client, has no dynamic user driven pattern creation feature so an Adapter must
     * make sure that each aggregation that it creates is used in at least one registered pattern:
     * if not there is no way that the Weaver client can use out of band knowledge to create queries
     * targeting the unregistered aggregations.
     *
     */
    @Override
    @SuppressWarnings("checkstyle:methodlength")
    public Collection<PatternDefinition> getPatternDefinitions() {
        // Containers
        List<ItemType> containersItemTypes = getItemTypesFromLocalIds(Arrays.asList(Types.CONTAINER_TYPE_ID,
                Types.IMAGE_TYPE_ID, Types.REGISTRY_TYPE_ID, Types.HOST_TYPE_ID));
        PatternDefinition containersPatternDef = createPatternDefinitionWithSingleInputPerThread(CONTAINERS_PATTERN,
                containersItemTypes, CONTAINERS_PATTERN, null, true);

        // Containers (Full)
        List<ItemType> containersFullItemTypes =
                getItemTypesFromLocalIds(Arrays.asList(Types.CONTAINER_TYPE_ID, Types.IMAGE_TYPE_ID,
                        Types.REGISTRY_TYPE_ID, Types.VOLUME_TYPE_ID, Types.PORT_TYPE_ID, Types.HOST_TYPE_ID));
        PatternDefinition containersFullPatternDef = createPatternDefinitionWithSingleInputPerThread(
                CONTAINERS_FULL_PATTERN, containersFullItemTypes, CONTAINERS_FULL_PATTERN, null, false);

        // List<ItemType> hostItemTypes =
        // getItemTypesFromLocalIds(Arrays.asList(Types.HOST_TYPE_ID));
        // createPatternDefinitionWithSingleInputPerThread(HOST_PATTERN, hostItemTypes,
        // HOST_PATTERN, null, true);
        //
        // List<ItemType> registryItemTypes =
        // getItemTypesFromLocalIds(Arrays.asList(Types.REGISTRY_TYPE_ID));
        // createPatternDefinitionWithSingleInputPerThread(REGISTRY_PATTERN, registryItemTypes,
        // REGISTRY_PATTERN, null,
        // false);
        //
        // List<ItemType> imageItemTypes =
        // getItemTypesFromLocalIds(Arrays.asList(Types.IMAGE_TYPE_ID));
        // createPatternDefinitionWithSingleInputPerThread(IMAGE_PATTERN, imageItemTypes,
        // IMAGE_PATTERN, null, false);
        //
        // List<ItemType> containerItemTypes =
        // getItemTypesFromLocalIds(Arrays.asList(Types.CONTAINER_TYPE_ID));
        // PatternDefinition containerPatternDef =
        // createPatternDefinitionWithSingleInputPerThread(CONTAINER_PATTERN,
        // containerItemTypes, CONTAINER_PATTERN, null, false);
        //
        // List<ItemType> volumeItemTypes =
        // getItemTypesFromLocalIds(Arrays.asList(Types.VOLUME_TYPE_ID));
        // createPatternDefinitionWithSingleInputPerThread(VOLUME_PATTERN, volumeItemTypes,
        // VOLUME_PATTERN, null, false);
        //
        // List<ItemType> portItemTypes =
        // getItemTypesFromLocalIds(Arrays.asList(Types.PORT_TYPE_ID));
        // createPatternDefinitionWithSingleInputPerThread(PORT_PATTERN, portItemTypes,
        // PORT_PATTERN, null, false);
        // Collection<PatternDefinition> list = new ArrayList<PatternDefinition>();

        // Introduce a default SORT for all Container threads.
        Map<String, Object> sortParams = new HashMap<String, Object>();
        sortParams.put(QueryOperation.PROPERTY, "core.containerId");
        sortParams.put(QueryOperation.ORDER, "ASC");

        containersPatternDef.getThreads().get(0).getQuery()
                .setOperationPipeline(Arrays.asList(new Operation(DefaultOperations.SORT_BY.toString(), sortParams)));
        containersFullPatternDef.getThreads().get(0).getQuery()
                .setOperationPipeline(Arrays.asList(new Operation(DefaultOperations.SORT_BY.toString(), sortParams)));

        Collection<PatternDefinition> list = new ArrayList<PatternDefinition>();

        list.add(containersPatternDef);
        list.add(containersFullPatternDef);
        // list.add(hostPatternDef);
        // list.add(registryPatternDef);
        // list.add(imagePatternDef);
        // list.add(containerPatternDef);
        // list.add(volumePatternDef);
        // list.add(portPatternDef);

        // list.add(fileSystemPatternDef);
        // list.add(segmentPatternDef);
        // list.add(fileSetPatternDef);
        // list.add(domainPatternDef);
        // list.add(physicalVolumePatternDef);
        // list.add(adeHostPatternDef);
        // list.add(mTreePatternDef);
        // list.add(sTreePatternDef);

        return list;
    }

    /***
     * BaseAdapter deals with user connections/disconnections and sessions by creating/deleting an
     * object implementing the ItemCollector interface for each new session. This object creation is
     * delegated to a specific subclass that must implement the factory method
     * getNewItemCollectorInstance(). An ItemCollector is therefore responsible for collecting Items
     * and creating grounded aggregations within the context of a session. ItemCollector is a
     * Runnable whose core functionality must be implemented in the
     * run(com.hp.hpl.loom.adapter.docker) method. BaseAdapter creates a scheduling worker thread
     * which, every schedulingInterval, tries to run all ItemCollectors known to the adapter. The
     * number of worker threads available to run those ItemCollectors is configured by the field
     * collectThread. Both parameters schedulingInterval and collectThreads are set in the relevant
     * configuration properties file.
     *
     * The schedulingInterval is available in the docker.properties file. The default value is 10000
     * ms (10 seconds). The default number of workingThreads is 1.
     *
     * @return new Item Collector Instance
     *
     */
    @Override
    protected ItemCollector getNewItemCollectorInstance(final Session session, final Credentials creds) {
        return new DockerDistributedCollector(session, this, adapterManager);
    }

    /***
     * Creates a provider, taking into consideration an authentication method.
     *
     * @return A provider that always returns true. I.e. Accepts any credential given by the user
     */
    @Override
    @SuppressWarnings("checkstyle:linelength")
    protected Provider createProvider(final String providerType, final String providerId, final String authEndpoint,
            final String providerName) {
        return new DockerProvider(providerType, providerId, authEndpoint, providerName,
                "com.hp.hpl.loom.adapter.docker");
    }

    /***
     * Loom obtains an adapter specific operations by calling registerQueryOperations() with a map
     * of existing default operations given as input argument so that the newly defined operations
     * can build on top of already existing ones (see Appendix B on the REST API document for more
     * details on the list of default operations implemented by Loom).
     * <p>
     * Note that the name given to the operation is turned into lower case and so is <strong>case
     * insensitive</strong>. At registration time, this name is made unique across the Loom
     * namespace by prefixing it with provider type and id.
     *
     */
    @Override
    @SuppressWarnings("checkstyle:linelength")
    public Map<String, QuadFunctionMeta> registerQueryOperations(
            final Map<String, QuadFunction<PipeLink<Fibre>, Map<String, Object>, Map<OperationErrorCode, String>, OperationContext, PipeLink<Fibre>>> map) {
        // We declare the new operation as a lambda to be stored by Loom's OperationManager, so that
        // our weavers can use it at any time in the future
        // The arguments to this lambda will give us all the context we need so that Loom knows what
        // ItemType, provider, etc. it has to deal with
        // when storing the operation
        QueryOperation extractValue = new QueryOperation((inputs, params, errors, context) -> {

            // convert map of inputs to a list containing the items on the first
            // entry only
            List<Fibre> input = LoomQueryUtils.getFirstInput(inputs, errors);

            // From all the files, we need to know what the maximum size and length are. For that,
            // we need to get stats on the attributes of the files. Loom offers a pre-defined
            // utility for adapter writers to do exactly that

            // From all the images, we need to know what the maximum size and oldest. For that,
            // we need to get statistics on the attributes of the images. Loom offers a pre-defined
            // utility for adapter writers to do exactly that
            Map<String, Number> statMap;
            try {
                statMap = StatUtils.getStatMap(context.getType(), input, context);
            } catch (Exception e) {
                errors.put(OperationErrorCode.NotReadableField, "attempted to fetch unaccessible property data.");
                return new PipeLink<Fibre>(0, new ArrayList<>(0));
            }

            // the keys of this map are the name of the attributes in the ItemType followed by:
            // _avg, _count, _max, _min, _sum, _geoMean, _sumSq _std, _var, _skew, _kurt, _median,
            // _mode From the list of statistics, we now need to fetch the ones we are looking for:
            // size_max
            final float maxSize =
                    statMap.get(SeparableItem.CORE_NAME + "size" + SupportedStats.MAX.toString()).longValue() * 1F;

            // for each item in our input, we now need to update the value variable
            ImageItem imageItem;
            List<Fibre> output = new ArrayList<>(input.size());

            float value;
            for (Fibre image : input) {
                imageItem = (ImageItem) image;
                value = (imageItem.getCore().getVirtualSize() / maxSize);
                imageItem.getCore().setVirtualSize((long) value);
                output.add(imageItem);
            }

            // return list of items with updated values in a format that Loom can understand
            return new PipeLink<Fibre>(0, output);
        }, false);

        // we declare a map to contain a key with the name of the new operation and the actual
        // operation itself. In this example we will declare just one operation
        Map<String, QuadFunctionMeta> ops = new HashMap<>(1);

        // note that the name of the operation (key of the map) will be automatically "nameschemed"
        // by Loom (i.e. preceded by the provider type and id to avoid name clashes)
        // prov.getProviderTypeAndId() + Provider.prov_separator + opId and it will be converted to
        // lower case (OP names are case INSENSITIVE).
        QuadFunctionMeta functionMeta =
                new QuadFunctionMeta(DockerDistributedAdapter.TODO_OPERATION, extractValue, false, false);
        ops.put(DockerDistributedAdapter.TODO_OPERATION, functionMeta);

        return ops;
    }

    /***
     * Fixes the repositories plural error.
     */
    @Override
    protected String createHumanReadableThreadName(final ItemType type) {
        String localId = type.getLocalId();

        if (type.getLocalId().equals(Types.REGISTRY_TYPE_ID)) {
            return "Registries";
        }

        return localId.substring(0, 1).toUpperCase() + localId.substring(1) + "s";
    }
}
