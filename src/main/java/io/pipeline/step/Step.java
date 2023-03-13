package io.pipeline.step;


import io.pipeline.logging.LoggingUtils;
import io.pipeline.monitor.StepMonitor;
import io.pipeline.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import io.pipeline.Pipeline;

import static java.lang.String.format;

public interface Step<I, O> {
     Logger log = LoggerFactory.getLogger(Step.class);

     String LAMBDA_TEMPLATE = "Lambda$position(%d)";

    enum StepStatus{
        SUCCESS, FAILURE;
    }
     class StepOutput<O>{
         private final O resultObj;
         private final StepStatus status;

         private final Exception lastException;

         private final String stepName;


         private StepOutput(O resultObj, StepStatus status, Exception lastException, String stepName) {
             this.resultObj = resultObj;
             this.status = status;
             this.lastException = lastException;
             this.stepName = stepName;
         }

         public O getResultObj() {
             return resultObj;
         }

         public Exception getLastException() {
             return lastException;
         }

         public String getStepName() {
             return stepName;
         }

         public boolean failed(){
             return StepStatus.FAILURE.equals(status);
         }

         public boolean succeeded(){
             return StepStatus.SUCCESS.equals(status);
         }

         public static <O> StepOutput<O> success(O resultObj){
             return new StepOutput<>(resultObj, StepStatus.SUCCESS, null, null);
         }

         public static <O> StepOutput<O> failure(Exception lastException, String stepName){
             return new StepOutput<>(null, StepStatus.FAILURE, lastException, stepName);
         }
     }

    default StepOutput<O> apply(Pipeline<?, ?> pipeline, int myPosition, StepMonitor monitor, final I in) {
        final var myName = getStepName(myPosition);

        MDC.put(LoggingUtils.MDC_STEP_NAME, myName);
        MDC.put(LoggingUtils.MDC_STEP_POSITION, String.valueOf(myPosition));

        log.debug("executing");
        final StepOutput<O> result =  monitor.clock(()  -> {

            try {
                return StepOutput.success(pipeline.getRetry().doTry(() -> process(in)));
            } catch (Exception e) {
                monitor.incrementFailureCount();
                return StepOutput.failure(e, myName);
            }

        });
        log.debug("Took {}ms", monitor.clockMean());

        return result;
    }

    default String getStepName(int stepPosition){
        final var stepName = this.getClass().getSimpleName();

        if(Utils.isLambda(stepName))
            return format(LAMBDA_TEMPLATE, stepPosition);

        return stepName;
    }

    O process(I in);
}
