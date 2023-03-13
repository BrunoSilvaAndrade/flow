package io.pipeline.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.micrometer.core.instrument.Metrics.globalRegistry;
import static java.util.Objects.isNull;

public class PipelineMonitor {
    public static final String PIPELINE_NAME_TAG = "pipeline.name";
    public static final String PIPELINE_EXECUTION_TIME_METRIC = "pipeline.execution.time";
    public static final String PIPELINE_EXECUTIONS_COUNT_METRIC = "pipeline.executions";
    public static final String PIPELINE_FAILURES_COUNT_METRIC = "pipeline.executions.with.error";

    public static final String PIPELINE_ON_ERROR_EXECUTION_TIME_METRIC = "pipeline.onError.execution.time";

    private final List<StepMonitor> stepMonitors = new ArrayList<>();
    private final String pipelineName;
    private MeterRegistry meterRegistry = globalRegistry;
    private LongTaskTimer executionsTimer;
    private Timer onErrorExecutionsTimer;
    private Counter executionsCount;
    private Counter failuresCount;

    public PipelineMonitor(String pipelineName) {
        this.pipelineName = pipelineName;
        initMeters();
    }

    public void initMeters(){
        this.onErrorExecutionsTimer = Timer.builder(PIPELINE_ON_ERROR_EXECUTION_TIME_METRIC).tag(PIPELINE_NAME_TAG, pipelineName).register(meterRegistry);
        this.executionsTimer = LongTaskTimer.builder(PIPELINE_EXECUTION_TIME_METRIC).tag(PIPELINE_NAME_TAG, pipelineName).register(meterRegistry);
        this.executionsCount = Counter.builder(PIPELINE_EXECUTIONS_COUNT_METRIC).tag(PIPELINE_NAME_TAG, pipelineName).register(meterRegistry);
        this.failuresCount = Counter.builder(PIPELINE_FAILURES_COUNT_METRIC).tag(PIPELINE_NAME_TAG, pipelineName).register(meterRegistry);
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public void setMeterRegistry(MeterRegistry meterRegistry){
        if(isNull(meterRegistry)) throw new AssertionError("<meterRegistry> cannot be null");

        this.meterRegistry = meterRegistry;
        initMeters();

        stepMonitors.forEach(StepMonitor::updateMeterRegistry);
    }


    public void incrementExecutionCount(){
        executionsCount.increment();
    }

    public void incrementFailureCount(){
        failuresCount.increment();
    }

    public <T> T clockExecution(Supplier<T> f){
        return executionsTimer.record(f);
    }

    public void clockOnErrorExecution(Runnable r){
        onErrorExecutionsTimer.record(r);
    }

    public double onErrorClockMean(){
        return onErrorExecutionsTimer.mean(TimeUnit.MILLISECONDS);
    }

    public void subscribe(StepMonitor stepMonitor){
        this.stepMonitors.add(stepMonitor);
    }

    public String getPipelineName() {
        return pipelineName;
    }
}
