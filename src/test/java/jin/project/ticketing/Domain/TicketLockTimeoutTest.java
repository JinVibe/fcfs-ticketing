package jin.project.ticketing.Domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TicketLockTimeoutTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAll();
    }

    @Test
    void lockRequestTimesOutWhenRowIsAlreadyLocked() throws Exception {
        Ticket ticket = ticketRepository.save(new Ticket(1));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch releaseLockLatch = new CountDownLatch(1);

        Future<?> lockHolder = executorService.submit(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    ticketRepository.findByIdWithPessimisticLock(ticket.getId())
                            .orElseThrow();
                    lockAcquiredLatch.countDown();

                    try {
                        releaseLockLatch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                })
        );

        try {
            assertTrue(lockAcquiredLatch.await(5, TimeUnit.SECONDS));

            assertThrows(
                    PessimisticLockingFailureException.class,
                    () -> transactionTemplate.executeWithoutResult(status ->
                            ticketRepository.findByIdWithPessimisticLock(ticket.getId())
                                    .orElseThrow()
                    )
            );
        } finally {
            releaseLockLatch.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
            executorService.shutdownNow();
        }
    }
}
