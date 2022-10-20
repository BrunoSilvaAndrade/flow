package pipeline.pipe;


import pipeline.Pipeline;
import pipeline.monitor.PipelineMonitor;
import pipeline.monitor.StepMonitor;
import pipeline.step.Step;

import static pipeline.step.Step.StepOutput.failure;

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
            return failure(currentOutput.getLastException(), currentOutput.getStepName());

        return step.apply(pipeline, stepPosition, stepMonitor, currentOutput.getResultObj());
    }
}
