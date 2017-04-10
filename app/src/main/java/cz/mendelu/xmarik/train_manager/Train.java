package cz.mendelu.xmarik.train_manager;

import java.util.ArrayList;

/**
 * Created by ja on 29. 5. 2016.
 * Class for train object description
 */
public class Train {
    public boolean statusOk;
    private String name;
    private String base;
    private boolean controled;
    private TrainFunction function[];
    private int speed;
    private int kmhSpeed;
    private boolean direction;
    private boolean totalManaged;
    private String err;
    private String token = null;
    private boolean authorized = false;
    private String owner;
    private String mark;
    private String note;
    private String userLokoName;
    private String lokoClass;
    private String trainSet;
    private ArrayList<String> functionNames;

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
    public Train(String name, boolean controled, boolean[] function, int speed, boolean direction, String id) {
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
    }

    /**
     *
     * @param s nazev
     * @param s1 majitel
     * @param s2 oznaceni
     * @param s3 poznamka
     * @param s4 adresa
     * @param s5 trida
     * @param s6 souprava
     * @param s7 stanoviste
     * @param s8 funkce
     */
    //string nazev|majitel|oznaceni|poznamka|adresa|trida|souprava|stanovisteA|funkce
    public Train(String s, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        this.userLokoName = s;
        this.owner = s1;
        this.mark = s2;
        this.note = s3;
        this.name = s4;
        this.lokoClass = s5;
        this.trainSet = s6;
        this.base = "-;LOK;" + s4;
        functionNames = new ArrayList<>();
        this.speed = 0;
        this.direction = false;
        int funcTmp = s8.length();
        this.function = new TrainFunction[funcTmp];

        for (int i = 0; i < funcTmp; i++) {
            this.function[i] = new TrainFunction("F" + i, "funkce", s8.charAt(i) == '1');
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
    public Train(String name, boolean controled, boolean[] function, int speed, boolean direction, String lokoName, int kmhSpeed) {
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
    }

    public ArrayList<String> getFunctionNames() {
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

    }

    public String getUserTrainInfo() {
        String classString = this.lokoClass != null
                ? ServerList.TRAINTYPE.values()
                [Integer.parseInt(this.lokoClass)].toString() : "nezadáno";
        return "Název: " + this.userLokoName + "\n" +
                "Majitel: " + this.owner + "\n" +
                "Označení: " + this.mark + "\n" +
                "Poznámka: " + this.note + "\n" +
                "Třída: " + classString + "\n" +
                "Souprava: " + this.trainSet + "\n";
    }

    public String toString() {
        return userLokoName +": "+name;
    }

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

    void setKmhSpeed(int kmhSpeed) {
        this.kmhSpeed = kmhSpeed;
    }

    String getUserLokoName() {
        return userLokoName;
    }

    String getDisplayLokoName() {
        return this.userLokoName + " : " + this.mark;
    }


    public String setDirection(boolean direction) {
        this.direction = direction;
        String tmp = direction ? "0" : "1";
        return base + ";D;" + tmp;
    }

    public String changeDirection() {
        this.direction = !this.direction;
        String tmp = direction ? "0" : "1";
        return base + ";D;" + tmp;
    }

    public String Release() {
        String text = null;
        text = base + "RELEASE";
        return text;
    }

    /**
     * - nastaveni rychlosti lokomotivy
     *
     * @return
     */
    public String GetSpeedTxt() {
        return base + ";SP;" + kmhSpeed;
    }

    /**
     * - nastaveni rychlosti a smeru lokomotivy
     *
     * @param dir
     * @return
     */
    public String GetSpeedTxt(boolean dir) {
        this.direction = dir;
        return base + ";SPD;" + kmhSpeed + ";" + direction;
    }

    /**
     * -;LOK;addr;F;F_left-F_right;states      - nastaveni funkci lokomotivy
     * napr.; or;LOK;F;0-4;00010 nastavi F3 a ostatni F vypne
     *
     * @param left
     * @param right
     * @return
     */
    public String getFunctionStr(int left, int right) {
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
    }

    public boolean getTotalManaged() {
        return this.totalManaged;
    }

    public void setTotalManaged(boolean totalManaged) {
        this.totalManaged = totalManaged;
    }

    /**
     * -;LOK;addr;TOTAL;[0,1] - nastaveni totalniho rucniho rizeni hnaciho vozidla
     *
     * @param totalManaged
     * @return
     */
    public String setTotalManged(boolean totalManaged) {
        this.totalManaged = totalManaged;

        return this.totalManaged ? base + ";TOTAL;1" : base + ";TOTAL;0";
    }

    /**
     * -;LOK;addr;STOP;                        - nouzove zastaveni
     *
     * @return
     */
    public String emergencyStop() {
        this.speed = 0;
        this.kmhSpeed = 0;
        return base + ";STOP;";
    }

    /**
     * -;LOK;addr;SP-S;sp_stupen[0-28]         - nastaveni rychlosti lokomotivy ve stupnich
     *
     * @return
     */
    public String GetSpeedSTxt() {
        return base + ";SP-S;" + speed;
    }

    /**
     * -;LOK;addr;SPD-S;sp_stupen;dir[0,1]     - nastaveni rychlosti a smeru lokomotivy ve stupnich
     *
     * @param dir
     * @return
     */
    public String GetSpeedSTxt(boolean dir) {
        this.direction = dir;
        return base + ";SPD-S;" + speed + ";" + direction;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.authorized = true;
        this.token = token;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        if (!authorized) this.token = null;
        this.authorized = authorized;
    }

    public void setFunction(int left, int right, boolean[] func) {
        for (int i = left; i < this.function.length && i <= right; i++) {
            function[i].checked = func[i];
        }
    }

    public void chageFunc(int position) {
        if (this.function.length > position) {
            if (this.function[position].isSelected()) {
                this.function[position].setSelected(false);
            } else this.function[position].setSelected(true);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Train train = (Train) o;
        if (!getName().equals(train.getName())) return false;

        return getUserLokoName().equals(train.getUserLokoName());
    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getUserLokoName().hashCode();

        return result;
    }

    public String nameString() {
        return this.userLokoName +"\n "+ this.name + " " + this.owner;
    }
}
