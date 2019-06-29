package cz.mendelu.xmarik.train_manager.models;

enum TrainFunctionType {
    PERMANENT,
    MOMENTARY,
}

/**
 * Decoder function, e. g. F0-F28.
 */
public class TrainFunction {
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

    public static TrainFunction[] DEF_FUNCTION = {
            new TrainFunction(0, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(1, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(2, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(3, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(4, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(5, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(6, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(7, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(8, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(9, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(10, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(11, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(12, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(13, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(14, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(15, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(16, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(17, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(18, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(19, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(20, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(21, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(22, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(23, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(24, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(25, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(26, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(27, "", false, TrainFunctionType.PERMANENT),
            new TrainFunction(28, "", false, TrainFunctionType.PERMANENT)
    };

}
