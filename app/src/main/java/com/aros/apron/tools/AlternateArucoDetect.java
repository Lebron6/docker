package com.aros.apron.tools;

import android.util.Log;

import com.aros.apron.entity.ArucoMarker;
import com.aros.apron.entity.Movement;
import com.aros.apron.manager.FlightManager;

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

public class AlternateArucoDetect {

    //没识别到二维码
    private boolean arucoNotFoundTag;

    private boolean isStartAruco;
    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    private String TAG = getClass().getSimpleName();
    Double resultYaw = 0.0;
    private List<ArucoMarker> mFindArucoList = new ArrayList<>();
    List<Mat> mArucoCornerList = new ArrayList<>();

    long startTime;
    long endTime;


    private AlternateArucoDetect() {
    }

    private static class OpenCVHelperHolder {
        private static final AlternateArucoDetect INSTANCE = new AlternateArucoDetect();
    }

    public static AlternateArucoDetect getInstance() {
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
                                if (idArray[i] == 20||idArray[i] == 21||idArray[i] == 22||idArray[i] == 23) {
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.37f));
                                }
                            }
                        }
                        moveOnArucoDetected(mFindArucoList, rgbMat.width(), rgbMat.height());
                    } else {
//                        if (!arucoNotFoundTag) {
//                            startTime = System.currentTimeMillis();
//                            arucoNotFoundTag = true;
//                        }
//                        endTime = System.currentTimeMillis();
//                        if (endTime - startTime > 1000 && endTime - startTime <= 10000) {
//                            if (Movement.getInstance().getFlyingHeight() > 2) {
                                DroneHelper.getInstance().moveVxVyYawrateHeight(0f, 0f, 0f, -0.6f);
//                            }
//                        }
                        //识别不到二维码的时间,如果大于6s,直接降落
                        if (Movement.getInstance().getFlyingHeight()<=2) {
                            canLanding=true;
//                            FlightManager.getInstance().stopArucoDetectAndLanding(3);
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


    //根据识别到的二维码移动无人机
    private void moveOnArucoDetected(List<ArucoMarker> arucoMarkers, int imageWidth, int imageHeight) {
        //计算标记中心
        double centerX = 0, centerY = 0;
        for (int i = 0; i < arucoMarkers.size(); i++) {
            centerX = centerX + Core.mean(arucoMarkers.get(i).getConner()).val[0] - (imageWidth / 2f);
            centerY = centerY + Core.mean(arucoMarkers.get(i).getConner()).val[1] - (imageHeight / 2f);
        }
        //计算相对于图像中心的图像矢量
        Scalar imageVector = new Scalar(centerX / arucoMarkers.size(), centerY / arucoMarkers.size());
        double outX;
        double outY;
        double outZ;

        if (
                arucoMarkers.get(0).getId() == 20||
                        arucoMarkers.get(0).getId() == 21||
                        arucoMarkers.get(0).getId() == 22||
                        arucoMarkers.get(0).getId() == 23&&Movement.getInstance().getFlyingHeight()>10
        ) {
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
            Aruco.estimatePoseSingleMarkers(conners, arucoMarkers.get(0).getSize(), cameraMatrix, distCoeffs, rvecs, tvecs);
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

            if (Math.abs(imageVector.val[0])<100){
                outX=0;
            }else{
                outX = imageVector.val[0] < 0 ? -(Math.abs(imageVector.val[0])/600)
                        : (Math.abs(imageVector.val[0])/600);
            }
            if (Math.abs(imageVector.val[1])<100) {
                outY=0;
            }else {
                outY = imageVector.val[1] < 0 ? (Math.abs(imageVector.val[1])/600)
                        : -(Math.abs(imageVector.val[1])/600);
            }
            outZ = -0.6f;
        }
        LogUtil.log(TAG, "Aruco:" + arucoMarkers.size()+ "  杆量x=" + outX + "  偏移:x=" + imageVector.val[0] + "    杆量y=" + outY + "  偏移:y=" + imageVector.val[1]);

        DroneHelper.getInstance().moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);

        if (Movement.getInstance().getFlyingHeight()<=2) {
            canLanding = true;
        } else {
            canLanding = false;
        }
    }

    public boolean canLanding;

    public boolean isCanLanding() {
        return canLanding;
    }

    //根据偏移量和高度决定X/Y轴移动速度
    private double updateOutXYSpeed(Double d) {
        double ultrasonicHeight = Movement.getInstance().getFlyingHeight();
        if (d > 500) {
            if (ultrasonicHeight > 6) {
                return 0.375;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.365;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.335;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.315;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.295;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.275;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.245;
            } else {
                return 0.235;
            }
        } else if (d <= 500 && d > 400) {
            if (ultrasonicHeight > 6) {
                return 0.375;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.355;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.335;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.315;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.295;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.275;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.235;
            } else {
                return 0.225;
            }
        } else if (d <= 400 && d > 300) {
            if (ultrasonicHeight > 6) {
                return 0.375;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.355;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.335;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.315;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.295;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.275;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.235;
            } else {
                return 0.225;
            }
        } else if (d <= 300 && d > 200) {
            if (ultrasonicHeight > 6) {
                return 0.365;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.315;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.295;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.285;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.275;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.265;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.235;
            } else {
                return 0.225;
            }
        } else if (d <= 200 && d > 150) {
            if (ultrasonicHeight > 6) {
                return 0.355;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.345;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.335;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.295;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.275;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.265;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.225;
            } else {
                return 0.225;
            }
        } else if (d <= 150 && d > 79) {
            if (ultrasonicHeight > 6) {
                return 0.295;
            } else if (ultrasonicHeight > 5 && ultrasonicHeight <= 6) {
                return 0.295;
            } else if (ultrasonicHeight > 4 && ultrasonicHeight <= 5) {
                return 0.295;
            } else if (ultrasonicHeight > 3 && ultrasonicHeight <= 4) {
                return 0.285;
            } else if (ultrasonicHeight > 2 && ultrasonicHeight <= 3) {
                return 0.275;
            } else if (ultrasonicHeight > 1 && ultrasonicHeight <= 2) {
                return 0.265;
            } else if (ultrasonicHeight > 0.1 && ultrasonicHeight <= 1) {
                return 0.225;
            } else {
                return 0.225;
            }
        } else {
            return 0.0;
        }
    }
    //根据不同高度决定下降多快
    private double updateOutDownSpeed() {
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        if (flyingHeight > 5) {
            return -0.575;
        } else if (flyingHeight <= 5 && flyingHeight > 3.5) {
            return -0.555;
        } else if (flyingHeight <= 3.5 && flyingHeight > 2.5) {
            return -0.535;
        } else if (flyingHeight <= 2.5 && flyingHeight > 2.0) {
            return -0.375;
        } else if (flyingHeight <= 2.0 && flyingHeight > 1.5) {
            return -0.235;
        } else if (flyingHeight <= 1.5 && flyingHeight > 1.0) {
            return -0.195;
        } else if (flyingHeight <= 1.0 && flyingHeight >= 0.1) {
            return -0.175;
        } else {
            return 0.0;
        }
    }
}
