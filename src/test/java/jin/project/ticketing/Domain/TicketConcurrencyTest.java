package jin.project.ticketing.Domain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketConcurrencyTest {

    private static final int INITIAL_QUANTITY = 10;
    private static final int WORKER_COUNT = 100;

    @Test
    void synchronizedIssuePreventsOverselling() throws InterruptedException {
        Ticket ticket = new Ticket(INITIAL_QUANTITY);
        // test에서는 정확한 값 측정을 위해 AtomicInteger 사용
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(WORKER_COUNT);
        // 스레드 실행 시점 조절
        CountDownLatch readyLatch = new CountDownLatch(WORKER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(WORKER_COUNT);

        try {
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
        } finally {
            executorService.shutdownNow();
        }

        assertEquals(10, successCount.get());
        assertEquals(90, failureCount.get());
        assertEquals(0, ticket.getRemainingQuantity());
    }
}
