package com.aros.apron.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.aros.apron.tools.LogUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncTaskUtil {

    public interface AsyncCallback<T>{
        void onResult(T result);
        void onError(Exception e);
    }

    // 异步方法，接受一个回调接口作为参数
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> void asyncMethod(AsyncCallback<T> callback, Supplier<T> task){
        CompletableFuture.supplyAsync(task).thenAccept(result ->{
            callback.onResult(result);
        }).exceptionally(ex ->{
            callback.onError((Exception) ex);
            return null;// 这里返回null，因为exceptionally的返回值不会被使用
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void main(String[] args) {
        // 使用异步方法，并传入回调接口的实现和一个Supplier来提供异步任务
        asyncMethod(new AsyncCallback<Integer>() {
            @Override
            public void onResult(Integer result) {
                // 处理异步任务的结果
            }

            @Override
            public void onError(Exception e) {
                // 处理异步任务中的异常
                e.printStackTrace();
            }
        },() -> {
            // 模拟一个耗时操作
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int re = 0;
            return re;
        });
    }
    // main方法会立即返回，不会等待异步任务完成。
    // 异步任务的结果将通过回调接口返回

//    固定时间（控制帧率）执行
    private Timer heartBeatTimer;
    private TimerTask heartBeatTask;
    public void start(){
        if (heartBeatTimer == null) {
            heartBeatTimer = new Timer();
        }
        if (heartBeatTask == null) {
            heartBeatTask = new TimerTask() {
                @Override
                public void run() {
                    int re = CameraControllerUtil.getInstance().sendVideoWithARInfoXFun(0);
                    if (re != 0 && re != 10){
                        LogUtil.log("TAG","发送失败");
                    }
                }
            };
        }
        //在延迟delay后执行task1次，之后定期period毫秒时间执行task
        heartBeatTimer.schedule(heartBeatTask, 0, 45);
    }
}
