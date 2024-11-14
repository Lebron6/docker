package com.aros.apron.util;

import com.aros.apron.tools.LogUtil;

public class VideoStreamThread implements Runnable {

    private SendVideoStream mSendVideoStream;
    private FileUtil mFileUtil;
    private final String TAG = "VideoStreamThread";


    public VideoStreamThread(FileUtil mFileUtil) {
        this.mFileUtil = mFileUtil;
    }

    @Override
    public void run() {
        long startTime;
        long endTime;
        while (!Thread.currentThread().isInterrupted()) { //   终结线程
            startTime = System.currentTimeMillis();
            int i = mSendVideoStream.sendVideoStreamFun();
            if (i != 0 && i != 10) {
                FileUtil.getInstance().onClear();
            }
            endTime = System.currentTimeMillis();
            //推太快就等待
            long delay = endTime - startTime;
            try {
                //将上一次的推流耗时近似当做下一次的耗时，也就是两帧之间是（40-delay）+下次网络推流用时delay，即保证每两帧间隔40ms(不用了)
                Thread.sleep((long) 36);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

        }
    }

    public void setStartListen(SendVideoStream sendVideoStream) {
        mSendVideoStream = sendVideoStream;
    }

    public interface SendVideoStream {
        int sendVideoStreamFun();
    }


    public void onClearEnqueue() {
        mFileUtil.onClear();
    }

    public void onDestroy() {
        Thread.currentThread().interrupt();
    }

    public void send(){

    }
}

