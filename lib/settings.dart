import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'util.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({Key? key}) : super(key: key);

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  
  
  bool _activateRecents = false;
  bool _isdefaultDialer = false;

  _SettingsPageState() {
    readValues();
    
  }
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.settings),
      ),
      body: Center(
        child: ListView(
          children: [
            SwitchListTile(
              value: _activateRecents, 
              onChanged: (v)async {
                if(!(await isDefaultDialer())){
                  
                }
                setState(() {
                  _activateRecents = v;
                });
                
                SharedPreferences prefs = await SharedPreferences.getInstance();
                prefs.setBool("activateRecents", v);
              },
             title: Text(AppLocalizations.of(context)!.activateRecents),)
          ],
        )
      )
    );  
  }

  void readValues() async{
    SharedPreferences prefs = await SharedPreferences.getInstance();

    if(await isDefaultDialer()){
      setState(() {
        _isdefaultDialer = true;
        _activateRecents = prefs.getBool("activateRecents") ?? false;
      });
    }else{
      _activateRecents = false;
      prefs.setBool("activateRecents", false);
    }
  }
}


