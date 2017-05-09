package cz.mendelu.xmarik.train_manager;

import java.util.Comparator;

import cz.mendelu.xmarik.train_manager.models.Server;

/**
 * Created by ja on 12. 10. 2016.
 */

public class CustomServerComparator implements Comparator<Server> {


    @Override
    public int compare(Server server, Server t1) {
        return server.name.compareTo(t1.name);
    }

}
