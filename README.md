Flow
===
---
Flow is a simple implementation of pipeline pattern but with presets for monitoring, logging, and retries.

MAIN FEATURES
===
---
* Flow enforces you make your code modular, reusable, and as small as possible
* Contextualized log, so you don't have to care about where the process is at. When you invoke the log It already knows what step is being processed
* Steps are re-tried using the last state stored in memory.
* Flow uses Micrometer library as a facade of metrics generation, so you can plug your compatible infrastructure register and use the native pipeline metrics generated.
* Metrics auto-generated give you, the pipeline's total execution and errors, the time the pipeline took to complete the process, the step's total executions and errors and the time it took to process and the ErrorHandler's total executions, and how long it took to handle the error

HOW TO USE
===
---

```java

import io.pipeline.Pipeline;

@Sfl4j
public class StringToLong implements Step<String, Long> {
    @Override
    public Long process(String in) {
        log.info("Turning <{}> from String to Long", in);
        return Long.parseLong(in);
    }
}


    final var pipe = Pipeline.init("MyPipeId")
            .next(new StringToLong())
            .next(longId -> {
                //Do something with a lambda if you need
                Step.log.info("Deleting document of id <{}>", longId);
                return myRespository.deleteById(longId);
            })
            //As you know the step above could be written like this:
            .next(myRepository::deleteById)
            .next(isDeleted -> {
                //Let's suppose that the repository would return a boolean indicating if the 
                //document was deleted or not
                if (isDeleted)
                    log.info("Heell yeah");
                else
                    log.info("The document doesnt exists");
            })
            .onError((o, stepException) -> log.error("An error occurred during pipeline processing", stepException));


//You can also copy a pipeline to a new one
//So you can modify the new one without affecting the original one;

    final var copy = pipe.copy("new copy id");
copy.onError((o,stepException)->{
        //So you can for example define a new ErrorHandler on the copy
        throw new RuntimeException("msg",stepException)
        });


        pipe.execute("35467890");
```
---
The statement above would produce a log like this below, following the logback config left in the resources
```text
[17:20:28.241] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[StringToLong] - executing
[17:20:28.242] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[StringToLong] - SimpleRetry: Executing attempt: 1
[17:20:28.242] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[StringToLong] - Turning <35467890> from String to Long
[17:20:28.242] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[StringToLong] - Took 0.00031ms
[17:20:28.242] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(2)] - executing
[17:20:28.242] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(2)] - SimpleRetry: Executing attempt: 1
[17:20:28.242] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(2)] - Deleting document of id <35467890>
[17:20:28.243] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(2)] - Took 0.0431ms
[17:20:28.243] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(3)] - executing
[17:20:28.243] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(3)] - SimpleRetry: Executing attempt: 1
[17:20:28.243] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(3)] - Heell yeah
[17:20:28.243] [main] DEBUG PipeLogger - Pipeline[MyPipeId] Step[Lambda$position(3)] - Took 0.0011ms
```
---
And assuming that you chose a Prometheus micrometer plugin and have an exporter already running, It would give some metrics like this:

The values and quantity of metrics are not real, It's just to give you an idea

```text
pipeline_executions_with_error_total{pipeline_name="MyPipeId",} 0.0
pipeline_onError_execution_time_seconds_max{pipeline_name="MyPipeId",} 0.0
pipeline_onError_execution_time_seconds_count{pipeline_name="MyPipeId",} 0.0
pipeline_onError_execution_time_seconds_sum{pipeline_name="MyPipeId",} 0.0
pipeline_executions_total{pipeline_name="MyPipeId",} 1.0
pipeline_execution_time_seconds_active_count{pipeline_name="MyPipeId",} 0.0
pipeline_execution_time_seconds_duration_sum{pipeline_name="MyPipeId",} 0.0
pipeline_execution_time_seconds_max{pipeline_name="MyPipeId",} 0.0
pipeline_step_failures_total{pipeline_name="MyPipeId",step_name="StringToLong",step_position="1",} 0.0
pipeline_step_failures_total{pipeline_name="MyPipeId",step_name="Lambda$position(2)",step_position="2",} 0.0
pipeline_step_execution_time_seconds_count{pipeline_name="MyPipeId",step_name="StringToLong",step_position="1",} 0.0
pipeline_step_execution_time_seconds_sum{pipeline_name="MyPipeId",step_name="StringToLong",step_position="1",} 0.0
pipeline_step_execution_time_seconds_count{pipeline_name="MyPipeId",step_name="Lambda$position(2)",step_position="2",} 0.0
pipeline_step_execution_time_seconds_sum{pipeline_name="MyPipeId",step_name="Lambda$position(2)",step_position="2",} 0.0
pipeline_step_execution_time_seconds_max{pipeline_name="MyPipeId",step_name="StringToLong",step_position="1",} 0.0
pipeline_step_execution_time_seconds_max{pipeline_name="MyPipeId",step_name="Lambda$position(2)",step_position="2",} 0.0
```