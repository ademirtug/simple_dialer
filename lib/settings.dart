import 'package:flutter/material.dart';
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
                if( v == true ){
                  if(!(await isDefaultDialer())){
                    showAlertDialog(context);
                    return;
                  }
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
        _activateRecents = prefs.getBool("activateRecents") ?? false;
      });
    }else{
      _activateRecents = false;
      prefs.setBool("activateRecents", false);
    }
  }


  void showAlertDialog(BuildContext context) {

    // set up the buttons
    Widget cancelButton = TextButton(
      child: Text(AppLocalizations.of(context)!.cancel),
      onPressed:  () {
        Navigator.of(context).pop();
      },
    );
    Widget continueButton = TextButton(
      child: Text(AppLocalizations.of(context)!.openSettings),
      onPressed:  () {
        openDefaultAppsSettings();
        Navigator.of(context).pop();
      },
    );

    // set up the AlertDialog
    AlertDialog alert = AlertDialog(
      title: Text(AppLocalizations.of(context)!.permissonNeeded),
      content: Text(AppLocalizations.of(context)!.defaultPhoneAppWarning),
      actions: [
        cancelButton,
        continueButton,
      ],
    );

    // show the dialog
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return alert;
      },
    );
  }

}


