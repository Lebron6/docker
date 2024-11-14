package com.aros.apron.util;

import android.os.Handler;
import android.os.Looper;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ArucoMarker;
import com.aros.apron.entity.Movement;
import com.aros.apron.manager.AlternateLandingManager;
import com.aros.apron.tools.DroneHelper;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MathUtils;
import com.aros.apron.tools.RotationConversion;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArucoUtil {
    private static class OpenCVHelperHolder {
        private static final ArucoUtil INSTANCE = new ArucoUtil();
    }

    private String TAG = getClass().getSimpleName();

    public static ArucoUtil getInstance() {
        return OpenCVHelperHolder.INSTANCE;
    }

    //没识别到二维码
    private boolean arucoNotFoundTag;
//    是否已经开始了检测二维码降落程序
    private boolean isStartAruco;
    Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250);
    //   单线程池
    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    private List<ArucoMarker> mFindArucoList = new ArrayList<>();
    List<Mat> mArucoCornerList = new ArrayList<>();
    private int detectedBigMarkerId;
    private int detectedSmallMarkerId;
    private boolean detectedSmallMarkers;
    //复降触发条件
    private boolean dropTimesTag;
    //复降次数
    private int dropTimes;
    //是否双挂
    private boolean isDoublePayload;
    //当确认识别单一的二维码后，有概率下降途中识别不到，此时次数超过15次,可以控制识别别的二维码
    private int sigleMarkerDetectFailsTimes;
    //触发去备降点
    private boolean triggerToAlternateLandingPoint;
    long startTime;
    long endTime;

    public boolean isDoublePayload() {
        return isDoublePayload;
    }

    public void setDoublePayload(boolean doublePayload) {
        isDoublePayload = doublePayload;
    }

    public void detectArucoTags(int height, int width, byte[] data, Dictionary dictionary) {
        if (isStartAruco || startFastStick) {
            return;
        }
        isStartAruco = true;
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
//                    数字图像在计算机中都是以矩阵的形式存储的，矩阵数据表示这图像的亮度，颜色等。
//                    OpenCV提供了Mat（矩阵matrix）来存储矩阵。Mat是一个类，也是1个容器。
//                    先存储第1个元素每个通道的数据，再存储第2个元素每个通道的数据
                    Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);//表示8位无符号1通道数据，用于表示8位灰度图
                    yuvMat.put(0, 0, data);
                    Mat rgbMat = new Mat();
//                Imgproc.cvtColor：将图像从一个颜色空间转换为另一个颜色空间。
                    Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2GRAY_420);
//                灰度
                    Mat grayImgMat = new Mat();
                    Imgproc.cvtColor(rgbMat, grayImgMat, Imgproc.COLOR_RGBA2GRAY);
                    MatOfInt ids = new MatOfInt();
                    mFindArucoList.clear();
                    mArucoCornerList.clear();
                    MatOfPoint2f corner = new MatOfPoint2f();
                /*
                检测，markerCorners（mArucoCornerList） 是检测出的图像的角的列表。对于每个marker，将返回按照原始顺序
                    排列的四个角（从左上角顺时针开始）,里面每个元素都是一个浮点型向量用Mat存，每个Mat都有四个行，存四个角。
                    因此，第一个点是左上角的角，紧接着右上角、右下角和左下角。
                markerIds(ids) 是在markerCorners检测出的所有maker的id列表.注意返回的markerCorners和markerIds 向量具有相同的大小。
                    二维码黑白二进制收尾相连的值
                 */
                    Aruco.detectMarkers(grayImgMat, dictionary, mArucoCornerList, ids);
                    if (ids.depth() > 0) {
                        arucoNotFoundTag = false;
//                    可视化
                        Aruco.drawDetectedMarkers(grayImgMat, mArucoCornerList, ids);
                        int[] idArray = ids.toArray();
                        findAruco(idArray);
                        if (mFindArucoList.size() == 0) {
                            sigleMarkerDetectFailsTimes++;
                            // TODO: 2024/10/11 一直有干扰aruco会一直重置二维码状态
//                        重新识别其他二维码
                            if (sigleMarkerDetectFailsTimes >= 20) {
                                sigleMarkerDetectFailsTimes = 0;
                                setDetectedBigMarkers();
                            }
                        } else {
//                        识別到有效二维码
                            sigleMarkerDetectFailsTimes = 0;
//                        可以分离多通道图像，可以得到各个通道的图像。
//                        指定要提取的通道，可以取值0（蓝色），1（绿色），2（红色），如果是BGR图像，或者是3（透明度，对于RGBA）
                            Core.extractChannel(mFindArucoList.get(0).getConner(), corner, 0);
                            Point[] points = corner.toArray();
//                        二维码的宽高
                            double width = calculateDistance(points[0], points[1]);
                            double height = calculateDistance(points[1], points[2]);
                            moveOnArucoDetected(mFindArucoList, rgbMat.width(), rgbMat.height(), width, height);
                        }
                        dropTimesTag = true;
                    } else {
                        if (!arucoNotFoundTag) {
//                        第一次没识别到二维码
                            startTime = System.currentTimeMillis();
                            arucoNotFoundTag = true;
                        }
                        endTime = System.currentTimeMillis();
                        if (endTime - startTime > 1000 && endTime - startTime <= 8000) {
                            if (Movement.getInstance().getFlyingHeight() <= 7) {
//                            拉高尝试识別
                                setDetectedBigMarkers();
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, 0.3);
                                if (dropTimes > Integer.parseInt(AMSConfig.getInstance().getAlternateLandingTimes())) {
                                    LogUtil.log(TAG, "超过复降限制,去备降点");
                                    AlternateLandingManager.getInstance().startTaskProcess(null);
                                    return;
                                }
                                // TODO: 2024/10/12 复降次数为什么在成功后下一帧不成功才算
                                if (dropTimesTag) {
                                    dropTimesTag = false;
                                    dropTimes++;
                                    LogUtil.log(TAG, "复降第:" + dropTimes + "次");
                                }
                            } else if (Movement.getInstance().getFlyingHeight() > 7) {
//                            尝试降低识別
                                setDetectedBigMarkers();
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -0.4);

                            }
                        } else if (endTime - startTime > 8000) {
                            // TODO: 2024/10/12 这个判断是干什么的
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
                }catch (Exception e){
                    isStartAruco = false;
                    mFindArucoList.clear();
                    mArucoCornerList.clear();
                }
            }
        });
    }

    private boolean startFastStick;
    private int handlerCallbackCount = 0; // 记录回调次数
    private boolean canLanding;
    public boolean isCanLanding() {
        return canLanding;
    }
    public void setCanLanding(boolean canLanding) {
        this.canLanding = canLanding;
        //测试重置未识别和识别时间,避免刚触发识别就飞向备降点
        startTime=0;
        endTime=0;
    }
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            performOperation();
            if (handlerCallbackCount < 10) {
                handler.postDelayed(this, 50); // 每 50 毫秒执行一次，1 秒内执行 20 次
            } else {
                performNextStep();
            }
        }
    };

    public void setDetectedBigMarkers() {
        detectedBigMarkerId = 0;
        detectedSmallMarkerId = 0;
//        detectedMediumMarkers = false;
        detectedSmallMarkers = false;
        startFastStick = false;
    }

    private double calculateDistance(Point p1, Point p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void findAruco(int[] idArray) {
        if (Movement.getInstance().getFlyingHeight() < 3) {
//            是否双挂
            if (isDoublePayload()) {
                if (mFindArucoList.isEmpty()) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 12) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 12;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 13)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 13) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 13;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }

                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 15)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 15) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 15;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }

                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 16)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 16) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 16;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 11)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 11) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 11;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 18)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 18) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 18;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 14)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 14) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 14;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 17)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 17) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 17;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 19)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 19) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 19;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
            }
            else {
//                和Switch语句的时间复杂度一样都是O(n)
                if (mFindArucoList.isEmpty()) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 15) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 15;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 16)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 16) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 16;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }

                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 14)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 14) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 14;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }

                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 12)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 12) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 12;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 13)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 13) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 13;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 11)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 11) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 11;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 18)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 18) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 18;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 19)) {
                    for (int i = 0; i < idArray.length; i++) {
                        if (idArray[i] == 19) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 19;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }
                if (mFindArucoList.isEmpty() && (detectedSmallMarkerId == 0 || detectedSmallMarkerId == 17)) {
                    for (int i = 0; i < idArray.length; i++) {

                        if (idArray[i] == 17) {
                            detectedSmallMarkers = true;
                            detectedSmallMarkerId = 17;
                            mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.03f));
                            return;
                        }
                    }
                }

            }
        }

        if (Movement.getInstance().getFlyingHeight() < 7.5) {
            //如果识别到小Aruco,则不再触发识别大Aruco
            if (Movement.getInstance().getFlyingHeight() > 0.7 && mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                    (detectedBigMarkerId == 0 || detectedBigMarkerId == 6
                            || detectedBigMarkerId == 1
                            || detectedBigMarkerId == 2
                            || detectedBigMarkerId == 3
                            || detectedBigMarkerId == 4
                            || detectedBigMarkerId == 5
                            || detectedBigMarkerId == 7
                            || detectedBigMarkerId == 8
                            || detectedBigMarkerId == 9)) {
                for (int i = 0; i < idArray.length; i++) {
                    if (idArray[i] == 6) {
                        detectedBigMarkerId = 6;
                        mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.09f));
                        return;
                    }
                }
            }

            if (Movement.getInstance().getFlyingHeight() > 0.7 && mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                    (detectedBigMarkerId == 0 || detectedBigMarkerId == 5
                            || detectedBigMarkerId == 1
                            || detectedBigMarkerId == 2
                            || detectedBigMarkerId == 3
                            || detectedBigMarkerId == 4
                            || detectedBigMarkerId == 7
                            || detectedBigMarkerId == 8
                            || detectedBigMarkerId == 9)) {
                for (int i = 0; i < idArray.length; i++) {
                    if (idArray[i] == 5) {
                        detectedBigMarkerId = 5;
                        mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.12f));
                        return;
                    }
                }
            }


            if (Movement.getInstance().getFlyingHeight() > 1 && mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                    (detectedBigMarkerId == 0 || detectedBigMarkerId == 8
                            || detectedBigMarkerId == 1
                            || detectedBigMarkerId == 2
                            || detectedBigMarkerId == 3
                            || detectedBigMarkerId == 4
                            || detectedBigMarkerId == 7
                            || detectedBigMarkerId == 9
                    )) {
                for (int i = 0; i < idArray.length; i++) {
                    if (idArray[i] == 8) {
                        detectedBigMarkerId = 8;
                        mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.09f));
                        return;
                    }
                }
            }

            if (Movement.getInstance().getFlyingHeight() > 1 && mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                    (detectedBigMarkerId == 0 || detectedBigMarkerId == 7
                            || detectedBigMarkerId == 1
                            || detectedBigMarkerId == 2
                            || detectedBigMarkerId == 3
                            || detectedBigMarkerId == 4
                            || detectedBigMarkerId == 9
                    )) {
                for (int i = 0; i < idArray.length; i++) {
                    if (idArray[i] == 7) {
                        detectedBigMarkerId = 7;
                        mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.09f));
                        return;
                    }
                }
            }

            if (Movement.getInstance().getFlyingHeight() > 1 && mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                    (detectedBigMarkerId == 0 || detectedBigMarkerId == 9
                            || detectedBigMarkerId == 1
                            || detectedBigMarkerId == 2
                            || detectedBigMarkerId == 3
                            || detectedBigMarkerId == 4
                    )) {
                for (int i = 0; i < idArray.length; i++) {
                    if (idArray[i] == 9) {
                        detectedBigMarkerId = 9;
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


    }

    Double resultYaw = 0.0;

    //根据识别到的二维码移动无人机
    private void moveOnArucoDetected(List<ArucoMarker> arucoMarkers, int imageWidth, int imageHeight, double arucoWidth, double arucoHeight) {

        //计算标记中心
//        double centerX = 0, centerY = 0;
//        for (int i = 0; i < arucoMarkers.size(); i++) {
//            centerX = centerX + Core.mean(arucoMarkers.get(i).getConner()).val[0] - (imageWidth / 2f);
//            centerY = centerY + Core.mean(arucoMarkers.get(i).getConner()).val[1] - (imageHeight / 2f);
//        }

        int id = arucoMarkers.get(0).getId();
        // 打印宽度和高度
//        LogUtil.log(TAG, "Aruco:" + mFindArucoList.get(0).getId() + "arucoW:" + arucoWidth + "imageW:" + imageWidth);
//        Core.mean().val[0]：得到中心趋势的x值，接近中间点的x值
        double centerX = Core.mean(arucoMarkers.get(0).getConner()).val[0] - (imageWidth / 2f);
        double centerY = Core.mean(arucoMarkers.get(0).getConner()).val[1] - (imageHeight / 2f);
        //计算相对于图像中心的图像矢量
//        Scalar imageVector = new Scalar(centerX / arucoMarkers.size(), centerY / arucoMarkers.size());
//        标量
        Scalar imageVector = new Scalar(centerX, centerY);
        double outX;
        double outY;
        double outZ;

        //相机内参
        Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F);
        cameraMatrix.put(0, 0, 1131.3484309796945);
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
//        得到的平移和旋转是以二维码中心点为原点的世界系在相机系下的位姿
        Aruco.estimatePoseSingleMarkers(conners, arucoMarkers.get(0).getSize(), cameraMatrix, distCoeffs, rvecs, tvecs);

        Mat tvec = tvecs.row(0);
        double z = tvec.get(0, 0)[2];
//        LogUtil.log(TAG, "z坐标:" + z + "融合高:" + Movement.getInstance().getUltrasonicHeight());
        if ((arucoMarkers.size() == 1) && Movement.getInstance().getFlyingHeight() > 3 && (
                id == 1
                        || id == 2
                        || id == 3
                        || id == 4
                        || id == 5
                        || id == 6
                        || id == 7
                        || id == 8
                        || id == 9
        )
        ) {
            //罗德里变换
            Mat R = new Mat(3, 3, CvType.CV_32FC1);
            Mat rvec = rvecs.row(0);
//            旋转向量转3x3的旋转矩阵
            Calib3d.Rodrigues(rvec, R);
//            矩阵转置
            Mat camR = R.t();
            //左乘
            Mat _camR = new Mat();
            Scalar _1 = new Scalar(-1.0);
            Core.multiply(camR, _1, _camR);
            //旋转矩阵转欧拉角
            List<Double> eulerAngles = RotationConversion.INSTANCE.rotationMatrixToEulerAngles(camR);
            //欧拉角转飞机偏航角度
            double yawCamera = MathUtils.toDegree(eulerAngles.get(2));
            if (yawCamera < 0) {
                if (yawCamera < -15) {
                    resultYaw = -30.0;
                } else {
                    resultYaw = 0.0;
                }
            } else {
                if (yawCamera >= 15) {
                    resultYaw = 30.0;
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
//        不为0代表还需要旋转
        if (resultYaw != 0.0) {
            outX = 0.0f;
            outY = 0.0f;
            outZ = 0.0f;
        } else {
            outX = imageVector.val[0] < 0 ? -updateOutXYSpeed(Math.abs(imageVector.val[0]))
                    : updateOutXYSpeed(Math.abs(imageVector.val[0]));
            outY = imageVector.val[1] < 0 ? updateOutXYSpeed(Math.abs(imageVector.val[1]))
                    : -updateOutXYSpeed(Math.abs(imageVector.val[1]));
            outZ = (Math.abs(imageVector.val[0]) < (Movement.getInstance().getFlyingHeight() > 1 ? 260 : 150))
                    && (Math.abs(imageVector.val[1]) < (Movement.getInstance().getFlyingHeight() > 1 ? 260 : 100))
                    ? updateOutDownSpeed() : 0f;
        }

        LogUtil.log(TAG, "Aruco=" + id + "  arucoW:" + arucoWidth + "  杆量x=" + outX + "  偏移:x=" + imageVector.val[0] + "    杆量y=" + outY + "  偏移:y=" + imageVector.val[1] + "  高度:z=" + Movement.getInstance().getFlyingHeight());

        DroneHelper.getInstance().moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);


        if (Math.abs(imageVector.val[0]) <= 150
                && Math.abs(imageVector.val[1]) <= 100 && (id == 11 || id == 12 || id == 13 || id == 14 || id == 15
                || id == 16 || id == 17 || id == 18 || id == 19)) {
            checkConditions(id, arucoWidth);
        } else {
            canLanding = false;
        }
    }

    public void checkConditions(int id, double arucoWidth) {
        double ultrasonicHeight = Movement.getInstance().getUltrasonicHeight();//超声波测高的值
        double flyingHeight = Movement.getInstance().getFlyingHeight();

        if ((ultrasonicHeight <= 5 && flyingHeight <= 1.7) || arucoWidth >= 120 || flyingHeight <= -0.5) {
            if (!startFastStick) {
                String logMessage = "";
                if (ultrasonicHeight <= 5 && flyingHeight <= 1.7) {
                    logMessage = "参考相对高度与融合高度降落:" + id + " arucoW" + arucoWidth +
                            " Flying Height:" + flyingHeight + "--" +
                            " Ultrasonic Height:" + ultrasonicHeight;
                } else if (arucoWidth >= 120) {
                    logMessage = "参考Aurco降落:" + id + " arucoW" + arucoWidth +
                            " Flying Height:" + flyingHeight + "--" +
                            " Ultrasonic Height:" + ultrasonicHeight;
                } else if (flyingHeight <= -0.5) {
                    logMessage = "参考相对高度降落:" + id + " arucoW" + arucoWidth +
                            " Flying Height:" + flyingHeight + "--" +
                            " Ultrasonic Height:" + ultrasonicHeight;
                }

                LogUtil.log(TAG, logMessage);
                startFastStick = true;
                handler.post(runnable);
            }
        }


    }

    //根据偏移量和高度决定X/Y轴移动速度
    private double updateOutXYSpeed(Double d) {
        double ultrasonicHeight = Movement.getInstance().getFlyingHeight();
        if (d > 500) {
            if (ultrasonicHeight > 6) {
                return 0.215;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.215;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.215;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.215;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.215;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.215;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.215;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 500 && d > 400) {
            if (ultrasonicHeight > 6) {
                return 0.205;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.205;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.205;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.205;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.205;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.205;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.205;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 400 && d > 300) {
            if (ultrasonicHeight > 6) {
                return 0.195;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.195;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.195;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.195;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.195;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.195;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.195;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 300 && d > 250) {
            if (ultrasonicHeight > 6) {
                return 0.185;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.185;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.185;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.185;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.185;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.185;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.185;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 250 && d > 200) {
            if (ultrasonicHeight > 6) {
                return 0.185;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.185;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.185;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.185;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.185;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.185;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.185;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 200 && d > 150) {
            if (ultrasonicHeight > 6) {
                return 0.175;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.175;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.175;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.175;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.175;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.175;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.175;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 150 && d > 100) {
            if (ultrasonicHeight > 6) {
                return 0.165;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.165;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.165;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.165;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.165;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.165;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.165;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.155;
            } else {
                return 0.145;
            }
        } else if (d <= 100 && d > 79) {
            if (ultrasonicHeight > 6) {
                return 0.095;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.095;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.095;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.095;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.095;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.095;
            } else if (ultrasonicHeight > 0.5 && ultrasonicHeight <= 1) {
                return 0.095;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 0.5) {
                return 0.065;
            } else {
                return 0.055;
            }
        } else {
            return 0.0;
        }
    }

    //根据不同高度决定下降多快
    private double updateOutDownSpeed() {
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        if (flyingHeight > 5) {
            return -0.455;
        } else if (flyingHeight <= 5 && flyingHeight > 3.5) {
            return -0.435;
        } else if (flyingHeight <= 3.5 && flyingHeight > 2.5) {
            return -0.395;
        } else if (flyingHeight <= 2.5 && flyingHeight > 2.0) {
            return -0.345;
        } else if (flyingHeight <= 2.0 && flyingHeight > 1.5) {
            return -0.275;
        } else if (flyingHeight <= 1.5 && flyingHeight > 1.0) {
            return -0.195;
        } else if (flyingHeight <= 1.0 && flyingHeight >= -0.5) {
            return -0.175;
        } else {
            return 0.0;
        }
    }

    private void performOperation() {
        DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -0.6);
        handlerCallbackCount++; // 增加计数器
    }

    private void performNextStep() {
        handler.removeCallbacks(runnable); // 防止重复执行
        handlerCallbackCount = 0;
        canLanding = true;
    }
}
