package cz.mendelu.xmarik.train_manager;

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
}
