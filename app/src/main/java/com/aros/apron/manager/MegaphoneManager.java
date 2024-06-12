package com.aros.apron.manager;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.nio.charset.StandardCharsets;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.megaphone.FileInfo;
import dji.v5.manager.aircraft.megaphone.MegaphoneIndex;
import dji.v5.manager.aircraft.megaphone.PlayMode;
import dji.v5.manager.aircraft.megaphone.UploadType;
import dji.v5.manager.aircraft.megaphone.WorkMode;
import dji.v5.manager.interfaces.IMegaphoneManager;


public class MegaphoneManager extends BaseManager {

    private MegaphoneManager() {
    }

    private static class MegaphoneHolder {
        private static final MegaphoneManager INSTANCE = new MegaphoneManager();
    }

    public static MegaphoneManager getInstance() {
        return MegaphoneHolder.INSTANCE;
    }

    //设置喊话器音量/播放模式
    public void startMegaphonePlay(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            IMegaphoneManager iMegaphoneManager = dji.v5.manager.aircraft.megaphone.MegaphoneManager.getInstance();
            iMegaphoneManager.setMegaphoneIndex(MegaphoneIndex.STARBOARD, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "喊话器位置设置成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "喊话器位置设置失败:" + idjiError.description());
                }
            });
            iMegaphoneManager.setVolume(message.getMegaphoneVolume(), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(client, message, "设置喊话器音量成功");
                    LogUtil.log(TAG, "喊话器音量设置成功");
                }
                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(client, message, "设置喊话器音量失败:" + error.description());
                    LogUtil.log(TAG, "设置喊话器音量失败:" + error.description());
                }
            });
            iMegaphoneManager.setPlayMode(message.getMegaphonePlayMode() == 1 ? PlayMode.SINGLE : PlayMode.LOOP, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "设置喊话器播放模式成功");
                }
                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(client, message, "设置喊话器播放模式失败:" + error.description());
                    LogUtil.log(TAG, "设置喊话器播放模式失败:" + error.description());
                }
            });
            iMegaphoneManager.setWorkMode(WorkMode.TTS, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(client, message);
                    LogUtil.log(TAG, "喊话器工作模式设置成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(client, message, "设置喊话器工作模式失败:" + error.description());
                    LogUtil.log(TAG, "喊话器工作模式设置失败:" + error.description());

                }
            });
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    iMegaphoneManager.startPushingFileToMegaphone(new FileInfo(UploadType.TTS_DATA, null,
                                    message.getMegaphoneWord().getBytes(StandardCharsets.UTF_8)),
                            new CommonCallbacks.CompletionCallbackWithProgress<Integer>() {
                                @Override
                                public void onProgressUpdate(Integer integer) {
                                    LogUtil.log(TAG, "喊话器内容上传进度:" + integer + "%");
                                }

                                @Override
                                public void onSuccess() {
                                    LogUtil.log(TAG, "喊话器内容上传成功");
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            startPlay(client, message);
                                        }
                                    }, 200);
                                }

                                @Override
                                public void onFailure(@NonNull IDJIError idjiError) {
                                    LogUtil.log(TAG, "喊话器内容上传失败:" + new Gson().toJson(idjiError));
                                }
                            });
                }
            }, 200);

        } else {
            sendMsg2Server(client, message, "飞控未连接");
            LogUtil.log(TAG, "飞控未连接");

        }

    }


    //播放
    public void startPlay(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            IMegaphoneManager iMegaphoneManager = dji.v5.manager.aircraft.megaphone.MegaphoneManager.getInstance();
            iMegaphoneManager.startPlay(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "喊话器播放成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "喊话器播放失败");
                }
            });
        } else {
            sendMsg2Server(client, message, "飞控未连接");
        }
    }

    //停止播放
    public void stopPlay(MqttAndroidClient client, MQMessage message) {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            IMegaphoneManager iMegaphoneManager = dji.v5.manager.aircraft.megaphone.MegaphoneManager.getInstance();
            iMegaphoneManager.stopPlay(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    sendMsg2Server(client, message);
                    LogUtil.log(TAG, "喊话器停止播放成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    sendMsg2Server(client, message, "喊话器停止播放失败:" + error.description());
                    LogUtil.log(TAG, "喊话器停止播放失败");
                }
            });
        } else {
            LogUtil.log(TAG, "喊话器停止播放失败");
        }
    }
}
