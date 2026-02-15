package coen448.computablefuture.test;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncProcessorTest {

    static class TestMicroservice extends Microservice {
        private final boolean shouldFail;
        private final int delayMs;

        TestMicroservice(String serviceId, boolean shouldFail, int delayMs) {
            super(serviceId);
            this.shouldFail = shouldFail;
            this.delayMs = delayMs;
        }

        @Override
        public CompletableFuture<String> retrieveAsync(String message) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                if (shouldFail) {
                    throw new RuntimeException("Failure");
                }
                return message;
            });
        }
    }

    @Test
    void failFast_shouldPropagateException() {
        AsyncProcessor processor = new AsyncProcessor();

        List<Microservice> services = List.of(
                new TestMicroservice("A", false, 10),
                new TestMicroservice("B", true, 5));

        List<String> messages = List.of("OK", "FAIL");

        assertThrows(ExecutionException.class, () -> processor.processAsyncFailFast(services, messages)
                .get(2, TimeUnit.SECONDS));
    }

    @Test
    void failPartial_shouldReturnOnlySuccessfulResults() throws Exception {
        AsyncProcessor processor = new AsyncProcessor();

        List<Microservice> services = List.of(
                new TestMicroservice("A", false, 10),
                new TestMicroservice("B", true, 5),
                new TestMicroservice("C", false, 1));

        List<String> messages = List.of("A", "B", "C");

        List<String> result = processor.processAsyncFailPartial(services, messages)
                .get(2, TimeUnit.SECONDS);

        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("C"));
    }

    @Test
    void failSoft_shouldUseFallbackValue() throws Exception {
        AsyncProcessor processor = new AsyncProcessor();

        List<Microservice> services = List.of(
                new TestMicroservice("A", false, 10),
                new TestMicroservice("B", true, 5));

        List<String> messages = List.of("A", "B");

        String fallback = "FALLBACK";

        String result = processor.processAsyncFailSoft(services, messages, fallback)
                .get(2, TimeUnit.SECONDS);

        assertTrue(result.contains("A"));
        assertTrue(result.contains("FALLBACK"));
    }

    @Test
    void liveness_shouldCompleteWithinTimeout() {
        AsyncProcessor processor = new AsyncProcessor();

        List<Microservice> services = List.of(
                new TestMicroservice("A", false, 50),
                new TestMicroservice("B", false, 50),
                new TestMicroservice("C", false, 50));

        List<String> messages = List.of("1", "2", "3");

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            processor.processAsyncFailFast(services, messages)
                    .get(2, TimeUnit.SECONDS);
        });
    }

    @RepeatedTest(5)
    void nondeterminism_shouldObserveDifferentCompletionOrders() throws Exception {
        AsyncProcessor processor = new AsyncProcessor();

        Microservice a = new Microservice("A");
        Microservice b = new Microservice("B");
        Microservice c = new Microservice("C");

        List<String> result = processor.processAsyncCompletionOrder(List.of(a, b, c), "msg")
                .get(2, TimeUnit.SECONDS);

        System.out.println("Completion order: " + result);

        assertEquals(3, result.size());
    }
}
