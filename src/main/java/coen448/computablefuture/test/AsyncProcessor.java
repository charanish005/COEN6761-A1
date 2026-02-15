package coen448.computablefuture.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AsyncProcessor {

    public CompletableFuture<String> processAsync(List<Microservice> services) {
        List<CompletableFuture<String>> futures = services.stream()
                .map(s -> s.retrieveAsync("hello"))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.joining(" ")));
    }

    public CompletableFuture<List<String>> processAsyncCompletionOrder(
            List<Microservice> services, String message) {

        List<String> completed = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> trackers = new ArrayList<>();

        for (Microservice s : services) {
            CompletableFuture<Void> t = s.retrieveAsync(message)
                    .thenAccept(completed::add);
            trackers.add(t);
        }

        return CompletableFuture.allOf(trackers.toArray(new CompletableFuture[0]))
                .thenApply(v -> new ArrayList<>(completed));
    }

    public CompletableFuture<String> processAsyncFailFast(
            List<Microservice> services,
            List<String> messages) {

        if (services == null || messages == null) {
            throw new IllegalArgumentException("services/messages cannot be null");
        }
        if (services.size() != messages.size()) {
            throw new IllegalArgumentException("services and messages must have the same size");
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            futures.add(services.get(i).retrieveAsync(messages.get(i)));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.joining(" ")));
    }

    public CompletableFuture<List<String>> processAsyncFailPartial(
            List<Microservice> services,
            List<String> messages) {

        if (services == null || messages == null) {
            throw new IllegalArgumentException("services/messages cannot be null");
        }
        if (services.size() != messages.size()) {
            throw new IllegalArgumentException("services and messages must have the same size");
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            CompletableFuture<String> f = services.get(i)
                    .retrieveAsync(messages.get(i))
                    .handle((val, ex) -> ex == null ? val : null);
            futures.add(f);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<String> processAsyncFailSoft(
            List<Microservice> services,
            List<String> messages,
            String fallbackValue) {

        if (services == null || messages == null) {
            throw new IllegalArgumentException("services/messages cannot be null");
        }
        if (services.size() != messages.size()) {
            throw new IllegalArgumentException("services and messages must have the same size");
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            CompletableFuture<String> f = services.get(i)
                    .retrieveAsync(messages.get(i))
                    .exceptionally(ex -> fallbackValue);
            futures.add(f);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.joining(" ")));
    }
}
