package com.android.hearwego;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Network;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.type.LatLng;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TmapAuthentication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

public class RouteGuideActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback{

    private View decorView; //full screen 객체 선언
    private int	uiOption; //full screen 객체 선언

    /*텍스트뷰 선언*/
    TextView destination_text;
    TextView guide_text;
    TextView reDistance_text;

    String appKey = "l7xx59d0bb77ddfc45efb709f48d1b31715c"; //appKey

    /*TMAP 필요 변수 선언*/
    TMapGpsManager tMapGpsManager = null;
    TMapView tMapView = null;
    TMapData tMapData = null;
    TMapPoint nowPoint = null;

    /*TTS 변수 설정*/
    TextToSpeech textToSpeech;

    /*버튼 선언*/
    Button nowgps_btn; //현재 위치 확인 버튼
    Button setStart_btn; //출발지 지정 버튼
    Button button_previous; //이전 버튼
    Button button_home; //홈 버튼

    /*경도, 위도 변수 선언*/
    Double latitude;
    Double longitude;
    String reDistnace;

    /*JSON 받아오기 위한 변수 선언*/
    String startX;
    String startY;
    String endX;
    String endY;
    String latData;
    String longData;
    String uu = null;
    URL url = null;
    HttpURLConnection urlConnection = null;

    /*JSON 변수 선언*/
    JSONObject root = null;
    ArrayList<TMapPoint> LatLngArrayList = new ArrayList<TMapPoint>();
    ArrayList<String> DescriptionList = new ArrayList<String>();

    /*실시간 음성안내를 위한 변수 선언*/
    int index = 0;
    int check = 0;
    int ep_distance = 0;
    Double g_latitude = 0.0;
    Double g_longitude = 0.0;
    String description = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_guide);

        ActionBar actionBar = getSupportActionBar(); //액션바(패키지명) 숨김처리
        actionBar.hide();

        /*전체 화면 모드 -> 소프트 키 없앰*/
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH )
            uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN )
            uiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
            uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility( uiOption );

        /*TextToSpeech 기본 설정*/
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.KOREAN);
                }
            }
        });

        /*TMAP 기본 설정*/
        tMapData = new TMapData();
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(appKey);
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setMinTime(1000);
        tMapGpsManager.setMinDistance(5);
        tMapGpsManager.setProvider(tMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();

        /*버튼 설정*/
        nowgps_btn = findViewById(R.id.button2_nowgps);
        setStart_btn = findViewById(R.id.button_setStartPoint);
        button_previous = findViewById(R.id.previous); //이전 이미지 버튼 객체 참조
        button_home = findViewById(R.id.home); // 홈 이미지 버튼 객체 참조

        /*인텐트 받아들임*/
        Intent intent = getIntent();
        String nameData = intent.getStringExtra("name");
        latData = intent.getStringExtra("latitude");
        longData = intent.getStringExtra("longitude");
        //Double latitude = intent.getDoubleExtra("latitude", 0);
        //Double longitude = intent.getDoubleExtra("longitude", 0);

        destination_text = findViewById(R.id.destination_text);
        destination_text.setText(nameData);
        guide_text = findViewById(R.id.guide_message);
        reDistance_text = findViewById(R.id.distance);

        /*현재 위치 확인 버튼 누를 시*/
        nowgps_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TMapPoint nowpoint = tMapView.getLocationPoint();

                tMapData.convertGpsToAddress(nowpoint.getLatitude(), nowpoint.getLongitude(), new TMapData.ConvertGPSToAddressListenerCallback() {
                    @Override
                    public void onConvertToGPSToAddress(String s) {
                        textToSpeech.setPitch(1.5f);
                        textToSpeech.setSpeechRate(1.0f);
                        textToSpeech.speak("현재 위치는 "+s+"입니다", TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
            }
        });

        /*출발지 설정 확인 버틀 누를 시*/
        setStart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TMapPoint nowpoint = tMapView.getLocationPoint();
                latitude = nowpoint.getLatitude();
                longitude = nowpoint.getLongitude();

                getRoute();
            }
        });

        //이전 버튼 누를 시 화면 전환
        button_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //홈 버튼 누를 시 화면 전환
        button_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RouteGuideActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    /*보행자 경로 JSON 파일을 가져오는 함수*/
    public void getRoute(){

        startX = Double.toString(longitude);
        startY = Double.toString(latitude);
        endX = longData;
        endY = latData;

        Log.d("JSON확인", startX+startY+endX+endY);

        try {
            String startName = URLEncoder.encode("출발지", "UTF-8");
            String endName = URLEncoder.encode("도착지", "UTF-8");

            System.out.println(endX + endY);

            uu = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&callback=result&appKey=" + appKey
                    + "&startX=" + startX + "&startY=" + startY + "&endX=" + endX + "&endY=" + endY
                    + "&startName=" + startName + "&endName=" + endName;

            System.out.println(uu);
            url = new URL(uu);


        } catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Accept-Charset", "utf-8");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Log.d("JSON확인", urlConnection.toString());
            NetworkTask networkTask = new NetworkTask(uu, null);
            networkTask.execute();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /*Asynctask 클래스 NetworkTask 생성*/
    public class NetworkTask extends AsyncTask<Void, Void, String>{
        private String url;
        private ContentValues values;


        public NetworkTask(String url, ContentValues values){
            this.url = url;
            this.values = values;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            String result; //요청 결과를 저장하는 변수
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values); //url로부터 결과를 얻어온다.
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.
            try{

                //전체 데이터를 제이슨 객체로 변환
                root = new JSONObject(s);
                Log.d("JSON확인", "제일 상위" + root);

                JSONArray featuresArray = root.getJSONArray("features"); //총 경로 횟수를 featuresArray에 저장
                Log.d("JSON확인-feaIndex", Integer.toString(featuresArray.length()));
                for(int i = 0;i<featuresArray.length();i++){
                    JSONObject featuresIndex = (JSONObject)featuresArray.get(i);
                    //Log.d("JSON확인-feaIndex", featuresIndex.toString());
                    JSONObject geometry = featuresIndex.getJSONObject("geometry");
                    JSONObject properties = featuresIndex.getJSONObject("properties");
                    //Log.d("JSON확인-geometry", geometry.toString());
                    String type = geometry.getString("type");
                    //Log.d("JSON확인-type", type);
                    JSONArray coordinatesArray = geometry.getJSONArray("coordinates");
                    //Log.d("JSON확인-coordinates", coordinatesArray.toString());
                    //type이 LineString일 경우
                    //coordinates의 length가 2 또는 3
                    //length() == 2라면, 인덱스가 1인것을 coordinates에 저장
                    //length() == 3도 마찬가지로, 인덱스가 1인것을 coordintates에 저장
                    /*if(i == featuresArray.length()-2){
                        JSONArray pointArray = (JSONArray)coordinatesArray.get(1);
                        //Log.d("JSON확인-pointArray1", pointArray.toString());
                        Double f_longitude = Double.parseDouble(pointArray.get(0).toString());
                        Double f_latitude = Double.parseDouble(pointArray.get(1).toString());
                        //Log.d("JSON확인-feaIndex3", f_latitude.toString() + f_longitude.toString());
                        LatLngArrayList.add(new TMapPoint(f_latitude, f_longitude));
                    }*/

                    if(type.equals("Point")){
                        //type이 point일 경우, coordinates의 length는 1밖에 없음
                        //Log.d("JSON확인-pointArray2", coordinatesArray.toString());
                        Double f_longitude = Double.parseDouble(coordinatesArray.get(0).toString());
                        Double f_latitude = Double.parseDouble(coordinatesArray.get(1).toString());
                        String description = properties.getString("description");
                        DescriptionList.add(description);
                        LatLngArrayList.add(new TMapPoint(f_latitude, f_longitude));
                        if(i==featuresArray.length()-1){
                            LatLngArrayList.add(new TMapPoint(f_latitude, f_longitude));
                        }
                    }
                }

                System.out.println("JSON확인-distance"+ ep_distance);
                System.out.println("JSON확인-feaIndex2" + LatLngArrayList);
                System.out.println("JSON확인-description" + DescriptionList);


                /*첫번째 설명, 남은 거리 구하기 위함*/
                description = DescriptionList.get(0);
                guide_text.setText(description);
                g_latitude = LatLngArrayList.get(1).getLatitude(); //위도
                g_longitude = LatLngArrayList.get(1).getLongitude(); //경도
                reDistnace = calcDistance(latitude, longitude, g_latitude, g_longitude);
                reDistance_text.setText(reDistnace + "m");
                //Log.d("JSON확인", reDistnace);


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /*남은 거리 구하는 함수*/
    public String calcDistance(double lat1, double long1, double lat2, double long2){
        double EARTH_R, Rad, radLat1, radLat2, radDist;
        double distance, ret;

        EARTH_R = 6372.8 * 1000;
        Rad = Math.PI/180;
        radLat1 = Rad * lat1;
        radLat2 = Rad * lat2;
        radDist = Rad * (long1 - long2);

        distance = Math.sin(radLat1) * Math.sin(radLat2);
        distance = distance + Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radDist);
        ret = EARTH_R * Math.acos(distance);

        double rslt = Math.round(ret);
        String result = Double.toString(rslt);

        return result;
    }

    /*사용자 위치가 변경되면 실행되는 함수*/
    @Override
    public void onLocationChange(Location location) {
        //현재 위치의 위도, 경도를 받아옴
        tMapView.setLocationPoint(location.getLongitude(), location.getLatitude());
        tMapView.setCenterPoint(location.getLongitude(), location.getLatitude());
        nowPoint = tMapView.getLocationPoint();
         //Log.d("현재위치-RouteGuide", nowPoint.toString());
        latitude = nowPoint.getLatitude();
        longitude = nowPoint.getLongitude();
         //Log.d("JSON확인1", latitude.toString() + longitude.toString());
        if(root != null){
            getDescription();
        }
    }

    public void getDescription(){
        Double latitude_gap = 0.0;
        Double longitude_gap = 0.0;

        if(index < LatLngArrayList.size()-1){;
            if(index == LatLngArrayList.size()-2){
                Log.d("JSON확인-check", Integer.toString(check));
                tMapGpsManager.CloseGps();
            }
            //Log.d("JSON실행?-4", "실행O");
            latitude_gap = Math.abs(latitude - g_latitude);
            longitude_gap = Math.abs(longitude - g_longitude);
            //Log.d("JSON실행?", "실행O");
            reDistnace = calcDistance(latitude, longitude, g_latitude, g_longitude);
            reDistance_text.setText(reDistnace+"m");
            Log.d("JSON실행-gLngLat", Double.toString(g_latitude) + Double.toString(g_longitude));
            //Log.d("JSON실행?-남은m", reDistnace+"m");

            if(latitude_gap <= 0.00001 || longitude_gap <= 0.00001){
                //Log.d("JSON실행?3", "실행됐니");
                g_latitude = LatLngArrayList.get(index+2).getLatitude();
                g_longitude = LatLngArrayList.get(index+2).getLongitude();
                reDistnace = calcDistance(latitude, longitude, g_latitude, g_longitude);
                reDistance_text.setText(reDistnace+"m");
                Log.d("JSON실행-gLngLat", Double.toString(g_latitude) + Double.toString(g_longitude));
                //Log.d("JSON실행?-남은m", reDistnace+"m");

                if(index < LatLngArrayList.size() -2){
                    guide_text.setText(DescriptionList.get(index+1));
                }

                if(index < LatLngArrayList.size()-2){
                    index = index +1;
                    check = check + 1;
                }
            }
        }



    }
    }



