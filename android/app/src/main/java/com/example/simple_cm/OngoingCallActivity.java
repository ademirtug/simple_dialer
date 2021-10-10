package info.pakkudev.simple_dialer;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.view.*;
import android.widget.*;
import android.os.Bundle;
import android.app.ActivityManager.AppTask ;
import android.os.Build.*;
import android.os.*;
import android.telecom.*;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.*;


import android.hardware.*;
import android.media.*;
import android.Manifest;

public class OngoingCallActivity extends AppCompatActivity implements SensorEventListener{


    AudioManager am = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        else{
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON 
            );
        }

        setContentView(R.layout.activity_ongoing_call);
       
        SimpleDialerCallService.csh.oca = this;
        
        ImageButton dismissButton = (ImageButton) findViewById(R.id.dismiss);
        dismissButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                SimpleDialerCallService.csh.dismiss();
                finish();
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
             
        //PROXIMITY LOUD SPEAKER
        SharedPreferences preferences = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE);
        SimpleDialerCallService.csh.autoSwitchLoudspeaker = preferences.getBoolean("flutter.autoSwitchLoudspeaker", false);
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApplicationContext().getSensorManager().registerListener(this, SimpleDialerCallService.csh.getProximitySensor()
        , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getApplicationContext().getSensorManager().unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!SimpleDialerCallService.csh.autoSwitchLoudspeaker)
            return;

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SimpleDialerCallService.csh.SENSOR_SENSITIVITY 
                && event.values[0] <= SimpleDialerCallService.csh.SENSOR_SENSITIVITY) {
                //near
                Toast.makeText(getApplicationContext(), "near", Toast.LENGTH_SHORT).show();
                switchLoudspeaker(false);
            } else {
                //far
                Toast.makeText(getApplicationContext(), "far", Toast.LENGTH_SHORT).show();
                switchLoudspeaker(true);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void switchLoudspeaker(boolean status){
        if(am == null)
            am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(status);
    }
    @Override
    public void finish() {
        if(SimpleDialerCallService.csh.ica != null){
            SimpleDialerCallService.csh.ica.finish();
            SimpleDialerCallService.csh.ica = null;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        }
        else {
            super.finish();
        }
    }
}