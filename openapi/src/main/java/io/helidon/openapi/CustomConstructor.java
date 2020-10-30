/*
 * Copyright (c) 2019-2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.helidon.openapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Specialized SnakeYAML constructor for modifying {@code Node} objects for OpenAPI types that have properties of type {@code Map}
 * and also support {@code add and remove operations for those properties.
 * <p>
 * Several MicroProfile OpenAPI interfaces behave this way. For example, {@code Paths} has a {@code pathItems} property, with the
 * typical get and set methods, as well as {@code addPathItem} and {@code removePathItem} methods.
 * Similarly with {@code SecurityRequirement} and its {@code schemes} property and {@code addScheme} and
 * {@code removeScheme} methods.
 *
 * <p>
 * The YAML/JSON representation for OpenAPI does not have explicit entries for {@code pathItems} within {@code Paths} or
 * {@code schemes} within {@code SecurityRequirement}. (If they did, we could have let the default SnakeYAML mapping deal with
 * deal with constructs like this.) Instead, we need to tailor the parsing of and output to YAML/JSON.
 *
 */
final class CustomConstructor extends Constructor {

    // maps OpenAPI interfaces which have a Map<?, type> property to info about the implementation type (for cases in which the
    // mapped-to type is NOT itself a list
//    private static final Map<Class<?>, Class<?>> CHILD_MAP_TYPES = new HashMap<>();
    private static final Map<Class<?>, TypeWithMapPropertyInfo<?, ?>> CHILD_MAP_TYPE_INFO = new HashMap<>();

    // maps OpenAPI interfaces which have a Map<?, List<type>> property to the type that appears in the list
    private static final Map<Class<?>, Class<?>> CHILD_MAP_OF_LIST_TYPES = new HashMap<>();

    static {
//        CHILD_MAP_TYPES.put(Paths.class, PathItem.class);
//        CHILD_MAP_TYPES.put(Callback.class, PathItem.class);
//        CHILD_MAP_TYPES.put(Content.class, MediaType.class);
//        CHILD_MAP_TYPES.put(APIResponses.class, APIResponse.class);
        CHILD_MAP_TYPE_INFO.put(Paths.class, new TypeWithMapPropertyInfo<Paths, PathItem>(Paths.class, PathItem.class,
                (String key, PathItem c, Paths p) -> p.addPathItem(key, c)));
        CHILD_MAP_TYPE_INFO.put(Callback.class, new TypeWithMapPropertyInfo<Callback, PathItem>(Callback.class, PathItem.class,
                (String key, PathItem c, Callback p) -> p.addPathItem(key, c)));
        CHILD_MAP_TYPE_INFO.put(Content.class, new TypeWithMapPropertyInfo<Content, MediaType>(Content.class, MediaType.class,
                (String key, MediaType c, Content p) -> p.addMediaType(key, c)));
        CHILD_MAP_TYPE_INFO.put(APIResponses.class,
                new TypeWithMapPropertyInfo<APIResponses, APIResponse>(APIResponses.class, APIResponse.class,
                (String key, APIResponse c, APIResponses p) -> p.addAPIResponse(key, c)));
        CHILD_MAP_OF_LIST_TYPES.put(SecurityRequirement.class, String.class);
    }

    CustomConstructor(TypeDescription td) {
        super(td);
    }

    @Override
    protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
        Class<?> parentType = node.getType();
        if (CHILD_MAP_TYPE_INFO.containsKey(parentType)) {
            TypeWithMapPropertyInfo<?, ?> info = CHILD_MAP_TYPE_INFO.get(parentType);
            node.getValue().forEach(tuple -> {
                Node valueNode = tuple.getValueNode();
                if (valueNode.getType() == Object.class) {
                    valueNode.setType(info.childType);
                }
                Node keyNode = tuple.getKeyNode();
                if (keyNode instanceof ScalarNode) {
                    ScalarNode scalarKeyNode = (ScalarNode) keyNode;
                    String keyValue = scalarKeyNode.getValue();
                    
                }

            });
            Map<Object, Object> map = createDefaultMap(node.getValue().size());
            super.constructMapping2ndStep(node, map);

            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                info.add(mapping, entry.getKey(), entry.getValue());
            }

        } else if (CHILD_MAP_OF_LIST_TYPES.containsKey(parentType)) {
            Class<?> childType = CHILD_MAP_OF_LIST_TYPES.get(parentType);
            node.getValue().forEach(tuple -> {
                Node valueNode = tuple.getValueNode();
                if (valueNode.getNodeId() == NodeId.sequence) {
                    SequenceNode seqNode = (SequenceNode) valueNode;
                    seqNode.setListType(childType);
                }
            });
        }
        super.constructMapping2ndStep(node, mapping);
    }

    @FunctionalInterface
    interface ChildAdder<C, P> {
        P apply(String k, C c, P p);

        default ChildAdder<C, P> andThen(
                Function<? super P, ? extends P> after) {
            Objects.requireNonNull(after);
            return (String key, C child, P parent) -> after.apply(apply(key, child, parent));
        }
    }

    private static class TypeWithMapPropertyInfo<P, C> {

        private final Class<P> parentType;
        private final Class<C> childType;
        private final ChildAdder<C, P> childAdder; //


        TypeWithMapPropertyInfo(Class<P> parentType, Class<C> childType, ChildAdder<C, P> childAdder) {
            this.parentType = parentType;
            this.childType = childType;
            this.childAdder = childAdder;
        }

        void add(Object p, Object key, Object c) {
            childAdder.apply(key.toString(), childType.cast(c), parentType.cast(p));
        }
    }
}
