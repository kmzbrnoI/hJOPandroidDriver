package cz.mendelu.xmarik.train_manager.models;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;

/**
 * Class Engine represents an engine.
 */
public class Engine {
    public static final int EXP_SPEED_UNKNOWN = -1;

    public enum Direction {
        FORWARD,
        BACKWARD,
    }

    // data:
    public String name;
    public String owner;
    public String label;
    public String note;
    public int addr;
    public String kind;
    public EngineFunction[] function;

    // state:
    public int stepsSpeed = 0;
    public int kmphSpeed = 0;
    public Direction direction = Direction.FORWARD;
    public boolean total = false;
    public boolean stolen = false;
    public int expSignalCode = -1;
    public String expSignalBlock;
    public int expSpeed = EXP_SPEED_UNKNOWN;

    // client-side props
    public boolean multitrack = false;

    /** Constructs train from server string.
     * @param data in format: název|majitel|označení|poznámka|DCC adresa|třída|číslo soupravy|
            orientace stanovište A|funkce|rychlost ve stupních|rychlost km/h|směr|
            id oblasti řízení|cv_take|cv_release|func_vyznamy|func_type
     */
    public Engine(String data) {
        updateFromServerString(data);
    }

    public void updateFromServerString(String data) {
        ArrayList<String> parsed = ParseHelper.parse(data, "|", "");
        ArrayList<String> functionStrs = ParseHelper.parse(parsed.get(15), ";", "");
        String functionTypes = (parsed.size() > 16) ? parsed.get(16) : "";

        this.name = parsed.get(0);
        this.owner = parsed.get(1);
        this.label = parsed.get(2);
        this.note = parsed.get(3);
        this.addr = Integer.parseInt(parsed.get(4));
        this.kind = parsed.get(5);

        this.stepsSpeed = Integer.parseInt(parsed.get(9));
        this.kmphSpeed = Integer.parseInt(parsed.get(10));
        this.direction = parsed.get(11).equals("1") ? Direction.BACKWARD : Direction.FORWARD;

        this.function = new EngineFunction[parsed.get(8).length()];
        for (int i = 0; i < function.length; i++) {
            String desc = (i < functionStrs.size()) ? functionStrs.get(i) : "";
            char status = (i < parsed.get(8).length()) ? parsed.get(8).charAt(i) : '0';
            char type = (i < functionTypes.length()) ? functionTypes.charAt(i) : 'P';
            this.function[i] = new EngineFunction(i, desc, status == '1', type);
        }
    }

    public void setDirection(Engine.Direction direction) {
        if (this.direction == direction)
            return;
        this.direction = direction;
        String strDir = (direction == Direction.BACKWARD) ? "1" : "0";
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";D;" + strDir);
    }

    public void setSpeedSteps(int steps) {
        if (this.stepsSpeed == steps)
            return;
        this.stepsSpeed = steps;
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";SP-S;" + steps);
    }

    public void setFunc(int id, boolean state) {
        if (function[id].checked == state)
            return;
        function[id].checked = state;
        String strState = state ? "1" : "0";
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";F;" + id + ";" + strState);
    }

    public void setTotal(boolean total) {
        if (this.total == total)
            return;
        this.total = total;
        String strTotal = total ? "1" : "0";
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";TOTAL;" + strTotal);
    }

    public void release() {
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";RELEASE");
    }

    public void please() {
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";PLEASE");
    }

    public void emergencyStop() {
        this.kmphSpeed = 0;
        this.stepsSpeed = 0;
        TCPClient.getInstance().send("-;LOK;" + this.addr + ";STOP");
    }

    public String getTitle() {
        String title = this.addr + " : " + this.name;
        if (!this.label.isEmpty())
            title += " (" + this.label + ")";
        return title;
    }

    public static Direction invertDirection(Direction d) {
        return (d == Direction.FORWARD) ? Direction.BACKWARD : Direction.FORWARD;
    }
}
