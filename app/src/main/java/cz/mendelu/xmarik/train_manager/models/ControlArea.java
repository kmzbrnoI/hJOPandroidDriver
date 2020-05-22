package cz.mendelu.xmarik.train_manager.models;

/**
 * ControlArea represents single station at the railroad. Each station consists of id and name.
 */
public class ControlArea {
    public String id;
    public String name;

    public ControlArea(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
