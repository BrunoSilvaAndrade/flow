package pipeline.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public interface ErrorHandler<T> extends BiConsumer<T, Exception> {
    Logger log = LoggerFactory.getLogger(ErrorHandler.class);
}
