package jin.project.ticketing.Domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public void issueTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithPessimisticLock(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));

        ticket.issue();
    }
}
