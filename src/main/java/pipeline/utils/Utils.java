package pipeline.utils;

import static java.util.Objects.isNull;

public class Utils {
    private Utils(){}
    public static final String LAMBDA_REFERENCE = "$$Lambda$";

    public static boolean isLambda(String className){
        if(isNull(className)) throw new AssertionError("<className> cannot be null");
        return className.contains(LAMBDA_REFERENCE);
    }
}
