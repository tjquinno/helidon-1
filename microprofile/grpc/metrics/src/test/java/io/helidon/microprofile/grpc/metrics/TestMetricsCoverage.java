/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.microprofile.grpc.metrics;

import io.helidon.microprofile.tests.junit5.AddBean;
import io.helidon.microprofile.tests.junit5.AddExtension;
import io.helidon.microprofile.tests.junit5.HelidonTest;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.spi.AnnotatedMethod;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@HelidonTest

@AddBean(CoverageTestBean.class)
@AddExtension(TestGrpcMetricsCdiExtension.class)
public class TestMetricsCoverage {

    @Test
    public void checkThatAllMetricsWereRemovedFromGrpcMethods() {

        Map<AnnotatedMethod, Set<Class<? extends Annotation>>> leftoverAnnotations =
                TestGrpcMetricsCdiExtension.remainingTestBeanMethodAnnotations();

        assertThat("Metrics annotations unexpectedly remain on method", leftoverAnnotations.keySet(), is(empty()));
    }

    @Test
    public void checkThatAllMetricsAnnotationsWereEncountered() {

        Set<Class<? extends Annotation>> metricsAnnotationsUnused =
                new HashSet<>(GrpcMetricsCdiExtension.METRICS_ANNOTATIONS_TO_CHECK);
        metricsAnnotationsUnused.removeAll(TestGrpcMetricsCdiExtension.metricsAnnotationsUsed());

        assertThat("The CoverageTestBean seems not to cover all known annotations", metricsAnnotationsUnused, is(empty()));
    }

    /**
     * Checks that {@code MetricsConfigurer} deals with all metrics annotations that are known to the main Helidon metrics CDI
     * extension.
     * <p>
     *     The {@code MetricsConfigurer} class has been changed to be data-driven. The
     *     {@code metricInfo} map contains all the metric-specific code (mostly as method references) so adding support for a new
     *     metrics (if more are added) happens on that one table.
     * </p>
     * <p>
     *     This test makes sure that the map created there contains an entry for every type of metric annotation known to Helidon
     *     Microprofile metrics.
     * </p>
     */
    @Test
    public void checkForAllMetricsInMetricInfo() {
        Set<Class<? extends Annotation>> metricsAnnotations =
                new HashSet<>(GrpcMetricsCdiExtension.METRICS_ANNOTATIONS_TO_CHECK);

        metricsAnnotations.removeAll(MetricsConfigurer.metricsAnnotationsSupported());
        assertThat("Metrics annotations not supported in MetricsConfigurer", metricsAnnotations, is(empty()));
    }
}
