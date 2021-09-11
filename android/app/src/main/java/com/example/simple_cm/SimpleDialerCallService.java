package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.os.PowerManager;
import android.content.*;

public class SimpleDialerCallService extends InCallService {
    PowerManager.WakeLock wakeLock, proximityLock;


    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        new OngoingCallData().setCall(call);
        if(call.getDetails().getCallDirection() == Call.Details.DIRECTION_OUTGOING){
            Intent ongoingCallScreen = new Intent(this, OngoingCallActivity.class);
            ongoingCallScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(ongoingCallScreen);
        }else{
                    
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SD WAKE LOCK");
            wakeLock.acquire();

            Intent incomingCallScreen = new Intent(this, IncomingCallActivity.class);
            incomingCallScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(incomingCallScreen);

            wakeLock.release();
        }
        
        proximityLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SD PROXIMITY LOCK");
        proximityLock.acquire();
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);

        new OngoingCallData().dismiss();
       
        if( proximityLock != null ){
            proximityLock.release();
        }
    }
}
