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
  bool _autoAcceptCalls = false;
  bool _autoSwitchLoudspeaker = false;


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
                    defaultAppPermissionNeeded(context);
                    return;
                  }
                }

                setState(() {
                  _activateRecents = v;
                });
                
                SharedPreferences prefs = await SharedPreferences.getInstance();
                prefs.setBool("activateRecents", v);

              },
              title: Text(AppLocalizations.of(context)!.activateRecents)
            ),
            SwitchListTile(
              value: _autoAcceptCalls, 
              onChanged: (v)async {

                setState(() {
                  _autoAcceptCalls = v;
                });

                SharedPreferences prefs = await SharedPreferences.getInstance();
                prefs.setBool("autoAcceptCalls", v);

              },
              title: Text(AppLocalizations.of(context)!.proximityAccept),
              subtitle: Text(AppLocalizations.of(context)!.proximityAcceptDescription),
            ),
            SwitchListTile(
              value: _autoSwitchLoudspeaker, 
              onChanged: (v)async {

                setState(() {
                  _autoSwitchLoudspeaker = v;
                });

                SharedPreferences prefs = await SharedPreferences.getInstance();
                prefs.setBool("autoSwitchLoudspeaker", v);

              },
              title: Text(AppLocalizations.of(context)!.proximityLoudspeaker),
              subtitle: Text(AppLocalizations.of(context)!.proximityLoudspeakerDescription),
            ),
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
      // if(! (prefs.getBool("neverAskDefaultPhoneApp") ?? false )){
      //   makeDefaultApp(context);
      // }
      _activateRecents = false;
      prefs.setBool("activateRecents", false);
    }
    _autoAcceptCalls = prefs.getBool("autoAcceptCalls") ?? false;
    _autoSwitchLoudspeaker = prefs.getBool("autoSwitchLoudspeaker") ?? false;
  }
  Color getColor(Set<MaterialState> states) {
    const Set<MaterialState> interactiveStates = <MaterialState>{
      MaterialState.pressed,
      MaterialState.hovered,
      MaterialState.focused,
    };
    if (states.any(interactiveStates.contains)) {
      return Colors.blue;
    }
    return Colors.red;
  }




}



