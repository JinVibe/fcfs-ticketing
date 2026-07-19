package jin.project.ticketing.Domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TicketDeadlockTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAll();
    }

    @Test
    void oppositeLockOrderCausesDeadlock() throws Exception {
        Ticket firstTicket = ticketRepository.save(new Ticket(1));
        Ticket secondTicket = ticketRepository.save(new Ticket(1));
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier firstLockBarrier = new CyclicBarrier(2);

        Future<Boolean> firstTransaction = executorService.submit(() ->
                executeWithLockOrder(
                        firstTicket.getId(),
                        secondTicket.getId(),
                        firstLockBarrier
                )
        );
        Future<Boolean> secondTransaction = executorService.submit(() ->
                executeWithLockOrder(
                        secondTicket.getId(),
                        firstTicket.getId(),
                        firstLockBarrier
                )
        );

        try {
            boolean firstTransactionAborted = firstTransaction.get(10, TimeUnit.SECONDS);
            boolean secondTransactionAborted = secondTransaction.get(10, TimeUnit.SECONDS);

            assertTrue(
                    firstTransactionAborted || secondTransactionAborted,
                    "DB가 데드락 상태의 트랜잭션을 중단하지 않았습니다."
            );
        } finally {
            executorService.shutdownNow();
        }
    }

    private boolean executeWithLockOrder(
            Long firstTicketId,
            Long secondTicketId,
            CyclicBarrier firstLockBarrier
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                ticketRepository.findByIdWithPessimisticLock(firstTicketId)
                        .orElseThrow();
                await(firstLockBarrier);
                ticketRepository.findByIdWithPessimisticLock(secondTicketId)
                        .orElseThrow();
            });
            return false;
        } catch (PessimisticLockingFailureException exception) {
            return true;
        }
    }

    private void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("데드락 테스트 대기가 중단되었습니다.", exception);
        } catch (BrokenBarrierException | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("두 트랜잭션이 첫 번째 락을 획득하지 못했습니다.", exception);
        }
    }
}
