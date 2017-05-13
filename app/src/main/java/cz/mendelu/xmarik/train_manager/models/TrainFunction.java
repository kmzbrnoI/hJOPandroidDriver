package cz.mendelu.xmarik.train_manager.models;

/**
 * Decoder function, e. g. F0-F28.
 */
public class TrainFunction {
    public int num;
    public String name;
    public boolean checked;

    public TrainFunction(int num, String name, boolean checked) {
        this.num = num;
        this.name = name;
        this.checked = checked;
    }

    public static TrainFunction[] DEF_FUNCTION = {
            new TrainFunction(0, "", false),
            new TrainFunction(1, "", false),
            new TrainFunction(2, "", false),
            new TrainFunction(3, "", false),
            new TrainFunction(4, "", false),
            new TrainFunction(5, "", false),
            new TrainFunction(6, "", false),
            new TrainFunction(7, "", false),
            new TrainFunction(8, "", false),
            new TrainFunction(9, "", false),
            new TrainFunction(10, "", false),
            new TrainFunction(11, "", false),
            new TrainFunction(12, "", false),
            new TrainFunction(13, "", false),
            new TrainFunction(14, "", false),
            new TrainFunction(15, "", false),
            new TrainFunction(16, "", false),
            new TrainFunction(17, "", false),
            new TrainFunction(18, "", false),
            new TrainFunction(19, "", false),
            new TrainFunction(20, "", false),
            new TrainFunction(21, "", false),
            new TrainFunction(22, "", false),
            new TrainFunction(23, "", false),
            new TrainFunction(24, "", false),
            new TrainFunction(25, "", false),
            new TrainFunction(26, "", false),
            new TrainFunction(27, "", false),
            new TrainFunction(28, "", false)
    };

}
