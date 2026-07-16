package jin.project.ticketing.Domain;

public class Ticket {

    private final int totalQuantity; // 총 티켓 수
    private int remainingQuantity; // 잔여 티켓 수

    public Ticket(int totalQuantity) {
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("총 티켓 수량은 0 이상이어야 합니다.");
        }

        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
    }

    public void issue() { // 잔여 수량 존재 시 티켓 수 -1
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
}
