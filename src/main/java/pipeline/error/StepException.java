package pipeline.error;

public class StepException extends RuntimeException {
    public StepException(String msg){
        super(msg);
    }

    public StepException(String msg, Throwable t) {
        super(msg, t);
    }
}
