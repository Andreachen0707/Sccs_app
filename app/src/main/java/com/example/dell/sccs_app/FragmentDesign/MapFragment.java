package com.example.dell.sccs_app.FragmentDesign;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Text;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.example.dell.sccs_app.Add_lamp;
import com.example.dell.sccs_app.Project;
import com.example.dell.sccs_app.R;
import com.example.dell.sccs_app.Util.DensityUtil;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.common.StringUtils;

import java.util.List;

import static com.example.dell.sccs_app.StaticValue.StationData;
import static com.example.dell.sccs_app.StaticValue.addaction;

/**
 * Created by dell on 2017/5/4.
 */

public class MapFragment extends Fragment {
    private MapView mMapView = null;
    private BaiduMap mBaidumap;

    private com.getbase.floatingactionbutton.FloatingActionButton mAddLamp;
    private com.getbase.floatingactionbutton.FloatingActionButton mAddControllor;

    private double mlongitude;
    private double mlatitude;
    private LocationClient mLocationClient;
    private LocationManager locationManager;
    private String provider;
    private boolean ifFrist = true;

    private View mView;
    private boolean modeFlag = true;
    private float zoomLevel;
    private MyLocationListener mLocationListener;
    private boolean isFirstLocation = true;

    private com.getbase.floatingactionbutton.FloatingActionButton scan;
    private com.getbase.floatingactionbutton.FloatingActionButton input;

    private EditText NAME;
    private EditText UID;
    private EditText GPS_1;
    private EditText GPS_2;
    private Button ok;
    private Button cancel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SDKInitializer.initialize(getActivity().getApplicationContext());
        mView = inflater.inflate(R.layout.map_fragment, container, false);

        mAddLamp = (com.getbase.floatingactionbutton.FloatingActionButton) mView.findViewById(R.id.addItemLamp);
        mAddControllor = (com.getbase.floatingactionbutton.FloatingActionButton) mView.findViewById(R.id.addItemControl);

        initMap();
        initLocation();
        //currentLocation();
        //初始化以后要获得一系列的参数 去获取集中器和灯的地理位置 然后标记在地图上 check!
        Handler info = new Handler();
        info.postDelayed(new maphandler(),2000);


        //添加新集中器时标记的方式



        mAddLamp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                addaction = 0;
                Add_scan_show();
            }
        });

        mAddControllor.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                addaction = 1;
                Add_scan_show();
                // Fragment_Project.notify();
            }
        });
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
    }

    private void initMap() {
        //获取地图控件引用
        mMapView = (MapView) mView.findViewById(R.id.baidu_map);
        // 不显示缩放比例尺
        mMapView.showZoomControls(false);
        // 不显示百度地图Logo
        mMapView.removeViewAt(1);
        //百度地图
        mBaidumap = mMapView.getMap();
        mBaidumap.setMyLocationEnabled(true);
        // 改变地图状态
        MapStatus mMapStatus = new MapStatus.Builder().zoom(15).build();
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaidumap.setMapStatus(mMapStatusUpdate);
        //设置地图状态改变监听器
        mBaidumap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus arg0) {
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus arg0) {
            }

            @Override
            public void onMapStatusChange(MapStatus arg0) {
                //当地图状态改变的时候，获取放大级别
                zoomLevel = arg0.zoom;
            }
        });
        mBaidumap.setOnMarkerClickListener(markerClick);
    }

    private void initLocation() {
        //定位客户端的设置
        mLocationClient = new LocationClient(getActivity());
        mLocationListener = new MyLocationListener();
        //注册监听
        mLocationClient.registerLocationListener(mLocationListener);
        //配置定位
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");//坐标类型
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//打开Gps
        option.setScanSpan(1000);//1000毫秒定位一次
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    private class maphandler implements Runnable
    {
        public void run()
        {
            if(StationData.size()!=0) {
                for(int t = 0;t<StationData.size();t++) {
                    double slatitude = StationData.get(t).getLat();
                    double slongitude = StationData.get(t).getLng();

                    LatLng sourceLatLng = new LatLng(slatitude,slongitude);
                    CoordinateConverter converter  = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS);
                    // sourceLatLng待转换坐标
                    converter.coord(sourceLatLng);
                    LatLng desLatLng = converter.convert();
                    slatitude = desLatLng.latitude;
                    slongitude = desLatLng.longitude;

                    mapAnnotation(StationData.get(t).getName(),slatitude,slongitude);
                }
            }
            else
                Toast.makeText(getActivity(),"Pull to refresh",Toast.LENGTH_SHORT).show();
        }
    }

    public static double convertToDouble(String number, double defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            return defaultValue;
        }

    }

    private void Add_scan_show(){
        /**
         *  Intent intent = new Intent();
         *  intent.setClass(getActivity(),Add_lamp.class);
         *  startActivity(intent);
         */
        final Dialog bottomDialog = new Dialog(getActivity(), R.style.BottomDialog);
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.activity_add_lamp, null);
        bottomDialog.setContentView(contentView);
        bottomDialog.setCanceledOnTouchOutside(true);

        scan = (com.getbase.floatingactionbutton.FloatingActionButton)contentView.findViewById(R.id.scan);
        input = (com.getbase.floatingactionbutton.FloatingActionButton)contentView.findViewById(R.id.input);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels - DensityUtil.dp2px(getActivity(), 16f);
        params.bottomMargin = DensityUtil.dp2px(getActivity(), 8f);
        contentView.setLayoutParams(params);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
        bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        bottomDialog.show();

        scan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setClass(getActivity(), CaptureActivity.class);
                startActivityForResult(intent,addaction);
            }
        });

        input.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Commit_show("");
            }
        });
    }

    private void Commit_show(String res){
        final Dialog bottomDialog = new Dialog(getActivity(), R.style.BottomDialog);
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.activity_commit, null);
        bottomDialog.setContentView(contentView);

        NAME = (EditText) contentView.findViewById(R.id.Text);
        UID = (EditText) contentView.findViewById(R.id.Text2);
        GPS_1 = (EditText) contentView.findViewById(R.id.Text3);
        GPS_2 = (EditText) contentView.findViewById(R.id.Text4);
        ok = (Button) contentView.findViewById(R.id.okButton);
        cancel = (Button) contentView.findViewById(R.id.cancelButton);

        String str ="000000000000";
        res=str.substring(0, 12-res.length())+res;

        UID.setText(res);
        GPS_1.setText(String.valueOf(mlatitude));
        GPS_2.setText(String.valueOf(mlongitude));

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels - DensityUtil.dp2px(getActivity(), 16f);
        params.bottomMargin = DensityUtil.dp2px(getActivity(), 8f);
        contentView.setLayoutParams(params);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
        bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        bottomDialog.show();

        ok.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //上传到服务器同时更新
                mapAnnotation(NAME.getText().toString(),convertToDouble(GPS_1.getText().toString(),0.00),convertToDouble(GPS_2.getText().toString(),0.00));
            }
        });

        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //cancel
                bottomDialog.dismiss();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((resultCode == 0x123)) {
                if (null != data) {
                    Bundle bundle = data.getExtras();
                    if (bundle == null)
                        return;

                    else {
                        String res = bundle.getString("scan_result");
                        switch (requestCode) {
                            case 0:
                                Log.i("scan_test","in");
                                Commit_show(res);
                                break;

                            case 1:
                               Commit_show(res);
                                break;
                            default:
                                break;

                    }
                }
            }

        }
    }


    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //将获取的location信息给百度map
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            mBaidumap.setMyLocationData(data);
            if (isFirstLocation) {
                //获取经纬度
                mlatitude = location.getLatitude();
                mlongitude = location.getLongitude();
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                //Toast.makeText(getActivity(),latitude+" "+latitude,Toast.LENGTH_SHORT).show();
                Log.i("location", mlatitude+" "+mlongitude);
                MapStatusUpdate status = MapStatusUpdateFactory.newLatLng(ll);

                mBaidumap.animateMapStatus(status);//动画的方式到中间
                isFirstLocation = false;
                //showInfo("位置：" + location.getAddrStr());
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }


    }

    public void mapAnnotation(String name,double lat,double lng)
    {
        Marker marker = null;
        //定义Maker坐标点
        LatLng point = new LatLng(lat, lng);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.ic_icon_controler_gray);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap);
        Bundle bundle = new Bundle();
        bundle.putSerializable("info", name);
        //在地图上添加Marker，并显示
        marker = (Marker) mBaidumap.addOverlay(option);
        marker.setExtraInfo(bundle);
    }

    BaiduMap.OnMarkerClickListener markerClick = new BaiduMap.OnMarkerClickListener() {
        /**
         * 地图 Marker 覆盖物点击事件监听函数
         * @param marker 被点击的 marker
         */
        public boolean onMarkerClick(Marker marker){
            InfoWindow mInfoWindow;

            Bundle res = marker.getExtraInfo();
            String a = res.getString("info");
            TextView location = new TextView(getActivity().getApplicationContext());
            location.setPadding(30, 20, 30, 50);
            location.setText(a);
            final LatLng ll = marker.getPosition();
            android.graphics.Point p = mBaidumap.getProjection().toScreenLocation(ll);
            p.y -= 50;
            LatLng llInfo = mBaidumap.getProjection().fromScreenLocation(p);
            //为弹出的InfoWindow添加点击事件
            mInfoWindow = new InfoWindow(location, llInfo, 0);
            //显示InfoWindow
            mBaidumap.showInfoWindow(mInfoWindow);
            return false;
        }
    };


    @Override
    public void onDestroy() {

        super.onDestroy();

        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理

        mMapView.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理

        mMapView.onPause();
    }
}



