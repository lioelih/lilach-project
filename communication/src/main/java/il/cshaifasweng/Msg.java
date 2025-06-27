package il.cshaifasweng;

import java.io.Serializable;
//this class is for communication between the client and server
public class Msg implements Serializable {
    private static final long serialVersionUID = 1L;
    private String action;
    private Object data;

    public Msg() {} //default constructor
    public Msg(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public void setAction(String action) {
        this.action = action;
    }
    public void setData(Object data) {
        this.data = data;
    }

    public String getAction() {
        return  action;
    }
    public Object getData() {return data;}
}
