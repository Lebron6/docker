package com.aros.apron.tools;

import android.util.Log;

import com.aros.apron.entity.ArucoMarker;
import com.aros.apron.entity.Movement;

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
                                if (idArray[i] == 7) {
                                    mFindArucoList.add(new ArucoMarker(idArray[i], mArucoCornerList.get(i), 0.8f));
                                    break;
                                }
                            }
                        }
                        moveOnArucoDetected(mFindArucoList, rgbMat.width(), rgbMat.height());
                    } else {
                        if (!arucoNotFoundTag) {
                            startTime = System.currentTimeMillis();
                            arucoNotFoundTag = true;
                        }
                        endTime = System.currentTimeMillis();
                        //识别不到二维码的时间,如果大于5s,直接降落
                        if (endTime - startTime > 5000) {
                            Log.e(TAG, "识别不到的时间:" + (endTime - startTime));
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

        double centerX = Core.mean(arucoMarkers.get(0).getConner()).val[0] - (imageWidth / 2f);
        double centerY = Core.mean(arucoMarkers.get(0).getConner()).val[1] - (imageHeight / 2f);
        //计算相对于图像中心的图像矢量
        Scalar imageVector = new Scalar(centerX, centerY);
        double outX;
        double outY;
        double outZ;

        if ((arucoMarkers.size() == 1) &&
                arucoMarkers.get(0).getId() == 7
        ) {
            //相机内参
            Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F);
            cameraMatrix.put(0, 0, 646.1198423802089);
            cameraMatrix.put(1, 1, 649.420087990641);
            cameraMatrix.put(0, 2, 483.1565243887304);
            cameraMatrix.put(1, 2, 381.43615954610954);
            cameraMatrix.put(2, 2, 1.0);
            //相机畸变
            Mat distCoeffs = Mat.zeros(5, 1, CvType.CV_64FC1);
            distCoeffs.put(0, 0, -0.03573595371849505);
            distCoeffs.put(1, 0, -0.09935474011298286);
            distCoeffs.put(2, 0, 0.006489622318657623);
            distCoeffs.put(3, 0, 0.0008973587999385083);
            distCoeffs.put(4, 0, 0.0861065987784395);
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
            outX = imageVector.val[0] < 0 ? -(Math.abs(imageVector.val[0] / 1025))
                    : Math.abs(imageVector.val[0] / 1025);
            outY = imageVector.val[1] < 0 ? (Math.abs(imageVector.val[1] / 1025))
                    : -(Math.abs(imageVector.val[1] / 1025));
            outZ = 0.3;
        }
        DroneHelper.getInstance().moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);

        if (Movement.getInstance().getFlyingHeight()<=3) {
            canLanding = true;
        } else {
            canLanding = false;
        }
    }

    private boolean canLanding;

    public boolean isCanLanding() {
        return canLanding;
    }

}
