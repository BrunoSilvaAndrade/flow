package pipeline.step;


import pipeline.Pipeline;
import pipeline.monitor.StepMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static pipeline.logging.LoggingUtils.*;

public interface Step<I, O> {
     Logger log = LoggerFactory.getLogger(Step.class);

    enum StepStatus{
        SUCCESS, FAILURE;
    }
     class StepOutput<O>{
         private final O resultObj;
         private final StepStatus status;
         private final Exception lastException;


         private StepOutput(O resultObj, StepStatus status, Exception lastException) {
             this.resultObj = resultObj;
             this.status = status;
             this.lastException = lastException;
         }

         public O getResultObj() {
             return resultObj;
         }

         public StepStatus getStatus() {
             return status;
         }

         public Exception getLastException() {
             return lastException;
         }

         public boolean failed(){
             return StepStatus.FAILURE.equals(status);
         }

         public boolean succeeded(){
             return StepStatus.SUCCESS.equals(status);
         }

         public static <O> StepOutput<O> success(O resultObj){
             return new StepOutput<>(resultObj, StepStatus.SUCCESS, null);
         }

         public static <O> StepOutput<O> failure(Exception lastException){
             return new StepOutput<>(null, StepStatus.FAILURE, lastException);
         }
     }

     O process(I in) throws Exception;

    default StepOutput<O> apply(Pipeline<?, ?> pipeline, String myName, int myPosition, StepMonitor monitor, final I in) {


        MDC.put(MDC_STEP_NAME, myName);
        MDC.put(MDC_STEP_POSITION, String.valueOf(myPosition));

        log.debug("executing");
        final StepOutput<O> result =  monitor.clock(()  -> {

            try {
                return StepOutput.success(pipeline.getRetry().doTry(() -> process(in)));
            } catch (Exception e) {
                monitor.incrementFailureCount();
                return StepOutput.failure(e);
            }

        });
        log.debug("Took {}ms", monitor.clockMean());

        return result;
    }
}
