package cz.mendelu.xmarik.train_manager.events;

public class EngineExpSpeedSetToZero {
    private final int addr;
    public EngineExpSpeedSetToZero(int addr) {
        this.addr = addr;
    }
    public int getAddr() { return addr; }
}
