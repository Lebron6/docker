package com.jby.apron.manager;

import static com.jby.apron.tools.Utils.getIDJIErrorMsg;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.jby.apron.base.BaseManager;
import com.jby.apron.entity.MessageDown;
import com.jby.apron.entity.XTTSParams;
import com.jby.apron.tools.LogUtil;
import com.jby.apron.tools.Utils;
import com.jby.apron.tools.XTtsPcmGenerator;
import com.iflytek.aikit.core.AiListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.common.recorder.PCMTools;
import dji.v5.manager.aircraft.megaphone.FileInfo;
import dji.v5.manager.aircraft.megaphone.MegaphoneInfo;
import dji.v5.manager.aircraft.megaphone.MegaphoneInfoListener;
import dji.v5.manager.aircraft.megaphone.MegaphoneManager;
import dji.v5.manager.aircraft.megaphone.PlayMode;
import dji.v5.manager.aircraft.megaphone.UploadType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class SpeakerManager extends BaseManager {


    private SpeakerManager() {
    }

    private static class SpeakerHolder {
        private static final SpeakerManager INSTANCE = new SpeakerManager();
    }

    public static SpeakerManager getInstance() {
        return SpeakerHolder.INSTANCE;
    }

    private int megaphoneStatus;

    public void initMegaphoneInfo() {
        MegaphoneManager.getInstance().addMegaphoneInfoListener(new MegaphoneInfoListener() {
            @Override
            public void onUpdateMegaphoneInfo(MegaphoneInfo megaphoneInfo) {
                if (megaphoneInfo != null) {
                    megaphoneStatus = megaphoneInfo.getStatus().ordinal();
                }
            }
        });
    }


    public void speakerAudioPlayStart(MessageDown message) {

        if (TextUtils.isEmpty(message.getData().getFile().getUrl())) {
            sendFailMsg2Server(message, "喊话文件下载地址为空");
            return;
        }
        Request request = new Request.Builder().url(message.getData().getFile().getUrl()).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendEvent2Server(".pcm文件下载失败:" + e.toString(), 2);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null) {
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fos = null;
                    // 储存下载文件的目录
                    File dir = new File(Environment.getExternalStorageDirectory().getPath());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File file = new File(dir, "voice.pcm");
                    try {
                        is = response.body().byteStream();
                        fos = new FileOutputStream(file);
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        sendEvent2Server("喊话.pcm文件下载成功", 1);
                        String opusFilePath = PCMTools.INSTANCE.convertToOpusFileSync(file.getAbsolutePath());

                        FileInfo fileInfo = new FileInfo(UploadType.VOICE_FILE,
                                new File(opusFilePath),
                                null);
                        MegaphoneManager.getInstance().startPushingFileToMegaphone(fileInfo,
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
                                                MegaphoneManager.getInstance().startPlay(new CommonCallbacks.CompletionCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        sendEvent2Server("喊话器播放音频成功", 1);
                                                    }

                                                    @Override
                                                    public void onFailure(@NonNull IDJIError error) {
                                                        sendEvent2Server("喊话器播放音频失败:" + getIDJIErrorMsg(error), 2);
                                                    }
                                                });
                                            }
                                        }, 1000);
                                    }

                                    @Override
                                    public void onFailure(@NonNull IDJIError error) {
                                        sendFailMsg2Server(message, "喊话器内容上传失败:" + getIDJIErrorMsg(error));
                                    }
                                });


                    } catch (Exception e) {
                        sendEvent2Server("喊话.pcm文件下载失败:" + e.toString(), 2);
                    }
                }
            }
        });
    }


    public void speakerReply(MessageDown message) {
        MegaphoneManager.getInstance().startPlay(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                sendMsg2Server(message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                sendFailMsg2Server(message, "喊话器播放音频失败:" + getIDJIErrorMsg(error));
            }
        });
    }

    public void speakerStop(MessageDown message) {
        MegaphoneManager.getInstance().stopPlay(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                sendMsg2Server(message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                sendFailMsg2Server(message, "喊话器停止播放失败:" + getIDJIErrorMsg(error));
            }
        });
    }

    public void speakerPlayModeSet(MessageDown message) {
        MegaphoneManager.getInstance().setPlayMode(message.getData().getPlay_mode() == 0 ?
                PlayMode.SINGLE : PlayMode.LOOP, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                sendMsg2Server(message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                sendFailMsg2Server(message, "喊话器播放模式设置失败:" + getIDJIErrorMsg(error));
            }
        });
    }

    public void speakerPlayVolumeSet(MessageDown message) {
        MegaphoneManager.getInstance().setVolume(message.getData().getPlay_volume(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                sendMsg2Server(message);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                sendFailMsg2Server(message, "喊话器音量设置失败:" + getIDJIErrorMsg(error));
            }
        });
    }

    private String text;

    public void speakerTTSPlayStart(MessageDown message, int type) {
        XTTSParams params = new XTTSParams();
        if (type == 0) {
            text = message.getData().getTts().getText();
        } else {
            if (megaphoneStatus == 2) {
                MegaphoneManager.getInstance().stopPlay(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        LogUtil.log(TAG, "终止喊话");
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        LogUtil.log(TAG, "终止喊话失败:" + getIDJIErrorMsg(error));

                    }
                });
            }
            params.vcn = message.getData().getType() == 0 ? "xiaofeng" : "xiaoyan";
            params.language = message.getData().getLanguage() == 0 ? 1 : 2;
            params.speed = message.getData().getSpeed();
            params.volume = message.getData().getVolume();
        }
        if (TextUtils.isEmpty(text)) {
            sendFailMsg2Server(message, "喊话失败:喊话内容为空");
            return;
        }
        XTtsPcmGenerator tts = new XTtsPcmGenerator();
        AiListener listener = tts.buildListener();
        tts.init(listener);

        File pcmFile = new File(Utils.getSDCardPath() + "/pcm", "tts_output_local.pcm");

        int synthToPcm = tts.synthToPcm(
                text,
                params,
                pcmFile
        );
        if (synthToPcm == 0) {
            sendMsg2Server(message);
            String opusFilePath = PCMTools.INSTANCE.convertToOpusFileSync(pcmFile.getAbsolutePath());

            FileInfo fileInfo = new FileInfo(UploadType.VOICE_FILE,
                    new File(opusFilePath),
                    null);
            MegaphoneManager.getInstance().startPushingFileToMegaphone(fileInfo,
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
                                    MegaphoneManager.getInstance().startPlay(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onSuccess() {
                                            sendEvent2Server("喊话器播放TTS音频成功", 1);
                                        }

                                        @Override
                                        public void onFailure(@NonNull IDJIError error) {
                                            sendEvent2Server("喊话器播放TTS音频失败:" + getIDJIErrorMsg(error), 2);
                                        }
                                    });
                                }
                            }, 1000);
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            sendFailMsg2Server(message, "喊话器内容上传失败:" + getIDJIErrorMsg(error));
                        }
                    });
        } else {
            sendFailMsg2Server(message, "tts合成失败");
        }
    }
}