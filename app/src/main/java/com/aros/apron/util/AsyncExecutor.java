package com.aros.apron.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncExecutor {


    // 定义一个回调接口，用于处理异步结果
    public interface AsyncCallback<T> {
        void onResult(T result);

        void onError(Exception e);
    }

    // 异步方法，接受一个需要被执行的方法（Callable）和一个回调接口
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <T> void executeAsync(Callable<T> task, AsyncCallback<T> callback) {
        Runnable taskWrapper = () -> {
            if (shouldStop.get()) {
                // 如果已经应该停止，则直接返回
                return;
            }
            CompletableFuture.supplyAsync(() -> {
                try {
                    // 执行传入的任务
                    return task.call();
                } catch (Exception e) {
                    // 如果任务执行过程中出现异常，则抛出运行时异常
                    throw new RuntimeException(e);
                }
            }).thenAccept(result -> {
                if (!"0".equals(result) || !"10".equals(result)) {
                    // 如果结果满足停止条件，则设置停止标志
                    shouldStop.set(true);
                    // 可以选择性地取消调度器中的所有任务
                    scheduler.shutdownNow(); // 这将尝试停止正在执行的任务并取消等待的任务
                } else {
                    // 异步任务成功完成，调用回调接口的onResult方法
                    callback.onResult(result);
                }

            }).exceptionally(ex -> {
                // 异步任务执行过程中出现异常，调用回调接口的onError方法
                callback.onError((Exception) ex);
                return null; // 这里返回null，因为exceptionally的返回值不会被使用
            });
        };

        // 使用scheduleAtFixedRate或scheduleWithFixedDelay来调度任务
        // 这里为了示例使用scheduleAtFixedRate，但注意它可能导致任务重叠
        // 安排任务以固定频率执行（初始延迟为0，之后每隔30毫秒执行一次）
        scheduler.scheduleAtFixedRate(taskWrapper, 0,30 , TimeUnit.MILLISECONDS);

        // 添加一个钩子来在程序退出时关闭调度器（如果尚未关闭）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }));
    }

    private static final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void main(String[] args) {
        // 定义一个需要被执行的方法，这里使用Callable接口
        Callable<String> myTask = () -> {
            // 模拟一个耗时操作
            Thread.sleep(2000);
            return "Task Completed"; // 返回任务的结果
        };

        // 使用异步方法，并传入回调接口的实现
        executeAsync(myTask, new AsyncCallback<String>() {
            @Override
            public void onResult(String result) {
                // 处理异步任务的结果
                System.out.println("Async task result: " + result);
            }

            @Override
            public void onError(Exception e) {
                // 处理异步任务中的异常
                e.printStackTrace();
            }
        });

        // 注意：main方法会立即返回，不会等待异步任务完成。
        // 异步任务的结果将通过回调接口返回。

        // 为了演示目的，我们可以添加一些代码来保持主线程运行，以便看到异步回调的输出。
        // 在实际应用中，通常不需要这样做，因为异步回调会在适当的时候被触发。
        try {
            Thread.sleep(3000); // 等待足够长的时间以看到异步回调的输出
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
