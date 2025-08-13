package com.aros.apron.entity;

public class PayloadInfo {
    /**
     * 当前负载索引类型
     * LEFT_OR_MAIN	负载挂载在飞行器的左弦（主）位置。
     * RIGHT	负载挂载在飞行器的右弦位置。
     * UP	负载挂载在飞行器的上方位置。
     * EXTERNAL	负载挂载在飞行器的扩展位置。在M300 RTK上，该位置为OSDK口。在M350 RTK上，该位置为E-PORT口。
     * PORT_1	负载挂载在飞行器的Port1位置
     * PORT_2	负载挂载在飞行器的Port2位置
     * PORT_3	负载挂载在飞行器的Port3位置
     * PORT_4	负载挂载在飞行器的Port4位置
     * PORT_5	负载挂载在飞行器的Port5位置
     * PORT_6	负载挂载在飞行器的Port6位置
     * PORT_7	负载挂载在飞行器的Port7位置
     */
    private String payloadIndexType;

    /**
     * 负载产品名称
     */
    private String productName;

    /**
     * 负载序列号
     */
    private String serialNumber;

    /**
     * 负载固件版本
     */
    private String firmwareVersion;

    public String getPayloadIndexType() {
        return payloadIndexType;
    }

    public void setPayloadIndexType(String payloadIndexType) {
        this.payloadIndexType = payloadIndexType;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
}
