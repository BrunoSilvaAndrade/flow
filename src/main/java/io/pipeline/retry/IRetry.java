package io.pipeline.retry;

public interface IRetry {
    @FunctionalInterface
    interface Retryable<T>{
        T doTry() throws Exception;
    }

    <T> T doTry(Retryable<T> retryable) throws Exception;
}
