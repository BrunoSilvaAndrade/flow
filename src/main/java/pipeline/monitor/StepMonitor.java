package pipeline.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static pipeline.monitor.PipelineMonitor.PIPELINE_NAME_TAG;

public class StepMonitor {
    public static final String STEP_NAME_TAG = "step.name";
    public static final String STEP_POSITION_TAG = "step.position";

    public static final String STEP_EXECUTION_TIME_METRIC = "pipeline.step.execution.time";
    public static final String STEP_FAILURES_COUNT_METRIC = "pipeline.step.failures";

    private final PipelineMonitor pipelineMonitor;
    private final String stepName;

    private final String stepPosition;

    private Counter failuresCount;
    private Timer executionsTimer;

    public StepMonitor(PipelineMonitor pipelineMonitor, String stepName, int stepPosition) {
        this.pipelineMonitor = pipelineMonitor;
        this.stepPosition = String.valueOf(stepPosition);
        this.stepName = stepName;

        pipelineMonitor.subscribe(this);
        initMeters();
    }

    public void initMeters(){
        final var register = pipelineMonitor.getMeterRegistry();
        final var pipelineName = pipelineMonitor.getPipelineName();

        executionsTimer = Timer.builder(STEP_EXECUTION_TIME_METRIC)
                .tags(PIPELINE_NAME_TAG, pipelineName, STEP_NAME_TAG, stepName, STEP_POSITION_TAG, stepPosition).register(register);

        failuresCount = Counter.builder(STEP_FAILURES_COUNT_METRIC)
                .tags(PIPELINE_NAME_TAG, pipelineName, STEP_NAME_TAG, stepName, STEP_POSITION_TAG, stepPosition).register(register);
    }

    public void updateMeterRegistry(){
        initMeters();
    }

    public void incrementFailureCount(){
        failuresCount.increment();
    }

    public <T> T clock(Supplier<T> f){
        return executionsTimer.record(f);
    }

    public double clockMean(){
        return executionsTimer.mean(TimeUnit.MILLISECONDS);
    }
}
