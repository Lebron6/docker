package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.FileUploadResult;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.autonavi.base.amap.mapcore.FileUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.MediaFileType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.media.MediaFile;
import dji.v5.manager.datacenter.media.MediaFileDownloadListener;
import dji.v5.manager.datacenter.media.MediaFileListState;
import dji.v5.manager.datacenter.media.MediaFileListStateListener;
import dji.v5.manager.datacenter.media.PullMediaFileListParam;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MediaManager extends BaseManager {

    private  final String TAG = "MediaManager";
    private  final String mediaFileDir = "/apronPic";
    private MediaFileListState mState = null;
    private List<MediaFile> mediaFiles = new ArrayList<>();
    private  MqttAndroidClient mqttClient;

    private MediaManager() {
    }

    private static class MediaManagerHolder {
        private static final MediaManager INSTANCE = new MediaManager();
    }

    public static MediaManager getInstance() {
        return MediaManager.MediaManagerHolder.INSTANCE;
    }

    public  void init(MqttAndroidClient mqttAndroidClient) {
        mqttClient = mqttAndroidClient;
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            MediaDataCenter.getInstance().getMediaManager().addMediaFileListStateListener(new MediaFileListStateListener() {
                @Override
                public void onUpdate(MediaFileListState mediaFileListState) {
                    mState = mediaFileListState;
                    Log.e(TAG, "当前媒体文件状态：" + mediaFileListState.name());
                }
            });
        }
    }

    private int enterPlayBackFailTimes;
    private boolean isEnablePlayback;

    public void enablePlayback() {
       MediaDataCenter.getInstance().getMediaManager().enable(new CommonCallbacks.CompletionCallback() {
           @Override
           public void onSuccess() {
               LogUtil.log(TAG, "进入媒体模式成功");
               pullMediaFileListFromCamera();
               isEnablePlayback=true;
           }

           @Override
           public void onFailure(@NonNull IDJIError idjiError) {
               LogUtil.log(TAG, "第"+enterPlayBackFailTimes+"次进入媒体模式失败:"+new Gson().toJson(idjiError));

               if (!isEnablePlayback){
                   new Handler().postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           if (enterPlayBackFailTimes < 10) {
                               enterPlayBackFailTimes++;
                               enablePlayback();
                           }else{
                               DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                               sendMissionExecuteEvents(mqttClient, "媒体模式进入失败:关机");
                           }
                       }
                   }, 1500);
               }
           }
       });
    }

    private void pullMediaFileListFromCamera(){
        MediaDataCenter.getInstance().getMediaManager().pullMediaFileListFromCamera(new PullMediaFileListParam.Builder().count(-1).build(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mState == MediaFileListState.UP_TO_DATE) {
                                    mediaFiles =
                                            MediaDataCenter.getInstance().getMediaManager().getMediaFileListData().getData();
                                    if (mediaFiles != null&&mediaFiles.size()>0) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            pullOriginalMediaFileFromCamera();
                                        }
                                    } else {
                                        LogUtil.log(TAG, "拉取媒体文件为空");
                                        DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                                        sendMissionExecuteEvents(mqttClient,"拉取媒体文件为空");
                                        disablePlayback();
                                        LogUtil.log(TAG, "发送关闭无人机");
                                    }
                                } else {
                                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                                    sendMissionExecuteEvents(mqttClient,"拉取媒体文件失败,当前状态:"+mState);
                                    LogUtil.log(TAG, "拉取媒体文件失败,当前状态:"+mState);
                                    disablePlayback();
                                    LogUtil.log(TAG, "发送关闭无人机");
                                }
                            }
                        },1000);
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        LogUtil.log(TAG, "拉取媒体文件失败:" + new Gson().toJson(idjiError));
                        DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                        sendMissionExecuteEvents(mqttClient,"拉取媒体文件失败");
                        disablePlayback();
                        LogUtil.log(TAG, "发送关闭无人机");
                    }
                }

        );
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public void pullOriginalMediaFileFromCamera() {
        final MediaFile mediaFile = mediaFiles.get(downLoadMediaFileIndex);

        if ((!PreferenceUtils.getInstance().getNeedUpLoadVideo() && mediaFile.getFileType() == MediaFileType.MP4)
                || !mediaFile.getFileName().contains(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))) {

            downLoadMediaFileIndex++;

            if (downLoadMediaFileIndex == mediaFiles.size()) {
                // This refers to when all files have been downloaded or failed. Clear SD card, cache, exit media mode, and shut down the drone
                downLoadMediaFileIndex = 0;
                removeAllFiles();
            } else {
                LogUtil.log(TAG, "Skipping file download: " + mediaFile.getFileName());
                pullOriginalMediaFileFromCamera();
            }
            return;
        }

        LogUtil.log(TAG, "File size: " + mediaFile.getFileSize());

        File dirs = new File(getSDCardPath() + mediaFileDir);
        if (!dirs.exists()) {
            dirs.mkdir();
        }

        String filePath = getSDCardPath() + mediaFileDir + "/" + mediaFile.getFileName();
        File file = new File(filePath);
        long offset = 0L;
        if (file.exists()) {
            offset = file.length();
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(file, true);
            long beginTime = System.currentTimeMillis();
            BufferedOutputStream bos = new BufferedOutputStream(outputStream);

            mediaFile.pullOriginalMediaFileFromCamera(0L, new MediaFileDownloadListener() {
                @Override
                public void onStart() {
                    // No action needed for start
                }

                @Override
                public void onProgress(long total, long current) {
                    int tmpProgress = (int) ((1.0 * current / total) * 100);
                    Log.e(TAG, "File " + downLoadMediaFileIndex + ": " + mediaFile.getFileName() + " Download Progress: " + tmpProgress + "%");
                }

                @Override
                public void onRealtimeDataUpdate(byte[] data, long position) {
                    try {
                        bos.write(data);
                        bos.flush();
                    } catch (IOException e) {
                        Log.e(TAG, "Write error: " + e.getMessage());
                    }
                }

                @Override
                public void onFinish() {
                    LogUtil.log(TAG, "File " + downLoadMediaFileIndex + " downloaded successfully.");
                    minIOUpLoad(file, mediaFile);
                    try {
                        outputStream.close();
                        bos.close();
                    } catch (IOException error) {
                        LogUtil.log(TAG, "File " + downLoadMediaFileIndex + " error: " + error.getMessage());
                        DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                    }
                }

                @Override
                public void onFailure(IDJIError error) {
                    LogUtil.log(TAG, "File " + downLoadMediaFileIndex + ": " + mediaFile.getFileName() + " download failed: " + new Gson().toJson(error));
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                    sendMissionExecuteEvents(mqttClient, "File " + downLoadMediaFileIndex + " download failed.");
                    downLoadMediaFileIndex = 0;
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error opening file: " + e.getMessage());
        }
    }

    private AmazonS3 s3 = new AmazonS3Client(new AWSCredentials() {
        @Override
        public String getAWSAccessKeyId() {
            return PreferenceUtils.getInstance().getAccessKey(); // minio的key
        }

        @Override
        public String getAWSSecretKey() {
            return PreferenceUtils.getInstance().getSecretKey(); // minio的密钥
        }
    }, Region.getRegion(Regions.US_EAST_1), new ClientConfiguration());

    @RequiresApi(Build.VERSION_CODES.O)
    public void minIOUpLoad(final File file, final MediaFile mediaFile) {
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        // 服务器地址
                        s3.setEndpoint(PreferenceUtils.getInstance().getUploadUrl()); // http://ip:端口号
                        boolean bucketExists = s3.doesBucketExist(PreferenceUtils.getInstance().getBucketName());
                        if (!bucketExists) {
                            s3.createBucket(PreferenceUtils.getInstance().getBucketName());
                        }

                        // 上传文件到网关MINIO存储服务
                        s3.putObject(
                                new PutObjectRequest(
                                        PreferenceUtils.getInstance().getBucketName(),
                                        "/" + PreferenceUtils.getInstance().getKey() + "/" +
                                                PreferenceUtils.getInstance().getSortiesId() + "/" + mediaFile.getFileName(),
                                        file
                                ).withProgressListener(new ProgressListener() {
                                    @Override
                                    public void progressChanged(ProgressEvent progressEvent) {
                                        switch (progressEvent.getEventCode()) {
                                            case ProgressEvent.PREPARING_EVENT_CODE:
                                                LogUtil.log(TAG, "Preparing to upload file " + downLoadMediaFileIndex);
                                                break;
                                            case ProgressEvent.STARTED_EVENT_CODE:
                                                long bytesTransferred = progressEvent.getBytesTransferred();
                                                int percentage = (int) ((bytesTransferred * 100) / file.length());
                                                LogUtil.log(TAG, "Upload started for file " + downLoadMediaFileIndex + ": " +
                                                        percentage + "% (" + bytesTransferred + " out of " + file.length() + " bytes)");
                                                break;
                                            case ProgressEvent.COMPLETED_EVENT_CODE:
                                                LogUtil.log(TAG, "Upload completed for file " + downLoadMediaFileIndex);
                                                break;
                                            case ProgressEvent.FAILED_EVENT_CODE:
                                                LogUtil.log(TAG, "Upload failed for file " + downLoadMediaFileIndex);
                                                break;
                                            case ProgressEvent.RESET_EVENT_CODE:
                                                LogUtil.log(TAG, "Upload reset for file " + downLoadMediaFileIndex);
                                                break;
                                        }
                                    }
                                })
                        );

                        // 获取文件上传后访问地址url
                        GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
                                PreferenceUtils.getInstance().getBucketName(),
                                "/" + PreferenceUtils.getInstance().getKey() + "/" + mediaFile.getFileName()
                        );
                        String url = s3.generatePresignedUrl(urlRequest).toString();

                        // 文件上传后访问地址url
                        emitter.onNext(url);
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // Handle on subscribe (optional)
                    }

                    @Override
                    public void onNext(String url) {
                        FileUploadResult fileUploadResult = new FileUploadResult();
                        fileUploadResult.setFileName(mediaFile.getFileName());
                        fileUploadResult.setFileSize(mediaFile.getFileSize());
                        fileUploadResult.setFileNum(mediaFiles.size());
                        fileUploadResult.setBuckName(PreferenceUtils.getInstance().getBucketName());
                        fileUploadResult.setObjectKey(PreferenceUtils.getInstance().getKey());
                        fileUploadResult.setSortiesId(PreferenceUtils.getInstance().getSortiesId());
                        fileUploadResult.setUrl(PreferenceUtils.getInstance().getUploadUrl());
                        fileUploadResult.setOffIndex(downLoadMediaFileIndex);
                        fileUploadResult.setFlightId(PreferenceUtils.getInstance().getFlightId());

                        sendFileUploadCallback(mqttClient, fileUploadResult);
                    }

                    @RequiresApi(Build.VERSION_CODES.O)
                    @Override
                    public void onError(Throwable e) {
                        // 每上传失败一张就清除缓存
                        FileUtil.deleteFile(file);
                        LogUtil.log(TAG, "Error uploading file " + downLoadMediaFileIndex + ": " + e.getMessage());

                        downLoadMediaFileIndex++;
                        if (downLoadMediaFileIndex == mediaFiles.size()) {
                            // 所有文件已经下载完成或失败，清空SD卡，缓存，退出媒体模式，发送无人机关机
                            downLoadMediaFileIndex = 0;
                            removeAllFiles();
                        } else {
                            pullOriginalMediaFileFromCamera();
                        }
                    }

                    @RequiresApi(Build.VERSION_CODES.O)
                    @Override
                    public void onComplete() {
                        // 每上传一张就清除缓存
                        FileUtil.deleteFile(file);
                        LogUtil.log(TAG, "File " + downLoadMediaFileIndex + " uploaded successfully.");
                        sendMissionExecuteEvents(mqttClient, "File " + downLoadMediaFileIndex + " uploaded");

                        downLoadMediaFileIndex++;
                        if (downLoadMediaFileIndex == mediaFiles.size()) {
                            // 所有文件已上传完成，清空SD卡，缓存，退出媒体模式，发送无人机关机
                            sendMissionExecuteEvents(mqttClient, "Media files upload completed");
                            removeAllFiles();
                            downLoadMediaFileIndex = 0;
                        } else {
                            pullOriginalMediaFileFromCamera();
                        }
                    }
                });
    }

    public void removeAllFiles() {
        MediaDataCenter.getInstance().getMediaManager().deleteMediaFiles(mediaFiles, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                LogUtil.log(TAG, "清除文件成功 ");
                sendMissionExecuteEvents(mqttClient,"媒体文件已清除");
                disablePlayback();
                LogUtil.log(TAG, "发送关闭无人机");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient);
                LogUtil.log(TAG, "清除文件失败: "+new Gson().toJson(idjiError));
                sendMissionExecuteEvents(mqttClient, "媒体文件清除失败");
                LogUtil.log(TAG, "发送关闭无人机");
            }
        });
    }

    //退出媒体模式
    public void  disablePlayback() {
        MediaDataCenter.getInstance().getMediaManager().disable(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "退出媒体模式成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "退出媒体模式失败:"+new Gson().toJson(idjiError));
            }
        });
    }

    private int downLoadMediaFileIndex = 0;

    private String getSDCardPath(){
         if (checkSDCard()) {
           return Environment.getExternalStorageDirectory()
                    .getPath();
        } else {
           return Environment.getExternalStorageDirectory()
                    .getParentFile().getPath();
        }
    }


    private boolean checkSDCard() {
        return TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }

    // 写入 exif 信息
    public void setMediaFileXMPCustomInfo(final MqttAndroidClient client, final MQMessage message) {
        MediaDataCenter.getInstance().getMediaManager().setMediaFileXMPCustomInfo(
                message.getXmpInfo(),
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        sendMsg2Server(client, message);
                        sendMissionExecuteEvents(client, "设置文件XMP:" + message.getXmpInfo());
                    }

                    @Override
                    public void onFailure(IDJIError error) {
                        sendMsg2Server(client, message, "写入exif失败: " + getIDJIErrorMsg(error));
                        LogUtil.log(TAG, "写入exif失败:"+new Gson().toJson(error));

                    }
                }
        );
    }
}
