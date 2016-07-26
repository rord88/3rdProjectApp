package com.ktds.queuing_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.estimote.sdk.SystemRequirementsChecker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.ktds.queuing_app.db.DBHelper;
import com.ktds.queuing_app.gcm.RegistrationIntentService;
import com.ktds.queuing_app.util.HttpClient;
import com.ktds.queuing_app.util.beacon.ActivityButtonController;
import com.ktds.queuing_app.util.beacon.BeaconManagerStore;
import com.ktds.queuing_app.util.beacon.BeaconRagingMonitoring;
import com.ktds.queuing_app.util.beacon.MyApplication;
import com.ktds.queuing_app.vo.QueuingVO;

public class MainActivity extends ActionBarActivity {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String TAG = "MainActivity";

    private BeaconRagingMonitoring beaconRagingMonitoring;

    private DBHelper dbHelper;

    private NumberTask numberTask;

    private Button btnQueuing;
    private Button btnQueuingCancle;
    private TextView tvNumber;

    private final BroadcastReceiver abcd = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(abcd, new IntentFilter("xyz"));

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);    //서비스 실행
        }


        btnQueuingCancle = (Button) findViewById(R.id.btnQueuingCancle);

        btnQueuing = (Button) findViewById(R.id.btnQueuing);
        btnQueuing.setEnabled(false);

        final SharedPreferences sharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);
        String queueNumber = sharedPreferences.getString("queueNumber", "0");

        tvNumber = (TextView) findViewById(R.id.tvNumber);
        tvNumber.setText(queueNumber);

        String buttonName = sharedPreferences.getString("buttonName", btnQueuing.getText().toString());

        boolean isIssuedToken = sharedPreferences.getBoolean("isIssuedToken", false);

        //버튼 비활성화
        //TODO : 다른기능들 완성되면 주석 풀기
        btnQueuing.setText(buttonName);

        BeaconManagerStore.getInstance().isIssuedToken = isIssuedToken;

        btnQueuingCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BeaconManagerStore.getInstance().isIssuedToken = false;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("queueNumber");
                editor.remove("isIssuedToken");
                editor.remove("buttonName");
                editor.commit();

                btnQueuing.setText(getString(R.string.btnName));
                tvNumber.setText("번호를 발급해주세요.");

                BeaconManagerStore.getInstance().startRangingAllBeacon(new ActivityButtonController() {
                    @Override
                    public void buttonEnable() {
                        btnQueuing.setEnabled(true);
                    }

                    @Override
                    public void buttonDisable() {
                        btnQueuing.setEnabled(false);
                    }

                    // 개선필요
                    @Override
                    public void action(QueuingVO queuingVO) {
                        if(dbHelper == null) {
                            dbHelper = new DBHelper(MainActivity.this, "Queuing", null, DBHelper.DB_VERSION);
                        }
                        dbHelper.setQueuingInfo(queuingVO);
                    }
                });

                btnQueuingCancle.setVisibility(View.GONE);
            }
        });

        if ( isIssuedToken ) {
            btnQueuingCancle.setVisibility(View.VISIBLE);
        }

        beaconRagingMonitoring = new BeaconRagingMonitoring();

        btnQueuing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueuingVO queuingVO = BeaconManagerStore.getInstance().getQueuingVO();
                numberTask = new NumberTask();
                numberTask.execute(queuingVO.getRegId(), queuingVO.getBranchId());
            }
        });



    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //블루투스 권한 승낙 및 블루투스 활성화 시키는 코드
        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        beaconRagingMonitoring.start(new ActivityButtonController() {
            @Override
            public void buttonEnable() {
                btnQueuing.setEnabled(true);
            }

            @Override
            public void buttonDisable() {
                btnQueuing.setEnabled(false);
            }

            // 개선필요
            @Override
            public void action(QueuingVO queuingVO) {
                if(dbHelper == null) {
                    dbHelper = new DBHelper(MainActivity.this, "Queuing", null, DBHelper.DB_VERSION);
                }
                dbHelper.setQueuingInfo(queuingVO);
            }
        });
    }

    @Override
    protected void onPause() {
        // 어플리케이션 내부 이외에 더이상 모니터링 하지 않겠다는 것임.
        //beaconManager.stopRanging(region);

        SharedPreferences sharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("queueNumber", tvNumber.getText().toString());
        editor.putString("buttonName", btnQueuing.getText().toString());
        editor.putBoolean("isIssuedToken", BeaconManagerStore.getInstance().isIssuedToken);
        editor.commit();

        super.onPause();
    }


    //    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if ( requestCode == 10000 ) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
//                dialog.setTitle("알림")
//                        .setMessage("어플리케이션을 사용할 수 없습니다.")
//                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                MainActivity.this.finish();
//                            }
//                        })
//                        .create()
//                        .show();
//            }
//        }
//    }

    // TODO : 보류
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(abcd);
        BeaconManagerStore.getInstance().stopRaging();
        MyApplication.stopMonitoring();
    }

    public class NumberTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            String regId = params[0];
            String branchId = params[1];

            HttpClient.Builder client = new HttpClient.Builder("POST", "http://172.20.10.5:8080/Queuing-web/m/getNewToken");

            client.addOrReplaceParameter("regId", regId);
            client.addOrReplaceParameter("branchId", branchId);

            Log.d("TestVO", regId);
            Log.d("TestVO", branchId);

            HttpClient post = client.create();
            post.request();
            String body = post.getBody();

            return body;
        }

        /**
         * { token : "100" , tokenId : "지점" }
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            Log.d("TestVO", s + "-----------------");
            Gson gson = new Gson();
            QueuingVO queuingVO = gson.fromJson(s, QueuingVO.class);

            String number = queuingVO.getToken();

            if (number.equals("0")) {
                //레지아이디 새로생성해서 다시받아서 다시보내기

            } else {

                BeaconManagerStore.getInstance().getQueuingVO().setToken(number);
                BeaconManagerStore.getInstance().stopRaging();
                MyApplication.stopMonitoring();

                tvNumber.setText(BeaconManagerStore.getInstance().getQueuingVO().getToken());
                btnQueuing.setEnabled(false);
                btnQueuingCancle.setVisibility(View.VISIBLE);
                BeaconManagerStore.getInstance().isIssuedToken = true;
                if ( BeaconManagerStore.getInstance().isIssuedToken ) {
                    btnQueuing.setText("호출 대기중..");
                }
            }
        }
    }
}
