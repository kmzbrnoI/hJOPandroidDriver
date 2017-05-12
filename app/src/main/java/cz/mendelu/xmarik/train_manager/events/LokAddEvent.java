package cz.mendelu.xmarik.train_manager.events;

public class LokAddEvent {
    private final int addr;
    public LokAddEvent(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
