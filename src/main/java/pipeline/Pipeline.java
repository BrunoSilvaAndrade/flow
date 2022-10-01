package pipeline;


import pipeline.error.ErrorHandler;
import pipeline.error.StepException;
import pipeline.monitor.PipelineMonitor;
import pipeline.retry.IRetry;
import pipeline.retry.SimpleRetry;
import pipeline.step.InitialStep;
import pipeline.step.Step;
import pipeline.step.StepConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static pipeline.logging.LoggingUtils.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Following the conventional pipeline pattern but with some additional functionality,
 * this class has to build and execute the given steps using {@link Step} class
 *
 * @author Bruno Silva de Andrade
 **/
public class Pipeline<I, O> {
    public static final Logger log = LoggerFactory.getLogger(Pipeline.class);
    private final PipelineMonitor monitor;
    private final String name;
    private final Step<I, O> current;

    private final int stepCount;

    private ErrorHandler<I> onErrorHandler;

    private IRetry retry = new SimpleRetry();

    private Pipeline(String name, Step<I, O> current, int stepCount) {
        this.name = name;
        this.current = current;
        this.stepCount = stepCount;
        monitor = new PipelineMonitor(name);
    }

    public String getName() {
        return name;
    }

    public PipelineMonitor getMonitor() {
        return monitor;
    }

    public void setRetry(IRetry retry) {
        if(isNull(retry)) throw new AssertionError("<retry> cannot be null");
        this.retry = retry;
    }

    public IRetry getRetry() {
        return retry;
    }

    public Pipeline<I, O> onError(ErrorHandler<I> errorHandler){
        this.onErrorHandler = errorHandler;
        return this;
    }

    public <O2> Pipeline<I, O2> next(Step<O, O2> next) {
        if(isNull(next)) throw new AssertionError("<next> cannot be null");

        final var stepCount = this.stepCount + 1;
        return new Pipeline<>(name, new StepConnector<>(current, next, monitor, stepCount), stepCount);
    }

    public O execute(I input){
        monitor.incrementExecutionCount();

        MDC.put(MDC_PIPELINE_NAME, name);
        final var output = monitor.clockExecution(() -> current.apply(this, null, 0, null, input));

        if(output.succeeded()){
            return output.getResultObj();
        }

        monitor.incrementFailureCount();

        final Exception e = output.getLastException();
        final StepException stepException;

        if(e instanceof StepException){
            stepException = (StepException)e;
            log.warn("Step intentionally interrupted cause: {}", e.getMessage());
        }else{
            stepException = new StepException(e.getMessage(), e);
            log.error("An unexpected error occurred on the current step", e);
        }

        if(nonNull(onErrorHandler)){
            MDC.put(MDC_STEP_NAME, getErrorHandlerName(onErrorHandler));
            log.debug("Error handler found! Invoking it");
            monitor.clockOnErrorExecution(() -> onErrorHandler.accept(input, e));
            log.debug("Took {}ms", monitor.onErrorClockMean());
            return null;
        }

        throw stepException;
    }

    public static <T> Pipeline<T, T> init(String id) {
        if(isNull(id) || id.isEmpty()) throw new AssertionError("Pipeline name cannot be null or empty");

        return new Pipeline<>(id, new InitialStep<>(), 0);
    }
}
