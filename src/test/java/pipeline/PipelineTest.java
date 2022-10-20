package pipeline;


import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pipeline.error.ErrorHandler;
import pipeline.error.StepException;
import pipeline.step.Step;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PipelineTest {

    @BeforeAll
    static void setUp(){
        Metrics.addRegistry(new SimpleMeterRegistry());
    }
    private static class Sum1Step implements Step<Integer, Integer> {

        @Override
        public Integer process(Integer in) {
            return in + 1;
        }
    }

    @Test
    void testDuplicatedPipelineName(){
        Pipeline.init("UniqueID");
        assertThrows(IllegalArgumentException.class, () -> Pipeline.init("UniqueID"));
    }

    @Test
    void testPipelineAssertions(){
        assertThrows(AssertionError.class, () -> Pipeline.init(null));
        assertThrows(AssertionError.class, () -> Pipeline.init(""));

        var pipe = Pipeline.init("id");
        assertThrows(AssertionError.class, () -> pipe.next(null));
    }

    @Test
    void testPipeline(){
        var result = Pipeline.<Integer>init("Test")
                .next(new Sum1Step())
                .next(in -> in + 1)
                .execute(1);

        assertEquals(3, result);
    }

    @Test
    void testPipelineErrorHandlingWithClass(){
        final var atomicBool = new AtomicBoolean(false);
        final var atomicThrowable = new AtomicReference<Throwable>();

        class TestErrorHandler implements ErrorHandler<String> {

            @Override
            public void accept(String integer, StepException e) {
                log.info("Handling error");

                atomicBool.set(true);
                atomicThrowable.set(e);
            }
        }

        final var pipe = Pipeline.<String>init("testPipelineErrorHandlingWithClass")
                .next(str -> {
                    throw new NoSuchElementException();
                })
                .onError(new TestErrorHandler());


        pipe.execute("Any input");

        assertTrue(atomicBool.get());
        assertNotNull(atomicThrowable.get());
    }


    @Test
    void testPipelineErrorHandlingWithLambda(){
        final var atomicBool = new AtomicBoolean(false);
        final var atomicThrowable = new AtomicReference<Throwable>();

        final var pipe = Pipeline.<String>init("testPipelineErrorHandlingWithLambda")
                .next(str -> {
                    throw new NoSuchElementException();
                })
                .onError((str, e) -> {
                    Step.log.warn("Handling error of pipeline {} and step {}", e.getPipelineName(), e.getStepName());
                    atomicBool.set(true);
                    atomicThrowable.set(e);
                });


        pipe.execute("Any input");

        assertTrue(atomicBool.get());
        assertNotNull(atomicThrowable.get());

        pipe.onError(null);

        assertThrows(StepException.class, () -> pipe.execute("Any input"));
    }


    @Test
    void testStepErrorException(){
        var pipe = Pipeline.<String>init("testStepErrorException")
                .next(in -> {
                    throw new NoSuchElementException();
                });

        assertThrows(StepException.class, () -> pipe.execute("Any input"));
    }

    @Test
    void testIntentionalStepException(){
        var pipe = Pipeline.<String>init("testIntentionalStepException")
                .next(in -> {
                    throw new StepException("MyPipeline", "MyStep","A intentional StepException throwing");
                });

        assertThrows(StepException.class, () -> pipe.execute("Some input"));
    }
}
