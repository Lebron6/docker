package com.aros.apron.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;

import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.sdk.keyvalue.value.airlink.WlmDongleInfo;
import dji.sdk.keyvalue.value.airlink.WlmDongleWorkState;
import dji.sdk.keyvalue.value.airlink.WlmLinkFrequenceBand;
import dji.sdk.keyvalue.value.airlink.WlmLinkQualityLevelInfo;
import dji.sdk.keyvalue.value.airlink.WlmLinkStatus;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.lte.LTEDongleInfoListener;
import dji.v5.manager.aircraft.lte.LTELinkInfo;
import dji.v5.manager.aircraft.lte.LTELinkInfoListener;


public class LTEManager extends BaseManager {


    private LTEManager() {
    }

    private static class LTEHolder {
        private static final LTEManager INSTANCE = new LTEManager();
    }

    public static LTEManager getInstance() {
        return LTEHolder.INSTANCE;
    }

    public void initLTEInfo() {

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {

            KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyWlmLinkStatus), this, new CommonCallbacks.KeyListener<WlmLinkStatus>() {
                @Override
                public void onValueChange(@Nullable WlmLinkStatus wlmLinkStatus, @Nullable WlmLinkStatus t1) {
                    if (t1 != null) {
                        Movement.getInstance().setLink_state_4g(t1.getIsLteLinkConnected() ? "1" : "0");
                        Movement.getInstance().setSdr_link_state(t1.getSdrFrequenceBand() == WlmLinkFrequenceBand.UNKNOWN ? "1" : "0");
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyWlmLinkQualityLevel), this, new CommonCallbacks.KeyListener<WlmLinkQualityLevelInfo>() {
                @Override
                public void onValueChange(@Nullable WlmLinkQualityLevelInfo wlmLinkQualityLevelInfo, @Nullable WlmLinkQualityLevelInfo t1) {
                    if (t1 != null) {
                        Movement.getInstance().setQuality_4g(t1.getLteLinkQualityLevel() + "");
                        Movement.getInstance().setGnd_quality_4g(t1.getLteLinkQualityLevel() + "");
                        Movement.getInstance().setUav_quality_4g(t1.getLteLinkQualityLevel() + "");
                    }
                }
            });
            KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyDownLinkQualityRaw), this, new CommonCallbacks.KeyListener<Integer>() {
                @Override
                public void onValueChange(@Nullable Integer integer, @Nullable Integer value) {
                    if (value != null) {
                        if (value >= 1 && value <= 20) {
                            Movement.getInstance().setSdr_quality("1");
                        } else if (value >= 21 && value <= 40) {
                            Movement.getInstance().setSdr_quality("2");
                        } else if (value >= 41 && value <= 60) {
                            Movement.getInstance().setSdr_quality("3");
                        } else if (value >= 61 && value <= 80) {
                            Movement.getInstance().setSdr_quality("4");
                        } else if (value >= 81 && value <= 100) {
                            Movement.getInstance().setSdr_quality("5");
                        } else {
                            Movement.getInstance().setSdr_quality("5");
                        }
                    }
                }
            });

            KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyFrequencyBand), this, new CommonCallbacks.KeyListener<FrequencyBand>() {
                @Override
                public void onValueChange(@Nullable FrequencyBand frequencyBand, @Nullable FrequencyBand t1) {
                    if (t1 != null) {
                        switch (t1) {
                            case BAND_2_DOT_4G:
                                Movement.getInstance().setSdr_freq_band("2.4");
                                Movement.getInstance().setFreq_band_4g("2.4");
                                break;
//                            case BAND_5_DOT_8G:
//                                Movement.getInstance().setSdr_freq_band("5.8");
//                                break;
                            default:
                                Movement.getInstance().setSdr_freq_band("5.8");
                                Movement.getInstance().setFreq_band_4g("5.8");
                                break;
                        }
                    }
                }
            });

            dji.v5.manager.aircraft.lte.LTEManager.getInstance().addLTELinkInfoListener(new LTELinkInfoListener() {
                @Override
                public void onLTELinkInfoUpdate(LTELinkInfo info) {
                    if (info != null) {
                        Movement.getInstance().setLink_workmode(info.getLTELinkType() + "");
                    }
                }
            });


            dji.v5.manager.aircraft.lte.LTEManager.getInstance().addLTEDongleInfoListener(new LTEDongleInfoListener() {
                @Override
                public void onLTEAircraftDongleInfoUpdate(@NonNull List<WlmDongleInfo> aircraftDongleInfos) {
                    if (aircraftDongleInfos != null) {
                        Movement.getInstance().setDongle_number(aircraftDongleInfos.size() + "");
                        Movement.getInstance().setSdr_link_state(
                                aircraftDongleInfos.get(0).getWorkState() == WlmDongleWorkState.REGISTER_ROAMING ? "1" : "0");
                    } else {
                        Movement.getInstance().setDongle_number("0");
                        Movement.getInstance().setSdr_link_state("0");
                    }
                }

                @Override
                public void onLTERemoteControllerDongleInfoUpdate(@NonNull List<WlmDongleInfo> remoteControllerDongleInfos) {

                }
            });

        } else {
            LogUtil.log(TAG, "初始化LTE失败");
        }
    }
}