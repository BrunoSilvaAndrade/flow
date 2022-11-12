package pipeline.pipe;

import pipeline.Pipeline;
import pipeline.step.Step;

public interface IPipe<I, O> {
    Step.StepOutput<O> apply(Pipeline<?,?> pipeline, I in);

    IPipe<I, O> copyFor(Pipeline<?, ?> pipeline);
}
