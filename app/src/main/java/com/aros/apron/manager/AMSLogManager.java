//package com.aros.apron.manager;
//
//import android.os.Build;
//import android.os.Environment;
//import android.text.TextUtils;
//
//import androidx.annotation.RequiresApi;
//
//import com.amazonaws.auth.AWSCredentials;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import com.aros.apron.base.BaseManager;
//import com.aros.apron.entity.FileUploadResult;
//import com.aros.apron.tools.LogUtil;
//
//import java.io.File;
//
//import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
//import io.reactivex.rxjava3.core.Observable;
//import io.reactivex.rxjava3.core.ObservableEmitter;
//import io.reactivex.rxjava3.core.ObservableOnSubscribe;
//import io.reactivex.rxjava3.core.Observer;
//import io.reactivex.rxjava3.disposables.Disposable;
//import io.reactivex.rxjava3.schedulers.Schedulers;
//
//public class AMSLogManager extends BaseManager {
//
//    private AMSLogManager() {
//    }
//
//    private static class MediaManagerHolder {
//        private static final AMSLogManager INSTANCE = new AMSLogManager();
//    }
//
//    public static AMSLogManager getInstance() {
//        return MediaManagerHolder.INSTANCE;
//    }
//
//
//    private boolean isUploadingAMSLog;
//
//    public boolean isUploadingAMSLog() {
//        return isUploadingAMSLog;
//    }
//
//    public void setUploadingAMSLog(boolean uploadingAMSLog) {
//        isUploadingAMSLog = uploadingAMSLog;
//    }
//
//    private File[] files = new File[]{};
//
//    public void enableLogList(MQMessage message) {
//        downLoadMediaFileIndex=0;
//        setUploadingAMSLog(true);
//        File logDir = new File(getLogDir());
//        if (logDir != null) {
//            files = logDir.listFiles();
//            if (files == null || files.length == 0) {
//                sendMsg2Server(message, "日志文件夹暂无日志文件");
//                setUploadingAMSLog(false);
//
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    pullOriginalFile(message);
//                }
//            }
//            sendMsg2Server(message);
//
//        } else {
//            sendMsg2Server(message, "日志文件夹为空");
//            setUploadingAMSLog(false);
//
//        }
//
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    public void pullOriginalFile(MQMessage message) {
//
//        LogUtil.log(TAG, "File size: " + files.length);
//
//        File logFile = new File(getSDCardPath() + "/DJIDemo/cache/log/" + files[downLoadMediaFileIndex].getName());
//        minIOUpLoadAMSLog(message, logFile);
//    }
//
//    private int uploadFailTime = 0;
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    public void minIOUpLoadAMSLog(MQMessage message, File file) {
//        AmazonS3 s3 = new AmazonS3Client(new AWSCredentials() {
//            @Override
//            public String getAWSAccessKeyId() {
//                return message.getAccess_key();
//            }
//
//            @Override
//            public String getAWSSecretKey() {
//                return message.getSecret_key();
//            }
//        });
//        Observable.create(new ObservableOnSubscribe<String>() {
//                    @Override
//                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
//                        // 服务器地址
//                        s3.setEndpoint(message.getUpload_url()); // http://ip:端口号
//                        boolean bucketExists = s3.doesBucketExist(message.getBucketName());
//                        if (!bucketExists) {
//                            s3.createBucket(message.getBucketName());
//                        }
//                        // 上传文件到网关MINIO存储服务
//                        s3.putObject(
//                                new PutObjectRequest(
//                                        message.getBucketName(),
//                                        "/" + message.getObjectKey() + "/" + file.getName(),
//                                        file
//                                )
//                        );
//
//                        // 获取文件上传后访问地址url
//                        GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
//                                message.getBucketName(),
//                                "/" + message.getObjectKey() + "/"
//                                        + file.getName()
//                        );
//                        String url = s3.generatePresignedUrl(urlRequest).toString();
//
//                        // 文件上传后访问地址url
//                        emitter.onNext(url);
//                        emitter.onComplete();
//                    }
//                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Observer<String>() {
//                    @Override
//                    public void onSubscribe(Disposable d) {
//                        // Handle on subscribe (optional)
//                    }
//
//                    @Override
//                    public void onNext(String url) {
//                        FileUploadResult fileUploadResult = new FileUploadResult();
//                        fileUploadResult.setFileName(file.getName());
//                        fileUploadResult.setResult(1);
//                        fileUploadResult.setFileNum(files.length);
//                        fileUploadResult.setBuckName(message.getBucketName());
//                        fileUploadResult.setObjectKey(message.getObjectKey());
//                        fileUploadResult.setUrl(message.getUpload_url());
//                        fileUploadResult.setOffIndex(downLoadMediaFileIndex);
//                        if (files.length==(downLoadMediaFileIndex+1)){
//                            fileUploadResult.setMsg("所有日志已上传完毕");
//                        }else{
//                            fileUploadResult.setMsg("文件:"+file.getName()+"已上传");
//                        }
//                        fileUploadResult.setProgress(String.valueOf(calculatePercentage(downLoadMediaFileIndex+1,files.length)));
//                        sendFileUploadCallback(60202,fileUploadResult);
//                    }
//
//                    @RequiresApi(Build.VERSION_CODES.O)
//                    @Override
//                    public void onError(Throwable e) {
//                        LogUtil.log(TAG, "Error uploading file " + downLoadMediaFileIndex +file.getName()+ ": " + e.getMessage());
//
////                            File tempFile = new File(file.getParent(), "temp_" + file.getName());
////                            try {
////                                // 复制文件内容到临时文件
////                                Files.copy(file.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
////                                // 上传临时文件
////                                minIOUpLoadAMSLog(message,tempFile);
////                            } catch (Exception ex) {
////                                throw new RuntimeException(ex);
////                            }
//                            downLoadMediaFileIndex++;
//                            if (downLoadMediaFileIndex == files.length) {
//                                // 所有文件已经下载完成或失败
//                                downLoadMediaFileIndex = 0;
//                                setUploadingAMSLog(false);
//
//                            } else {
//                                pullOriginalFile(message);
//                            }
//
//                    }
//
//                    @RequiresApi(Build.VERSION_CODES.O)
//                    @Override
//                    public void onComplete() {
//                        LogUtil.log(TAG, "File " + downLoadMediaFileIndex +file.getName()+ " uploaded successfully.");
//                        sendMissionExecuteEvents("第" + downLoadMediaFileIndex + "个AMS日志已上传");
//
//                        downLoadMediaFileIndex++;
//                        if (downLoadMediaFileIndex == files.length) {
//                            // 所有文件已上传完成，清空SD卡，缓存，退出媒体模式，发送无人机关机
//                            sendMissionExecuteEvents("所有AMS日志已上传完毕");
//                            downLoadMediaFileIndex = 0;
//                            setUploadingAMSLog(false);
//
//                        } else {
//                            pullOriginalFile(message);
//                        }
//                    }
//                });
//    }
//
//    private int downLoadMediaFileIndex = 0;
//
//    public String getLogDir() {
//        return getSDCardPath() + "/DJIDemo/cache/log"; // 默认缓存路径
//    }
//
//    private String getSDCardPath() {
//        String sdCardPathString = "";
//        if (checkSDCard()) {
//            sdCardPathString = Environment.getExternalStorageDirectory()
//                    .getPath();
//        } else {
//            sdCardPathString = Environment.getExternalStorageDirectory()
//                    .getParentFile().getPath();
//        }
//
//        return sdCardPathString;
//    }
//
//    private boolean checkSDCard() {
//        return TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
//    }
//
//    public  int calculatePercentage(int a, int b) {
//        if (b == 0) {
//            return 0; // 避免除以0错误
//        }
//        return (a * 100) / b;
//    }
//
//}
