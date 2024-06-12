package com.aros.apron.tools;

import static dji.sdk.keyvalue.value.product.ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES;

import android.util.Log;

import com.aros.apron.constant.AMSConfig;
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

public class OpenCVHelper {


    private boolean isStartAruco;
    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    private String TAG = getClass().getSimpleName();
    Double resultYaw = 0.0;
    private String productType;

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    private OpenCVHelper() {
    }

    private static class OpenCVHelperHolder {
        private static final OpenCVHelper INSTANCE = new OpenCVHelper();
    }

    public static OpenCVHelper getInstance() {
        return OpenCVHelper.OpenCVHelperHolder.INSTANCE;
    }

    //没识别到二维码时,此值为true
    private boolean arucoNotFoundTag;
    //触发去备降点
    private boolean triggerToAlternateLandingPoint;
    long startTime;
    long endTime;

    public void detectArucoTags(int height, int width, byte[] data, Dictionary dictionary, DroneHelper droneHelper) {
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
                    List<Mat> corners = new ArrayList<>();
                    Aruco.detectMarkers(grayImgMat, dictionary, corners, ids);

                    if (ids.depth() > 0) {
                        Log.e(TAG, "识别到的二维码:" + ids.toArray().length);
                        arucoNotFoundTag = false;
                        //御三T才需要位姿检测
                        if (ids.toArray()[0] != 19 && productType.equals(DJI_MAVIC_3_ENTERPRISE_SERIES.name())) {
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
                            Aruco.estimatePoseSingleMarkers(corners, 0.15f, cameraMatrix, distCoeffs, rvecs, tvecs);
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
                            Double yawCamera = MathUtils.toDegree(eulerAngles.get(2));
                            if (yawCamera < 0) {
                                if (yawCamera < -10) {
                                    resultYaw = -30.0;
                                } else {
                                    resultYaw = 0.0;
                                }
                            } else {
                                if (yawCamera > 10) {
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
                        moveOnArucoDetected(ids.toArray(), corners, droneHelper, rgbMat.width(), rgbMat.height());

                    } else {
                        if (!arucoNotFoundTag) {
                            startTime = System.currentTimeMillis();
                            arucoNotFoundTag = true;
                        }
                        endTime = System.currentTimeMillis();
                        //记录第一次识别不到二维码的时间,如果小于20s,拉高或拉低复降,否则降落至备降点
                        if (endTime - startTime > 1000) {
                            if (Movement.getInstance().getFlyingHeight() <= 6) {
                                //可能由于appCrash后，识别不到二维码，尝试将飞机拉高识别
                                droneHelper.moveVxVyYawrateHeight(0f, 0f, 0f, 0.3);
//                                LogUtil.log(TAG, "up");
                            } else if (Movement.getInstance().getFlyingHeight() > 6) {
                                //可能是由于飞机太高，识别不到二维码，尝试将飞机拉低识别
//                                LogUtil.log(TAG, "down");
                                droneHelper.moveVxVyYawrateHeight(0f, 0f, 0f, -0.3);
                            }
                        } else if (endTime - startTime > 12000) {
                            if (!triggerToAlternateLandingPoint) {
                                triggerToAlternateLandingPoint = true;
                                LogUtil.log(TAG, "去备降点");
                                AlternateLandingManager.getInstance().startTaskProcess();
                            }
                        }
                    }
                    ids.release();
                    yuvMat.release();
                    grayImgMat.release();
                    rgbMat.release();
                    grayImgMat.release();
                    isStartAruco = false;
                } catch (Exception e) {
                    isStartAruco = false;
                }
            }
        });
    }


    private boolean detectedBigMarkers = true;

    public boolean isDetectedBigMarkers() {
        return detectedBigMarkers;
    }

    public void setDetectedBigMarkers(boolean detectedBigMarkers) {
        this.detectedBigMarkers = detectedBigMarkers;
    }

    private void moveOnArucoDetected(int[] ids, List<Mat> corners, DroneHelper droneHelper, int imageWidth, int imageHeight) {
        int findAruco = corners.size();
        ////计算标记中心
        double centerX = 0, centerY = 0;
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        for (int i = 0; i < corners.size(); i++) {
            //识别到小码并且高度较低才会停止识别大二维码
            if (ids[i] == 19 && flyingHeight < 3) {
                detectedBigMarkers = false;
                findAruco = 1;
                centerX = Core.mean(corners.get(i)).val[0] - (imageWidth / 2f);
                centerY = Core.mean(corners.get(i)).val[1] - (imageHeight / 2f);
                resultYaw = 0.0;
            } else {
                if (detectedBigMarkers == true) {
                    if (ids[i] == 5 || ids[i] == 6) {
                        findAruco--;
                    } else {
                        centerX = centerX + Core.mean(corners.get(i)).val[0] - (imageWidth / 2f);
                        centerY = centerY + Core.mean(corners.get(i)).val[1] - (imageHeight / 2f);
                    }
                }
            }
        }
        //使所需的标签位于图像框的中心
        //计算相对于图像中心的图像矢量
        Scalar imageVector = new Scalar(centerX / findAruco, centerY / findAruco);
//        Log.e(TAG, "偏移量：" + imageVector.val[0] + "," + imageVector.val[1]);
        double outX;
        double outY;
        double outZ;
        //先旋转,再平移或降落
        if (resultYaw != 0.0) {
            outX = 0.0;
            outY = 0.0;
            outZ = 0.0;
        } else if (((ids.length == 1 && ids[0] == 5) || (ids.length == 1 && ids[0] == 6))
                && flyingHeight < 3) {//如果镜头里只有一个5/6小码，拉高去识别19号大码
            outX = 0.0;
            outY = 0.0;
            outZ = 0.3;
        } else {
            outX = imageVector.val[0] < 0 ? -updateOutXYSpeed(Math.abs(imageVector.val[0]))
                    : updateOutXYSpeed(Math.abs(imageVector.val[0]));
            outY = imageVector.val[1] < 0 ? updateOutXYSpeed(Math.abs(imageVector.val[1]))
                    : -updateOutXYSpeed(Math.abs(imageVector.val[1]));
            outZ = (Math.abs(imageVector.val[0]) < (Movement.getInstance().getFlyingHeight() > 1 ? 200 : 100)
                    && (Math.abs(imageVector.val[1]) < (Movement.getInstance().getFlyingHeight() > 1 ? 200 : 100))
            )? updateOutDownSpeed() : 0f;
//            outZ = (Math.abs(imageVector.val[0]) < 200)
//                    && (Math.abs(imageVector.val[1]) <  200)
//                    ? updateOutDownSpeed() : 0f;
        }

        droneHelper.moveVxVyYawrateHeight(outX,
                outY,
                resultYaw, outZ);
        if (Math.abs(imageVector.val[0]) <= 80 && Math.abs(imageVector.val[1]) <= 80 && ids.length == 1 && ids[0] == 19) {
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
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        if (d > 500) {
            if (flyingHeight > 6) {
                return 0.235;
            } else if (flyingHeight > 5 && flyingHeight <= 6) {
                return 0.215;
            } else if (flyingHeight > 4 && flyingHeight <= 5) {
                return 0.195;
            } else if (flyingHeight > 3 && flyingHeight <= 4) {
                return 0.165;
            } else if (flyingHeight > 2 && flyingHeight <= 3) {
                return 0.145;
            } else if (flyingHeight > 1 && flyingHeight <= 2) {
                return 0.145;
            } else if (flyingHeight > 0.1 && flyingHeight <= 1) {
                return 0.125;
            } else {
                return 0.095;
            }
        } else if (d <= 500 && d > 400) {
            if (flyingHeight > 6) {
                return 0.215;
            } else if (flyingHeight > 5 && flyingHeight <= 6) {
                return 0.195;
            } else if (flyingHeight > 4 && flyingHeight <= 5) {
                return 0.175;
            } else if (flyingHeight > 3 && flyingHeight <= 4) {
                return 0.165;
            } else if (flyingHeight > 2 && flyingHeight <= 3) {
                return 0.145;
            } else if (flyingHeight > 1 && flyingHeight <= 2) {
                return 0.145;
            } else if (flyingHeight > 0.1 && flyingHeight <= 1) {
                return 0.125;
            } else {
                return 0.095;
            }
        } else if (d <= 400 && d > 300) {
            if (flyingHeight > 6) {
                return 0.215;
            } else if (flyingHeight > 5 && flyingHeight <= 6) {
                return 0.195;
            } else if (flyingHeight > 4 && flyingHeight <= 5) {
                return 0.185;
            } else if (flyingHeight > 3 && flyingHeight <= 4) {
                return 0.165;
            } else if (flyingHeight > 2 && flyingHeight <= 3) {
                return 0.145;
            } else if (flyingHeight > 1 && flyingHeight <= 2) {
                return 0.135;
            } else if (flyingHeight > 0.1 && flyingHeight <= 1) {
                return 0.125;
            } else {
                return 0.085;
            }
        } else if (d <= 300 && d > 200) {
            if (flyingHeight > 6) {
                return 0.195;
            } else if (flyingHeight > 5 && flyingHeight <= 6) {
                return 0.195;
            } else if (flyingHeight > 4 && flyingHeight <= 5) {
                return 0.175;
            } else if (flyingHeight > 3 && flyingHeight <= 4) {
                return 0.145;
            } else if (flyingHeight > 2 && flyingHeight <= 3) {
                return 0.135;
            } else if (flyingHeight > 1 && flyingHeight <= 2) {
                return 0.125;
            } else if (flyingHeight > 0.1 && flyingHeight <= 1) {
                return 0.085;
            } else {
                return 0.065;
            }
        } else if (d <= 200 && d > 100) {
            if (flyingHeight > 6) {
                return 0.195;
            } else if (flyingHeight > 5 && flyingHeight <= 6) {
                return 0.175;
            } else if (flyingHeight > 4 && flyingHeight <= 5) {
                return 0.165;
            } else if (flyingHeight > 3 && flyingHeight <= 4) {
                return 0.155;
            } else if (flyingHeight > 2 && flyingHeight <= 3) {
                return 0.125;
            } else if (flyingHeight > 1 && flyingHeight <= 2) {
                return 0.125;
            } else if (flyingHeight > 0.1 && flyingHeight <= 1) {
                return 0.075;
            } else {
                return 0.065;
            }
        } else if (d <= 100 && d > 50) {
            if (flyingHeight > 6) {
                return 0.175;
            } else if (flyingHeight > 5 && flyingHeight <= 6) {
                return 0.175;
            } else if (flyingHeight > 4 && flyingHeight <= 5) {
                return 0.175;
            } else if (flyingHeight > 3 && flyingHeight <= 4) {
                return 0.145;
            } else if (flyingHeight > 2 && flyingHeight <= 3) {
                return 0.125;
            } else if (flyingHeight > 1 && flyingHeight <= 2) {
                return 0.105;
            } else if (flyingHeight > 0.1 && flyingHeight <= 1) {
                return 0.075;
            } else {
                return 0.065;
            }
        } else {
            return 0.0;
        }
    }

    //根据不同高度决定下降多快
    private double updateOutDownSpeed() {
        double flyingHeight = Movement.getInstance().getFlyingHeight();
        double ultrasonicHeight = Movement.getInstance().getUltrasonicHeight();
        if (flyingHeight > 8) {
            return -0.475;
        } else if (flyingHeight <= 8 && flyingHeight > 7) {
            return -0.405;
        } else if (flyingHeight <= 7 && flyingHeight > 6) {
            return -0.375;
        } else if (flyingHeight <= 6 && flyingHeight > 5) {
            return -0.335;
        } else if (flyingHeight <= 5 && flyingHeight > 4) {
            return -0.315;
        } else if (flyingHeight <= 4 && flyingHeight > 3) {
            return -0.225;
        } else if (flyingHeight <= 3 && flyingHeight > 2) {
            return -0.185;
        } else if (flyingHeight <= 2 && flyingHeight > 1.2) {
            return -0.165;
        } else if (flyingHeight <= 1.2&&flyingHeight> AMSConfig.getInstance().getDescentAltitude() - 0.2) {
            return -0.135;
        } else if (flyingHeight<= AMSConfig.getInstance().getDescentAltitude()-0.2){
            if (ultrasonicHeight> AMSConfig.getInstance().getDescentUltrasonicAltitude()){
                return -0.135;
            }else {
                return 0.0;
            }
        }else{
            return 0.0;
        }
    }

    private List<Double> rotationMatrixToEulerAngles(Mat R) {
        double sy = Math.sqrt(R.get(0, 0)[0] * R.get(0, 0)[0] + R.get(1, 0)[0] * R.get(1, 0)[0]);
        boolean singular = sy < 1e-6;

        List<Double> eulerAngles = new ArrayList<>();

        if (!singular) {
            double x = Math.atan2(R.get(2, 1)[0], R.get(2, 2)[0]);
            double y = Math.atan2(-R.get(2, 0)[0], sy);
            double z = Math.atan2(R.get(1, 0)[0], R.get(0, 0)[0]);
            eulerAngles.add(x);
            eulerAngles.add(y);
            eulerAngles.add(z);
        } else {
            double x = Math.atan2(-R.get(1, 2)[0], R.get(1, 1)[0]);
            double y = Math.atan2(-R.get(2, 0)[0], sy);
            double z = 0.0;
            eulerAngles.add(x);
            eulerAngles.add(y);
            eulerAngles.add(z);
        }

        return eulerAngles;
    }

    public boolean containsElement(int[] array, int target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) {
                return true;
            }
        }
        return false;
    }
}
