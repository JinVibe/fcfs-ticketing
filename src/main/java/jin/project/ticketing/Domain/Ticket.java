package jin.project.ticketing.Domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int totalQuantity;
    private int remainingQuantity;

    protected Ticket() {
    }

    public Ticket(int totalQuantity) {
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("총 티켓 수량은 0 이상이어야 합니다.");
        }

        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
    }

    public synchronized void issue() { // 잔여 수량 존재 시 티켓 수 -1
        // synchronized 추가 
        if (remainingQuantity <= 0) {
            throw new OutOfStockException();
        }

        remainingQuantity--;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public Long getId() {
        return id;
    }
}
