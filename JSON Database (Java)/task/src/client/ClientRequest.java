package client;

public class ClientRequest {

    String type;
    Object key;
    Object value;

    public ClientRequest(String type, Object key, Object value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}