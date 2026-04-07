package com.jby.apron.tools;

import android.util.Log;

import com.jby.apron.entity.XTTSParams;
import com.iflytek.aikit.core.AeeEvent;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiInput;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiText;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class XTtsPcmGenerator {

    private static final String TAG = "XTTS";
    private static final String ABILITY_ID = "e2e44feff";

    private AiHandle aiHandle;
    private FileOutputStream pcmOut;


    public void init(AiListener listener) {
        AiHelper.getInst().registerListener(ABILITY_ID, listener);
    }

    /**
     * 合成文本为 PCM 文件
     */
    public int synthToPcm(
            String text,
            XTTSParams params,
            File pcmFile
    ) {
        File parent = pcmFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            pcmOut = new FileOutputStream(pcmFile, false);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        AiInput.Builder paramBuilder = AiInput.builder();
        paramBuilder.param("vcn", params.vcn);
        paramBuilder.param("language", params.language);
        paramBuilder.param("textEncoding", "UTF-8");
        paramBuilder.param("pitch", params.pitch);
        paramBuilder.param("speed", params.speed);
        paramBuilder.param("volume", params.volume);

        aiHandle = AiHelper.getInst().start(
                ABILITY_ID,
                paramBuilder.build(),
                null
        );

        if (aiHandle.getCode() != 0) {
            LogUtil.log(TAG, "start failed: " + aiHandle.getCode());
            return aiHandle.getCode();
        }

        AiText aiText = AiText.get("text")
                .data(text)
                .valid();

        AiRequest request = AiRequest.builder()
                .payload(aiText)
                .build();

        int ret = AiHelper.getInst().write(request, aiHandle);
        if (ret != 0) {
            LogUtil.log(TAG, "write failed: " + ret);
            return ret;
        }

        return 0;
    }

    /**
     * 内部使用的监听器
     */
    public AiListener buildListener() {
        return new AiListener() {

            @Override
            public void onResult(int handleID, List<AiResponse> list, Object usrContext) {
                if (list == null){
                    LogUtil.log(TAG,"list == null");
                    return;
                }

                for (AiResponse resp : list) {
                    if ("audio".equals(resp.getKey())) {
                        byte[] pcm = resp.getValue();
                        if (pcm != null && pcm.length > 0) {
                            writePcm(pcm);
                        }
                    }
                }
            }

            @Override
            public void onEvent(int handleID, int event, List<AiResponse> eventData, Object usrContext) {
                if (event == AeeEvent.AEE_EVENT_END.getValue()) {
                    close();
                }
            }

            @Override
            public void onError(int handleID, int err, String msg, Object usrContext) {
                LogUtil.log(TAG, "XTTS error: " + err + ", " + msg);
                close();
            }
        };
    }

    private synchronized void writePcm(byte[] data) {
        try {
            if (pcmOut != null) {
                pcmOut.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (pcmOut != null) {
                pcmOut.flush();
                pcmOut.close();
                pcmOut = null;
            }
        } catch (Exception ignored) {}

        if (aiHandle != null) {
            AiHelper.getInst().end(aiHandle);
            aiHandle = null;
        }
    }

    /**
     * 释放引擎（App 退出时调用）
     */
//    public void release() {
//        AiHelper.getInst().engineUnInit(ABILITY_ID);
//    }
}