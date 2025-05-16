package cz.mendelu.xmarik.train_manager.models;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.events.EngineChangeEvent;
import cz.mendelu.xmarik.train_manager.events.EngineEvent;
import cz.mendelu.xmarik.train_manager.events.EngineRespEvent;
import cz.mendelu.xmarik.train_manager.events.EngineTotalChangeErrorEvent;
import cz.mendelu.xmarik.train_manager.network.TCPClient;
import cz.mendelu.xmarik.train_manager.helpers.ParseHelper;

/**
 * Class Engine represents an engine.
 */
public class Engine {
    public static final int EXP_SPEED_UNKNOWN = -1;
    public static final int SIGNAL_UNKNOWN = -1;

    public enum Direction {
        FORWARD,
        BACKWARD,
    };

    public enum ExpDirection {
        UNKNOWN,
        FORWARD,
        BACKWARD,
    };

    // data:
    public String name;
    public String owner;
    public String designation;
    public String note;
    public int addr;
    public int kind;
    public EngineFunction[] function;
    public int vmax;

    // state:
    public int stepsSpeed = 0;
    public int kmphSpeed = 0;
    public Direction direction = Direction.FORWARD;
    public boolean total = false;
    public boolean stolen = false;
    public int expSignalCode = SIGNAL_UNKNOWN;
    public String expSignalBlock;
    public int expSpeed = EXP_SPEED_UNKNOWN;
    public ExpDirection expDirection = ExpDirection.UNKNOWN;

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
        this.designation = parsed.get(2);
        this.note = parsed.get(3);
        this.addr = Integer.parseInt(parsed.get(4));
        this.kind = Integer.parseInt(parsed.get(5));

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

        this.vmax = (parsed.size() > 17) ? Integer.parseInt(parsed.get(17)) : 120;
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
        if (!this.designation.isEmpty())
            title += " (" + this.designation + ")";
        return title;
    }

    public boolean isMyControl() {
        return (this.total) && (!this.stolen);
    }

    private void change() {
        EventBus.getDefault().post(new EngineChangeEvent(this.addr));
    }

    public String kindStr(Context context) {
        return switch (this.kind) {
            case 0 -> context.getString(R.string.engine_kind_steam);
            case 1 -> context.getString(R.string.engine_kind_diesel);
            case 2 -> context.getString(R.string.engine_kind_motor);
            case 3 -> context.getString(R.string.engine_kind_electric);
            case 4 -> context.getString(R.string.engine_kind_car);
            default -> context.getString(R.string.engine_kind_unknown);
        };
    }

    public static Direction invertDirection(Direction d) {
        return (d == Direction.FORWARD) ? Direction.BACKWARD : Direction.FORWARD;
    }

    public static String expDirectionStrfy(Context context, ExpDirection dir) {
        return switch (dir) {
            case ExpDirection.FORWARD -> context.getString(R.string.ta_direction_forward);
            case ExpDirection.BACKWARD -> context.getString(R.string.ta_direction_backwards);
            default -> "---";
        };
    }

    public String expDirectionStr(Context context) {
        return expDirectionStrfy(context, this.expDirection);
    }

    public void totalEvent(final ArrayList<String> parsed) {
        final boolean total = parsed.get(4).equals("1");
        if (this.total == total) {
            this.change();
        } else {
            this.total = total;
            EventBus.getDefault().post(new EngineTotalChangeErrorEvent(this.addr, total));
        }
    }

    public void respEvent(final ArrayList<String> parsed) {
        if (parsed.get(4).equalsIgnoreCase("OK"))
            this.kmphSpeed = Integer.parseInt(parsed.get(6));
        EventBus.getDefault().post(new EngineRespEvent(parsed));
    }

    public void spdEvent(final ArrayList<String> parsed) {
        this.kmphSpeed = Integer.parseInt(parsed.get(4));
        this.stepsSpeed = Integer.parseInt(parsed.get(5));
        this.direction = (parsed.get(6).equals("1") ? Engine.Direction.BACKWARD : Engine.Direction.FORWARD);
        this.change();
    }

    public void fEvent(final ArrayList<String> parsed) {
        String[] f = parsed.get(4).split("-");
        if (f.length == 1) {
            this.function[Integer.parseInt(f[0])].checked = (parsed.get(5).equals("1"));
        } else if (f.length == 2) {
            int from = Integer.parseInt(f[0]);
            int to = Integer.parseInt(f[1]);
            for (int i = from; i <= to; i++)
                this.function[i].checked = (parsed.get(5).charAt(i-from) == '1');
        }
        this.change();
    }

    public void expSpdEvent(final ArrayList<String> parsed) {
        final String expSpeed = parsed.get(4);
        this.expSpeed = (!expSpeed.equals("-")) ? Integer.parseInt(expSpeed) : EXP_SPEED_UNKNOWN;

        if (parsed.size() > 5) {
            final String expDirection = parsed.get(5);
            switch (expDirection) {
                case "0": this.expDirection = ExpDirection.FORWARD; break;
                case "1": this.expDirection = ExpDirection.BACKWARD; break;
                default:  this.expDirection = ExpDirection.UNKNOWN; break;
            }
        } else {
            this.expDirection = Engine.ExpDirection.UNKNOWN;
        }

        this.change();
    }

    public void expSignalEvent(final ArrayList<String> parsed) {
        this.expSignalBlock = parsed.get(4);
        try {
            this.expSignalCode = Integer.parseInt(parsed.get(5));
        } catch (NumberFormatException e) {
            this.expSignalCode = -1;
        }
        this.change();
    }
}
