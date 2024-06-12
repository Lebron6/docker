package com.aros.apron.view;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;

public class LongTouchBtn extends Button {
	
	/**
	 * 记录当前自己定义Btn是否按下
	 */
	private boolean clickdown = false;
	
	/**
	 * 下拉刷新的回调接口
	 */
	private LongTouchListener mListener;
	
	/**
	 * button长按时 间隔多少毫秒来处理 回调方法 
	 */
	private int mtime;
	
	/**
	 * 构造函数
	 * @param context
	 * @param attrs
	 */
	public LongTouchBtn(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 处理touch事件
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			clickdown = true;
			new LongTouchTask().execute();

			Log.i("huahua", "按下");
		}
		else if(event.getAction() == MotionEvent.ACTION_UP)
		{
			clickdown = false;
			Log.i("huahua", "弹起");
		}
		return super.onTouchEvent(event);
	}

	/**
	 * 使当前线程睡眠指定的毫秒数。
	 * 
	 * @param time
	 *            指定当前线程睡眠多久，以毫秒为单位
	 */
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 处理长按的任务
	 */
	class  LongTouchTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			while(clickdown)
			{
				sleep(mtime);
				publishProgress(0);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			mListener.onLongTouch();
		}
		
	}
	
	/**
	 * 给长按btn控件注冊一个监听器。
	 * 
	 * @param listener
	 *            监听器的实现。

	 * @param time
	 *            多少毫秒时间间隔 来处理一次回调方法
	 */
	public void setOnLongTouchListener(LongTouchListener listener, int time) {
		mListener = listener;
		mtime = time;
		
	}
	
	/**
	 * 长按监听接口，使用button长按的地方应该注冊此监听器来获取回调。


	 */
	public interface LongTouchListener {

		/**
		 * 处理长按的回调方法
		 */
		void onLongTouch();
	}
}
