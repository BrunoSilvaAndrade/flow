package pipeline.logging;

import pipeline.step.Step;

import java.util.function.BiConsumer;

import static java.lang.String.format;
import static java.util.Objects.isNull;

public final class LoggingUtils {
    private LoggingUtils(){}
    public static final String MDC_STEP_POSITION = "step.position";
    public static final String MDC_PIPELINE_NAME = "pipeline.name";
    public static final String MDC_STEP_NAME = "step.name";

    public static final String LAMBDA_TEMPLATE = "Lambda.position(%d)";
    public static final String LAMBDA_REFERENCE = "$$Lambda$";
    public static final String ON_ERROR_NAME = "onError";

    public static String getStepName(Step<?,?> step, int stepPosition){
        final var stepName = step.getClass().getSimpleName();

        if(isLambda(stepName))
            return format(LAMBDA_TEMPLATE, stepPosition);

        return stepName;
    }

    public static String getErrorHandlerName(BiConsumer<?, ?> consumer){
        final var consumerName = consumer.getClass().getSimpleName();

        if(isLambda(consumerName))
            return ON_ERROR_NAME;
        return consumerName;
    }

    public static boolean isLambda(String className){
        if(isNull(className)) throw new AssertionError("<className> cannot be null");
        return className.contains(LAMBDA_REFERENCE);
    }
}
