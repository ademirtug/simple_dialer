package info.pakkudev.simple_dialer;


import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.app.role.*;
import android.content.*;
import android.content.pm.*;
import android.os.Build.*;
import android.os.*;
import android.net.*;
import android.database.*;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.media.*;
import java.text.*;
import java.util.*;


public class MainActivity extends FlutterActivity {
    Collator coll = Collator.getInstance(Locale.getDefault());
    static final String CHANNEL = "info.pakkudev.simple_dialer/channel";
    MethodChannel.Result _result = null;
    //Registrar registrar;


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler(
                (call, result) -> {
                    switch(call.method){
                        case "getDefaultDialer": {
                            result.success(getDefaultDialer());
                            break;
                        }
                        case "askToBeDefaultDialer" :{
                            _result = result;
                            askToBeDefaultDialer();
                            break;
                        }
                        case "openDefaultAppsSettings":{
                            result.success(openDefaultAppsSettings());
                            break;
                        }
                        case "callNumber":{
                            String number = call.argument("number");
                            callNumber(number);
                            break;
                        }
                        default: {
                            result.notImplemented();
                            break;
                        }
                    }
                }
            );
    }
 
     @Override
    public void onCreate(Bundle savedInstanceState) {
        getIntent().putExtra("enable-software-rendering", true);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            _result.success(true);
        }
    }
    private String getDefaultDialer() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            return tm.getDefaultDialerPackage();
        }else{
            return "info.pakkudev.simple_dialer";
        }
    }
    private void askToBeDefaultDialer() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ){
            RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 1);
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getActivity().getPackageName());
            startActivityForResult(intent, 1);          
        }else{
            setResult(1, null);
        }
    }
    private String getDefaultDialer2 (){
        Intent mainIntent = new Intent(Intent.ACTION_DIAL, null);
        mainIntent.addCategory(Intent.CATEGORY_DEFAULT);
        List<ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities(mainIntent, 0);
        if( !pkgAppsList.isEmpty() ) {
            ActivityInfo info = pkgAppsList.get(0).activityInfo;
            return info.packageName;
        }else {
            return "";
        }
    }
    private boolean openDefaultAppsSettings(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        }else{
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        }
        return true;
    }
    private void callNumber(String number){
        System.out.println("callnumber");
        System.out.println(number);
        Intent phoneCallIntent = new Intent(Intent.ACTION_CALL);
        phoneCallIntent.setData(Uri.parse("tel:911"));
        this.startActivity(phoneCallIntent);
    }

}
