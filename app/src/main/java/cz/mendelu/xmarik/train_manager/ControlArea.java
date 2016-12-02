package cz.mendelu.xmarik.train_manager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ja on 17. 6. 2016.
 */
public class ControlArea {
    private String id;
    private String name;
    private List<Train> trains;
    private String storedTxt;

    public ControlArea(String id, String name, List<Train> trains) {
        this.id = id;
        this.name = name;
        this.trains = trains;
        this.storedTxt =  this.name +"-|||-"+ this.id;
    }

    public ControlArea(String id, String name) {
        this.id = id;
        this.name = name;
        this.trains = new ArrayList<>();
        this. storedTxt= this.name +"-|||-"+ this.id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Train> getTrains() {
        return trains;
    }

    public String getString() {
        return this.storedTxt;
    }

    public void addTrain(Train newTrain) {
        if(this.trains == null)
        {
            trains = new ArrayList<>();
        }
        trains.add(newTrain);
    }

}
