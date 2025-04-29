package com.aros.apron.entity;


public class ApronExecutionStatus {

    private static class ApronexecutionHolder {
        private static final ApronExecutionStatus INSTANCE = new ApronExecutionStatus();
    }

    private ApronExecutionStatus() {
    }

    public static final ApronExecutionStatus getInstance() {
        return ApronexecutionHolder.INSTANCE;
    }

    //服务端响应了机库入库指令
    private boolean serverReplyDockIn;
    //服务端响应了飞机关机指令
    private boolean serverReplyDroneShut;

    public boolean isServerReplyDockIn() {
        return serverReplyDockIn;
    }

    public void setServerReplyDockIn(boolean serverReplyDockIn) {
        this.serverReplyDockIn = serverReplyDockIn;
    }

    public boolean isServerReplyDroneShut() {
        return serverReplyDroneShut;
    }

    public void setServerReplyDroneShut(boolean serverReplyDroneShut) {
        this.serverReplyDroneShut = serverReplyDroneShut;
    }
}
