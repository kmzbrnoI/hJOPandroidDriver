package cz.mendelu.xmarik.train_manager.models;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.ServerList;
import cz.mendelu.xmarik.train_manager.TCPClientApplication;
import cz.mendelu.xmarik.train_manager.TrainFunction;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;

/**
 * Class Train represents a train.
 */
public class Train {
    // data:
    public String name;
    public String owner;
    public String label;
    public String note;
    public int addr;
    public String kind;
    public TrainFunction function[];

    // state:
    public int stepsSpeed = 0;
    public int kmphSpeed = 0;
    public boolean direction = false;
    public boolean total = false;
    public boolean stolen = false;

    /**
     * test constructor
     * is not necessary in other cases
     * @param name
     * @param controled
     * @param function
     * @param speed
     * @param direction
     * @param id
     */
    /*public Train(String name, boolean controled, boolean[] function, int speed, boolean direction, String id) {
        this.name = name;
        this.base = "-;LOK;" + id;
        this.controled = controled;
        this.function = new TrainFunction[function.length];
        for (int i = 0; i < function.length; i++) {
            this.function[i] = new TrainFunction("F" + i, "funkce", function[i]);
        }
        this.speed = speed;
        this.direction = direction;
        this.err = null;
        this.authorized = false;
        this.statusOk = true;
        functionNames = new ArrayList<>();
    }*/

    /** Constructs train from server string.
     * @param data in format: název|majitel|označení|poznámka|DCC adresa|třída|číslo soupravy|
            orientace stanovište A|funkce|rychlost ve stupních|rychlost km/h|směr|
            id oblasti řízení|cv_take|cv_release|func_vyznamy
     */
    public Train(String data) {
        updateFromServerString(data);
    }

    public void updateFromServerString(String data) {
        ArrayList<String> parsed = ParseHelper.parse(data, "|", "");
        ArrayList<String> functionStrs = ParseHelper.parse(parsed.get(15), ";", "");

        name = parsed.get(0);
        owner = parsed.get(1);
        label = parsed.get(2);
        note = parsed.get(3);
        addr = Integer.parseInt(parsed.get(4));
        kind = parsed.get(5);

        stepsSpeed = Integer.parseInt(parsed.get(9));
        kmphSpeed = Integer.parseInt(parsed.get(10));
        direction = parsed.get(11).equals("1");

        function = new TrainFunction[parsed.get(8).length()];
        for (int i = 0; i < function.length; i++) {
            String desc = "";
            if (i < functionStrs.size()) desc = functionStrs.get(i);
            this.function[i] = new TrainFunction(i, desc, parsed.get(8).charAt(i) == '1');
        }
    }

    /**
     * other test costructor do not use at normal application work
     * @param name
     * @param controled
     * @param function
     * @param speed
     * @param direction
     * @param lokoName
     * @param kmhSpeed
     */
    /*public Train(String name, boolean controled, boolean[] function, int speed, boolean direction, String lokoName, int kmhSpeed) {
        this.name = name;
        this.base = "-;LOK;test;";
        this.controled = controled;
        this.function = new TrainFunction[function.length];
        for (int i = 0; i < function.length; i++) {
            this.function[i] = new TrainFunction("F" + i, "funkce", function[i]);
        }
        this.speed = speed;
        this.direction = direction;
        this.err = null;
        this.authorized = false;
        this.statusOk = true;
        functionNames = new ArrayList<>();
        this.userLokoName = lokoName;
        this.kmhSpeed = kmhSpeed;
        this.mark = "zančení";
    }*/

    /*public ArrayList<String> getFunctionNames() {
        return functionNames;
    }

    public void setFunctionNames(String names) {

        String[] tmp = names.split(";");
        int i = 0;
        for (String s : tmp) {
            s = s.replace("\\{", "");
            s = s.replace("{", "");
            s = s.replaceAll("\\}", "");
            this.functionNames.add(s);
            this.function[i].setName(s);
            i++;
            if (i >= function.length) break;
        }

    }*/

    public String getUserTrainInfo() {
        /*String classString = this.lokoClass != null
                ? ServerList.TRAINTYPE.values()
                [Integer.parseInt(this.lokoClass)].toString() : "nezadáno";
        return "Název: " + this.userLokoName + "\n" +
                "Majitel: " + this.owner + "\n" +
                "Označení: " + this.mark + "\n" +
                "Poznámka: " + this.note + "\n" +
                "Třída: " + classString + "\n" +
                "Souprava: " + this.trainSet + "\n";*/
        return "";
    }

    /*public String toString() {
        return userLokoName +": "+name;
    }*/
/*
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public boolean isControled() {
        return controled;
    }

    public void setControled(boolean controled) {
        this.controled = controled;
    }

    public TrainFunction[] getFunction() {
        return function;
    }

    public void setFunction(boolean[] function) {
        this.function = new TrainFunction[function.length];
        for (int i = 0; i < function.length; i++) {
            this.function[i] = new TrainFunction("F" + i, "funkce", function[i]);
        }
    }

    public void setFunction(TrainFunction[] function) {
        this.function = function;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public boolean isDirection() {
        return direction;
    }

    public String getBase() {
        return base;
    }

    public int getKmhSpeed() {
        return kmhSpeed;
    }

    public void setKmhSpeed(int kmhSpeed) {
        this.kmhSpeed = kmhSpeed;
    }

    public String getUserLokoName() {
        return userLokoName;
    }

    public String getDisplayLokoName() {
        return this.userLokoName + " : " + this.mark;
    }

*/
    public void setDirection(boolean direction) {
        if (this.direction == direction) return;
        this.direction = direction;
        String strDir = direction ? "1" : "0";
        TCPClientApplication.getInstance().send("-;LOK;" + String.valueOf(this.addr) + ";D;" + strDir);
    }

    public void setSpeedSteps(int steps) {
        if (this.stepsSpeed == steps) return;
        this.stepsSpeed = steps;
        TCPClientApplication.getInstance().send("-;LOK;" + String.valueOf(this.addr) + ";SP-S;" + steps);
    }

    public void setTotal(boolean total) {
        if (this.total == total) return;
        this.total = total;
        String strTotal = total ? "1" : "0";
        TCPClientApplication.getInstance().send("-;LOK;" + String.valueOf(this.addr) + ";TOTAL;" + strTotal);
    }

  /*  public String changeDirection() {
        this.direction = !this.direction;
        String tmp = direction ? "0" : "1";
        return base + ";D;" + tmp;
    }*/

    public void release() {
        TCPClientApplication.getInstance().send("-;LOK;" + String.valueOf(this.addr) + ";RELEASE");
    }

    /**
     * - nastaveni rychlosti lokomotivy
     *
     * @return
     */
    /*public String GetSpeedTxt() {
        return base + ";SP;" + kmhSpeed;
    }*/

    /**
     * - nastaveni rychlosti a smeru lokomotivy
     *
     * @param dir
     * @return
     */
    /*public String GetSpeedTxt(boolean dir) {
        this.direction = dir;
        return base + ";SPD;" + kmhSpeed + ";" + direction;
    }*/

    /**
     * -;LOK;addr;F;F_left-F_right;states      - nastaveni funkci lokomotivy
     * napr.; or;LOK;F;0-4;00010 nastavi F3 a ostatni F vypne
     *
     * @param left
     * @param right
     * @return
     */
    /*public String getFunctionStr(int left, int right) {
        String fce = "";
        if (right < this.function.length)
            if (!(left < 0)) {
                for (int i = left; i <= right; i++) {
                    if (this.function[i].isSelected()) {
                        fce = fce + "1";
                    } else fce = fce + "0";
                }
                return base + ";F;" + left + "-" + right + ";" + fce;
            }
        return null;
    }*/

    /*public boolean getTotalManaged() {
        return this.totalManaged;
    }*/

    /*public void setTotalManaged(boolean totalManaged) {
        this.totalManaged = totalManaged;
    }*/

    /**
     * -;LOK;addr;TOTAL;[0,1] - nastaveni totalniho rucniho rizeni hnaciho vozidla
     *
     * @param totalManaged
     * @return
     */
    /*public String setTotalManged(boolean totalManaged) {
        this.totalManaged = totalManaged;

        return this.totalManaged ? base + ";TOTAL;1" : base + ";TOTAL;0";
    }*/

    public void emergencyStop() {
        kmphSpeed = 0;
        stepsSpeed = 0;
        TCPClientApplication.getInstance().send("-;LOK;" + String.valueOf(this.addr) + ";STOP");
    }

    /**
     * -;LOK;addr;SP-S;sp_stupen[0-28]         - nastaveni rychlosti lokomotivy ve stupnich
     *
     * @return
     */
    /*public String GetSpeedSTxt() {
        return base + ";SP-S;" + speed;
    }*/

    /**
     * -;LOK;addr;SPD-S;sp_stupen;dir[0,1]     - nastaveni rychlosti a smeru lokomotivy ve stupnich
     *
     * @param dir
     * @return
     */
    /*public String GetSpeedSTxt(boolean dir) {
        this.direction = dir;
        return base + ";SPD-S;" + speed + ";" + direction;
    }*/

    /*public String getToken() {
        return token;
    }*/

    /*public void setToken(String token) {
        this.authorized = true;
        this.token = token;
    }*/

    /*public boolean isAuthorized() {
        return authorized;
    }*/

    /*public void setAuthorized(boolean authorized) {
        if (!authorized) this.token = null;
        this.authorized = authorized;
    }*/

    public void setFunc(int id, boolean state) {
        if (function[id].checked == state) return;
        function[id].checked = state;
        String strState = state ? "1" : "0";
        TCPClientApplication.getInstance().send("-;LOK;" + String.valueOf(this.addr) + ";F;" +
            String.valueOf(id) + ";" + strState);
    }

    /*public void chageFunc(int position) {
        if (this.function.length > position) {
            if (this.function[position].isSelected()) {
                this.function[position].setSelected(false);
            } else this.function[position].setSelected(true);
        }
    }*/

    /*@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Train train = (Train) o;
        if (!getName().equals(train.getName())) return false;

        return getUserLokoName().equals(train.getUserLokoName());
    }*/

    /*@Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getUserLokoName().hashCode();

        return result;
    }*/

    /*public String nameString() {
        return this.userLokoName +"\n "+ this.name + " " + this.owner;
    }*/
}
