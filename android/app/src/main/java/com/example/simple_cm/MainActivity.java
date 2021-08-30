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
import android.net.*;
import android.database.*;
import android.content.*;
import android.provider.ContactsContract;




public class MainActivity extends FlutterActivity {
    Collator coll = Collator.getInstance(Locale.getDefault());
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
                    }else if(call.method.equals("getContacts")){
                        List<Contact> contacts = getPhoneContacts();
                        List<String> scontacts = new ArrayList<>();

                        // for (Contact contact : contacts) {
                        //     scontacts.add(contact.toJson());
                        // }
                        result.success(scontacts);
                    } 
                    else {
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
    private void sortContacts(List<Contact> contacts){
        coll.setStrength(Collator.PRIMARY);
        Collections.sort(contacts, new Comparator<Contact>(){
            public int compare(Contact c1, Contact c2) {
                return coll.compare(c1.displayName, c2.displayName);
            }
        });
    }
    private List<Contact> getPhoneContacts() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        }, null, null, null);
        List<Contact> lst = new ArrayList<Contact>();

        if(cur == null)
            return lst;
        if( cur.getCount() < 1)
            return lst;

        while (cur.moveToNext()) {
            Contact sc = new Contact();
            //String[] cn = cur.getColumnNames();
            sc.id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
            sc.displayName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            sc.accountType = getAccountType(sc.id);

            if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0)
                sc.numbers = getPhoneNumbers(sc.id);

            lst.add(sc);
        }

        cur.close();
        return lst;
    }

    List<String> getPhoneNumbers(String id){
        List<String> numbers = new ArrayList<>();
        if(id.isEmpty())
            return numbers;

        Cursor pCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

        while (pCur.moveToNext()) {
            numbers.add(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
        }
        pCur.close();
        return numbers;
    }

    private String getAccountType(String id){
        String at = "";
        if(id.isEmpty())
            return "";

        Cursor atCur = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI , null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

        while (atCur.moveToNext()) {
            at = atCur.getString(atCur.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));
        }
        return at;
    }    
    
}

class Contact {
    public String id = "";
    public String displayName = "";
    public String email = "";
    public String accountType = "";
    public List<String> numbers = new ArrayList<String>();

    Contact(){}
    Contact(String _name, String[] _numbers, String _email){
        displayName = _name;
        email = _email;

        Collections.addAll(numbers, _numbers);
        //number = _number.replaceAll("\\+", "");
    }
    String toJson(){
        String jsnNumbers = "[";

        for (int i = 0; i < numbers.size(); ++i){
            jsnNumbers += "\""+ numbers.get(i) + "\"";

            if( i+1 < numbers.size() )
                jsnNumbers+=", ";
        }
        jsnNumbers+="]";

        return String.format("{\"id\":\"%s\", \"name\":\"%s\",\"numbers\":%s,\"email\":\"%s\"}",
                id, displayName, jsnNumbers, email);
    }
}
