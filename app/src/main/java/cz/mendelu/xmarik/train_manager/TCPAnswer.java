package cz.mendelu.xmarik.train_manager;

/**
 * Created by ja on 13. 10. 2016.
 */

public class TCPAnswer {
    private String title;
    private String body;
    private Integer type;

    public TCPAnswer(String title, String body) {
        this.title = title;
        this.body = body;
        this.type = null;
    }

    public TCPAnswer(String title, String body, int type) {
        this.title = title;
        this.body = body;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
