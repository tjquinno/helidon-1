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
package io.helidon.microprofile.metrics.micrometer;


import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import io.helidon.metrics.micrometer.MicrometerSupport;
import io.helidon.microprofile.metrics.MetricUtil;
import io.helidon.microprofile.metrics.MetricUtil.LookupResult;
import io.helidon.microprofile.metrics.MetricsCdiExtensionBase;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;


/**
 * CDI extension for handling Micrometer artifacts.
 */
public class MicrometerMetricsCdiExtension extends MetricsCdiExtensionBase<Meter> {

    private static final Logger LOGGER = Logger.getLogger(MicrometerMetricsCdiExtension.class.getName());

    private static final List<Class<? extends Annotation>> METRIC_ANNOTATIONS
            = Arrays.asList(Counted.class, Timed.class);

    private final Set<Class<?>> metricsAnnotatedClasses = new HashSet<>();
    private final Set<Class<?>> metricsAnnotatedClassesProcessed = new HashSet<>();

    private final MicrometerSupport micrometerSupport;
    private final MeterRegistry meterRegistry;

    public MicrometerMetricsCdiExtension() {
        super(LOGGER, Set.of(Counted.class, Timed.class), MeterProducer.class);
        micrometerSupport = MicrometerSupport.create();
        meterRegistry = micrometerSupport.registry();
    }

    @Override
    protected <E extends Member & AnnotatedElement>
    void register(E element, Class<?> clazz, LookupResult<? extends Annotation> lookupResult) {
        Annotation annotation = lookupResult.getAnnotation();

        if (annotation instanceof Counted) {
            Counter counter = MeterProducer.produceCounter(meterRegistry,(Counted) annotation);
            LOGGER.log(Level.FINE, () -> "Registered counter " + counter.getId().toString());
        } else if (annotation instanceof Timed) {
            Timer timer = MeterProducer.produceTimer(meterRegistry, (Timed) annotation);
            if (timer != null) {
                LOGGER.log(Level.FINE, () -> "Registered timer " + timer.getId()
                        .toString());
            } else {
                LongTaskTimer longTaskTimer = MeterProducer.produceLongTaskTimer(meterRegistry, (Timed) annotation);
                if (longTaskTimer != null) {
                    LOGGER.log(Level.FINE, () -> "Registered long task timer " + longTaskTimer.getId()
                            .toString());
                }
            }
        }
    }

    /**
     * Initializes the extension prior to bean discovery.
     *
     * @param discovery bean discovery event
     */
    void before(@Observes BeforeBeanDiscovery discovery) {
        LOGGER.log(Level.FINE, () -> "Before bean discovery " + discovery);

        // Initialize our implementation
        MeterRegistryProducer.clear();

        // Register beans manually
        discovery.addAnnotatedType(MeterRegistryProducer.class, "MeterRegistryProducer");
        discovery.addAnnotatedType(MeterProducer.class, "MeterProducer");

        discovery.addAnnotatedType(InterceptorCounted.class, "InterceptorCounted");
        discovery.addAnnotatedType(InterceptorTimed.class, "InterceptorTimed");
    }


}
