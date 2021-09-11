package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.os.PowerManager;
import android.content.*;

public class CallService extends InCallService {
    Intent callScreen;
    PowerManager.WakeLock wakeLock, proximityLock;


    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SD WAKE LOCK");
        wakeLock.acquire();

        new OngoingCall().setCall(call);
        
        callScreen = new Intent(this, incall_activity.class);
        callScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callScreen);
        wakeLock.release();

        proximityLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SD PROXIMITY LOCK");
        proximityLock.acquire();
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        new OngoingCall().setCall(null);
        callScreen = null;

        if( proximityLock != null ){
            proximityLock.release();
        }
    }
}
