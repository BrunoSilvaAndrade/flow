package io.pipeline.pipe;


import io.pipeline.step.Step;
import io.pipeline.Pipeline;

public class InitialPipe<T> implements IPipe<T, T> {
    @Override
    public Step.StepOutput<T> apply(Pipeline<?, ?> pipeline, final T in){
        return Step.StepOutput.success(in);
    }

    @Override
    public IPipe<T, T> copyFor(Pipeline<?, ?> pipeline) {
        return new InitialPipe<>();
    }
}
