package info.pakkudev.simple_dialer;
import androidx.appcompat.app.AppCompatActivity;

import android.view.*;
import android.widget.*;
import android.os.Bundle;
import android.app.ActivityManager.AppTask ;
import android.os.Build.*;
import android.os.*;
import android.telecom.*;

public class OngoingCallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_call);
        
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


        if(SimpleDialerCallService.csh.oca != null )
            SimpleDialerCallService.csh.oca.finish();

        SimpleDialerCallService.csh.oca = this;
        
        ImageButton rejectButton = (ImageButton) findViewById(R.id.reject);
        rejectButton.setOnClickListener( new View.OnClickListener() {
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
    }

    @Override
    public void finish() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        }
        else {
            super.finish();
        }
    }
}