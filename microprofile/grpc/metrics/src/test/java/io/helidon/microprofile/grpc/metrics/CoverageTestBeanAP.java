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
 */
package io.helidon.microprofile.grpc.metrics;

import io.helidon.microprofile.grpc.metrics.CoverageTestBeanAP.EntryPoint;
import io.helidon.microprofile.metrics.MetricsCdiExtension;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.interceptor.Interceptor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Coverage Test Bean Processor.
 */
@SupportedAnnotationTypes(value = {
        "io.helidon.microprofile.grpc.metrics.CoverageTestBeanAP.EntryPoint",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@EntryPoint
public class CoverageTestBeanAP extends AbstractProcessor {

    @interface EntryPoint { }

    private boolean done;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!done) {
            System.out.println(MetricsCdiExtension.METRIC_ANNOTATIONS);
            // visit the interceptor to get the list of annotations
            Filer filer = processingEnv.getFiler();
            String pkg = CoverageTestBeanAP.class.getPackageName();
            try {
                JavaFileObject fileObject = filer.createSourceFile(pkg + "." + "CoverageTestBean");
                try (BufferedWriter bw = new BufferedWriter(fileObject.openWriter())) {
                    bw.append("package " + pkg + ";\n");
                    bw.append("class CoverageTestBean { }\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            done = true;
        }
        return true;
    }
}
