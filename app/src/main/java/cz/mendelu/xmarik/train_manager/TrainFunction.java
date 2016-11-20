package cz.mendelu.xmarik.train_manager;

/**
 * Created by ja on 5. 7. 2016.
 */
public class TrainFunction {
    String code;
    String name;
    boolean checked;

    /**
     *
     * @param code
     * @param name
     * @param checked
     */
    public TrainFunction(String code, String name, boolean checked) {
        this.code = code;
        this.name = name;
        this.checked = checked;
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return checked;
    }
    public void setSelected(boolean selected) {
        this.checked = selected;
    }
}
