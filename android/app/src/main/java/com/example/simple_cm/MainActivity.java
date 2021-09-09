package dev.pakku.simple_dialer;


import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.content.*;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.net.*;
import android.database.*;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import java.text.*;
import java.util.*;


public class MainActivity extends FlutterActivity {
    Collator coll = Collator.getInstance(Locale.getDefault());
    private static final String CHANNEL = "dev.pakku.simple_dialer/channel";

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

                // final SimContact contact = SimContact.fromMap((HashMap)call.arguments);
                // String sr = this.addSimContact(contact);
                // if (sr.isEmpty()) {
                //   result.success(null);
                // } else {
                //   result.error(null, "Failed to add the contact: "+sr, null);
                // }
                // break;
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
        TelecomManager manger= (TelecomManager) getSystemService(TELECOM_SERVICE);
        return manger.getDefaultDialerPackage();
    }


}
