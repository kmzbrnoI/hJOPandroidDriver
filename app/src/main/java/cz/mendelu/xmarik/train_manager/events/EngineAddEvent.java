package cz.mendelu.xmarik.train_manager.events;

public class EngineAddEvent {
    private final int addr;
    public EngineAddEvent(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
