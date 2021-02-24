# gRPC Metrics

## Adding support for a new metric type
From time to time, the metrics spec evolves and new metrics appear. 
To add support for a new metric, follow these steps.

### Update `GrpcMetrics`
The class contains a very simple method for each metric type like this:
```java
public static GrpcMetrics concurrentGauge() {...}
```
Just add a new method following the same pattern for the new metric type.

### Update `MetricsConfigurer`
Update the map near the top of the file to add the new metric annotation. 
The rest of the logic uses the map rather than hard-coded methods.

### Update `GrpcMetricsCdiExtension`

On the `registerMetrics` method, add the new metrics annotation(s) to the `@WithAnnotation` list of 
metrics 
annotations.

### Update `CoverageTestBean`
This class contains one method for each metric type. Follow the pattern to add a new method, 
annotated with the new metric annotation.