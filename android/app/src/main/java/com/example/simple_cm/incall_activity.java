package info.pakkudev.simple_dialer;
import androidx.appcompat.app.AppCompatActivity;

import android.view.*;
import android.widget.*;
import android.os.Bundle;
import android.telecom.VideoProfile;
import android.app.ActivityManager.AppTask ;
import android.os.Build.*;
import android.os.*;

public class incall_activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incall);

        Button acceptButton = (Button) findViewById(R.id.accept);
        acceptButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                //call.answer(VideoProfile.STATE_AUDIO_ONLY);
                new OngoingCall().answer();
                acceptButton.setVisibility(View.GONE);
            }
        });
        
        Button rejectButton = (Button) findViewById(R.id.reject);
        rejectButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                //call.answer(VideoProfile.STATE_AUDIO_ONLY);
                new OngoingCall().hangup();
                finish();
            }
        });

        TextView displayName = (TextView)findViewById(R.id.displayName);
        displayName.setText(new OngoingCall().getDisplayName()); 


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