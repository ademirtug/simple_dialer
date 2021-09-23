package info.pakkudev.simple_dialer;
import androidx.appcompat.app.AppCompatActivity;

import android.view.*;
import android.widget.*;
import android.os.Bundle;
import android.app.ActivityManager.AppTask ;
import android.os.Build.*;
import android.os.*;
import android.telecom.*;
import android.content.*;
import android.telephony.PhoneNumberUtils; 
import android.telecom.Call;
import android.telecom.VideoProfile;
import org.jetbrains.annotations.Nullable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccount.Builder;
import android.provider.*;
import android.database.*;
import android.content.Context;
import android.net.Uri; 



public class CallStateHandler extends Call.Callback{
    Context _ctx = null;
    Call _call = null;
    boolean _isIncomingCall = false;
    public AppCompatActivity ica;
    public AppCompatActivity oca;


    CallStateHandler(Context ctx, Call call){
        _ctx = ctx;
        _call = call;
        _call.registerCallback((Call.Callback)this);
    }

    @Override
    public void onStateChanged(Call call, int newState) {
        super.onStateChanged(call, newState);
        switch(newState){
            case Call.STATE_RINGING : {
                System.out.println("STATE_RINGING");
                onRinging();
                break;
            }
            case Call.STATE_DIALING : {
                System.out.println("STATE_DIALING");
                onDialing();
                break;
            }
            case Call.STATE_DISCONNECTED : {
                System.out.println("STATE_DISCONNECTED");
                onDisconnected();
                break;
            }
            case Call.STATE_ACTIVE : {
                System.out.println("STATE_ACTIVE");
                onActivated();
                break;
            }

            default:
                break;
        }
    }

    public void answer() {
        if( _call != null){
            _call.answer(VideoProfile.STATE_AUDIO_ONLY);
        }
    }

    public void dismiss() {
        //System.out.println("dismiss()");
        if(_call != null ){
            _call.disconnect();
            _call = null;
        }

        if( oca != null ){
            oca.finish();
            oca = null;
        }
        if(ica != null ){
            ica.finish();
            ica = null;
        }
    }

    public void onRinging(){
       _isIncomingCall = true;
       Intent incomingCallScreen = new Intent(_ctx, IncomingCallActivity.class);
       incomingCallScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       _ctx.startActivity(incomingCallScreen);
    }
    public void onDialing(){
        Intent ongoingCallScreen = new Intent(_ctx, OngoingCallActivity.class);
        ongoingCallScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _ctx.startActivity(ongoingCallScreen);
    }
    public void onActivated(){

    }
    public void onDisconnected(){

    }

    public boolean isIncomingCall(){
        return _isIncomingCall;
    }
    public String getContactDisplayName(){
        return _call.getDetails().getContactDisplayName();
    }
    public String getCallPhoneNumber(){
        return PhoneNumberUtils.formatNumber(_call.getDetails().getHandle().toString().replaceAll("tel:", "")).replaceAll("%2B", "+");
    }
        public String getCallerDisplayName(ContentResolver cr){
        String tel = getCallPhoneNumber();
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,tel);

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName="";
        Cursor cursor=cr.query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }
        return contactName.isEmpty() ? tel : contactName;
    }
}