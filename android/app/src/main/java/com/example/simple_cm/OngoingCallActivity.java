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
        OngoingCallData.OngoingCallDataActivity = this;
        
        ImageButton rejectButton = (ImageButton) findViewById(R.id.reject);
        rejectButton.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                new OngoingCallData().dismiss();
                finish();
            }
        });

        TextView displayName = (TextView)findViewById(R.id.displayName);
        displayName.setText(new OngoingCallData().getCallerDisplayName(getContentResolver()));
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