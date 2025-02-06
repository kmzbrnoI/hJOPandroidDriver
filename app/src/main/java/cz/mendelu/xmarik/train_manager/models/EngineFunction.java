package cz.mendelu.xmarik.train_manager.models;

/**
 * Decoder function, e. g. F0-F28.
 */
public class EngineFunction {
    public enum EngineFunctionType {
        PERMANENT,
        MOMENTARY,
    }

    public int num;
    public String name;
    public boolean checked;
    public EngineFunctionType type;

    public EngineFunction(int num, String name, boolean checked, EngineFunctionType type) {
        this.num = num;
        this.name = name;
        this.checked = checked;
        this.type = type;
    }

    public EngineFunction(int num, String name, boolean checked, char type) {
        this(num, name, checked, CharToFType(type));
    }

    public static EngineFunctionType CharToFType(char type) {
        return ((type == 'M') || (type == 'm')) ? EngineFunctionType.MOMENTARY : EngineFunctionType.PERMANENT;
    }

}
