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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import io.helidon.microprofile.metrics.MetricsCdiExtension;
import org.eclipse.microprofile.metrics.annotation.Metric;

/**
 * Generates test beans for metrics annotation coverage testing.
 */
@SupportedAnnotationTypes(value = {"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)

public class CoverageTestBeanAP extends AbstractProcessor {

    private static final String PACKAGE = CoverageTestBeanAP.class.getPackageName();

    private static final List<String> LINE_TEMPLATES = List.of(
            "package %1$s;",
            "import %2$s;",
            "import io.helidon.microprofile.grpc.core.GrpcMethod;",
            "import javax.enterprise.context.Dependent;",
            "@Dependent",
            "public class %4$s extends CoverageTestBeanBase {",
            "    @GrpcMethod(type = io.grpc.MethodDescriptor.MethodType.UNARY)",
            "    @%3$s(name = \"coverage%3$s\")",
            "    public void measuredMethod() { }",
            "}");

    private static final List<String> CATALOG_LINE_TEMPLATES = List.of(
            "package %1$s;",
            "import java.util.List;",
            "import javax.enterprise.context.ApplicationScoped;",
            "@ApplicationScoped",
            "class CoverageTestBeanCatalog implements TestMetricsCoverage.GeneratedBeanCatalog {",
            "    @Override",
            "    public List<Class<? extends CoverageTestBeanBase>> generatedBeanClasses() {",
            "        return List.of(%2$s);",
            "    }",
            "}");

    private boolean done = false; // suppress odd double executions

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!done) {
            Filer filer = processingEnv.getFiler();

            Set<Class<? extends Annotation>> annotationsToTest = new HashSet<>(MetricsCdiExtension.METRIC_ANNOTATIONS);
            annotationsToTest.remove(Metric.class);

            Set<String> generatedSimpleClassNames = new HashSet<>();

            System.out.println("Starting CoverageTestBeanXXX generation");
            for (Class<? extends Annotation> a : annotationsToTest) {
                String generatedClassName = "CoverageTestBean" + a.getSimpleName();
                generatedSimpleClassNames.add(generatedClassName + ".class");

                try {
                    JavaFileObject fileObject = filer.createSourceFile(outputFileName(generatedClassName));
                    try (PrintWriter pw = new PrintWriter(fileObject.openWriter())) {
                        LINE_TEMPLATES.forEach(t -> pw.println(
                                String.format(t, PACKAGE, a.getName(), a.getSimpleName(), generatedClassName)));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Error generating " + outputFileName(generatedClassName), e);
                }
            }
            String classNamesList = generatedSimpleClassNames.stream().collect(Collectors.joining(","));
            try {
                JavaFileObject catalogFileObject =
                        filer.createSourceFile(outputFileName("CoverageTestBeanCatalog"));
                try (PrintWriter pw = new PrintWriter(catalogFileObject.openWriter())) {
                    CATALOG_LINE_TEMPLATES.forEach(t -> pw.println(
                            String.format(t, PACKAGE, classNamesList
                                    )));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error generating generated bean catalog", e);
            }

            done = true;
        }
        return true;
    }

    private static String outputFileName(String simpleClassName) {
        return String.format("%s.%s", PACKAGE, simpleClassName);
    }
}
