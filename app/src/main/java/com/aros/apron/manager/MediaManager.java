package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

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
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.DjiMetaData;
import com.aros.apron.tools.DjiXmpParser;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.aros.apron.tools.TimeUtil;
import com.autonavi.base.amap.mapcore.FileUtil;
import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.MediaFileType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.media.MediaFile;
import dji.v5.manager.datacenter.media.MediaFileDownloadListener;
import dji.v5.manager.datacenter.media.MediaFileListDataSource;
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

    /* ===== 已上传文件名集合（本次任务内有效） ===== */
    private final Set<String> uploadedFileNames = new HashSet<>();
    /* ===== S3 bucket是否已检查 ===== */
    private volatile boolean bucketChecked = false;
    /* ===== 下载失败重试计数 ===== */
    private int downloadFailTimes = 0;
    private static final int MAX_DOWNLOAD_RETRY = 3;
    private MediaManager() {
    }

    private static class MediaManagerHolder {
        private static final MediaManager INSTANCE = new MediaManager();
    }

    public static MediaManager getInstance() {
        return MediaManagerHolder.INSTANCE;
    }

    public  void init() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            MediaFileListDataSource source = new MediaFileListDataSource.Builder().setIndexType(ComponentIndexType.PORT_1).build();
            MediaDataCenter.getInstance().getMediaManager().setMediaFileDataSource(source);
            MediaDataCenter.getInstance().getMediaManager().addMediaFileListStateListener(new MediaFileListStateListener() {
                @Override
                public void onUpdate(MediaFileListState mediaFileListState) {
                    mState = mediaFileListState;
                    LogUtil.log(TAG, "当前媒体文件状态：" + mediaFileListState.name());
                }
            });
        }
    }

    private int enterPlayBackFailTimes;
    private boolean isEnablePlayback;

    public void enablePlayback() {
        // 每次进入媒体模式时清空已上传文件集合
        uploadedFileNames.clear();
        bucketChecked = false;
        downloadFailTimes = 0;
        // 重置失败计数和标志
        enterPlayBackFailTimes = 0;
        isEnablePlayback = false;
        // 重置拉取文件列表相关的计数器
        pullMediaFileListFromCameraFailTimes = 0;
        updatingWaitCount = 0;
        pullqwq = false;
        pullStartTime = System.currentTimeMillis(); // 开始计时

        LogUtil.log(TAG, "已清空上传文件集合");

        MediaDataCenter.getInstance().getMediaManager().enable(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "进入媒体模式成功");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MediaFileListDataSource source = new
                                MediaFileListDataSource.Builder().setIndexType(ComponentIndexType.PORT_1).build();
                        MediaDataCenter.getInstance().getMediaManager().setMediaFileDataSource(source);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                pullMediaFileListFromCamera();
                            }
                        }, 3000);
                    }
                }, 3000);
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                LogUtil.log(TAG, "第" + enterPlayBackFailTimes + "次进入媒体模式失败:" + new Gson().toJson(idjiError));
                if (!isEnablePlayback) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (enterPlayBackFailTimes < 10) {
                                enterPlayBackFailTimes++;
                                enablePlayback();
                            } else {
                                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                                sendEvent2Server("媒体模式进入失败:关机",1);
                            }
                        }
                    }, 1500);
                }
            }
        });
    }

    private int pullMediaFileListFromCameraFailTimes;

    private int updatingWaitCount = 0;
    private static final int MAX_UPDATING_WAIT = 15; // 最多等待15秒，无文件时快速跳过
    private boolean pullqwq = false;
    private boolean isPullMediaFileListFromCameraSuccess;
    private long pullStartTime = 0; // 记录整个拉取流程开始时间
    private static final int MAX_PULL_DURATION = 25; // 整个拉取流程最多25秒，超时强制关机

    private void pullMediaFileListFromCamera() {
        // 全局超时检查：防止状态机异常导致无限循环
        long elapsed = (System.currentTimeMillis() - pullStartTime) / 1000;
        if (elapsed >= MAX_PULL_DURATION) {
            LogUtil.log(TAG, "拉取流程总耗时 " + elapsed + "s，超时强制关机");
            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
            sendEvent2Server("媒体文件拉取超时", 2);
            disablePlayback();
            return;
        }

        mState = MediaDataCenter.getInstance().getMediaManager().getMediaFileListState();
        LogUtil.log(TAG, "当前状态：" + mState + "，准备拉取文件列表（已耗时" + elapsed + "s）");

        // 1. 当状态为IDLE时，需要调用pullMediaFileListFromCamera拉取全量数据
        // 2. 当状态为UP_TO_DATE时，表示拉取完成，可以获取数据
        if (mState == MediaFileListState.IDLE) {
            // 状态为IDLE，开始拉取文件列表
            LogUtil.log(TAG, "状态为IDLE，开始拉取文件列表");
            MediaDataCenter.getInstance().getMediaManager()
                    .pullMediaFileListFromCamera(new PullMediaFileListParam.Builder().count(-1).build(),
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    LogUtil.log(TAG, "拉取文件列表成功");
                                    // 重置pullqwq标志，下次调用重新从IDLE拉取
                                    pullqwq = false;
                                    // 拉取成功后，等待状态变为UP_TO_DATE
                                    new Handler().postDelayed(MediaManager.this::pullMediaFileListFromCamera, 1000);
                                }

                                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "拉取媒体文件失败: " + new Gson().toJson(idjiError));
                    // 失败后重试，最多重试3次
                    if (pullMediaFileListFromCameraFailTimes < 5) {
                        pullMediaFileListFromCameraFailTimes++;
                        LogUtil.log(TAG, "第" + pullMediaFileListFromCameraFailTimes + "次重试...");
                        new Handler().postDelayed(MediaManager.this::pullMediaFileListFromCamera, 2000);
                    } else {
                        LogUtil.log(TAG, "重试次数达到上限，拉取失败");
                        ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                        sendEvent2Server("拉取媒体文件失败",2);
                        disablePlayback();
                        LogUtil.log(TAG, "发送关闭无人机");
                    }
                }
            });
        } else if (mState == MediaFileListState.UP_TO_DATE) {
            // 状态为UP_TO_DATE，获取文件列表数据
            LogUtil.log(TAG, "状态为UP_TO_DATE，获取文件列表数据");
            try {
                // 确保获取文件列表数据
                List<MediaFile> rawList = MediaDataCenter.getInstance().getMediaManager().getMediaFileListData().getData();

                // 检查文件列表是否为空
                if (rawList == null || rawList.isEmpty()) {
                    LogUtil.log(TAG, "文件列表为空，重试拉取");
                    // 状态已经是UP_TO_DATE时，空列表可能确实无文件，快速重试2次后放弃
                    if (pullMediaFileListFromCameraFailTimes < 2) {
                        pullMediaFileListFromCameraFailTimes++;
                        LogUtil.log(TAG, "第" + pullMediaFileListFromCameraFailTimes + "次重试...");
                        new Handler().postDelayed(MediaManager.this::pullMediaFileListFromCamera, 2000);
                    } else {
                        LogUtil.log(TAG, "UP_TO_DATE状态文件列表持续为空，确认无文件");
                        ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                        sendEvent2Server("拉取媒体文件失败",2);
                        disablePlayback();
                        LogUtil.log(TAG, "发送关闭无人机");
                    }
                    return;
                }

                LogUtil.log(TAG, "原始文件列表数量: " + rawList.size());

                // 过滤已上传文件
                mediaFiles = new ArrayList<>();
                for (MediaFile mf : rawList) {
                    if (!uploadedFileNames.contains(mf.getFileName())) {
                        mediaFiles.add(mf);
                    } else {
                        LogUtil.log(TAG, "跳过已上传文件: " + mf.getFileName());
                    }
                }
                // 修复：在过滤后设置任务媒体计数
                Movement.getInstance().setTask_media_count(mediaFiles.size());
                LogUtil.log(TAG, "过滤后文件数量: " + mediaFiles.size());
//                if(PreferenceUtils.getInstance().getMissionType()==0){
//                    sendFlightTaskProgress2Server();
//                }

                if (mediaFiles.isEmpty()) {
                    LogUtil.log(TAG, "所有文件均已上传，直接清理");
                    downLoadMediaFileIndex = 0;
                    // 提前设置关机标志，让 aircraftStoredReply 能立即回复成功
                    ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                    removeAllFiles();
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pullOriginalMediaFileFromCamera();
                }
            } catch (Exception e) {
                LogUtil.log(TAG, "获取文件列表数据失败: " + e.getMessage());
                // 发生异常时快速重试2次
                if (pullMediaFileListFromCameraFailTimes < 2) {
                    pullMediaFileListFromCameraFailTimes++;
                    LogUtil.log(TAG, "第" + pullMediaFileListFromCameraFailTimes + "次重试...");
                    new Handler().postDelayed(MediaManager.this::pullMediaFileListFromCamera, 2000);
                } else {
                    LogUtil.log(TAG, "重试次数达到上限，拉取失败");
                    ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                    sendEvent2Server("拉取媒体文件失败",2);
                    disablePlayback();
                    LogUtil.log(TAG, "发送关闭无人机");
                }
            }
        } else {
            // 其他状态（如UPDATING），等待状态变化
            LogUtil.log(TAG, "状态为" + mState + "，等待状态变化... (count=" + updatingWaitCount + ")");
            updatingWaitCount++;

            // 增加超时处理，避免无限等待
            if (updatingWaitCount >= MAX_UPDATING_WAIT) {
                LogUtil.log(TAG, "等待状态变化超时，强制关机");
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                sendEvent2Server("媒体文件状态更新超时",2);
                disablePlayback();
                LogUtil.log(TAG, "发送关闭无人机");
                return;
            } else {
                if (pullqwq == false) {
                    MediaDataCenter.getInstance().getMediaManager().pullMediaFileListFromCamera(new
                            PullMediaFileListParam.Builder().count(-1).build(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG, "拉取成功");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            LogUtil.log(TAG, "拉取失败");
                        }
                    });
                    pullqwq = true;
                }
            }
            new Handler().postDelayed(MediaManager.this::pullMediaFileListFromCamera, 1000);
        }
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
        downloadFailTimes = 0;

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

        FileOutputStream outputStream = null;
        BufferedOutputStream bos = null;
        try {
            final FileOutputStream finalOutputStream = new FileOutputStream(file, true);
            outputStream = finalOutputStream;
            long beginTime = System.currentTimeMillis();
            final BufferedOutputStream finalBos = new BufferedOutputStream(finalOutputStream);
            bos = finalBos;

            mediaFile.pullOriginalMediaFileFromCamera(offset, new MediaFileDownloadListener() {
                @Override
                public void onStart() {
                    // No action needed for start
                }

                @Override
                public void onProgress(long total, long current) {
                    int tmpProgress = (int) ((1.0 * current / total) * 100);
                    Log.e(TAG, "File " + downLoadMediaFileIndex + ": " +
                            mediaFile.getFileName() + " Download Progress: " + tmpProgress + "%");
                }

                @Override
                public void onRealtimeDataUpdate(byte[] data, long position) {
                    try {
                        finalBos.write(data);
                        finalBos.flush();
                    } catch (IOException e) {
                        Log.e(TAG, "Write error: " + e.getMessage());
                    }
                }

                @Override
                public void onFinish() {
                    LogUtil.log(TAG, "File:" + downLoadMediaFileIndex + "fileName:" +
                            mediaFile.getFileName() + " downloaded successfully.");
                    minIOUpLoad(file, mediaFile);
                    try {
                        if (finalBos != null) finalBos.close();
                        if (finalOutputStream != null) finalOutputStream.close();
                    } catch (IOException error) {
                        LogUtil.log(TAG, "File " + downLoadMediaFileIndex + " error: " + error.getMessage());
                        ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                        sendEvent2Server("文件流关闭失败", 2);
                        disablePlayback();
                        LogUtil.log(TAG, "发送关闭无人机");
                    }
                }

                @Override
                public void onFailure(IDJIError error) {
                    LogUtil.log(TAG, "File " + downLoadMediaFileIndex + ": " +
                            mediaFile.getFileName() + " download failed: " + new Gson().toJson(error));
                    // 清理临时文件
                    if (file.exists()) {
                        file.delete();
                    }
                    // 关闭文件流
                    try {
                        if (finalBos != null) finalBos.close();
                        if (finalOutputStream != null) finalOutputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing file: " + e.getMessage());
                    }
                    // 下载失败重试机制
                    if (downloadFailTimes < MAX_DOWNLOAD_RETRY) {
                        downloadFailTimes++;
                        LogUtil.log(TAG, "第" + downloadFailTimes + "次下载失败，2秒后重试同一文件");
                        new Handler().postDelayed(() -> {
                            pullOriginalMediaFileFromCamera();
                        }, 2000);
                    } else {
                        ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                        sendEvent2Server("第" + downLoadMediaFileIndex + "个文件下载失败(已重试" + MAX_DOWNLOAD_RETRY + "次)",2);
                        disablePlayback();
                        LogUtil.log(TAG, "发送关闭无人机");
                    }
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error opening file: " + e.getMessage());
            // 发生异常时也要确保关机
            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
            sendEvent2Server("文件打开失败",2);
            disablePlayback();
            LogUtil.log(TAG, "发送关闭无人机");
            // 关闭文件流
            try {
                if (bos != null) bos.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException ex) {
                Log.e(TAG, "Error closing file: " + ex.getMessage());
            }
        }
    }

    private AmazonS3 s3 = new AmazonS3Client(new AWSCredentials() {
        @Override
        public String getAWSAccessKeyId() {
//            return PreferenceUtils.getInstance().getAccessKey(); // minio的key
//            return "3RFFH8NUGKFG0ZBYGF7R"; // minio的key
            return "admin"; // minio的key
        }

        @Override
        public String getAWSSecretKey() {
//            return PreferenceUtils.getInstance().getSecretKey(); // minio的密钥
//            return "pMMrVHQA4rN1J9xjTCpOtx+YSzK+TTRvaswaLwpX"; // minio的密钥
            return "m9v8OCjt9b522ju"; // minio的密钥
        }
    }, Region.getRegion(Regions.US_EAST_1));

    @RequiresApi(Build.VERSION_CODES.O)
    public void minIOUpLoad(final File file, final MediaFile mediaFile) {

        LogUtil.log(TAG, "文件路径=" + file.getAbsolutePath() + ", 文件大小=" + file.length());
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        // 服务器地址
//                        s3.setEndpoint(PreferenceUtils.getInstance().getUploadUrl()); // http://ip:端口号
//                        s3.setEndpoint("http://8.134.104.234:9000"); // http://ip:端口号
                        s3.setEndpoint("http://192.168.19.10:9000"); // http://ip:端口号
                        // Bucket只在首次上传时检查创建，后续上传不再重复请求
                        if (!bucketChecked) {
                            synchronized (this) {
                                if (!bucketChecked) {
//                                    boolean bucketExists = s3.doesBucketExist(PreferenceUtils.getInstance().getBucketName());
                                    boolean bucketExists = s3.doesBucketExist("honghu-uav");
                                    if (!bucketExists) {
//                                        s3.createBucket(PreferenceUtils.getInstance().getBucketName());
                                        s3.createBucket("honghu-uav");
                                    }
                                    bucketChecked = true;
                                }
                            }
                        }

                        // 上传文件到网关MINIO存储服务
                        s3.putObject(
                                new PutObjectRequest(
//                                        PreferenceUtils.getInstance().getBucketName(),
                                        "honghu-uav",
//                                        "/" + PreferenceUtils.getInstance().getObjectKey() + "/" + mediaFile.getFileName(),
                                        "/" + "media" + "/" + mediaFile.getFileName(),
                                        file
//                    new PutObjectRequest(
//                                        PreferenceUtils.getInstance().getBucketName(),
//                                        "/" + PreferenceUtils.getInstance().getObjectKey() + "/" +
//                                                PreferenceUtils.getInstance().getFlightId() + "/" + mediaFile.getFileName(),
//                                        file
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
//                                PreferenceUtils.getInstance().getBucketName(),
                                "honghu-uav",
//                                "/" + PreferenceUtils.getInstance().getObjectKey() + "/"
                                "/" + "media" + "/"
                                        + mediaFile.getFileName()
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
                        // 上传成功，重置下载重试计数
                        downloadFailTimes = 0;
                        mediaFile.pullXMPFileDataFromCamera(new CommonCallbacks.CompletionCallbackWithParam<String>() {
                            @Override
                            public void onSuccess(String s) {
                                DjiMetaData metaData =
                                        DjiXmpParser.parse(s);

                                //上传完成发送事件
                                sendMediaUpload2Server(metaData.getAbsoluteAltitude(),
                                        metaData.getRelativeAltitude(),
                                        TimeUtil.convertIsoToChineseFormat(metaData.getCreateDate()),
                                        metaData.getGimbalYawDegree(),
                                        metaData.getGpsLongitude(),
                                        metaData.getGpsLatitude(),
                                        mediaFile.getFileName(), mediaFiles.size(), downLoadMediaFileIndex);
                            }

                            @Override
                            public void onFailure(@NonNull IDJIError idjiError) {
                                LogUtil.log(TAG, "获取xmp失败：" + getIDJIErrorMsg(idjiError));
                                //上传完成发送事件
                                sendMediaUpload2Server("0",
                                        "0",
                                        mediaFile.getDate().getYear()+"-"
                                                +mediaFile.getDate().getMonth()+"-"
                                                +mediaFile.getDate().getDay()+" "+
                                                mediaFile.getDate().getHour()+":"+
                                                mediaFile.getDate().getMinute()+":"+
                                                +mediaFile.getDate().getSecond(),
                                        "0",
                                        "0",
                                        "0",
                                        mediaFile.getFileName(), mediaFiles.size(), downLoadMediaFileIndex);
                            }
                        });
                    }

                    @RequiresApi(Build.VERSION_CODES.O)
                    @Override
                    public void onError(Throwable e) {
                        // 每上传失败一张就清除缓存
                        uploadedFileNames.add(mediaFile.getFileName());
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
                        // 每上传一张就清除缓存，并记录已上传文件名
                        uploadedFileNames.add(mediaFile.getFileName());
                        FileUtil.deleteFile(file);
                        LogUtil.log(TAG, "File " + downLoadMediaFileIndex + " uploaded successfully.");
                        sendEvent2Server( "第" + downLoadMediaFileIndex + "个文件已上传",1);

                        downLoadMediaFileIndex++;
                        if (downLoadMediaFileIndex == mediaFiles.size()) {
                            // 所有文件已上传完成，清空SD卡，缓存，退出媒体模式，发送无人机关机
                            sendEvent2Server( "媒体文件已上传完毕",1);
                            removeAllFiles();
                            downLoadMediaFileIndex = 0;
                        } else {
                            pullOriginalMediaFileFromCamera();
                        }
                    }
                });
    }

    public void removeAllFiles() {
        // 确保即使没有文件也能正常关机
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            LogUtil.log(TAG, "没有文件需要清除，直接关机");
            ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
            sendEvent2Server("没有媒体文件需要清除",1);
            disablePlayback();
            LogUtil.log(TAG, "发送关闭无人机");
            return;
        }

        MediaDataCenter.getInstance().getMediaManager().deleteMediaFiles(mediaFiles, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                LogUtil.log(TAG, "清除文件成功 ");
                sendEvent2Server("媒体文件已清除",1);
                disablePlayback();
                LogUtil.log(TAG, "发送关闭无人机");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                ApronExecutionStatus.getInstance().setAircraftWaitShutDown(true);
                LogUtil.log(TAG, "清除文件失败: " + new Gson().toJson(idjiError));
                sendEvent2Server("媒体文件清除失败",2);
                LogUtil.log(TAG, "发送关闭无人机");
            }
        });
    }

    //退出媒体模式
    public void  disablePlayback() {
        // 任务结束，停止视频流刷新定时器
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


}