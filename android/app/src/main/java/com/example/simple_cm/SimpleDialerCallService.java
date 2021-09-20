package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.os.PowerManager;
import android.content.*;
import android.app.KeyguardManager;


public class SimpleDialerCallService extends InCallService {
    PowerManager.WakeLock wakeLock, proximityLock;
    public static CallStateHandler csh = null;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        csh = new CallStateHandler(this, call);


        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        if( call.getState() == Call.STATE_RINGING ){
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SD WAKE LOCK");
            wakeLock.acquire();
            csh.onStateChanged(call, Call.STATE_RINGING);
            wakeLock.release();
        }

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km .newKeyguardLock("SD KEYGUARDLOCK");
        kl.disableKeyguard();
        
        proximityLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SD PROXIMITY LOCK");
        proximityLock.acquire();
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);

        if(csh!=null)
            csh.dismiss();

        if( proximityLock != null )
            proximityLock.release();
    }
}
