package jin.project.ticketing.Domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTest { // 굳이 짜야 하는 코드일까?

    @Test
    void issueDecreasesQuantity() {
        Ticket ticket = new Ticket(10);

        ticket.issue();

        assertEquals(9, ticket.getRemainingQuantity());
    }

    @Test
    void issueThrowsWhenOutOfStock() {
        Ticket ticket = new Ticket(0);

        assertThrows(OutOfStockException.class, ticket::issue);
    }
}
