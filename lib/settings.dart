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
  bool _isChecked = false;

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
      if(! (prefs.getBool("neverAskDefaultPhoneApp") ?? false )){
        makeDefaultApp(context);
      }
      _activateRecents = false;
      prefs.setBool("activateRecents", false);
    }
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

  void makeDefaultApp(BuildContext context){
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

    AlertDialog alert = AlertDialog(
      title: Text(AppLocalizations.of(context)!.makeDefault),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(AppLocalizations.of(context)!.makeDefaultMessage),
          LabeledCheckbox()
        ],
      ),
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
  void defaultAppPermissionNeeded(BuildContext context) {
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

class LabeledCheckboxStateless extends StatelessWidget {
  const LabeledCheckboxStateless({
    Key? key,
    required this.label,
    required this.padding,
    required this.value,
    required this.onChanged,
  }) : super(key: key);

  final String label;
  final EdgeInsets padding;
  final bool value;
  final Function onChanged;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        onChanged(!value);
      },
      child: Padding(
        padding: padding,
        child: Row(
          children: <Widget>[
            
            Checkbox(
              value: value,
              onChanged: (bool? newValue) {
                onChanged(newValue);
              },
            ),
            Expanded(child: Text(label))
          ],
        ),
      ),
    );
  }
}

/// This is the stateful widget that the main application instantiates.
class LabeledCheckbox extends StatefulWidget {
  const LabeledCheckbox({Key? key}) : super(key: key);

  @override
  State<LabeledCheckbox> createState() => _LabeledCheckboxState();
}

/// This is the private State class that goes with LabeledCheckbox.
class _LabeledCheckboxState extends State<LabeledCheckbox> {
  bool _isSelected = false;

  @override
  Widget build(BuildContext context) {
    return LabeledCheckboxStateless(
      label: AppLocalizations.of(context)!.neverAsk,
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      value: _isSelected,
      onChanged: (bool newValue) async {
        setState(() {
          _isSelected = newValue;
        });
        SharedPreferences prefs = await SharedPreferences.getInstance();
        prefs.setBool("neverAskDefaultPhoneApp", newValue);
      }
    );
  }
}

