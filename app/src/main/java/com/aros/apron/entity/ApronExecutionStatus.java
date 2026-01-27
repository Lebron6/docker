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
    //服务端是否响应了开舱门指令
    private boolean serverReplyDockOpen;
    //服务端是否响应了TaskFail
    private boolean serverReplyTaskFail;
    //用于确认飞机此时可以关机的状态(APC流程默认)
    private boolean isAircraftWaitShutDown=true;

    public boolean isServerReplyTaskFail() {
        return serverReplyTaskFail;
    }

    public void setServerReplyTaskFail(boolean serverReplyTaskFail) {
        this.serverReplyTaskFail = serverReplyTaskFail;
    }

    public boolean isAircraftWaitShutDown() {
        return isAircraftWaitShutDown;
    }

    public void setAircraftWaitShutDown(boolean aircraftWaitShutDown) {
        isAircraftWaitShutDown = aircraftWaitShutDown;
    }

    public boolean isServerReplyDockOpen() {
        return serverReplyDockOpen;
    }

    public void setServerReplyDockOpen(boolean serverReplyDockOpen) {
        this.serverReplyDockOpen = serverReplyDockOpen;
    }

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
