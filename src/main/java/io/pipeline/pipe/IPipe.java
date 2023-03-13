package io.pipeline.pipe;

import io.pipeline.step.Step;
import io.pipeline.Pipeline;

public interface IPipe<I, O> {
    Step.StepOutput<O> apply(Pipeline<?,?> pipeline, I in);

    IPipe<I, O> copyFor(Pipeline<?, ?> pipeline);
}
