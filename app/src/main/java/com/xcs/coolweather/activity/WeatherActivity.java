package com.xcs.coolweather.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xcs.coolweather.R;
import com.xcs.coolweather.util.HttpCallbackListener;
import com.xcs.coolweather.util.HttpUtil;
import com.xcs.coolweather.util.Utility;

/**
 * Created by xcs on 2017/3/9 0009.
 */
public class WeatherActivity extends Activity {
    private LinearLayout mWeatherInfoLayout;
    private TextView mCityNameText;
    private TextView mPublishText;
    private TextView mWeatherDespText;
    private TextView mTemp1Text;
    private TextView mTemp2Text;
    private TextView mCurrentDateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);
        mWeatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
        mCityNameText = (TextView) findViewById(R.id.city_name);
        mPublishText = (TextView) findViewById(R.id.publish_text);
        mWeatherDespText = (TextView)findViewById(R.id.weather_desp);
        mTemp1Text = (TextView) findViewById(R.id.temp1);
        mTemp2Text = (TextView) findViewById(R.id.temp2);
        mCurrentDateText = (TextView)findViewById(R.id.current_date);
        String countyCode = getIntent().getStringExtra("county_code");
        if (!TextUtils.isEmpty(countyCode)){
            mPublishText.setText("同步中...");
            mWeatherInfoLayout.setVisibility(View.INVISIBLE);
            mCityNameText.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);
        }else{
            showWeather();
        }
    }

    private void showWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCityNameText.setText(preferences.getString("city_name",""));
        mTemp1Text.setText(preferences.getString("temp1",""));
        mTemp2Text.setText(preferences.getString("temp2",""));
        mWeatherDespText.setText(preferences.getString("weather_desp",""));
        mPublishText.setText("今天"+preferences.getString("publish_time","")+"发布");
        mCurrentDateText.setText(preferences.getString("current_date",""));
        mWeatherInfoLayout.setVisibility(View.VISIBLE);
        mCityNameText.setVisibility(View.VISIBLE);
    }

    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city"+countyCode+".xml";
        queryFromServer(address,"countyCode");
    }

    private void queryWeatherInfo(String weatherCode){
        String address = "http://www.weather.com.cn/data/cityinfo/"+weatherCode +".html";
        queryFromServer(address,"weatherCode");
    }

    private void queryFromServer(String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                if ("countyCode".equals(type)){
                    if (!TextUtils.isEmpty(response)){
                        String[] array = response.split("\\|");
                        if (array != null && array.length ==2){
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }else if ("weatherCode".equals(type)){
                        Utility.handleWeatherResponse(WeatherActivity.this,response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showWeather();
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                 runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         mPublishText.setText("同步失败");
                     }
                 });
            }
        });
    }
}
