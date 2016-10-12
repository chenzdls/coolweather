package com.example.coolweather.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.coolweather.R;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpCallbackListener;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

public class WeatherActivity extends Activity implements OnClickListener {

	private LinearLayout weatherInfoLayout;
	private TextView cityNameText;
	private TextView publishText;
	private TextView weatherDescText;
	private TextView temp1Text;
	private TextView temp2Text;
	private TextView currentDateText;
	// 切换城市按钮
	private Button switchButton;
	// 更新天气按钮
	private Button refreshWeather;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		// 初始化各控件
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDescText = (TextView) findViewById(R.id.weather_desc);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		switchButton = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		switchButton.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);

		String countyCode = getIntent().getStringExtra("county_code");
		if (!TextUtils.isEmpty(countyCode)) {
			// 有县级代号时就去查询天气
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		} else {
			// 没有县级代号时就直接显示SharedPreferences中的天气信息
			showWeather();
		}

	}

	/**
	 * 查询县级天气对应的天气代号
	 */
	private void queryWeatherCode(String countyCode) {
		/* Log.d("WeatherActivity", "进入queryWeatherCode()方法"); */
		String address = "http://www.weather.com.cn/data/list3/city"
				+ countyCode + ".xml";
		queryFromServer(address, "countyCode");

	}

	/**
	 * 根据代号再去查询天气信息
	 */
	private void queryWeatherInfo(String weatherCode) {
		/* Log.d("WeatherActivity", "进入queryWeatherInfo()方法"); */
		String address = "http://www.weather.com.cn/data/cityinfo/"
				+ weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}

	/**
	 * 根据传入的地址和类型向服务器查询:1.天气代号 或 2.天气信息
	 */
	private void queryFromServer(final String address, final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String response) {
				if ("countyCode".equals(type)) {
					if (!TextUtils.isEmpty(response)) {
						// 从服务器返回的数据中解析出天气代号
						// 例如:050102|101050102中101050102就是黑龙江哈尔滨双城的天气代号
						String[] array = response.split("\\|");
						if (array != null && array.length == 2) {
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}
					}

				} else if ("weatherCode".equals(type)) {
					// 处理返回的天气信息
					Utility.handleWeatherResponse(WeatherActivity.this,
							response);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							showWeather();
						}

					});

				}

			}

			@Override
			public void onError(Exception e) {
				Log.d("WeatherActivity", "打印异常堆栈消息");
				e.printStackTrace();
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						publishText.setText("同步失败");
					}

				});

			}

		});

	}

	/**
	 * 从SharedPreferences文件中读取天气信息并显示到界面上
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDescText.setText(prefs.getString("weather_desc", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.switch_city: {
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
		}
		case R.id.refresh_weather: {
			publishText.setText("同步中");
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			String weatherCode = pref.getString("weather_code", "");
			if (!TextUtils.isEmpty(weatherCode)) {
				queryWeatherInfo(weatherCode);
			}
			break;
		}
		default:
			break;

		}

	}
}
