//package com.aros.apron.view;
//
//import static org.opencv.core.Core.BORDER_DEFAULT;
//import static java.lang.Math.abs;
//
//import android.content.Context;
//import android.util.Log;
//
//import com.aros.apron.tools.DroneHelper;
//
//import org.opencv.aruco.Aruco;
//import org.opencv.aruco.Dictionary;
//import org.opencv.core.Core;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfDouble;
//import org.opencv.core.MatOfInt;
//import org.opencv.core.MatOfPoint3f;
//import org.opencv.core.MatOfRect;
//import org.opencv.core.Point;
//import org.opencv.core.Rect;
//import org.opencv.core.Scalar;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.objdetect.CascadeClassifier;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class OpenCVHelper {
//    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
//    private Context context;
//    private MatOfPoint3f objPoints;
//    private Mat intrinsic;
//    private MatOfDouble distortion;
//    private Mat logoImg;
//    private MatOfPoint3f objectPoints;
//
//    public OpenCVHelper(Context context) {
//        this.context = context;
//    }
//
//    public Mat defaultImageProcessing(Mat input) {
//        Imgproc.putText(input, "Default", new Point(150, 40), 1, 4, new Scalar(255, 255, 255), 2, 8, false);
//        return input;
//    }
//
//    public Mat convertToGray(Mat input) {
//        Mat output = new Mat();
//        Imgproc.cvtColor(input, output, Imgproc.COLOR_RGBA2GRAY);
//        return output;
//    }
//
//    public Mat detectEdgesUsingCanny(Mat input) {
//        Mat output = new Mat();
//        Imgproc.Canny(input, output, 80, 100);
//        return output;
//    }
//
//    public Mat detectEdgesUsingLaplacian(Mat input) {
//        Mat grayImg = new Mat();
//        Mat intermediateMat = new Mat();
//        Mat output = new Mat();
//        Imgproc.cvtColor(input, grayImg, Imgproc.COLOR_RGBA2GRAY);
//        Imgproc.GaussianBlur(grayImg, grayImg, new Size(3, 3), 0, 0);
//        Imgproc.Laplacian(grayImg, intermediateMat, CvType.CV_8U, 3, 1, 0, BORDER_DEFAULT);
//        Core.convertScaleAbs(intermediateMat, intermediateMat, 10, 0);
//        Imgproc.cvtColor(intermediateMat, output, Imgproc.COLOR_GRAY2RGBA, 4);
//        grayImg.release();
//        intermediateMat.release();
//        return output;
//    }
//
//    public Mat blurImage(Mat input) {
//        Mat grayMat = new Mat();
//        Mat output = new Mat();
//        Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_RGBA2GRAY);
//        Imgproc.GaussianBlur(grayMat, output, new Size(35, 35), 0, 0);
//        grayMat.release();
//        return output;
//    }
//
//    public Mat detectFaces(Mat input, CascadeClassifier faceDetector) {
//        Mat grayImgMat = new Mat();
//        MatOfRect faces = new MatOfRect();
//        Mat output;
//        Imgproc.cvtColor(input, grayImgMat, Imgproc.COLOR_RGBA2GRAY);
//        if (faceDetector != null) {
//            faceDetector.detectMultiScale(grayImgMat, faces, 1.1, 2, 2, new Size(60, 60), new Size());
//        }
//        output = input;
//        Rect[] facesArray = faces.toArray();
//        for (Rect rect : facesArray) {
//            Imgproc.rectangle(output, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
//        }
//        return output;
//    }
//
//    boolean isDetectAruco;
//    public ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
//    private List<Mat> corners = new ArrayList<>();
//
//    public void detectArucoTags(int height, int width, byte[] data, Dictionary dictionary, DroneHelper droneHelper) {
//
//        if (isDetectAruco == true) {
//            return;
//        }
//        isDetectAruco = true;
//        mThreadPool.execute(new Runnable() {
//            @Override
//            public void run() {
//                Mat yuvMat = new Mat(height, width, CvType.CV_8UC1);
//                yuvMat.put(0, 0, data);
//
//                Mat rgbMat = new Mat();
//                Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2BGR_I420);
//
//                //灰度
//                Mat grayImgMat = new Mat();
//                Imgproc.cvtColor(rgbMat, grayImgMat, Imgproc.COLOR_RGBA2GRAY);
//
//                MatOfInt ids = new MatOfInt();
//                corners.clear();
//
//                Aruco.detectMarkers(grayImgMat, dictionary, corners, ids);
//                if (ids.empty() || corners.isEmpty()) {
//                    isDetectAruco = false;
//                    grayImgMat.release();
//                    yuvMat.release();
//                    rgbMat.release();
//                    ids.release();
//                    return;
////                    moveOnArucoDetected(ids, corners, droneHelper, 1920, 1080);
//
////            moveOnArucoDetected(ids, corners, droneHelper, rgbMat.width(), rgbMat.height());
//                }
//                //相机内参
//                Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F);
//                cameraMatrix.put(0, 0, 1035.501071149292);//fx
//                cameraMatrix.put(1, 1, 1035.4725889980984);//fy
//                cameraMatrix.put(0, 2, 713.2867513159875);//cx
//                cameraMatrix.put(1, 2, 542.4491896129153);//cy
//                cameraMatrix.put(2, 2, 1.0);
//
//                //相机畸变
//                Mat distCoeffs = Mat.zeros(3, 3, CvType.CV_64FC1);
//                cameraMatrix.put(0, 0, 0.3519238102526651);//k1
//                cameraMatrix.put(1, 0, -1.4538841685400365);   //k2
//                cameraMatrix.put(2, 0, 0.00022919790876455443);   //p1
//                cameraMatrix.put(3, 0, 0.0012223205821680879);  //p2
//                cameraMatrix.put(4, 2, 2.0070528327672754); //k3
//
//
//                //旋转矩阵
//                Mat rvecs = new Mat();
//                //位移矩阵
//                Mat tvecs = new Mat();
//
//                //aruco二维码尺寸(单位m), 请修改成正确的大小
//                float arucoMarkerSize = 0.1f;
//
//                //姿态预估
//                Aruco.estimatePoseSingleMarkers(
//                        corners,
//                        arucoMarkerSize,
//                        cameraMatrix,
//                        distCoeffs,
//                        rvecs,
//                        tvecs
//                );
//                //计算平均偏移距离。假设四个Aruco码分别位于降落平台的四个角，计算它们的平均位置，然后计算无人机相对于降落平台中心的偏移距离。
//                double centerX = (corners.get(0).get(0, 0)[0] + corners.get(1).get(0, 0)[0] + corners.get(2).get(0, 0)[0] + corners.get(3).get(0, 0)[0]) / 4;
//                double centerY = (corners.get(0).get(0, 1)[0] + corners.get(1).get(0, 1)[0] + corners.get(2).get(0, 1)[0] + corners.get(3).get(0, 1)[0]) / 4;
//
//                // X 方向偏移
//                Double droneOffsetX = centerX - grayImgMat.cols() / 2;
//                // Y 方向偏移
//                Double droneOffsetY = centerY - grayImgMat.rows() / 2;
//
//                //如果相对于降落平台中心的偏移的大小达到合适的大小，直接降落，如下是例子
//                if (abs(droneOffsetX) <= 0.08 && abs(droneOffsetY) <= 0.08) {
//                    //landing()
//                }
//                Log.e("偏移量", droneOffsetX + "-----" + droneOffsetY);
//                grayImgMat.release();
//                yuvMat.release();
//                rgbMat.release();
//                ids.release();
//                rvecs.release();
//                tvecs.release();
//                corners.clear();
//                isDetectAruco = false;
//            }
//        });
//
//
//    }
//
//
//    private void moveOnArucoDetected(Mat ids,
//                                     List<Mat> corners,
//                                     DroneHelper droneHelper,
//                                     int imageWidth,
//                                     int imageHeight) {
//        ////执行逻辑来决定将无人机移动到哪里
//        ////计算标记中心
//        Scalar markerCenter = new Scalar(0, 0);
//        for (Mat corner : corners) {
//            markerCenter = Core.mean(corner);
//        }
////
//        //使所需的标签位于图像框的中心
//        //计算相对于图像中心的图像矢量
//        Scalar imageVector = new Scalar(markerCenter.val[0] - imageWidth / 2f, markerCenter.val[1] - imageHeight / 2f);
//        //将矢量从图像坐标转换为无人机导航坐标
//        Scalar motionVector = convertImageVectorToMotionVector(imageVector);
//        //如果没有检测到标签，则无需移动
//        if (ids.size().empty()) {
//            motionVector = new Scalar(0, 0);
//        }
//        Log.e("偏移量", "0=" + imageVector.val[0] + "----" + "1=" + imageVector.val[1]);
//        if (imageVector.val[0] < 100 && imageVector.val[0] > -100 && imageVector.val[1] < 100 && imageVector.val[1] > -100) {
//            droneHelper.moveVxVyYawrateHeight(0f, 0f, 0f, -0.25f);
//        } else {
//            droneHelper.moveVxVyYawrateHeight(imageVector.val[0] < 0 ? -abs(imageVector.val[0] / 2000) : abs(imageVector.val[0] / 2000), imageVector.val[1] < 0 ? abs(imageVector.val[1] / 2000) : -abs(imageVector.val[1] / 2000), 0f, 0f);
//        }
//    }
//
////    public void doDroneMoveUsingImage(Mat input, DroneHelper droneHelper) {
////        /*
////         * Remember this function is called every time
////         * a frame is available. So don't do long loop here.
////         */
////        Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2YUV);
////        extractChannel(input, input, 0);
////        FlightControlData controlData =
////                new FlightControlData(0.1f, 0.0f, 0.0f, 0.0f); //pitch, roll, yaw, verticalThrottle
////        droneHelper.sendMovementCommand(controlData);
////    }
//
//    public Mat doAROnImage(Mat input, Dictionary dictionary, DroneHelper droneHelper) {
//        //TODO:
//        // Since this is the bonus part, only high-level instructions will be provided
//        // One way you can do this is to:
//        // 1. Identify the Aruco tags with corner pixel location
//        //    Hint:Aruco.detectMarkers(...)
//        // 2. For each corner in 3D space, define their 3D locations
//        //    The 3D locations you defined here will determine the origin of your coordinate frame
//        // 3. Given the 3D locations you defined, their 2D pixel location in the image, and camera parameters
//        //    You can calculate the 6 DOF of the camera relative to the tag coordinate frame
//        //    Hint: Calib3d.solvePnP(...)
//        // 4. To put artificial object in the image, you need to create 3D points first and project them into 2D image
//        //    With the projected image points, you can draw lines or polygon
//        //    Hint: Calib3d.projectPoints(...);
//        // 5. To put dji image on a certain location,
//        //    you need find the homography between the projected 4 corners and the 4 corners of the logo image
//        //    Hint: Calib3d.findHomography(...);
//        // 6. Once the homography is found, warp the image with perspective
//        //    Hint: Imgproc.warpPerspective(...);
//        // 7. Now you have the warped logo image in the right location, just overlay them on top of the camera image
//        Mat output = new Mat();
//        Mat grayMat = convertToGray(input);
//
//        //TODO: Do your magic!!!
//        // Hint how to overlay warped logo onto the original camera image
//        /*
//            Imgproc.cvtColor(logoWarped, grayMat, Imgproc.COLOR_BGR2GRAY);
//            Imgproc.threshold(grayMat, grayMat, 0, 255, Imgproc.THRESH_BINARY);
//            bitwise_not(grayMat, grayInv);
//            input.copyTo(src1Final, grayInv);
//            logoWarped.copyTo(src2Final, grayMat);
//            Core.add(src1Final, src2Final, output);
//         */
//        //end magic
//
//        if (output.empty()) {
//            output = grayMat;
//        }
//
//        return output;
//    }
//
//
////    public void startDoAR(DroneHelper droneHelper) {
////        droneHelper.enterVirtualStickMode();
////        droneHelper.setVerticalModeToAbsoluteHeight();
////        Bitmap bMap = BitmapFactory.decodeResource(context.getResources(), R.drawable.dji_logo);
////        logoImg = new Mat();
////        Utils.bitmapToMat(bMap, logoImg);
////
////        //Camera calibration code
////        intrinsic = new Mat(3, 3, CvType.CV_32F);
////        intrinsic.put(0,
////                0,
////                1.2702029303551683e+03,
////                0.,
////                7.0369652952332717e+02,
////                0.,
////                1.2682183239938338e+03,
////                3.1342369745005681e+02,
////                0.,
////                0.,
////                1.);
////
////        distortion = new MatOfDouble(3.2177759275048554e-02,
////                1.1688831035623757e+00,
////                -1.6742357543049650e-02,
////                1.4173384809091350e-02,
////                -6.1914718831876847e+00);
////
////        // Please measure the marker size in Meter and enter it here
////        double markerSizeMeters = 0.13;
////        double halfMarkerSize = markerSizeMeters * 0.5;
////
////        // Self-defined tag location in 3D, this is used in step 2 in doAR
////        objPoints = new MatOfPoint3f();
////        List<Point3> point3List = new ArrayList<>();
////        point3List.add(new Point3(-halfMarkerSize, -halfMarkerSize, 0));
////        point3List.add(new Point3(-halfMarkerSize, halfMarkerSize, 0));
////        point3List.add(new Point3(halfMarkerSize, halfMarkerSize, 0));
////        point3List.add(new Point3(halfMarkerSize, -halfMarkerSize, 0));
////        objPoints.fromList(point3List);
////
////        // AR object points in 3D, this is used in step 4 in doAR
////        objectPoints = new MatOfPoint3f();
////
////        List<Point3> point3DList = new ArrayList<>();
////        point3DList.add(new Point3(-halfMarkerSize, -halfMarkerSize, 0));
////        point3DList.add(new Point3(-halfMarkerSize, halfMarkerSize, 0));
////        point3DList.add(new Point3(halfMarkerSize, halfMarkerSize, 0));
////        point3DList.add(new Point3(halfMarkerSize, -halfMarkerSize, 0));
////        point3DList.add(new Point3(-halfMarkerSize, -halfMarkerSize, markerSizeMeters));
////        point3DList.add(new Point3(-halfMarkerSize, halfMarkerSize, markerSizeMeters));
////        point3DList.add(new Point3(halfMarkerSize, halfMarkerSize, markerSizeMeters));
////        point3DList.add(new Point3(halfMarkerSize, -halfMarkerSize, markerSizeMeters));
////
////        objectPoints.fromList(point3DList);
////    }
//
//    private Scalar convertImageVectorToMotionVector(Scalar imageVector) {
//        double pX = -imageVector.val[1];
//        double pY = imageVector.val[0];
//        double divisor = Math.sqrt((pX * pX) + (pY * pY));
//        pX = pX / divisor;
//        pY = pY / divisor;
//
//        return new Scalar(pX * 0.2, pY * 0.2);
//    }
//
//
//}
