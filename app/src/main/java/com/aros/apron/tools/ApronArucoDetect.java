package com.aros.apron.tools;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ArucoMarker;
import com.aros.apron.entity.ArucoMarkerDimensions;
import com.aros.apron.entity.Movement;
import com.aros.apron.manager.AlternateLandingManager;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApronArucoDetect {

    //是否触发识别(如果丢失图传，此值为false)
    private boolean isTriggerSuccess;

    //没识别到二维码
    private boolean arucoNotFoundTag;

    private boolean isStartAruco=false;
    //    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    Future<?> lastFuture = null;

    private String TAG = getClass().getSimpleName();
    Double resultYaw = 0.0;
    private List<ArucoMarker> mFindArucoList = new ArrayList<>();
    List<Mat> mArucoCornerList = new ArrayList<>();
    //触发去备降点
    private boolean triggerToAlternateLandingPoint;
    long startTime;
    long endTime;
    private int detectedBigMarkerId;
    private int detectedSmallMarkerId;
    private boolean detectedMediumMarkers;
    private boolean detectedSmallMarkers;
    //当确认识别单一的二维码后，有概率下降途中识别不到，此时次数超过15次,可以控制识别别的二维码
    private int sigleMarkerDetectFailsTimes;
    //复降触发条件
    private boolean dropTimesTag;
    //复降次数
    private int dropTimes;
    //是否双挂
    private boolean isDoublePayload;

    public PIDControl pidControlX = null;
    public PIDControl pidControlY = null;

    //为了解决降落后不停浆问题，增加记录，开启速降时startFastStick之后的过滤次数
    public int checkThrowingErrorsTimes;

    public int getCheckThrowingErrorsTimes() {
        return checkThrowingErrorsTimes;
    }

    public void setCheckThrowingErrorsTimes(int checkThrowingErrorsTimes) {
        this.checkThrowingErrorsTimes = checkThrowingErrorsTimes;
    }

    public boolean isTriggerSuccess() {
        return isTriggerSuccess;
    }

    public void setTriggerSuccess(boolean triggerSuccess) {
        isTriggerSuccess = triggerSuccess;
    }

    public boolean isDoublePayload() {
        return isDoublePayload;
    }

    public void setDoublePayload(boolean doublePayload) {
        isDoublePayload = doublePayload;
    }


    private ApronArucoDetect() {
    }

    private static class OpenCVHelperHolder {
        private static final ApronArucoDetect INSTANCE = new ApronArucoDetect();
    }

    public static ApronArucoDetect getInstance() {
        return OpenCVHelperHolder.INSTANCE;
    }

    public void init() {
        pidControlX = new PIDControl(0.8f, 0.002f, 0.09f, 0.05f, 2.0f, 0.05f);
        pidControlY = new PIDControl(0.8f, 0.002f, 0.09f, 0.05f, 2.0f, 0.05f);
        pidControlX.reset();
        pidControlY.reset();
    }


    public void detectArucoTags(int height, int width, byte[] data, Dictionary dictionary) {
        //这里说明图传正常
        isTriggerSuccess=true;
        Movement.getInstance().setVirtualStickEnableReason(2);
        if (isStartAruco || startFastStick) {
            LogUtil.log(TAG, "过滤:" + isStartAruco + startFastStick);
            if (!isStartAruco&&startFastStick){
                checkThrowingErrorsTimes++;
            }
            return;
        }
        isStartAruco = true;
        if (lastFuture != null && !lastFuture.isDone()) {
            LogUtil.log(TAG, "break---");
            lastFuture.cancel(true);
        }
        lastFuture =executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
                    yuvMat.put(0, 0, data);
                    Mat rgbMat = new Mat();
                    Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2BGR_I420);
                    // 灰度
                    Mat grayImgMat = new Mat();
                    Imgproc.cvtColor(rgbMat, grayImgMat, Imgproc.COLOR_RGBA2GRAY);
                    MatOfInt ids = new MatOfInt();
                    mFindArucoList.clear();
                    mArucoCornerList.clear();
                    MatOfPoint2f corner = new MatOfPoint2f();

                    Aruco.detectMarkers(grayImgMat, dictionary, mArucoCornerList, ids);
                    if (ids.depth() > 0) {
                        arucoNotFoundTag = false;
                        int[] idArray = ids.toArray();
                        int ultrasonicHeight = Movement.getInstance().getUltrasonicHeight();
                        double flyingHeight = Movement.getInstance().getFlyingHeight();
                        if (idArray.length == 1 && idArray[0] == 5) {
                            Core.extractChannel(mArucoCornerList.get(0), corner, 0);
                            Point[] points = corner.toArray();
                            // 计算宽度（两个相邻角点之间的距离）
                            double width = calculateDistance(points[0], points[1]);
                            if (!startFastStick&&width >= 1250 && ultrasonicHeight <= 4) {
                                String logMessage = "参考5号Marker尺寸降落:" + idArray[0] + " arucoW" + width +
                                        " Flying Height:" + flyingHeight + "--" +
                                        " Ultrasonic Height:" + ultrasonicHeight;
                                startFastStick = true;
                                handler.post(runnable);
                                LogUtil.log(TAG, logMessage);
                                return;
                            }
                        }
                        if (idArray.length == 1 && idArray[0] == 6) {
                            Core.extractChannel(mArucoCornerList.get(0), corner, 0);
                            Point[] points = corner.toArray();
                            // 计算宽度（两个相邻角点之间的距离）
                            double width = calculateDistance(points[0], points[1]);
                            if (!startFastStick&&width >= 960 && ultrasonicHeight <= 2) {
                                String logMessage = "参考6号Marker尺寸降落:" + idArray[0] + " arucoW" + width +
                                        " Flying Height:" + flyingHeight + "--" +
                                        " Ultrasonic Height:" + ultrasonicHeight;
                                startFastStick = true;
                                handler.post(runnable);
                                LogUtil.log(TAG, logMessage);
                                return;
                            }
                        }
                        if (idArray[0] == 11 || idArray[0] == 12 || idArray[0] == 13 ||
                                idArray[0] == 14 || idArray[0] == 15 || idArray[0] == 16 || idArray[0] == 17
                                || idArray[0] == 18 || idArray[0] == 19) {
                            if (!startFastStick) {
                                if ((idArray.length >= 5 && ultrasonicHeight <= 2 && flyingHeight < 3)) {
                                    String logMessage = "参考Marker数目降落:" + idArray.length +
                                            " Flying Height:" + flyingHeight + "--" +
                                            " Ultrasonic Height:" + ultrasonicHeight;
                                    startFastStick = true;
                                    handler.post(runnable);
                                    LogUtil.log(TAG, logMessage);
                                    return;
                                } else {
                                    Core.extractChannel(mArucoCornerList.get(0), corner, 0);
                                    Point[] points = corner.toArray();
                                    // 计算宽度（两个相邻角点之间的距离）
                                    double width = calculateDistance(points[0], points[1]);
                                    // 计算高度（另外两个相邻角点之间的距离）
//                                double height = calculateDistance(points[1], points[2]);
                                    //存在识别到小Marker，但参考5号的尺寸降落的可能性
                                    if (width >= 260 && idArray.length >= 5 && ultrasonicHeight <= 2) {
                                        String logMessage = "参考Marker尺寸降落:" + idArray[0] + " arucoW" + width +
                                                " Flying Height:" + flyingHeight + "--" +
                                                " Ultrasonic Height:" + ultrasonicHeight;
                                        startFastStick = true;
                                        handler.post(runnable);
                                        LogUtil.log(TAG, logMessage);
                                        return;
                                    }
                                }
                            }
                        }

                        findAruco(idArray);

                        if (mFindArucoList.size() == 0) {
                            sigleMarkerDetectFailsTimes++;
                            if (sigleMarkerDetectFailsTimes >= 25) {
                                sigleMarkerDetectFailsTimes = 0;
                                setDetectedBigMarkers();
                                LogUtil.log(TAG, "重置识别二维码状态:" + idArray.length+" id:"+idArray[0]);
                                markerId5MaxFindHeight=-20.0;
                            }
                        } else {
                            sigleMarkerDetectFailsTimes = 0;
                            Core.extractChannel(mFindArucoList.get(0).getConner(), corner, 0);
                            Point[] points = corner.toArray();
                            // 计算宽度（两个相邻角点之间的距离）
                            double width = calculateDistance(points[0], points[1]);
                            moveOnArucoDetected(mFindArucoList, rgbMat.width(), rgbMat.height(), width);
                        }
                        dropTimesTag = true;
                    }

                    else {
                        if (!arucoNotFoundTag) {
                            startTime = System.currentTimeMillis();
                            arucoNotFoundTag = true;
                        }
                        endTime = System.currentTimeMillis();
                        //记录第一次识别不到二维码的时间,如果小于20s,拉高或拉低复降,否则降落至备降点
                        if (endTime - startTime > 700 && endTime - startTime <= 8000) {
                            if (Movement.getInstance().getFlyingHeight() <= 7) {
                                //可能由于appCrash后，识别不到二维码，尝试将飞机拉高识别
                                setDetectedBigMarkers();
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, 0.7);
                                if (dropTimes > Integer.parseInt(AMSConfig.getInstance().getAlternateLandingTimes())) {
                                    LogUtil.log(TAG, "超过复降限制,去备降点");
                                    AlternateLandingManager.getInstance().startTaskProcess(null);
                                    return;
                                }
                                if (dropTimesTag) {
                                    dropTimesTag = false;
                                    dropTimes++;
//                                    virtualStickAdvancedParam=0.145;
                                    LogUtil.log(TAG, "复降第:" + dropTimes + "次");
                                }
                            } else if (Movement.getInstance().getFlyingHeight() > 7) {
                                //可能是由于飞机太高，识别不到二维码，尝试将飞机拉低识别
                                setDetectedBigMarkers();
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -0.4);
                            }
                        } else if (endTime - startTime > 8000) {
                            if (!triggerToAlternateLandingPoint) {
                                triggerToAlternateLandingPoint = true;
                                LogUtil.log(TAG, "判定未识别到二维码,飞往备降点");
                                AlternateLandingManager.getInstance().startTaskProcess(null);
                            }
                        }
                    }
                    ids.release();
                    yuvMat.release();
                    rgbMat.release();
                    grayImgMat.release();
                    mFindArucoList.clear();
                    mArucoCornerList.clear();
                    corner.release();
                    isStartAruco = false;
                } catch (Exception e) {
                    isStartAruco = false;
                    mFindArucoList.clear();
                    mArucoCornerList.clear();
                }
            }
        },0, TimeUnit.MILLISECONDS);


    }


    /**
     * 61     * 计算两个点之间的欧几里得距离
     * 62     * @param p1 第一个点
     * 63     * @param p2 第二个点
     * 64     * @return 两点之间的距离
     * 65
     */
    private double calculateDistance(Point p1, Point p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    //飞机椭球高度返回过低，导致5号Marker过早的不识别，悬停在3-4米处(浙江海事，汾湖演示出现过)
    private double markerId5MaxFindHeight=0.7;
    private double markerId1234MinFindHeight=7;
    public void findAruco(int[] idArray) {
        if (Movement.getInstance().getFlyingHeight()<markerId1234MinFindHeight){
            if (Movement.getInstance().getFlyingHeight() <= 1.5
                    || Movement.getInstance().getUltrasonicHeight() < 15) {
                if (isDoublePayload) {
                    if (mFindArucoList.isEmpty()) {
                        for (int i = 0; i < idArray.length; i++) {
                            if (idArray[i] == 19) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 19;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 20 && detectedSmallMarkerId != 19) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 20;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 17 && detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 17;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }

                            if (idArray[i] == 18 && detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 18;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }

                            if (idArray[i] == 15 && detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 15;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 16  && detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 16;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 24  && detectedSmallMarkerId != 16&& detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 24;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 25  && detectedSmallMarkerId != 24&& detectedSmallMarkerId != 16&& detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 25;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 23  && detectedSmallMarkerId != 25&& detectedSmallMarkerId != 24&& detectedSmallMarkerId != 16&& detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 23;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 13  && detectedSmallMarkerId != 23 && detectedSmallMarkerId != 25&& detectedSmallMarkerId != 24&& detectedSmallMarkerId != 16&& detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 13;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 14  && detectedSmallMarkerId != 13&& detectedSmallMarkerId != 23 && detectedSmallMarkerId != 25&& detectedSmallMarkerId != 24&& detectedSmallMarkerId != 16&& detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 14;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 22   && detectedSmallMarkerId != 14&& detectedSmallMarkerId != 13&& detectedSmallMarkerId != 23 && detectedSmallMarkerId != 25&& detectedSmallMarkerId != 24&& detectedSmallMarkerId != 16&& detectedSmallMarkerId != 15&& detectedSmallMarkerId != 18&& detectedSmallMarkerId != 20 && detectedSmallMarkerId != 19&& detectedSmallMarkerId != 17) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 22;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                        }
                    }

                } else {
                    if (mFindArucoList.isEmpty()) {
                        for (int i = 0; i < idArray.length; i++) {
                            if (idArray[i] == 15) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 15;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 16 && detectedSmallMarkerId != 15) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 16;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }

                            if (idArray[i] == 13&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 13;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }

                            if (idArray[i] == 14&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16&&detectedSmallMarkerId!=13) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 14;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }

                            if (idArray[i] == 17&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16&&detectedSmallMarkerId!=14&&detectedSmallMarkerId!=13) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 17;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }

                            if (idArray[i] == 18&&detectedSmallMarkerId!=17&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16&&detectedSmallMarkerId!=14&&detectedSmallMarkerId!=13) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 18;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 23&&detectedSmallMarkerId!=18&&detectedSmallMarkerId!=17&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16&&detectedSmallMarkerId!=14&&detectedSmallMarkerId!=13) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 23;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 22&&detectedSmallMarkerId!=23&&detectedSmallMarkerId!=18&&detectedSmallMarkerId!=17&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16&&detectedSmallMarkerId!=14&&detectedSmallMarkerId!=13) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 22;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                            if (idArray[i] == 24&&detectedSmallMarkerId!=22&&detectedSmallMarkerId!=24&&detectedSmallMarkerId!=18&&detectedSmallMarkerId!=17&&detectedSmallMarkerId!=15&&detectedSmallMarkerId!=16&&detectedSmallMarkerId!=14&&detectedSmallMarkerId!=13) {
                                detectedSmallMarkers = true;
                                detectedSmallMarkerId = 22;
                                mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                                return;
                            }
                        }
                    }
                }
            }

            if (Movement.getInstance().getFlyingHeight() > markerId5MaxFindHeight &&
                    mFindArucoList.isEmpty() && !detectedSmallMarkers) {

                if (
                        (detectedBigMarkerId == 0
                                || detectedBigMarkerId == 1
                                || detectedBigMarkerId == 2
                                || detectedBigMarkerId == 3
                                || detectedBigMarkerId == 4
                                || detectedBigMarkerId == 5
                                || detectedBigMarkerId == 6
                        )) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 5) {
                            detectedBigMarkerId = 5;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.15f));
                            return;
                        }
                    }
                }

                if (
                        (detectedBigMarkerId == 0
                                || detectedBigMarkerId == 1
                                || detectedBigMarkerId == 2
                                || detectedBigMarkerId == 3
                                || detectedBigMarkerId == 4
                                || detectedBigMarkerId == 6
                        )) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 6) {
                            detectedBigMarkerId = 6;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.24f));
                            return;
                        }
                    }
                }


            }

            if (Movement.getInstance().getFlyingHeight() > 2.5) {
                if (mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                        (detectedBigMarkerId == 0 || detectedBigMarkerId == 1 || detectedBigMarkerId == 2
                                || detectedBigMarkerId == 3 || detectedBigMarkerId == 4)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 1) {
                            detectedBigMarkerId = 1;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.18f));
                            return;
                        }
                    }
                }

                if (mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 ||
                        detectedBigMarkerId == 2 || detectedBigMarkerId == 3 || detectedBigMarkerId == 4)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 2) {
                            detectedBigMarkerId = 2;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.18f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 3)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 3) {
                            detectedBigMarkerId = 3;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.18f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 4)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 4) {
                            detectedBigMarkerId = 4;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.18f));
                            return;
                        }
                    }
                }
            }

        } else {
            for (int i = 0; i < idArray.length; i++) {
                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), ArucoMarkerDimensions.getSizeById(idArray[i])));
            }
        }

    }


    public void setDetectedBigMarkers() {
        detectedBigMarkerId = 0;
        detectedSmallMarkerId = 0;
        detectedMediumMarkers = false;
        detectedSmallMarkers = false;
        startFastStick = false;
        isStartAruco = false;
    }

    //根据识别到的二维码移动无人机
    private void moveOnArucoDetected(List<ArucoMarker> arucoMarkers, int imageWidth, int imageHeight, double arucoWidth) {
        int id = arucoMarkers.get(0).getId();
        int ultrasonicHeight = Movement.getInstance().getUltrasonicHeight();
        // 计算图像中心
        Point imgCenter = new Point(imageWidth / 2.0, imageHeight / 2.0);
        double sumX = 0, sumY = 0;
        int markerCount = arucoMarkers.size();
        // 遍历所有 marker 计算中心点
        for (int i = 0; i < markerCount; i++) {
            Mat markerCorners = arucoMarkers.get(i).getConner();
            double centerX = 0, centerY = 0;

            for (int j = 0; j < 4; j++) {
                double[] point = markerCorners.get(0, j);
                centerX += point[0];
                centerY += point[1];
            }
            centerX /= 4.0;
            centerY /= 4.0;

            sumX += centerX;
            sumY += centerY;
        }

        // 计算所有 marker 的平均中心点
        double avgCenterX = sumX / markerCount;
        double avgCenterY = sumY / markerCount;

        // 计算整体偏移量
        double offsetX = avgCenterX - imgCenter.x;
        double offsetY = avgCenterY - imgCenter.y;
        double outX;
        double outY;
        double outZ;

        //相机内参
        Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F);
        cameraMatrix.put(0, 0,  1131.3484309796945);
        cameraMatrix.put(1, 1, 1143.0319750579686);
        cameraMatrix.put(0, 2, 676.696876660099);
        cameraMatrix.put(1, 2, 532.6254545540435);
        cameraMatrix.put(2, 2, 1.0);
        //相机畸变
        Mat distCoeffs = Mat.zeros(5, 1, CvType.CV_64FC1);
        distCoeffs.put(0, 0, -0.16879686656897544);
        distCoeffs.put(1, 0, 0.4252674979687209);
        distCoeffs.put(2, 0, 0.004260616672174669);
        distCoeffs.put(3, 0, 0.010597384861297276);
        distCoeffs.put(4, 0, -0.6032569042575567);
        //旋转矩阵
        Mat rvecs = new Mat();
        //位移矩阵
        Mat tvecs = new Mat();
        //姿态预估
        List<Mat> conners = new ArrayList<>();
        conners.add(arucoMarkers.get(0).getConner());
        Aruco.estimatePoseSingleMarkers(conners, arucoMarkers.get(0).getSize(), cameraMatrix, distCoeffs, rvecs, tvecs);

        Mat tvec = tvecs.row(0);
        double z = tvec.get(0, 0)[2];
        double x = tvec.get(0,0)[0];
        double y = tvec.get(0,0)[1];
        if (z > 2 && (
                id == 1
                        || id == 2
                        || id == 3
                        || id == 4
                        || id == 5
                        || id == 6
        )
        ) {
            //罗德里变换
            Mat R = new Mat(3, 3, CvType.CV_32FC1);
            Mat rvec = rvecs.row(0);
            Calib3d.Rodrigues(rvec, R);
            Mat camR = R.t();
            //左乘
            Mat _camR = new Mat();
            Scalar _1 = new Scalar(-1.0);
            Core.multiply(camR, _1, _camR);
            //旋转向量转欧拉角
            List<Double> eulerAngles = RotationConversion.INSTANCE.rotationMatrixToEulerAngles(camR);
            //欧拉角转飞机偏航角度
            double yawCamera = MathUtils.toDegree(eulerAngles.get(2));
//            LogUtil.log(TAG,"偏航角度："+yawCamera);
            if (yawCamera < 0) {
                if (yawCamera < -15 && z < 9 && z > 3) {
                    if (yawCamera < -80) {
                        resultYaw = -40.0;
                    } else {
                        resultYaw = -30.0;
                    }
                } else {
                    resultYaw = 0.0;
                }
            } else {
                if (yawCamera >= 15 && z < 9 && z > 3) {
                    if (yawCamera > 80) {
                        resultYaw = 40.0;
                    } else {
                        resultYaw = 30.0;
                    }
                } else {
                    resultYaw = 0.0;
                }
            }
            rvecs.release();
            tvecs.release();
            rvec.release();
            camR.release();
            _camR.release();
            R.release();
        } else {
            resultYaw = 0.0;
        }


        //先旋转,再平移或降落
        double absX = Math.abs(offsetX);
        double absY = Math.abs(offsetY);

        if (resultYaw != 0.0) {
            outX = 0.0f;
            outY = 0.0f;
            outZ = 0.0f;
        } else {

            if(z <=0.4){
                pidControlX.setInputFilterAll((float)offsetX/1750);
                pidControlY.setInputFilterAll(-(float)offsetY/1750);
                if (pidControlX.get_pid()<0){
                    if (pidControlX.get_pid()<-0.125){
                        outX=absX<120?0:-0.125;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }else{
                    if (pidControlX.get_pid()>0.125){
                        outX=absX<120?0:0.125;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }

                if (pidControlY.get_pid()<0){
                    if (pidControlY.get_pid()<-0.125){
                        outY=absY<120?0:-0.125;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }else{
                    if (pidControlY.get_pid()>0.125){
                        outY=absY<120?0:0.125;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }

                outZ = (absX < 230)
                        && (absY < 250)
                        ? -0.2 : 0;
            }else if(z <=0.7){
                pidControlX.setInputFilterAll((float)offsetX/1750);
                pidControlY.setInputFilterAll(-(float)offsetY/1750);
                if (pidControlX.get_pid()<0){
                    if (pidControlX.get_pid()<-0.135){
                        outX=absX<120?0:-0.135;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }else{
                    if (pidControlX.get_pid()>0.135){
                        outX=absX<120?0:0.135;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }

                if (pidControlY.get_pid()<0){
                    if (pidControlY.get_pid()<-0.135){
                        outY=absY<120?0:-0.135;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }else{
                    if (pidControlY.get_pid()>0.135){
                        outY=absY<120?0:0.135;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }

                outZ = (absX < 250)
                        && (absY < 250)
                        ? -0.35 : 0;
            }else if(z <=1){
                pidControlX.setInputFilterAll((float)offsetX/1550);
                pidControlY.setInputFilterAll(-(float)offsetY/1550);
                if (pidControlX.get_pid()<0){
                    if (pidControlX.get_pid()<-0.145){
                        outX=absX<120?0:-0.145;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }else{
                    if (pidControlX.get_pid()>0.145){
                        outX=absX<120?0:0.145;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }

                if (pidControlY.get_pid()<0){
                    if (pidControlY.get_pid()<-0.145){
                        outY=absY<120?0:-0.145;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }else{
                    if (pidControlY.get_pid()>0.145){
                        outY=absY<120?0:0.145;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }

                outZ = (absX < 180)
                        && (absY < 180)
                        ? -0.4 : 0;
            }else if(z <=1.5){
                pidControlX.setInputFilterAll((float)offsetX/1450);
                pidControlY.setInputFilterAll(-(float)offsetY/1450);
                if (pidControlX.get_pid()<0){
                    if (pidControlX.get_pid()<-0.185){
                        outX=absX<120?0:-0.185;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }else{
                    if (pidControlX.get_pid()>0.185){
                        outX=absX<120?0:0.185;
                    }else{
                        outX=absX<120?0:pidControlX.get_pid();
                    }
                }

                if (pidControlY.get_pid()<0){
                    if (pidControlY.get_pid()<-0.185){
                        outY=absY<120?0:-0.185;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }else{
                    if (pidControlY.get_pid()>0.185){
                        outY=absY<120?0:0.185;
                    }else{
                        outY=absY<120?0:pidControlY.get_pid();
                    }
                }

                outZ = (absX < 180)
                        && (absY < 180)
                        ? -0.4 : 0;
            }else if(z <=2){
                pidControlX.setInputFilterAll((float)offsetX/1350);
                pidControlY.setInputFilterAll(-(float)offsetY/1350);
                outX = absX<120?0:pidControlX.get_pid();
                outY = absY<120?0:pidControlY.get_pid();
                outZ = (absX < 200)
                        && (absY < 200)
                        ? -0.575 : 0;

            }else if(z <=3){
                pidControlX.setInputFilterAll((float)offsetX/1250);
                pidControlY.setInputFilterAll(-(float)offsetY/1250);
                outX = absX<120?0:pidControlX.get_pid();
                outY = absY<120?0:pidControlY.get_pid();
                outZ = (absX < 200)
                        && (absY < 200)
                        ? -0.575 : 0;
            }else if(z <=5){
                pidControlX.setInputFilterAll((float)offsetX/1050);
                pidControlY.setInputFilterAll(-(float)offsetY/1050);
                outX = absX<120?0:pidControlX.get_pid();
                outY = absY<120?0:pidControlY.get_pid();
                outZ = (absX < 200)
                        && (absY < 200)
                        ? -0.575 : 0;
            }else if (z <= 7) {
                pidControlX.setInputFilterAll((float)offsetX/950);
                pidControlY.setInputFilterAll(-(float)offsetY/950);
                outX = absX<120?0:pidControlX.get_pid();
                outY = absY<120?0:pidControlY.get_pid();
                outZ = (absX < 200)
                        && (absY < 200)
                        ? -0.575 : 0;

            }else {
                pidControlX.setInputFilterAll((float)offsetX/850);
                pidControlY.setInputFilterAll(-(float)offsetY/850);
                outX = absX<80?0:pidControlX.get_pid();
                outY = absY<80?0:pidControlY.get_pid();
                outZ = (absX < 130)
                        && (absY < 130)
                        ? -0.65 : 0;
            }
        }

        LogUtil.log(TAG,
                " 杆量x=" + outX +
                        " 偏移x=" + offsetX +
                        " 杆量y=" + outY +
                        " 偏移y=" + offsetY +
                        " Id=" + id +
                        " Size=" + arucoMarkers.size() +
                        " 宽度=" + arucoWidth +
                        " 椭球=" + Movement.getInstance().getFlyingHeight() +
                        " 融合=" + ultrasonicHeight +
                        " X=" + x +
                        " Z=" + z
        );
        if ((Math.abs(outX)>0.3||Math.abs(outY)>0.3)&&(Movement.getInstance().getFlyingHeight()<3.5)){
            if (ultrasonicHeight<=25){
                outX=outX>0?0.135:-0.135;
                outY=outY>0?0.135:-0.135;
                LogUtil.log(TAG,"过滤帧："+" 杆量x=" + outX+" 杆量y=" + y);
            }else{
                LogUtil.log(TAG,"过滤帧>："+" 杆量x=" + outX+" 杆量y=" + y);
            }
        }
        DroneHelper.getInstance().moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);

        if (id == 11 || id == 12 || id == 13 || id == 14 || id == 15
                || id == 16 || id == 17 || id == 18 || id == 19|| id == 20
                || id == 21|| id == 22|| id == 23|| id == 24|| id == 25) {
            checkConditions(absX, absY, id, arucoWidth);

        } else {
            canLanding = false;
        }
    }


    private void checkConditions(double absX, double absY, int id, double arucoWidth) {
        double ultrasonicHeight = Movement.getInstance().getUltrasonicHeight();
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        boolean xy = absX < 250 && absY < 300;
        String logMessage = "";
        if (!startFastStick) {

//            if (absX <= 250 && absY <= 300 && ultrasonicHeight <= 4 && flyingHeight <= 3) {
            if (absX <= 230 && absY <= 250 && ultrasonicHeight <= 4 && flyingHeight <= 3) {
                logMessage = "参考融合高度降落:" + id + " arucoW" + arucoWidth +
                        " Flying Height:" + flyingHeight + "--" +
                        " Ultrasonic Height:" + ultrasonicHeight;
                LogUtil.log(TAG, logMessage);
                startFastStick = true;
                handler.post(runnable);
                return;
            }
            if (xy && arucoWidth >= 280) {
                logMessage = "参考ArUco宽度+偏移量降落:" + id + " arucoW" + arucoWidth +
                        " Flying Height:" + flyingHeight + "--" +
                        " Ultrasonic Height:" + ultrasonicHeight;
                LogUtil.log(TAG, logMessage);
                startFastStick = true;
                handler.post(runnable);
                return;
            }
            if (arucoWidth >= 480) {
                logMessage = "参考ArUco宽度降落:" + id + " arucoW" + arucoWidth +
                        " Flying Height:" + flyingHeight + "--" +
                        " Ultrasonic Height:" + ultrasonicHeight;
                LogUtil.log(TAG, logMessage);
                startFastStick = true;
                handler.post(runnable);
            }
        }
    }
    public boolean startFastStick;

    public boolean isStartFastStick() {
        return startFastStick;
    }

    public void setStartFastStick(boolean startFastStick) {
        this.startFastStick = startFastStick;
    }

    public boolean canLanding;

    public boolean isCanLanding() {
        return canLanding;
    }

    public void setCanLanding(boolean canLanding) {
        //测试重置未识别和识别时间,避免刚触发识别就飞向备降点
        startTime = 0;
        endTime = 0;
        this.canLanding = canLanding;

    }



    private int handlerCallbackCount = 0; // 记录回调次数
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            performOperation();
            if (handlerCallbackCount < 20) {
                handler.postDelayed(this, 50); // 每 50 毫秒执行一次，1 秒内执行 20 次
            } else {
                performNextStep();
            }
        }
    };

    private void performOperation() {
        LogUtil.log(TAG,"快速下拉中..."+handlerCallbackCount);
        DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -4);
        handlerCallbackCount++; // 增加计数器
    }

    private void performNextStep() {
        canLanding = true;
        handlerCallbackCount = 0;
        dropTimes = 0;//手动测试避免多次累加后直接飞往备降点
        markerId5MaxFindHeight = 0.7;
        LogUtil.log(TAG,"下拉完成：触发下一步自动降落");
        handler.removeCallbacks(runnable); // 防止重复执行

    }

}