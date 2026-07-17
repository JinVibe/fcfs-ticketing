package jin.project.ticketing.Domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TicketServiceConcurrencyTest {

    private static final int INITIAL_QUANTITY = 10;
    private static final int WORKER_COUNT = 100;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAll();
    }

    @Test
    void pessimisticLockPreventsOverselling() throws InterruptedException {
        Ticket ticket = ticketRepository.save(new Ticket(INITIAL_QUANTITY));
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(WORKER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(WORKER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(WORKER_COUNT);

        try {
            for (int worker = 0; worker < WORKER_COUNT; worker++) {
                executorService.submit(() -> {
                    readyLatch.countDown();

                    try {
                        startLatch.await();
                        ticketService.issueTicket(ticket.getId());
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
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
        }

        Ticket persistedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

        assertEquals(10, successCount.get());
        assertEquals(90, failureCount.get());
        assertEquals(0, persistedTicket.getRemainingQuantity());
    }
}
