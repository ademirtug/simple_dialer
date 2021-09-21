import 'package:contacts_service/contacts_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart';

List<Contact> contacts = [];
bool askedToBeDefaultDialer = false;

const MethodChannel channel = const MethodChannel('info.pakkudev.simple_dialer/channel');

Future<bool> isDefaultDialer() async {
  String dialer = await channel.invokeMethod("getDefaultDialer");
  if(dialer == "info.pakkudev.simple_dialer")
    return true;
  return false;
}
Future<void> askToBeDefautDialer() async {
  await channel.invokeMethod("askToBeDefaultDialer");
}
Future<void> openDefaultAppsSettings() async {
  await channel.invokeMethod("openDefaultAppsSettings");
}


Future<bool> requestPermissions(Permission pta)async {

  if(!askedToBeDefaultDialer){
    await askToBeDefautDialer();
    askedToBeDefaultDialer = true;
  }

  var ps = await pta.status;

  if( ps == PermissionStatus.granted )
    return true;

  if(ps == PermissionStatus.permanentlyDenied){
    openAppSettings();
    return false;
  }

  if( ps == PermissionStatus.denied )
  {
    var ps2 = await pta.request();

    if( ps2 == PermissionStatus.granted )
      return true;
    else if(ps2 == PermissionStatus.permanentlyDenied){
      openAppSettings();
      return false;
    }
  }
  
  return false;
}

Future<bool> makeDefaultApp(BuildContext context) async{
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

  showDialog(
    context: context,
    builder: (BuildContext context) {
      return alert;
    },
  ).then((value){return true;});
  return false;
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

class LabeledCheckbox extends StatefulWidget {
  const LabeledCheckbox({Key? key}) : super(key: key);

  @override
  State<LabeledCheckbox> createState() => _LabeledCheckboxState();
}

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