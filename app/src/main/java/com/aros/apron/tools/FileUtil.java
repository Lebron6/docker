package com.aros.apron.tools;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.aros.apron.entity.H264Frame;
import com.aros.apron.tools.LogUtil;
import com.gosuncn.lib28181agent.Jni28181AgentSDK;
import com.gosuncn.lib28181agent.bean.AngleEvent;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文件工具类
 */
public class FileUtil {

    private static final int NULL_FRAME = -1;
    private static final int I_FRAME = 1;
    private static final int P_FRAME = 2;
    private static final int B_FRAME = 3;
    private final static String TAG = "FileUtil";

    private static class FileUtilHolder {
        private static final FileUtil INSTANCE = new FileUtil();
    }

    private FileUtil() {
    }

    public static final FileUtil getInstance() {
        return FileUtil.FileUtilHolder.INSTANCE;
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            //一些三方的文件浏览器会进入到这个方法中，例如ES
            //QQ文件管理器不在此列
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                // 不同系统获取的id开头可能不一样，在这后面便是真实的地址
                if (id.startsWith("msf:")) {
                    return id.replaceFirst("msf:", "");
                }
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {// MediaStore (and general)
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();


            if (isQQMediaDocument(uri)) {
                String path = uri.getPath();
                File fileDir = Environment.getExternalStorageDirectory();
                File file = new File(fileDir, path.substring("/QQBrowser".length(), path.length()));
                return file.exists() ? file.toString() : null;
            }

            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {// File

            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    /**
     * 使用第三方qq文件管理器打开
     *
     * @param uri
     * @return
     */
    public static boolean isQQMediaDocument(Uri uri) {
        return "com.tencent.mtt.fileprovider".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    public static int determineFrameType(byte[] videoBuffer, int size) {
        // Implement your logic to determine the frame type
        // This is a simplified example and may not be accurate
        if (videoBuffer[0] == 0x00 && videoBuffer[1] == 0x00 && videoBuffer[2] == 0x01) {
            // This is a heuristic approach and may not be accurate
            if ((videoBuffer[3] & 0x1F) == 5) {
                return I_FRAME;
            } else if ((videoBuffer[3] & 0x1F) == 1) {
                return P_FRAME;
            }
        }
        return NULL_FRAME;

//        int startCodeSize = getStartCodeSize(videoArray);
//        int naluType = (videoArray[startCodeSize] & 0x1f);
//        if (naluType == 5) {
//            return 1;
//        }
//        if (naluType == 1 || naluType == 1 || naluType == 3 || naluType == 4 || naluType == 6) {
//            return -1;
//        }
//        if (naluType == 7 || naluType == 8) {
//            return 2;
//        }
//        return -1;
    }

    static int getStartCodeSize(byte[] byteBuffer) {
        int startCodeSize = 0;
        if (byteBuffer[0] == 0x00 && byteBuffer[1] == 0x00
                && byteBuffer[2] == 0x00 && byteBuffer[3] == 0x01
        ) {
            //match 00 00 00 01
            startCodeSize = 4;
        } else if (byteBuffer[0] == 0x00 && byteBuffer[1] == 0x00
                && byteBuffer[2] == 0x01
        ) {
            //match 00 00 01
            startCodeSize = 3;
        }
        return startCodeSize;
    }

    private static final int Unknown = -1;

    public static int getFrameType1(byte[] data) {
        if (data == null || data.length < 5) {
            return Unknown;
        }

        int nalStartIndex = findNalStartCode(data);
        if (nalStartIndex == -1) {
            return Unknown;
        }

        // NAL unit type for H.264 (last 5 bits of the byte after start code)
        int nalUnitTypeH264 = data[nalStartIndex + 4] & 0x1F;

        // NAL unit type for H.265 (first 6 bits of the byte after start code)
        int nalUnitTypeH265 = (data[nalStartIndex + 5] & 0x7E) >> 1;

        // Determine the format and return the frame type
        if (nalUnitTypeH264 >= 1 && nalUnitTypeH264 <= 23) {
            // H.264 detected
            return getH264FrameType(nalUnitTypeH264);
        } else if (nalUnitTypeH265 >= 0 && nalUnitTypeH265 <= 31) {
            // H.265 detected
            return getH265FrameType(nalUnitTypeH265);
        }

        return Unknown;
    }

    private static int getH264FrameType(int nalUnitType) {
        switch (nalUnitType) {
            case 5:
//                LogUtil.log("帧格式","::::::::::"+I_FRAME);
                return I_FRAME;
            case 1:
//                LogUtil.log("帧格式","::::::::::"+P_FRAME);
                return P_FRAME;
            case 7:
//                LogUtil.log("帧格式","::::::::::"+Unknown);
                return Unknown;
            case 8:
//                LogUtil.log("帧格式","::::::::::"+Unknown);
                return Unknown;
            default:
//                LogUtil.log("帧格式","::::::::::"+Unknown);
                return Unknown;
        }
    }

    private static int getH265FrameType(int nalUnitType) {
        switch (nalUnitType) {
            case 19:
                return I_FRAME;
            case 1:
                return P_FRAME;
            case 32:
                return Unknown;
            case 33:
                return Unknown;
            case 34:
                return Unknown;
            default:
                return Unknown;
        }
    }

    private static int findNalStartCode(byte[] data) {
        for (int i = 0; i < data.length - 4; i++) {
            if (data[i] == 0x00 && data[i + 1] == 0x00 &&
                    data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                return i;
            } else if (data[i] == 0x00 && data[i + 1] == 0x00 &&
                    data[i + 2] == 0x01) {
                return i;
            }
        }
        return -1;
    }
//    public static int getFrameType(byte[] data) {
//        if (data == null || data.length < 5) {
//            return Unknown;
//        }
//
//        int nalStartIndex = findNalStartCode(data);
//        if (nalStartIndex == -1 || nalStartIndex + 5 > data.length) {
//            return Unknown;
//        }
//
//        // NAL unit type is the last 5 bits of the first byte after the start code
//        int nalUnitType = data[nalStartIndex + 4] & 0x1F;
//
//        switch (nalUnitType) {
//            case 5:
//                return I_frame;
//            case 1:
//                return P_frame;
//            case 7:
////                return "SPS";
//                return Unknown;
//            case 8:
////                return "PPS";
//                return Unknown;
//            default:
////                return "Other";
//                return Unknown;
//        }
//    }
//
//    private static int findNalStartCode(byte[] data) {
//        for (int i = 0; i < data.length - 4; i++) {
//            if (data[i] == 0x00 && data[i + 1] == 0x00 &&
//                    data[i + 2] == 0x00 && data[i + 3] == 0x01) {
//                return i;
//            } else if (data[i] == 0x00 && data[i + 1] == 0x00 &&
//                    data[i + 2] == 0x01) {
//                return i;
//            }
//        }
//        return -1;
//    }

    private final int BUFFER_QUEUE_SIZE = 120;
    private BlockingQueue<H264Frame> frameQueue = new LinkedBlockingQueue<>(BUFFER_QUEUE_SIZE);
    // 帧数据缓存池，每次回调的大小基本为2032
	//    private final int FRAME_BUFFER_SIZE = 1024 * 1024;   // 最大不能超过1024*1024（1M），当前申请150K
//    private final int FILE_BUFFER_SIZE = 1024 * 1024 * 30;
//    private ByteBuffer frameBuffer = ByteBuffer.allocate(FRAME_BUFFER_SIZE);
//    private ByteBuffer outputBuffer = null;
//    private AtomicInteger currentFrameType = new AtomicInteger(Types.I_FRAME);
    private int gopNum = 1;
    private float gopDropPer = 24;
    private Lock lock = new ReentrantLock();
    private int drup = 0;
    public void enqueueFrameBuffer(byte[] bufferData, int size, int type) {
        // 校验接收到的videoBuffer，等待缓存池缓存到一帧再放到缓存队列
//        if (frameBuffer == null || !isRtpRunning.get() || isDestory) {
//            return;
//        }
        byte[] data = new byte[size];

        System.arraycopy(bufferData, 0, data, 0, size);

        H264Frame h264Frame = new H264Frame();
        h264Frame.setFrameLen(data.length);
        h264Frame.setFrameData(data);
        h264Frame.setTimestamp(System.currentTimeMillis());
        h264Frame.setFrameType(type);

//        frameQueue.add(h264Frame);
        if (!frameQueue.offer(h264Frame)) {
            if (lock.tryLock()) {
                try {
                    synchronized (frameQueue) {//防止丢帧时获取帧的线程获取到一帧需要丢的帧
                        LogUtil.log(TAG, "队列满了，丢栈顶帧直到下一个I帧");
                        frameQueue.poll();
                        while (frameQueue.peek().getFrameType() != 1) {
                            frameQueue.poll();
                        }
                        frameQueue.add(h264Frame);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            } else {//如果获取帧线程先加上锁了，则优先采用获取帧方法的丢帧，丢尾部帧
                while (frameQueue.offer(h264Frame)) {
                }//等待，直到获取帧方法的丢帧结束，这里正常add，因为获取帧方法的丢尾帧没有给队列加锁
            }
        }
        LogUtil.log(TAG, "当前视频帧缓存队列长度-----" + frameQueue.size());
    }

    public H264Frame getEnqueueFrame() {
        if (frameQueue == null || frameQueue.size() == 0) {
            return null;
        }
        H264Frame currentFrame = frameQueue.poll();
        if (currentFrame.getFrameType() == 1) {
            gopNum = 1;
        } else {
            gopNum++;
            if (drup > 0 ){
                if (gopNum % drup == 0)
                if (lock.tryLock()){
                    try {
                        if (currentFrame.getFrameType() != 1)//I帧不丢
                            currentFrame = frameQueue.poll();//丢掉当前帧获取下一帧
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        lock.unlock();
                    }
                }
            }
        }
//            每秒30帧，每帧间隔33ms，当前不是I帧且间隔超过33ms就丢掉当前帧
//        long curTime = System.currentTimeMillis();
//        if (Math.abs(currentFrame.getTimestamp() - curTime) > 1000 && currentFrame.getFrameType() != 1) {//开始丢帧时后面的帧全部丢掉直到遇见下一个I帧
//            LogUtil.log(TAG,"超时帧:"+currentFrame.getTimestamp()+"::::::"+curTime);
//            while (true) {
//                currentFrame = frameQueue.poll();
//                if (currentFrame.getFrameType() != 1 ) {
//                    continue;
//                }
//                break;
//            }
//        }

//        在延迟达到1秒和0.5秒时采取间隔丢帧策略，分别得到25帧和26帧
        if (System.currentTimeMillis() - currentFrame.getTimestamp() < 1000 && System.currentTimeMillis() - currentFrame.getTimestamp() > 500){
            drup = 7;//26帧
        }else if (System.currentTimeMillis() - currentFrame.getTimestamp() > 1000){
            drup = 6;//25帧
        }else {
            drup = -1;
        }

        if (frameQueue.size() > (double)BUFFER_QUEUE_SIZE * 0.6 && frameQueue.size() < (double)BUFFER_QUEUE_SIZE * 0.8) {
            gopDropPer = 25;
        } else if (frameQueue.size() > (double)BUFFER_QUEUE_SIZE * 0.8) {//到了百分之八十要满了，每个GOP的最后七帧丢掉
            gopDropPer = 23;
        }else {//没大于百分之六十就不丢
            gopDropPer = 300;
        }
        //即使插入时加锁丢帧了，到这里也不影响，当前判断为I帧，不会进入while
        if (gopNum > gopDropPer) {
            if (lock.tryLock()) {//获取到锁则采用丢尾帧
//                LogUtil.log(TAG, "=--=-=-=-=-=-=--=队列要满了");
                try {
                    while (frameQueue.peek().getFrameType() != 1) {
                        frameQueue.poll();
//                        LogUtil.log(TAG, "丢掉一帧，队列要满了" + gopNum);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            } else {
                //没获取到则说明添加帧方法采用丢帧方法，这里不需要丢帧了，并且给frameQueue加锁了，待会获取帧时会等待，不会获取到废弃帧
            }
        }
        return currentFrame;
    }

    public void onClear() {
        if (frameQueue != null)
            frameQueue.clear();
    }

    public void onDestroy() {
        if (frameQueue != null)
            frameQueue.clear();
        frameQueue = null;
    }

    // NAL单元类型
    private static final int NAL_P = 1;
    private static final int NAL_B = 2;
    private static final int NAL_I = 5;

    // 获取NAL单元类型
    private static int getNalType(byte[] nalUnit) {
        // H.264 NAL单元的类型位于起始字节的第1位到第5位
        return (nalUnit[0] & 0x1F);
    }

    // 判断帧类型
    public static int getFrameType(byte[] frame) {
	        // 获取字节码流中的第一个NAL单元（假设视频帧数据是以NAL单元为基本单位）
        byte[] nalUnit = getFirstNalUnit(frame);
        if (nalUnit == null) {
            return Unknown; // 未知
        }

        int nalType = getNalType(nalUnit);
        switch (nalType) {
            case NAL_I:
//                LogUtil.log("帧格式","::::::::::"+I_FRAME);
                return I_FRAME; // I帧
            case NAL_P:
//                LogUtil.log("帧格式","::::::::::"+P_FRAME);
                return P_FRAME; // P帧
            case NAL_B:
//                LogUtil.log("帧格式","::::::::::"+B_FRAME);
                return B_FRAME; // B帧
            default:
//                LogUtil.log("帧格式","::::::::::"+Unknown);
	                return Unknown; // 其他类型的NAL单元（例如SPS、PPS等）
        }
    }

    // 从视频帧数据中提取第一个NAL单元
    private static byte[] getFirstNalUnit(byte[] frame) {
        // 这里简化处理，实际情况可能需要更复杂的逻辑来处理起始码
        int startCodeLength = 3;
        if (frame != null && frame.length >= startCodeLength) {
            // 查找起始码0x000001或0x00000001
            for (int i = 0; i < frame.length - startCodeLength; i++) {
                if ((frame[i] == 0x00 && frame[i + 1] == 0x00 && frame[i + 2] == 0x01) ||
                        (frame[i] == 0x00 && frame[i + 1] == 0x00 && frame[i + 2] == 0x00 && frame[i + 3] == 0x01)) {
                    return Arrays.copyOfRange(frame, i + startCodeLength, frame.length);
                }
            }
        }
        return null;
    }

    private Timer heartBeatTimer;
    private TimerTask heartBeatTask;
    public final int HEART_BEAT_INTERVAL = 29 * 1000;

    // 启动心跳线程
    public void startHeartBeatTask() {
        if (heartBeatTimer == null) {
            heartBeatTimer = new Timer();
        }
        if (heartBeatTask == null) {
            heartBeatTask = new TimerTask() {
                @Override
                public void run() {
                    int code = Jni28181AgentSDK.getInstance().sendHeartBeat();
//                    LogUtil.log(TAG, "发送心跳信息：" + code);
                }
            };
        }
        //在延迟delay后执行task1次，之后定期period毫秒时间执行task
        heartBeatTimer.schedule(heartBeatTask, 100, HEART_BEAT_INTERVAL);
    }

    // 停止心跳线程
    public void stopHeartBeatTask() {
        LogUtil.log(TAG, "停止心跳线程：");
        if (heartBeatTimer != null && heartBeatTask != null) {
            heartBeatTimer.purge();
            heartBeatTimer.cancel();
            heartBeatTask = null;
            heartBeatTimer = null;
        }
    }

    public AngleEvent countCmos(float cmosH, float cmosW, double currentZoom) {
        AngleEvent angleEvent = new AngleEvent();
        float angleH = (float)(2.0D * Math.atan((double)(cmosW / (float)(2 * currentZoom))) * 360.0D / 2.0D / 3.141592653589793D);
        float angleV = (float)(2.0D * Math.atan((double)(cmosH / (float)(2 * currentZoom))) * 360.0D / 2.0D / 3.141592653589793D);
        angleEvent.setAngleH(angleH);
        angleEvent.setAngleV(angleV);
        return angleEvent;
    }
}
