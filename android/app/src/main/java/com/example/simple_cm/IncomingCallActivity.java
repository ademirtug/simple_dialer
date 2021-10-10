package info.pakkudev.simple_dialer;
import androidx.appcompat.app.AppCompatActivity;

import android.view.*;
import android.widget.*;
import android.os.Bundle;
import android.app.ActivityManager.AppTask ;
import android.os.Build.*;
import android.os.*;
import android.telecom.*;
import android.content.*;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.*;
import android.graphics.*;
import java.io.*;
import android.hardware.*;
import android.media.*;

public class IncomingCallActivity extends AppCompatActivity implements SensorEventListener{
    boolean screenVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        }
        else{
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON 
            );
        }
        
        setContentView(R.layout.activity_incoming_call);

        ImageButton acceptButton = (ImageButton) findViewById(R.id.accept);
        SimpleDialerCallService.csh.ica = this;

        acceptButton.setOnClickListener( 
            new View.OnClickListener() {
            public void onClick(View v) {
                acceptCall();
            }
        });
        
        ImageButton dismissButton = (ImageButton) findViewById(R.id.dismiss);
        dismissButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                SimpleDialerCallService.csh.dismiss();

            }
        });

        String dn = SimpleDialerCallService.csh.getCallerDisplayName(getContentResolver()).trim();
        String pn = SimpleDialerCallService.csh.getCallPhoneNumber().trim();

        TextView currentAction = (TextView)findViewById(R.id.currentAction);
        currentAction.setText( SimpleDialerCallService.csh.isIncomingCall() ? R.string.incomingCall : R.string.makingCall);

        TextView displayName = (TextView)findViewById(R.id.displayName);
        displayName.setText(dn);

        if( dn.isEmpty() ){
            displayName.setText(pn);
        }
        if( !dn.equals(pn) ){
            TextView callingNumber = (TextView)findViewById(R.id.callingNumber);
            callingNumber.setText(pn);    
        }

        ImageView iv = findViewById(R.id.imageView);
        if(SimpleDialerCallService.csh.getCallerPhoto() != null){
            iv.setImageBitmap(SimpleDialerCallService.csh.getCallerPhoto());
        }

        //PROXIMITY AUTO ACCEPT
        SharedPreferences preferences = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE);
        SimpleDialerCallService.csh.autoAcceptCalls = preferences.getBoolean("flutter.autoAcceptCalls", false);
    }

    public void acceptCall(){
        SimpleDialerCallService.csh.answer();

        Intent ongoingCallScreen = new Intent(IncomingCallActivity.this, OngoingCallActivity.class);
        startActivity(ongoingCallScreen);
    }


    @Override
    protected void onResume() {
        super.onResume();
        SimpleDialerCallService.csh.getSensorManager().registerListener(this, SimpleDialerCallService.csh.getProximitySensor()
        , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SimpleDialerCallService.csh.getSensorManager().unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!SimpleDialerCallService.csh.autoAcceptCalls)
            return;

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SimpleDialerCallService.csh.SENSOR_SENSITIVITY 
                && event.values[0] <= SimpleDialerCallService.csh.SENSOR_SENSITIVITY) {
                if(screenVisible)
                    acceptCall();
            } else {
                screenVisible = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    public void finish() {
        //System.out.println("IncomingCall::finish()");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        }
        else {
            super.finish();
        }
    }

}