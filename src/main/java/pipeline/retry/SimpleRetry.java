package pipeline.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* This class is a provided implementation of a SimpleRetry strategy
* */
public final class SimpleRetry implements IRetry{
    public static final Logger log = LoggerFactory.getLogger(SimpleRetry.class);
    public static final int DEFAULT_ATTEMPTS = 5;

    private final int attempts;


    public SimpleRetry(int attempts){
        this.attempts = attempts;
    }

    public SimpleRetry(){
        this(DEFAULT_ATTEMPTS);
    }


    /**
    * This method takes the initial number of attempts stored in this class,
    * and it starts making the first attempt without checking if there is remaining retries.
    * If it was a successful execution then it returns the result value
    * if not it will check if there is remaining retries, if so it will retry and decrement
    * the remaining retries
    *
    * @param retryable {@link Retryable<T>}
    * @return the generic type parametrized
    * */
    @Override
    public <T> T doTry(Retryable<T> retryable) throws Exception {
        var remainingAttempts =  attempts;
        Exception lastException;

        do{
            final var currentAttempt = (attempts + 1) - remainingAttempts;

            try {
                log.debug("SimpleRetry: Executing attempt: {}", currentAttempt);
                return retryable.doTry();
            }catch (Exception e){
                log.warn("SimpleRetry: Attempt {} failed - exception class: {} - msg: {}", currentAttempt, e.getClass().getName(), e.getMessage());

                lastException = e;
                remainingAttempts--;
            }

        }while(remainingAttempts > 0);

        log.error("All {} attempts failed", attempts);
        throw lastException;
    }
}
