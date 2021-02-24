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

import io.helidon.microprofile.grpc.core.Grpc;
import io.helidon.microprofile.grpc.core.GrpcMethod;

import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.enterprise.context.Dependent;

@Dependent
@Grpc()

public class CoverageTestBeanOld {

    // There should be one method with each kind of metric annotation (except Metric).
    @Counted(name = "counter")
    @GrpcMethod(type = io.grpc.MethodDescriptor.MethodType.UNARY)
    public void counted() {}

    @Metered(name = "meter")
    @GrpcMethod(type = io.grpc.MethodDescriptor.MethodType.UNARY)
    public void metered() {}

    @Timed(name = "timer")
    @GrpcMethod(type = io.grpc.MethodDescriptor.MethodType.UNARY)
    public void timed() {}

    @ConcurrentGauge(name = "concurrentGauge")
    @GrpcMethod(type = io.grpc.MethodDescriptor.MethodType.UNARY)
    public void gauged() {}

    @SimplyTimed(name = "simplyTimed")
    @GrpcMethod(type = io.grpc.MethodDescriptor.MethodType.UNARY)
    public void simplyTimed() {}
}
