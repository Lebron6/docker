package com.aros.apron.manager;


import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;

import java.util.Map;

import dji.sdk.keyvalue.value.payload.WidgetValue;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.aircraft.payload.PayloadCenter;
import dji.v5.manager.aircraft.payload.PayloadIndexType;
import dji.v5.manager.interfaces.IPayloadManager;


public class PayloadWidgetManager extends BaseManager {


    private PayloadWidgetManager() {
    }

    private static class PayloadWidgetHolder {
        private static final PayloadWidgetManager INSTANCE = new PayloadWidgetManager();
    }

    public static PayloadWidgetManager getInstance() {
        return PayloadWidgetHolder.INSTANCE;
    }

    public void setPayloadWidgetValue(){
        Map<PayloadIndexType, IPayloadManager> payloadManagerMap = PayloadCenter.getInstance().getPayloadManager();
        WidgetValue widgetValue=new WidgetValue();
//        widgetValue.setValue();
//        widgetValue.setIndex();
//        widgetValue.setType();
        payloadManagerMap.get(PayloadIndexType.RIGHT).setWidgetValue(widgetValue, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {

            }
        });
    }
}
