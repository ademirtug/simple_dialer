package info.pakkudev.simple_dialer;
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

public class MainActivity extends FlutterActivity {
  private static final String CHANNEL = "info.pakkudev.simple_dialer/channel";
    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
        .setMethodCallHandler(
            (call, result) -> {
                try{
                    // Note: this method is invoked on the main thread.
                    if (call.method.equals("sortList")) {
                        //List<String> sorted = sortList(call.argument("unsortedList"));
                        String slist = sortStringList(call.argument("arg1"));
                        result.success(slist);
                    } else {
                        result.notImplemented();
                    }
                }catch(Exception e){
                    Log.d("error", e.getMessage());
                }
                
            }
        );
    }
    private String sortStringList(String input){
        List<String> lst = Arrays.asList(input.split(";"));
        lst = sortList(lst);
        String ret ="";

        for (String e: lst) {
            ret+= e + ";";
        }

        return ret;
    }

    private List<String> sortList(List<String> input){
        Collator nc = Collator.getInstance(Locale.getDefault());
        nc.setStrength(Collator.PRIMARY);

        Collections.sort(input, nc);

        return input;
    }
    
}
