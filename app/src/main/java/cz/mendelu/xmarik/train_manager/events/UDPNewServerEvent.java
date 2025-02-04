package cz.mendelu.xmarik.train_manager.events;

import cz.mendelu.xmarik.train_manager.models.Server;

public class UDPNewServerEvent {
    public final Server server;

    public UDPNewServerEvent(Server s) {
        this.server = s;
    }
}
