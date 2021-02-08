package cz.mendelu.xmarik.train_manager.events;

public class LokTotalChangeErrorEvent {
    private final int addr;
    private final boolean total;
    public LokTotalChangeErrorEvent(int addr, boolean total) {
        this.addr = addr;
        this.total = total;
    }
    public int getAddr() { return addr; }
    public boolean getTotal() { return total; }
}
