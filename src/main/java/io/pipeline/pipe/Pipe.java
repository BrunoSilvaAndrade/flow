package io.pipeline.pipe;


import io.pipeline.monitor.PipelineMonitor;
import io.pipeline.monitor.StepMonitor;
import io.pipeline.step.Step;
import io.pipeline.Pipeline;

public class Pipe<I, O, O2> implements IPipe<I, O2> {
    private final IPipe<I, O> previousPipe;

    private final Step<O, O2> step;
    private final int stepPosition;
    private final StepMonitor stepMonitor;

    public Pipe(IPipe<I, O> previousPipe, Step<O, O2> step, PipelineMonitor pipelineMonitor, int stepCount){
        super();
        this.previousPipe = previousPipe;

        this.step = step;
        this.stepPosition = stepCount;
        this.stepMonitor = new StepMonitor(pipelineMonitor, step.getStepName(stepPosition), stepPosition);
    }

    @Override
    public Step.StepOutput<O2> apply(Pipeline<?, ?> pipeline, final I in){
        final var currentOutput = previousPipe.apply(pipeline, in);

        if(currentOutput.failed())
            return Step.StepOutput.failure(currentOutput.getLastException(), currentOutput.getStepName());

        return step.apply(pipeline, stepPosition, stepMonitor, currentOutput.getResultObj());
    }

    @Override
    public IPipe<I, O2> copyFor(Pipeline<?, ?> pipeline){
        return new Pipe<>(previousPipe.copyFor(pipeline), step, pipeline.getMonitor(), stepPosition);
    }
}
