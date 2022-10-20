package pipeline.error;

public class StepException extends RuntimeException {
    private final String pipelineName;
    private final String stepName;

    public StepException(String pipelineName, String stepName, String msg){
        super(msg);
        this.pipelineName = pipelineName;
        this.stepName = stepName;
    }

    public StepException(String pipelineName, String stepName, String msg, Throwable t){
        super(msg, t);
        this.pipelineName = pipelineName;
        this.stepName = stepName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStepName() {
        return stepName;
    }
}
