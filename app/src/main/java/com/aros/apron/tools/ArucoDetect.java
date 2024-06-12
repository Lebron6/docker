package com.aros.apron.tools;

import static dji.sdk.keyvalue.value.product.ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES;

import android.util.Log;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.ArucoMarker;
import com.aros.apron.entity.Movement;
import com.aros.apron.manager.AlternateLandingManager;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArucoDetect {

    //没识别到二维码
    private boolean arucoNotFoundTag;

    private boolean isStartAruco;
    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    private ArucoDetect() {
    }

    private static class OpenCVHelperHolder {
        private static final ArucoDetect INSTANCE = new ArucoDetect();
    }

    public static ArucoDetect getInstance() {
        return OpenCVHelperHolder.INSTANCE;
    }


    public void detectArucoTags(int height, int width, byte[] data, Dictionary dictionary) {
        if (isStartAruco) {
            return;
        }
        isStartAruco = true;
        mThreadPool.execute(new Runnable() {
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
                    Aruco.detectMarkers(grayImgMat, dictionary, mArucoCornerList, ids);
                    if (ids.depth() > 0) {
                        arucoNotFoundTag = false;
                        int[] idArray = ids.toArray();
                        if (mFindArucoList.isEmpty()) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 12) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId = 12;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 13)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 13) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =13;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }

                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 15)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 15) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =15;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }

                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 16)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 16) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =16;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 11)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 11) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =11;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 18)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 18) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =18;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 14)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 14) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =14;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 17)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 17) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =17;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.isEmpty()&& (detectedSmallMarkerId == 0 || detectedBigMarkerId == 19)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 19) {
                                    detectedSmallMarkers = true;
                                    detectedSmallMarkerId =19;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }




                        if (Movement.getInstance().getFlyingHeight() > 1.2&&mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                                (detectedBigMarkerId == 0 || detectedBigMarkerId == 5
                                        || detectedBigMarkerId == 1
                                        || detectedBigMarkerId == 2
                                        || detectedBigMarkerId == 3
                                        || detectedBigMarkerId == 4)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 5) {
                                    detectedBigMarkerId = 5;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }

                        //如果识别到小Aruco,则不再触发识别大Aruco
                        if (Movement.getInstance().getFlyingHeight() > 1.2&&mFindArucoList.isEmpty() && !detectedSmallMarkers &&
                                (detectedBigMarkerId == 0 || detectedBigMarkerId == 6
                                        || detectedBigMarkerId == 1
                                        || detectedBigMarkerId == 2
                                        || detectedBigMarkerId == 3
                                        || detectedBigMarkerId == 4)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 6) {
                                    detectedBigMarkerId = 6;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }

                        if (Movement.getInstance().getFlyingHeight() > 2 && mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 1)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 1) {
                                    detectedBigMarkerId = 1;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (Movement.getInstance().getFlyingHeight() > 2 && mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 2)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 2) {
                                    detectedBigMarkerId = 2;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (Movement.getInstance().getFlyingHeight() > 3 && mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 3)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 3) {
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (Movement.getInstance().getFlyingHeight() > 3 && mFindArucoList.isEmpty() && !detectedSmallMarkers && (detectedBigMarkerId == 0 || detectedBigMarkerId == 4)) {
                            for (int i = 0; i < idArray.length; i++) {
                                if (idArray[i] == 4) {
                                    detectedBigMarkerId = 4;
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i)));
                                    break;
                                }
                            }
                        }
                        if (mFindArucoList.size() == 0) {
                            sigleMarkerDetectFailsTimes++;
                            if (sigleMarkerDetectFailsTimes >= 40) {
                                sigleMarkerDetectFailsTimes=0;
                                setDetectedBigMarkers();
                                LogUtil.log(TAG, "标定的二维码未被识别,重置识别二维码状态");
                            }
                        } else {
                            sigleMarkerDetectFailsTimes = 0;
                            moveOnArucoDetected(mFindArucoList, rgbMat.width(), rgbMat.height());
                        }
                        dropTimesTag = true;
                    } else {
                        if (!arucoNotFoundTag) {
                            startTime = System.currentTimeMillis();
                            arucoNotFoundTag = true;
                        }
                        endTime = System.currentTimeMillis();
                        //记录第一次识别不到二维码的时间,如果小于20s,拉高或拉低复降,否则降落至备降点
                        if (endTime - startTime > 1000 && endTime - startTime <= 12000) {
                            if (Movement.getInstance().getFlyingHeight() <= 7) {
                                //可能由于appCrash后，识别不到二维码，尝试将飞机拉高识别
                                setDetectedBigMarkers();
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, 0.3);
                                if (dropTimes > Integer.parseInt(AMSConfig.getInstance().getAlternateLandingTimes())) {
                                    LogUtil.log(TAG, "超过复降限制,去备降点");
                                    AlternateLandingManager.getInstance().startTaskProcess();
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
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -0.3);
                            }
                        } else if (endTime - startTime > 12000) {
                            if (!triggerToAlternateLandingPoint) {
                                triggerToAlternateLandingPoint = true;
                                LogUtil.log(TAG, "判定未识别到二维码,飞往备降点");
                                AlternateLandingManager.getInstance().startTaskProcess();
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
                    isStartAruco = false;
                } catch (Exception e) {
                    isStartAruco = false;
                    mFindArucoList.clear();
                    mArucoCornerList.clear();
                }
            }
        });
    }


    public void setDetectedBigMarkers() {
        detectedBigMarkerId = 0;
        detectedSmallMarkerId = 0;
        detectedMediumMarkers = false;
        detectedSmallMarkers = false;
    }

    //根据识别到的二维码移动无人机
    private void moveOnArucoDetected(List<ArucoMarker> arucoMarkers, int imageWidth, int imageHeight) {

        //计算标记中心
//        double centerX = 0, centerY = 0;
//        for (int i = 0; i < arucoMarkers.size(); i++) {
//            centerX = centerX + Core.mean(arucoMarkers.get(i).getConner()).val[0] - (imageWidth / 2f);
//            centerY = centerY + Core.mean(arucoMarkers.get(i).getConner()).val[1] - (imageHeight / 2f);
//        }
        double centerX = Core.mean(arucoMarkers.get(0).getConner()).val[0] - (imageWidth / 2f);
        double centerY =  Core.mean(arucoMarkers.get(0).getConner()).val[1] - (imageHeight / 2f);
        //计算相对于图像中心的图像矢量
//        Scalar imageVector = new Scalar(centerX / arucoMarkers.size(), centerY / arucoMarkers.size());
        Scalar imageVector = new Scalar(centerX, centerY);

        double outX;
        double outY;
        double outZ;
        //御三T机型因为脚架较低，容易受机库磁场影响导致起飞时偏航角记录不正确，
        //需要进行姿态预估旋转机身,这里判断只有在识别到大二维码时才会进行姿态预估
        if ((arucoMarkers.size() == 1) && (
                arucoMarkers.get(0).getId() == 1
                        || arucoMarkers.get(0).getId() == 2
                        || arucoMarkers.get(0).getId() == 3
                        || arucoMarkers.get(0).getId() == 4
                        || arucoMarkers.get(0).getId() == 5)
                && productType.equals(DJI_MAVIC_3_ENTERPRISE_SERIES.name())) {
            //相机内参
            Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F);
            cameraMatrix.put(0, 0, 1035.501071149292);
            cameraMatrix.put(1, 1, 1035.4725889980984);
            cameraMatrix.put(0, 2, 713.2867513159875);
            cameraMatrix.put(1, 2, 542.4491896129153);
            cameraMatrix.put(2, 2, 1.0);
            //相机畸变
            Mat distCoeffs = Mat.zeros(5, 1, CvType.CV_64FC1);
            distCoeffs.put(0, 0, 0.3519238102526651);
            distCoeffs.put(1, 0, -1.4538841685400365);
            distCoeffs.put(2, 0, 0.00022919790876455443);
            distCoeffs.put(3, 0, 0.0012223205821680879);
            distCoeffs.put(4, 0, 2.0070528327672754);
            //旋转矩阵
            Mat rvecs = new Mat();
            //位移矩阵
            Mat tvecs = new Mat();
            //姿态预估
            List<Mat> conners = new ArrayList<>();
            conners.add(arucoMarkers.get(0).getConner());
            Aruco.estimatePoseSingleMarkers(conners, 0.15f, cameraMatrix, distCoeffs, rvecs, tvecs);
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
            Double yawCamera = MathUtils.toDegree(eulerAngles.get(2));
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
        if (resultYaw != 0.0) {
            outX = 0.0f;
            outY = 0.0f;
            outZ = 0.0f;
        } else {
            Log.e(TAG, "识别到的二维码:" + arucoMarkers.get(0).getId() + "--偏移量:x=" + imageVector.val[0] + "y=" + imageVector.val[1]);
            outX = imageVector.val[0] < 0 ? -updateOutXYSpeed(Math.abs(imageVector.val[0]))
                    : updateOutXYSpeed(Math.abs(imageVector.val[0]));
            outY = imageVector.val[1] < 0 ? updateOutXYSpeed(Math.abs(imageVector.val[1]))
                    : -updateOutXYSpeed(Math.abs(imageVector.val[1]));
            outZ = (Math.abs(imageVector.val[0]) <(Movement.getInstance().getFlyingHeight()>1?260:150))
                    && (Math.abs(imageVector.val[1]) <(Movement.getInstance().getFlyingHeight()>1?260:150))
                    ? updateOutDownSpeed() : 0f;
        }

        DroneHelper.getInstance().moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);
//        if (Math.abs(imageVector.val[0]) <= 80
//                && Math.abs(imageVector.val[1]) <= 80
//                && (arucoMarkers.size() > 1 || arucoMarkers.get(0).getId() == 19)) {

            if (Math.abs(imageVector.val[0]) <= 80
                    && Math.abs(imageVector.val[1]) <= 80) {
            canLanding = true;
            Log.e(TAG, "可以降落");
        } else {
            canLanding = false;
        }
    }

    private boolean canLanding;

    public boolean isCanLanding() {
        return canLanding;
    }

    public void setCanLanding(boolean canLanding) {
        this.canLanding = canLanding;
    }


    //根据偏移量和高度决定X/Y轴移动速度
    private double updateOutXYSpeed(Double d) {
        double ultrasonicHeight = Movement.getInstance().getFlyingHeight();
        if (d > 500) {
            if (ultrasonicHeight > 6) {
                return 0.275f;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.255f;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.235f;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.215f;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.195f;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.175f;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.145f;
            } else {
                return 0.135f;
            }
        } else if (d <= 500 && d > 400) {
            if (ultrasonicHeight > 6) {
                return 0.275f;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.255f;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.235f;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.215f;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.195f;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.175f;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.135f;
            } else {
                return 0.125f;
            }
        } else if (d <= 400 && d > 300) {
            if (ultrasonicHeight > 6) {
                return 0.275f;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.255f;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.235f;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.215f;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.195f;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.175f;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.135f;
            } else {
                return 0.125f;
            }
        } else if (d <= 300 && d > 200) {
            if (ultrasonicHeight > 6) {
                return 0.265f;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.215f;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.195f;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.185f;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.175f;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.165f;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.135f;
            } else {
                return 0.125f;
            }
        } else if (d <= 200 && d > 150) {
            if (ultrasonicHeight > 6) {
                return 0.255f;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.245f;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.235f;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.195f;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.175f;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.165f;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.125f;
            } else {
                return 0.125f;
            }
        } else if (d <= 150 && d > 79) {
            if (ultrasonicHeight > 6) {
                return 0.195f;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.195f;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.195f;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.185f;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.175f;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.165f;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.125f;
            } else {
                return 0.125f;
            }
        } else {
            return 0.0f;
        }
    }

    //根据不同高度决定下降多快
    private double updateOutDownSpeed() {
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        if (flyingHeight > 5) {
            return -0.475f;
        } else if (flyingHeight <= 5 && flyingHeight > 3.5) {
            return -0.455f;
        } else if (flyingHeight <= 3.5 && flyingHeight > 2.5) {
            return -0.435f;
        } else if (flyingHeight <= 2.5 && flyingHeight > 2.0) {
            return -0.415f;
        } else if (flyingHeight <= 2.0 && flyingHeight > 1.5) {
            return -0.275f;
        } else if (flyingHeight <= 1.5 && flyingHeight > 1.0) {
            return -0.235f;
        } else if (flyingHeight <= 1.0&&flyingHeight>=0.1) {
            return -0.175f;
        } else {
            return 0.0f;
        }
    }
}
