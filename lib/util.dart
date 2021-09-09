import 'package:contacts_service/contacts_service.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';


List<Contact> contacts = [];
const MethodChannel channel = const MethodChannel('dev.pakku.simple_dialer/channel');

Future<bool> isDefaultDialer() async {
  String dialer = await channel.invokeMethod("getDefaultDialer");
  if(dialer == "dev.pakku.simple_dialer")
    return true;
  return false;
}
Future<void> openDefaultAppsSettings() async {
  await channel.invokeMethod("openDefaultAppsSettings");
}
Future<bool> requestPermissions(Permission pta)async {
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
