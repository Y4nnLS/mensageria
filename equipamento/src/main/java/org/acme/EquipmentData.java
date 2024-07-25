package org.acme;

public class EquipmentData {

    private int value;
    private String timestamp;

    public EquipmentData() {}

    public EquipmentData(int value, String timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
