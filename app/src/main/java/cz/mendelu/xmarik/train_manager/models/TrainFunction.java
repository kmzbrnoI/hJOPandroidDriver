package cz.mendelu.xmarik.train_manager.models;

/**
 * Decoder function, e. g. F0-F28.
 */
public class TrainFunction {
    public enum TrainFunctionType {
        PERMANENT,
        MOMENTARY,
    }

    public int num;
    public String name;
    public boolean checked;
    public TrainFunctionType type;

    public TrainFunction(int num, String name, boolean checked, TrainFunctionType type) {
        this.num = num;
        this.name = name;
        this.checked = checked;
        this.type = type;
    }

    public TrainFunction(int num, String name, boolean checked, char type) {
        this(num, name, checked, CharToFType(type));
    }

    public static TrainFunctionType CharToFType(char type) {
        return ((type == 'M') || (type == 'm')) ? TrainFunctionType.MOMENTARY : TrainFunctionType.PERMANENT;
    }

}
