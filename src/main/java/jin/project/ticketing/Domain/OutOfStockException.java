package jin.project.ticketing.Domain;

public class OutOfStockException extends RuntimeException { // 재고 없을 때

    public OutOfStockException() {
        super("남은 티켓이 없습니다.");
    }
}
