package com.xcs.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xcs.coolweather.R;
import com.xcs.coolweather.db.CoolWeatherDB;
import com.xcs.coolweather.model.City;
import com.xcs.coolweather.model.County;
import com.xcs.coolweather.model.Province;
import com.xcs.coolweather.util.HttpCallbackListener;
import com.xcs.coolweather.util.HttpUtil;
import com.xcs.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xcs on 2017/3/9 0009.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog mProgressDialog;
    private TextView mTitleText;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private CoolWeatherDB mCoolWeatherDB;
    private List<String> mDataList = new ArrayList<>();

    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;

    private Province mSelectedProvince;
    private City mSelectCity;
    private int mCurrentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("city_selected",false)){
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        mListView = (ListView)findViewById(R.id.list_view);
        mTitleText = (TextView)findViewById(R.id.title_text);
        mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mDataList);
        mListView.setAdapter(mAdapter);
        mCoolWeatherDB = CoolWeatherDB.getInstance(this);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCurrentLevel == LEVEL_PROVINCE){
                    mSelectedProvince = mProvinceList.get(position);
                    queryCities();
                }else if (mCurrentLevel == LEVEL_CITY){
                    mSelectCity = mCityList.get(position);
                    queryCounties();
                }else if (mCurrentLevel == LEVEL_COUNTY){
                    String countyCode = mCountyList.get(position).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces(){
        mProvinceList = mCoolWeatherDB.loadProvinces();
        if (mProvinceList.size() >0){
            mDataList.clear();
            for (Province p: mProvinceList
                 ) {
                mDataList.add(p.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mTitleText.setText("中国");
            mCurrentLevel = LEVEL_PROVINCE;
        }else{
            queryFromServer(null,"province");
        }
    }

    private void queryCities(){
        mCityList = mCoolWeatherDB.loadCities(mSelectedProvince.getId());
        if (mCityList.size() >0){
             mDataList.clear();
            for (City c: mCityList
                    ) {
                mDataList.add(c.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mTitleText.setText(mSelectedProvince.getProvinceName());
            mCurrentLevel = LEVEL_CITY;
        }else{
            queryFromServer(mSelectedProvince.getProvinceCode(),"city");
        }
    }

    private void queryCounties(){
        mCountyList = mCoolWeatherDB.loadCounties(mSelectCity.getId());
        if (mCountyList.size() >0){
            mDataList.clear();
            for (County c: mCountyList
                    ) {
                mDataList.add(c.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mTitleText.setText(mSelectCity.getCityName());
            mCurrentLevel = LEVEL_COUNTY;
        }else{
            queryFromServer(mSelectCity.getCityCode(),"county");
        }
    }

    private void queryFromServer(final String code,final String type) {
        String address;
       if (!TextUtils.isEmpty(code)){
           address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
       }else{
           address = "http://www.weather.com.cn/data/list3/city.xml";
       }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvincesResponse(mCoolWeatherDB,response);
                }else if ("city".equals(type)){
                    result = Utility.handleCitiesResponse(mCoolWeatherDB,response,mSelectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountiesResponse(mCoolWeatherDB,response,mSelectCity.getId());
                }
                if (result){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if (mProgressDialog == null){
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }
    
    private void closeProgressDialog(){
     if (mProgressDialog != null){
         mProgressDialog.dismiss();
     }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentLevel == LEVEL_COUNTY){
            queryCities();
        }else if (mCurrentLevel == LEVEL_CITY){
            queryProvinces();
        }else{
            finish();
        }
    }
}
