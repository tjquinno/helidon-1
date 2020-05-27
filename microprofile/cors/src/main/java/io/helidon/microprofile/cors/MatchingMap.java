/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.microprofile.cors;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import io.helidon.webserver.PathMatcher;

/**
 * Map-like data structure in which not only does a key map to a value, but a key pattern also maps to a value.
 *
 * <p>Keys are always sought using {@link #computeIfAbsent(Object, Function)} rather than {@code get}.
 * The key-to-value mapping is used if it exists. Otherwise, the key value is checked against the currently-known
 * patterns. The value associated with the first pattern that matches the key is used as the value for that key, and
 * the key/value pair is added to the direct key-to-value map.
 * </p>
 *
 * <p>
 * If the key-to-value map contains no entry for the key, and if no known pattern matches the key, then
 * {@code computeIfAbsent} invokes the provided function -- which takes the key and returns a
 * pattern-matcher-value triple -- and updates the three internal maps: key-to-value, pattern-to-value, and
 * pattern-to-matcher.
 * </p>
 *
 * @param <K> key type
 * @param <V> value type
 */
class MatchingMap<K, V> {

    /**
     * Gathers all useful information when dealing with a missing key, primarily used as a way for the caller-provided
     * factory function in {@link #computeIfAbsent(Object, Function)} to return three values:
     * the pattern (which matches the key), the {@code PathMatcher} for checking candidate key values using the pattern,
     * and the value to which the key and the pattern should map.
     *
     * @param <K> key type
     * @param <V> value type
     */
    static class PatternMatcherValue<K, V>  {

        private final K pattern;
        private final PathMatcher matcher;
        private final V value;

        private PatternMatcherValue(K pattern, V value) {
            this.pattern = pattern;
            this.matcher = pattern == null ? null : PathMatcher.create(pattern.toString());
            this.value = value;
        }

        static <K, V> PatternMatcherValue<K, V> create(K pattern, V value) {
            return new PatternMatcherValue<>(pattern, value);
        }
    }

    // Records path pattern-to-PathMatcher mappings
    private final Map<K, PathMatcher> patternToMatcher = new LinkedHashMap<>();

    private final Map<PathMatcher, V> matcherToValue = new LinkedHashMap<>();

    // Records exact key-to-value mappings
    private final Map<K, V> values = new HashMap<>();

    static <K, V> MatchingMap<K, V> create() {
        return new MatchingMap<>();
    }

    private MatchingMap() {
    }

    private V matchThenPut(K key) {
        // Find the first successful matcher, if any, and use its value to populate the key-to-value map.
        String keyString = key.toString();
        for (Map.Entry<PathMatcher, V> entry : matcherToValue.entrySet()) {
            PathMatcher.Result matchResult = entry.getKey().match(keyString);
            if (matchResult.matches()) {
                values.put(key, entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Computes the value for the specified key, either
     * <ul>
     *     <li>retrieving it from the values map, if necessary finding a compatible matcher and using its associated
     *     value for the key's value in the value map, or</li>
     *     <li>creating a new matcher and a new value for a new pattern, adding the pattern/matcher pair to the pattern/matcher
     *     map, adding the matcher/value pair to the matcherToValue map, and adding the key/value pair to the values map.</li>
     * </ul>
     *
     * @param key key for which a value is required
     * @param patternMatcherValueFactory factory to create the triple of pattern, matcher, and value if no value for
     *                                   the key can be found or created without a new matcher
     * @return corresponding value, null if none can be found or created
     */
    V computeIfAbsent(K key, Function<? super K, ? extends PatternMatcherValue<K, V>> patternMatcherValueFactory) {
        if (values.containsKey(key)) {
            return values.get(key);
        }

        // Retrieving using matchers might populate the key-to-value map with the result, so check it again.
        V result = matchThenPut(key);
        if (values.containsKey(key)) {
            return result;
        }

        PatternMatcherValue<K, V> tuple = patternMatcherValueFactory.apply(key);
        if (tuple != null) {
            if (tuple.pattern != null) {
                patternToMatcher.put(tuple.pattern, tuple.matcher);
            }
            if (tuple.matcher != null) {
                matcherToValue.put(tuple.matcher, tuple.value);
            }
            values.put(key, tuple.value);
            return tuple.value;
        }

        // Record that this key has no value to speed up future searches for this key.
        values.put(key, null);
        return null;
    }
}
