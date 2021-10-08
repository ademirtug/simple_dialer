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

public class IncomingCallActivity extends AppCompatActivity {

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

        acceptButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                SimpleDialerCallService.csh.answer();

                Intent ongoingCallScreen = new Intent(IncomingCallActivity.this, OngoingCallActivity.class);
                startActivity(ongoingCallScreen);
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

        //System.out.println("INCOMING ACTIVITY ONCREATE END");
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