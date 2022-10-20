package pipeline;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import pipeline.error.ErrorHandler;
import pipeline.error.StepException;
import pipeline.monitor.PipelineMonitor;
import pipeline.pipe.IPipe;
import pipeline.pipe.InitialPipe;
import pipeline.pipe.Pipe;
import pipeline.retry.IRetry;
import pipeline.retry.SimpleRetry;
import pipeline.step.Step;

import java.util.HashMap;
import java.util.Map;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static pipeline.logging.LoggingUtils.MDC_PIPELINE_NAME;

/**
 * Following the conventional pipeline pattern but with some additional functionality,
 * this class has to build and execute the given steps using {@link Step} class
 *
 * @author Bruno Silva de Andrade
 **/
public class Pipeline<I, O> {
    public static final Logger log = LoggerFactory.getLogger(Pipeline.class);
    private static final Map<String, Pipeline<?,?>> register = new HashMap<>();
    private final PipelineMonitor monitor;
    private final String name;
    private final IPipe<I, O> current;

    private final int stepCount;

    private ErrorHandler<I> onErrorHandler;

    private IRetry retry = new SimpleRetry();

    private Pipeline(String name, IPipe<I, O> current, int stepCount) {
        this.name = name;
        this.current = current;
        this.stepCount = stepCount;
        monitor = new PipelineMonitor(name);
        register.put(name, this);
    }

    private Pipeline(String name, IPipe<I, O> current){
        this(name, current, 0);
    }

    public String getName() {
        return name;
    }

    public PipelineMonitor getMonitor() {
        return monitor;
    }

    public Pipeline<I, O> setRetry(IRetry retry) {
        if(isNull(retry)) throw new AssertionError("<retry> cannot be null");
        this.retry = retry;

        return this;
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
        final var nextStartCounting = this.stepCount + 1;

        return new Pipeline<>(name, new Pipe<>(current, next, monitor, nextStartCounting), nextStartCounting);
    }

    public O execute(I input){
        monitor.incrementExecutionCount();

        MDC.put(MDC_PIPELINE_NAME, name);
        final var output = monitor.clockExecution(() -> current.apply(this, input));

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
            stepException = new StepException(name, output.getStepName(), e.getMessage(), e);
            log.error("An unexpected error occurred on the current step", e);
        }

        if(nonNull(onErrorHandler)){
            log.debug("Error handler found! Invoking it");
            monitor.clockOnErrorExecution(() -> onErrorHandler.apply(input, stepException));
            log.debug("Took {}ms", monitor.onErrorClockMean());
            return null;
        }

        throw stepException;
    }

    public static <T> Pipeline<T, T> init(String id) {
        if(isNull(id) || id.isEmpty()) throw new AssertionError("Pipeline name cannot be null or empty");

        synchronized (register){
            if(register.containsKey(id)) throw new IllegalArgumentException("Id: <" + id + "> is already being used");
        }

        return new Pipeline<>(id, new InitialPipe<>());
    }
}
