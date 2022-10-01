package pipeline.step;

import pipeline.Pipeline;
import pipeline.monitor.PipelineMonitor;
import pipeline.monitor.StepMonitor;

import static pipeline.logging.LoggingUtils.getStepName;
import static pipeline.step.Step.StepOutput.failure;

public class StepConnector<I, O, O2> implements Step<I, O2> {
    private final Step<I, O> current;
    private final String currentName;
    private final int currentPosition;
    private final StepMonitor currentMonitor;

    private final Step<O, O2> next;
    private final String nextName;
    private final int nextPosition;
    private final StepMonitor nextMonitor;

    public StepConnector(Step<I, O> current, Step<O, O2> next, PipelineMonitor pipelineMonitor, int stepCount){
        super();
        this.current = current;
        this.currentPosition = stepCount - 1;
        this.currentName = getStepName(current, currentPosition);
        this.currentMonitor = new StepMonitor(pipelineMonitor, this.currentName);

        this.next = next;
        this.nextPosition = stepCount;
        this.nextName = getStepName(next, nextPosition);
        this.nextMonitor = new StepMonitor(pipelineMonitor, this.nextName);
    }

    @Override
    public O2 process(I in) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StepOutput<O2> apply(Pipeline<?, ?> pipeline, String myName, int myPosition, StepMonitor monitor, final I in){


        final var currentOutput = current.apply(pipeline, currentName, currentPosition, currentMonitor, in);
        if(currentOutput.failed()) return failure(currentOutput.getLastException());


        return next.apply(pipeline, nextName, nextPosition, nextMonitor, currentOutput.getResultObj());
    }
}
