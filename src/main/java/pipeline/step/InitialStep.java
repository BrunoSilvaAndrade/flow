package pipeline.step;


public class InitialStep<T> implements Step<T, T> {
    @Override
    public T process(T in) {
        log.info("Pipeline invoked");
        log.debug("{} started with input: {}", getClass().getSimpleName(), in);

        return in;
    }
}
