package pipeline.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.function.BiConsumer;

import static pipeline.logging.LoggingUtils.MDC_STEP_NAME;
import static pipeline.utils.Utils.isLambda;

public interface ErrorHandler<T> extends BiConsumer<T, StepException> {
    Logger log = LoggerFactory.getLogger(ErrorHandler.class);
    String ON_ERROR_NAME = "onError";

    default void apply(T t, StepException e){
        MDC.put(MDC_STEP_NAME, getErrorHandlerName());
        accept(t, e);
    }

    default String getErrorHandlerName(){
        final var consumerName = this.getClass().getSimpleName();

        if(isLambda(consumerName))
            return ON_ERROR_NAME;
        return consumerName;
    }
}
