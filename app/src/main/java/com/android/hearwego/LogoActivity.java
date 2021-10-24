package com.android.hearwego;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import org.jetbrains.annotations.NotNull;

public class LogoActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private View decorView; //full screen 객체 선언
    private int	uiOption; //full screen 객체 선언
    private SignInButton btn_Google; //구글 로그인 버튼
    private FirebaseAuth auth; //firebase 인증 객체
    private GoogleApiClient googleApiClient; //구글 API 클라리언트 객체
    private static final int REQ_SIGN_GOOGLE = 100; //구글 로그인 결과 코드
    public static Context context_logo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logo);

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } //gps 기능 permission request

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
        auth = FirebaseAuth.getInstance(); //파이어베이스 인증 객체 초기화
        btn_Google = findViewById(R.id.btn_Google);
        btn_Google.setOnClickListener(new View.OnClickListener() { //구글 로그인 버튼 클릭시 동작
            @Override
            public void onClick(View v) {
                Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(intent, REQ_SIGN_GOOGLE);
            }
        });
    }
    //구글 로그인 요청했을 때 결과값을 되돌려받는 곳
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_SIGN_GOOGLE){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){ //인증 결과가 성공적이면 실행
                GoogleSignInAccount account = result.getSignInAccount(); //구글 로그인 정보를 담고 있는 변수 (닉네임, 이메일 주소, 등)
                resultLogin(account); //로그인 결과 값 출력 수행하는 메소드 호출
            }
        }
    }
    //로그인 결과 값 출력 수행하는 메소드
    private void resultLogin(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<AuthResult> task) { //최종 구글 로그인 성공 여부 검사
                        if(task.isSuccessful()){ //로그인이 성공했으면
                            Toast.makeText(LogoActivity.this, "환영합니다", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                            intent.putExtra("name", account.getDisplayName());
                            intent.putExtra("imageurl", String.valueOf(account.getPhotoUrl())); //String.valueOf(): 특정 자료형을 String 형태로 변환시키는 방법
                            startActivity(intent);
                        } else{ //로그인이 실패했으면
                            Toast.makeText(LogoActivity.this, "로그인 실패", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        context_logo = this;
    }

    @Override
    public void onConnectionFailed(@NonNull @org.jetbrains.annotations.NotNull ConnectionResult connectionResult) {
        
    }
    //로그아웃 함수
    public void withdraw() {
        auth.getCurrentUser().delete();
    }
}

