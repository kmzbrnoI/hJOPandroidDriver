package cz.mendelu.xmarik.train_manager.events;

public class LokChangeEvent {
    private final int addr;
    public LokChangeEvent(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
