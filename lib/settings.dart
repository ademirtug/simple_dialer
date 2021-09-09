import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';


class SettingsPage extends StatefulWidget {
  const SettingsPage({Key? key}) : super(key: key);

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  static const MethodChannel _channel = const MethodChannel('dev.pakku.simple_dialer/channel');
  
  bool _activateRecents = false;
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
    String dialer = await _channel.invokeMethod("getDefaultDialer");

    SharedPreferences prefs = await SharedPreferences.getInstance();
    setState(() {
      _activateRecents = prefs.getBool("activateRecents") ?? false;
    });
  }
}


