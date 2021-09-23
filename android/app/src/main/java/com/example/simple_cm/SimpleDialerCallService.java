package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.os.PowerManager;
import android.content.*;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Build.*;

import android.view.*;
import android.widget.*;
import android.os.Bundle;
import android.app.ActivityManager.AppTask ;
import android.os.*;
import android.telecom.*;


public class SimpleDialerCallService extends InCallService {
    PowerManager.WakeLock wakeLock, proximityLock;
    public static CallStateHandler csh = null;
    KeyguardLock keyguard;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        csh = new CallStateHandler(this, call);

        System.out.println("onCallAdded");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SD WAKE LOCK");
        wakeLock.acquire();

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguard = km.newKeyguardLock("SD KEYGUARD LOCK");
        keyguard.disableKeyguard();

        proximityLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SD PROXIMITY LOCK");
        proximityLock.acquire();
        wakeLock.release();

        
        if( call.getState() == Call.STATE_RINGING ){
            csh.onStateChanged(call, Call.STATE_RINGING);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        //System.out.println("onCallRemoved");
        
        if(csh!=null)
            csh.dismiss();

        if( proximityLock != null )
            proximityLock.release();
        if( keyguard != null )
            keyguard.reenableKeyguard();

        proximityLock = null;
        keyguard = null;
    }
}
