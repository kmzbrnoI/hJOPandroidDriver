package cz.mendelu.xmarik.train_manager.events;

public class LokRemoveEvent {
    private final int addr;
    public LokRemoveEvent(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
