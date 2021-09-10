package info.pakkudev.simple_dialer;


import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.content.*;
import android.content.pm.*;
import android.os.Build.*;
import android.os.*;
import android.net.*;
import android.database.*;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import java.text.*;
import java.util.*;


public class MainActivity extends FlutterActivity {
    Collator coll = Collator.getInstance(Locale.getDefault());
    private static final String CHANNEL = "info.pakkudev.simple_dialer/channel";

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
                case "openDefaultAppsSettings":{
                    result.success(openDefaultAppsSettings());
                    break;
                }
                case "userConsent":{
                    result.success(true);
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
        // use software rendering (ideally only when you need to)
        getIntent().putExtra("enable-software-rendering", true);

        // start Flutter
        super.onCreate(savedInstanceState);
    }

    private String getDefaultDialer() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            return tm.getDefaultDialerPackage();
        }else{
            return "info.pakkudev.simple_dialer";
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
    private String getDefaultDialer3 (){
        Intent mainIntent = new Intent(Intent.ACTION_DIAL, null);
        mainIntent.addCategory(Intent.CATEGORY_DEFAULT);
        final ResolveInfo mInfo = getPackageManager().resolveActivity(mainIntent, 0);;

        if( mInfo != null)
            return mInfo.activityInfo.applicationInfo.packageName;
        else
            return "";


    }
    private boolean openDefaultAppsSettings(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        }else{
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        }
        return true;
    }
    // private boolean userConsent(){
    //     if (getSystemService(TelecomManager::class.java).defaultDialerPackage != packageName) {
    //         Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
    //         .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
    //         .let(::startActivity);
    //     }
    //     return true;
    // }
}
