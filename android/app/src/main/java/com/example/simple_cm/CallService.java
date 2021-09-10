package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;

public class CallService extends InCallService {

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);


        call.answer(VideoProfile.STATE_AUDIO_ONLY);
        //call.disconnect();
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        //new OngoingCall().setCall(null);
    }
}
