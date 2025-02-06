package cz.mendelu.xmarik.train_manager.events;

public class EngineRemoveEvent {
    private final int addr;
    public EngineRemoveEvent(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
