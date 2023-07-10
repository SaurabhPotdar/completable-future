package com.tce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CompletableFutureTests {

    private static final String JOB_ID = "jobId";

    @DisplayName("Chaining CFs")
    @Test
    void chainingCompletableFutures() {
        final CompletableFuture<Map<String, String>> future = CompletableFuture.supplyAsync(() -> {
            final Map<String, String> map = new HashMap<>();
            map.put(JOB_ID, "11");
            return map;
        });
        final String jobId = future.thenApply(map -> {
                    System.out.println(map.get(JOB_ID));
                    return map;
                })
                .thenApply(map -> map.get(JOB_ID))
                .join();
        assertEquals("11", jobId);
    }

    private record User(String id) {
    }

    private CompletableFuture<User> getUserDetails() {
        return CompletableFuture.supplyAsync(() -> new User("usr-1"));
    }

    private CompletableFuture<Integer> getCreditRating(final User ignored) {
        return CompletableFuture.supplyAsync(() -> 800);
    }

    @DisplayName("thenCompose")
    @Test
    void thenCompose() {
        //getCreditRating() runs when we get result from getUserDetails()
        final Integer rating = getUserDetails().thenCompose(this::getCreditRating).join();
        assertEquals(800, rating);
    }

    @DisplayName("thenCombine")
    @Test
    void thenCombine() {
        //We want two Futures to run independently and do something after both are complete.
        System.out.println("Retrieving weight.");
        final CompletableFuture<Double> weightInKgFuture = CompletableFuture.supplyAsync(() -> 65.0);

        System.out.println("Retrieving height.");
        final CompletableFuture<Double> heightInCmFuture = CompletableFuture.supplyAsync(() -> 177.8);

        System.out.println("Calculating BMI.");
        final CompletableFuture<Double> combinedFuture = weightInKgFuture
                .thenCombine(heightInCmFuture, (weightInKg, heightInCm) -> {
                    Double heightInMeter = heightInCm / 100;
                    return weightInKg / (heightInMeter * heightInMeter);
                });
		assertEquals(20, Math.floor(combinedFuture.join()));
    }

	@Nested
	@DisplayName("Multiple tasks in parallel")
	class Parallel {

		//If we don't pass out executor then CF uses ForkJoinPool.commonPool
		private final Executor executor = Executors.newFixedThreadPool(5);

		final List<String> strings = List.of("11", "12", "13", "14");

		private CompletableFuture<Integer> parse(final String s) {
			return CompletableFuture.supplyAsync(() -> {
				System.out.println(s + " : " + Thread.currentThread().getName());
				return Integer.parseInt(s);
			}, executor);
		}

		private Integer parseInt(final String s) {
			System.out.println(s + " : " + Thread.currentThread().getName());
			return Integer.parseInt(s);
		}

		@DisplayName("Return void")
		@Test
		void returnVoid() {
			final List<CompletableFuture<Integer>> futures = strings.stream()
					.map(this::parse)
					.toList();
			//Returns void
			final CompletableFuture<Void> allFutures = CompletableFuture.allOf(
					futures.toArray(new CompletableFuture[0]));
			final Void ignored = allFutures.join();
		}

		@DisplayName("Return response")
		@Test
		void returnResponse() {
			final List<CompletableFuture<Integer>> futures = strings.stream()
					.map(this::parse)
					.toList();
			final List<Integer> result = futures.stream().map(CompletableFuture::join).toList();
			System.out.println(result);
		}

		@DisplayName("Parallel stream")
		@Test
		void parallelStream() {
			//uses ForkJoinPool.commonPool
			final List<Integer> result = strings.parallelStream().map(this::parseInt).toList();
			System.out.println(result);
		}

	}

}