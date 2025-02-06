package cz.mendelu.xmarik.train_manager.events;

public class EngineChangeEvent {
    private final int addr;
    public EngineChangeEvent(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
