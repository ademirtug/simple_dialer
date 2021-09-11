
package info.pakkudev.simple_dialer;

import android.telecom.Call;
import android.telecom.VideoProfile;

import org.jetbrains.annotations.Nullable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccount.Builder;
import android.content.*;
import android.provider.*;
import android.database.*;
import android.content.Context;
import android.net.Uri; 
import androidx.appcompat.app.AppCompatActivity;

public class OngoingCallData {

    private static Call call;
    public static AppCompatActivity OngoingCallDataActivity;

    private Object callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int newState) {
            super.onStateChanged(call, newState);
        }
    };

    public final void setCall(@Nullable Call value) {
        if (value != null) {
            value.registerCallback((Call.Callback)callback);
        }
        call = value;
    }

    public void answer() {
        if( call != null){
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
        }
    }

    public void dismiss() {
        if(call != null ){
            call.disconnect();
            call.unregisterCallback((Call.Callback)callback);
            call = null;
        }

        if( OngoingCallData.OngoingCallDataActivity != null ){
            OngoingCallData.OngoingCallDataActivity.finish();
            OngoingCallData.OngoingCallDataActivity = null;
        }
    }
    public String getContactDisplayName(){
        return call.getDetails().getContactDisplayName();
    }
    public String getCallerDisplayName(ContentResolver cr){
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            call.getDetails().getHandle().toString().replaceAll("tel:", ""));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName="";
        Cursor cursor=cr.query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }
        return contactName;
    }

}