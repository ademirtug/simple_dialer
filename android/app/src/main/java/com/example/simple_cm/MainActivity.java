package dev.pakku.simple_dialer;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import java.text.*;
import java.util.*;
import android.util.*;
import android.net.*;
import android.database.*;
import android.content.*;
import android.provider.ContactsContract;
import android.app.AlertDialog;



public class MainActivity extends FlutterActivity {
    Collator coll = Collator.getInstance(Locale.getDefault());
    private static final String CHANNEL = "dev.pakku.simple_dialer/channel";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
    }
 
     @Override
    public void onCreate(Bundle savedInstanceState) {

        // use software rendering (ideally only when you need to)
        //getIntent().putExtra("enable-software-rendering", true);

        // start Flutter
        super.onCreate(savedInstanceState);
    }
}
