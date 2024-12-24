
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

    //没识别到二维码
    private boolean arucoNotFoundTag;

    private boolean isStartAruco=false;
    //    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    Future<?> lastFuture = null;

    private String TAG = getClass().getSimpleName();
    Double resultYaw = 0.0;
    private String productType;
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

    public boolean isDoublePayload() {
        return isDoublePayload;
    }

    public void setDoublePayload(boolean doublePayload) {
        isDoublePayload = doublePayload;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    private ApronArucoDetect() {
    }

    private static class OpenCVHelperHolder {
        private static final ApronArucoDetect INSTANCE = new ApronArucoDetect();
    }

    public static ApronArucoDetect getInstance() {
        return OpenCVHelperHolder.INSTANCE;
    }


    public void detectArucoTags(int height, int width, byte[] data, Dictionary dictionary) {
        if (startFastStick) {
            LogUtil.log(TAG, "速降中");
        }
        if (isStartAruco || startFastStick) {
            return;
        }
        isStartAruco = true;
        if (lastFuture != null && !lastFuture.isDone()) {
            LogUtil.log(TAG,"break---");
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
                            if (!startFastStick&&width >= 1100 && ultrasonicHeight <= 4) {
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
                            if (!startFastStick&&width >= 1250 && ultrasonicHeight <= 3) {
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
                                idArray[0] == 14 || idArray[0] == 15 || idArray[0] == 16 || idArray[0] == 17 || idArray[0] == 18 || idArray[0] == 19) {
                            if (!startFastStick) {
                                if ((idArray.length >= 6 && ultrasonicHeight <= 3 && flyingHeight < 3)) {
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
                                    if (width >= 260 && idArray.length >= 5 && ultrasonicHeight <= 3) {
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
                        if (endTime - startTime > 600 && endTime - startTime <= 8000) {
                            if (Movement.getInstance().getFlyingHeight() <= 7) {
                                //可能由于appCrash后，识别不到二维码，尝试将飞机拉高识别
                                setDetectedBigMarkers();
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, 0.5);
                                if (dropTimes > Integer.parseInt(AMSConfig.getInstance().getAlternateLandingTimes())) {
                                    LogUtil.log(TAG, "超过复降限制,去备降点");
                                    AlternateLandingManager.getInstance().startTaskProcess(null);
                                    return;
                                }
                                if (dropTimesTag) {
                                    dropTimesTag = false;
                                    dropTimes++;
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
                    grayImgMat.release();
                    ids.release();
                    yuvMat.release();
                    grayImgMat.release();
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


    public void findAruco(int[] idArray) {
        if (Movement.getInstance().getFlyingHeight()<7){
            if (Movement.getInstance().getFlyingHeight() <= 1.5 || Movement.getInstance().getUltrasonicHeight() < 15) {
                if (isDoublePayload()) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 11 || idArray[i] == 12 || idArray[i] == 13 || idArray[i] == 14 || idArray[i] == 15
                                || idArray[i] == 16) {
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), ArucoMarkerDimensions.getSizeById(idArray[i])));
                        }
                    }
                } else {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 11 || idArray[i] == 12 || idArray[i] == 13 || idArray[i] == 14 || idArray[i] == 15
                                || idArray[i] == 16 || idArray[i] == 17 || idArray[i] == 18 || idArray[i] == 19) {
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), ArucoMarkerDimensions.getSizeById(idArray[i])));
                        }
                    }
                }
            }

            if (Movement.getInstance().getFlyingHeight() > 0.7 && mFindArucoList.isEmpty() && !detectedSmallMarkers) {

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
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.12f));
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
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.09f));
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
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.295f));
                            return;
                        }
                    }
                }

                if (mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 ||
                        detectedBigMarkerId == 2 || detectedBigMarkerId == 3 || detectedBigMarkerId == 4)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 2) {
                            detectedBigMarkerId = 2;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.295f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 3)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 3) {
                            detectedBigMarkerId = 3;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.295f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 4)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 4) {
                            detectedBigMarkerId = 4;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.295f));
                            return;
                        }
                    }
                }
            }

        } else {
            for (int i = 0; i < idArray.length; i++) {
                if (idArray[i]==1|| idArray[i] == 2 || idArray[i] == 3 || idArray[i] == 4 || idArray[i] == 5
                        || idArray[i] == 6|| idArray[i] == 7|| idArray[i] == 8|| idArray[i] == 9){
                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), ArucoMarkerDimensions.getSizeById(idArray[i])));
                }
            }

//                mFindArucoList.add(new ArucoMarker(idArray[0], mArucoCornerList.get(0), ArucoMarkerDimensions.getSizeById(idArray[0])));


        }



    }


    public void setDetectedBigMarkers() {
        detectedBigMarkerId = 0;
        detectedSmallMarkerId = 0;
        detectedMediumMarkers = false;
        detectedSmallMarkers = false;
        startFastStick = false;

    }

    //根据识别到的二维码移动无人机
    private void moveOnArucoDetected(List<ArucoMarker> arucoMarkers, int imageWidth, int imageHeight, double arucoWidth) {
        int id = arucoMarkers.get(0).getId();
        //计算标记中心
        double centerX = 0, centerY = 0;
        Scalar imageVector;
//        if (Movement.getInstance().getFlyingHeight() >= 7) {
        for (int i = 0; i < arucoMarkers.size(); i++) {
            centerX = centerX + Core.mean(arucoMarkers.get(i).getConner()).val[0] - (imageWidth / 2f);
            centerY = centerY + Core.mean(arucoMarkers.get(i).getConner()).val[1] - (imageHeight / 2f);
        }
        imageVector = new Scalar(centerX / arucoMarkers.size(), centerY / arucoMarkers.size());
//        } else {
//            centerX = Core.mean(arucoMarkers.get(0).getConner()).val[0] - (imageWidth / 2f);
//            centerY = Core.mean(arucoMarkers.get(0).getConner()).val[1] - (imageHeight / 2f);
//            //计算相对于图像中心的图像矢量
//            imageVector = new Scalar(centerX, centerY);
//        }


        // 打印宽度和高度
//        LogUtil.log(TAG, "Aruco:" + mFindArucoList.get(0).getId() + "arucoW:" + arucoWidth + "imageW:" + imageWidth);


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
//        LogUtil.log(TAG, "z坐标:" + z + "融合高:" + Movement.getInstance().getUltrasonicHeight());
        if (Movement.getInstance().getFlyingHeight() > 3 && (
                id == 1
                        || id == 2
                        || id == 3
                        || id == 4
                        || id == 5
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
                if (yawCamera < -15 && Movement.getInstance().getFlyingHeight() < 9 && Movement.getInstance().getFlyingHeight() > 5) {
                    if (yawCamera < -80) {
                        resultYaw = -40.0;
                    } else {
                        resultYaw = -30.0;
                    }
                } else {
                    resultYaw = 0.0;
                }
            } else {
                if (yawCamera >= 15 && Movement.getInstance().getFlyingHeight() < 9 && Movement.getInstance().getFlyingHeight() > 5) {
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
        double absX = Math.abs(imageVector.val[0]);
        double absY = Math.abs(imageVector.val[1]);
        if (resultYaw != 0.0) {
            outX = 0.0f;
            outY = 0.0f;
            outZ = 0.0f;
        } else {
            outX = imageVector.val[0] < 0 ? -updateOutXYSpeed(absX)
                    : updateOutXYSpeed(absX);
            outY = imageVector.val[1] < 0 ? updateOutXYSpeed(absY)
                    : -updateOutXYSpeed(absY);
            if (Movement.getInstance().getFlyingHeight() > 9) {
                outZ = (absX < 150)
                        && (absY < 100)
                        ? -0.675 : 0f;
            } else {
                int xf,yf;
                if (Movement.getInstance().getFlyingHeight()<=3&&Movement.getInstance().getUltrasonicHeight()<=4){
                    xf=180;
                    yf=160;
                }else{
                    xf = 200;
                    yf = 200;
                }
                outZ = (absX < xf)
                        && (absY < yf)
                        ? updateOutDownSpeed() : 0f;
            }

        }

        LogUtil.log(TAG, "MarkerId=" + id +
                " 宽度=" + arucoWidth +
                " 杆量x=" + outX +
                " 偏移=" + imageVector.val[0] +
                " 椭球=" + Movement.getInstance().getFlyingHeight() +
                " 融合=" + Movement.getInstance().getUltrasonicHeight()
        );

        DroneHelper.getInstance().moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);

        if (id == 11 || id == 12 || id == 13 || id == 14 || id == 15
                || id == 16 || id == 17 || id == 18 || id == 19) {
            checkConditions(absX, absY, id, arucoWidth);
        } else {
            canLanding = false;
        }
    }


    private void checkConditions(double absX, double absY, int id, double arucoWidth) {
        double ultrasonicHeight = Movement.getInstance().getUltrasonicHeight();
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        boolean xy = absX <= 180 && absY <= 160;
        String logMessage = "";
        if (!startFastStick) {

            if (absX <= 100 && absY <= 100 && ultrasonicHeight <= 4 && flyingHeight <= 3) {
                logMessage = "参考融合高度降落:" + id + " arucoW" + arucoWidth +
                        " Flying Height:" + flyingHeight + "--" +
                        " Ultrasonic Height:" + ultrasonicHeight;
                LogUtil.log(TAG, logMessage);
                startFastStick = true;
                handler.post(runnable);
                return;
            }
            if (xy && arucoWidth >= 240) {
                logMessage = "参考Aurco偏移量降落:" + id + " arucoW" + arucoWidth +
                        " Flying Height:" + flyingHeight + "--" +
                        " Ultrasonic Height:" + ultrasonicHeight;
                LogUtil.log(TAG, logMessage);
                startFastStick = true;
                handler.post(runnable);
            }
        }
    }
    private boolean startFastStick;

    private boolean canLanding;

    public boolean isCanLanding() {
        return canLanding;
    }

    public void setCanLanding(boolean canLanding) {
        //测试重置未识别和识别时间,避免刚触发识别就飞向备降点
        startTime = 0;
        endTime = 0;
        this.canLanding = canLanding;

    }


    //根据偏移量和高度决定X/Y轴移动速度
    private double updateOutXYSpeed(double d) {
        double ultrasonicHeight = Movement.getInstance().getFlyingHeight();
        if (d > 500) {
            if (ultrasonicHeight > 9) {
                return 0.325;
            } 
//            else if (ultrasonicHeight > 1.5 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1.5) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            } 
            else {
                return 0.145;
            }
        } else if (d <= 500 && d > 400) {
            if (ultrasonicHeight > 9) {
                return 0.325;
            }
//            else if (ultrasonicHeight > 1.5 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1.5) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            }
            else {
                return 0.145;
            }
        } else if (d <= 400 && d > 300) {
            if (ultrasonicHeight > 9) {
                return 0.325;
            }
//            else if (ultrasonicHeight > 1.5 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1.5) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            }
            else {
                return 0.145;
            }
        } else if (d <= 300 && d > 250) {
            if (ultrasonicHeight > 9) {
                return 0.295;
            }
//            else if (ultrasonicHeight > 1.5 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1.5) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            }
            else {
                return 0.145;
            }
        } else if (d <= 250 && d > 200) {
            if (ultrasonicHeight > 9) {
                return 0.295;
            }
//            else if (ultrasonicHeight > 1.5 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1.5) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            }
            else {
                return 0.145;
            }
        }else if (d <= 200 && d > 150) {
            if (ultrasonicHeight > 9) {
                return 0.275;
            }
//            else if (ultrasonicHeight > 4 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 4) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            }
            else {
                return 0.145;
            }
        } else if (d <= 150 && d > 100) {
//            if (ultrasonicHeight > 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 9) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 4) {
//                return 0.145;
//            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
//                return 0.145;
//            } else {
                return 0.145;
//            }
        } else if (d <= 100 && d > 79) {
            if (ultrasonicHeight > 9) {
                return 0;
            } else if (ultrasonicHeight > 1.5 && ultrasonicHeight <= 9) {
                return 0.145;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1.5) {
                return 0.0;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.0;
            } else {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }


    //根据不同高度决定下降多快
    private double updateOutDownSpeed() {
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        if (flyingHeight > 1) {
            return -0.675;
        } else if (flyingHeight <= 1.0 && flyingHeight >= 0.5) {
            return -0.475;
        } else {
            return -0.235;
        }
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
        DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -2);
        handlerCallbackCount++; // 增加计数器
    }

    private void performNextStep() {
        handler.removeCallbacks(runnable); // 防止重复执行
        handlerCallbackCount=0;
        canLanding = true;
    }


}