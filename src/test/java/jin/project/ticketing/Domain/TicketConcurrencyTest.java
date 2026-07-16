package jin.project.ticketing.Domain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketConcurrencyTest { // 핵심

    private static final int INITIAL_QUANTITY = 10;
    private static final int WORKER_COUNT = 100; // 100개 동시 작업
    private static final int MAX_ATTEMPTS = 10000;

    @Test
    void concurrentIssueCausesRaceCondition() throws InterruptedException {
        boolean raceConditionDetected = false;
        ExecutorService executorService = Executors.newFixedThreadPool(WORKER_COUNT);

        try {
            for (int attempt = 0; attempt < MAX_ATTEMPTS && !raceConditionDetected; attempt++) {
                Ticket ticket = new Ticket(INITIAL_QUANTITY);
                AtomicInteger successCount = new AtomicInteger();
                AtomicInteger failureCount = new AtomicInteger();
                CountDownLatch readyLatch = new CountDownLatch(WORKER_COUNT);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(WORKER_COUNT);

                for (int worker = 0; worker < WORKER_COUNT; worker++) {
                    executorService.submit(() -> {
                        readyLatch.countDown();

                        try {
                            startLatch.await();
                            ticket.issue();
                            successCount.incrementAndGet();
                        } catch (OutOfStockException exception) {
                            failureCount.incrementAndGet();
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }

                assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
                startLatch.countDown();
                assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

                raceConditionDetected =
                        successCount.get() > INITIAL_QUANTITY
                                || ticket.getRemainingQuantity() != 0;
            }
        } finally {
            executorService.shutdownNow();
        }

        assertTrue(raceConditionDetected, "동시 발급 과정에서 Race Condition이 관찰되지 않았습니다.");
    }
}
