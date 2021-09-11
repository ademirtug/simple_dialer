
package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.VideoProfile;

import org.jetbrains.annotations.Nullable;

public class OngoingCall {

    private static Call call;

    private Object callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int newState) {
            super.onStateChanged(call, newState);
            //state.onNext(newState);
            // switch(newState){
            //     case STATE_DISCONNECTED:
            //     {
                    

            //         break;
            //     }
                
            //     default:
            //     {
                    
            //         break;
            //     }
            // }
        }
    };

    public final void setCall(@Nullable Call value) {
        if (call != null) {
            call.unregisterCallback((Call.Callback)callback);
        }

        if (value != null) {
            value.registerCallback((Call.Callback)callback);

        }

        call = value;
    }

    public void answer() {
        assert call != null;
        call.answer(VideoProfile.STATE_AUDIO_ONLY);
    }

    public void hangup() {
        assert call != null;
        call.disconnect();
    }
    public String getDisplayName(){
        return call.getDetails().getContactDisplayName();
    }

}