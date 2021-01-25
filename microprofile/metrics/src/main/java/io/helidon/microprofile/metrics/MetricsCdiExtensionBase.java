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
package io.helidon.microprofile.metrics;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.inject.Qualifier;
import javax.interceptor.Interceptor;

import io.helidon.microprofile.metrics.MetricUtil.LookupResult;

import static io.helidon.microprofile.metrics.MetricUtil.lookupAnnotation;
import static io.helidon.microprofile.metrics.MetricUtil.registerMetric;

/**
 * Abstract superclass of metrics-related CDI extensions.
 *
 * @param <T> Common supertype of all metrics managed by the extension
 */
public abstract class MetricsCdiExtensionBase<T> implements Extension {
    private final Map<Bean<?>, AnnotatedMember<?>> producers = new HashMap<>();

    private final Set<Class<?>> annotatedClasses = new HashSet<>();
    private final Set<Class<?>> annotatedClassesProcessed = new HashSet<>();
    private final Set<Class<? extends Annotation>> annotations;

    private final Logger logger;
    private final Class<?> ownProducer;

    protected MetricsCdiExtensionBase(Logger logger, Set<Class<? extends Annotation>> annotations, Class<?> ownProducer) {
        this.logger = logger;
        this.annotations = annotations;
        this.ownProducer = ownProducer; // class containing producers provided by this module
    }

    protected Set<Class<?>> annotatedClasses() {
        return annotatedClasses;
    }

    protected Set<Class<?>> annotatedClassesProcessed() {
        return annotatedClassesProcessed;
    }

    protected void clearAnnotationInfo(@Observes AfterDeploymentValidation adv) {
        if (logger.isLoggable(Level.FINE)) {
            Set<Class<?>> annotatedClassesIgnored = new HashSet<>(annotatedClasses());
            annotatedClassesIgnored.removeAll(annotatedClassesProcessed());
            if (!annotatedClassesIgnored.isEmpty()) {
                logger.log(Level.FINE, () ->
                        "Classes originally found with metrics annotations that were not processed, probably "
                                + "because they were vetoed:" + annotatedClassesIgnored.toString());
            }
        }
        annotatedClasses.clear();
        annotatedClassesProcessed.clear();
    }

    /**
     * Observes all beans but immediately dismisses ones for which the Java class was not previously noted
     * during {@code ProcessAnnotatedType} (which recorded only classes with metrics annotations).
     *
     * @param pmb event describing the managed bean being processed
     */
    protected void registerMetrics(@Observes ProcessManagedBean<?> pmb) {
        AnnotatedType<?> type =  pmb.getAnnotatedBeanClass();
        Class<?> clazz = type.getJavaClass();
        if (!annotatedClasses.contains(clazz)) {
            return;
        }
        // Recheck for Interceptor.
        if (type.isAnnotationPresent(Interceptor.class)) {
            logger.log(Level.FINE, "Ignoring metrics defined on type " + clazz.getName()
                    + " because a CDI portable extension added @Interceptor to it dynamically");
            return;
        }
        annotatedClassesProcessed.add(clazz);

        logger.log(Level.FINE, () -> "Processing annotations for " + clazz.getName());

        // Process methods keeping non-private declared on this class
        for (AnnotatedMethod<?> annotatedMethod : type.getMethods()) {
            if (Modifier.isPrivate(annotatedMethod.getJavaMember().getModifiers())) {
                continue;
            }
            annotations.forEach(annotation -> {
                for (LookupResult<? extends Annotation> lookupResult : MetricUtil.lookupAnnotations(
                        type, annotatedMethod, annotation)) {
                    // For methods, register the metric only on the declaring
                    // class, not subclasses per the MP Metrics 2.0 TCK
                    // VisibilityTimedMethodBeanTest.
                    if (lookupResult.getType() != MetricUtil.MatchingType.METHOD
                            || clazz.equals(annotatedMethod.getJavaMember()
                            .getDeclaringClass())) {
                        registerMetric(annotatedMethod.getJavaMember(), clazz, lookupResult);
                    }
                }
            });
        }

        // Process constructors
        for (AnnotatedConstructor<?> annotatedConstructor : type.getConstructors()) {
            Constructor c = annotatedConstructor.getJavaMember();
            if (Modifier.isPrivate(c.getModifiers())) {
                continue;
            }
            annotations.forEach(annotation -> {
                LookupResult<? extends Annotation> lookupResult
                        = lookupAnnotation(c, annotation, clazz);
                if (lookupResult != null) {
                    registerMetric(c, clazz, lookupResult);
                }
            });
        }
    }

    /**
     * Registers a metric based on an annotation site.
     *
     * @param element the Element hosting the annotation
     * @param clazz the class on which the hosting Element appears
     * @param lookupResult result of looking up an annotation on an element, its class, and its ancestor classes
     * @param <E> type of method or field or constructor
     */
    protected abstract <E extends Member & AnnotatedElement>
    void register(E element, Class<?> clazz, LookupResult<? extends Annotation> lookupResult);

    /**
     * Checks to make sure the annotated type is not abstract and is not an interceptor.
     *
     * @param pat {@code ProcessAnnotatedType} event
     * @return true if the annotated type should be kept for potential processing later; false otherwise
     */
    protected boolean checkCandidateMetricClass(ProcessAnnotatedType<?> pat) {
        AnnotatedType<?> annotatedType = pat.getAnnotatedType();
        Class<?> clazz = annotatedType.getJavaClass();

        // Abstract classes are handled when we deal with a concrete subclass. Also, ignore if @Interceptor is present.
        if (annotatedType.isAnnotationPresent(Interceptor.class)
                || Modifier.isAbstract(clazz.getModifiers())) {
            logger.log(Level.FINER, () -> "Ignoring " + clazz.getName()
                    + " with annotations " + annotatedType.getAnnotations()
                    + " for later processing: "
                    + (Modifier.isAbstract(clazz.getModifiers()) ? "abstract " : "")
                    + (annotatedType.isAnnotationPresent(Interceptor.class) ? "interceptor " : ""));
            return false;
        }
        logger.log(Level.FINE, () -> "Accepting " + clazz.getName() + " for later bean processing");
        return true;
    }

    /**
     * Make sure the annotated type is neither abstract nor an interceptor and stores the Java class.
     *
     * @param pat {@code ProcessAnnotatedType} event
     * @return true if the annotated type should be kept for potential processing later; false otherwise
     */
    protected boolean checkAndRecordCandidateMetricClass(ProcessAnnotatedType<?> pat) {
        boolean result = checkCandidateMetricClass(pat);
        if (result) {
            annotatedClasses.add(pat.getAnnotatedType().getJavaClass());
        }
        return result;
    }


    /**
     * Records metric producer fields defined by the application. Ignores producers
     * with non-default qualifiers and library producers.
     *
     * @param ppf Producer field.
     */
    protected void recordProducerFields(@Observes ProcessProducerField<? extends T, ?> ppf) {
        recordProducerMember("recordProducerFields", ppf.getAnnotatedProducerField(), ppf.getBean());
    }

    /**
     * Records metric producer methods defined by the application. Ignores producers
     * with non-default qualifiers and library producers.
     *
     * @param ppm Producer method.
     */
    protected void recordProducerMethods(@Observes ProcessProducerMethod<? extends T, ?> ppm) {
        recordProducerMember("recordProducerMethods", ppm.getAnnotatedProducerMethod(), ppm.getBean());
    }

    protected Map<Bean<?>, AnnotatedMember<?>> producers() {
        return producers;
    }

    private void recordProducerMember(String logPrefix, AnnotatedMember<?> member, Bean<?> bean) {
        logger.log(Level.FINE, () -> logPrefix + " " + bean.getBeanClass());
        if (!ownProducer.equals(bean.getBeanClass())) {
            Set<Class<? extends Annotation>> siteAnnotationTypes = new HashSet<>();

            for (Annotation memberAnnotation : member.getAnnotations()) {
                Class<? extends Annotation> memberAnnotationType = memberAnnotation.annotationType();
                if (annotations.contains(memberAnnotationType)) {
                    siteAnnotationTypes.add(memberAnnotationType);
                }
            }
            if (!siteAnnotationTypes.isEmpty()) {
                Optional<Class<? extends Annotation>> hasQualifier
                        = siteAnnotationTypes
                        .stream()
                        .filter(annotationType -> annotationType.isAnnotationPresent(Qualifier.class))
                        .findFirst();
                // Ignore producers with non-default qualifiers
                if (!hasQualifier.isPresent() || Default.class.isInstance(hasQualifier.get())) {
                    producers.put(bean, member);
                }
            }
        }
    }


}
