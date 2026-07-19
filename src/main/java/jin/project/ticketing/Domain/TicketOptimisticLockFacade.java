package jin.project.ticketing.Domain;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class TicketOptimisticLockFacade {

    private static final int MAX_RETRY_COUNT = 100;

    private final TicketOptimisticLockService ticketOptimisticLockService;

    public TicketOptimisticLockFacade(
            TicketOptimisticLockService ticketOptimisticLockService
    ) {
        this.ticketOptimisticLockService = ticketOptimisticLockService;
    }

    public void issueTicket(Long ticketId) {
        for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
            try {
                ticketOptimisticLockService.issueTicket(ticketId);
                return;
            } catch (ObjectOptimisticLockingFailureException exception) {
                // 낙관적 락 충돌은 최신 버전을 다시 읽어 재시도한다.
            }
        }

        throw new IllegalStateException("낙관적 락 재시도 횟수를 초과했습니다.");
    }
}
