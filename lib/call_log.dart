import 'dart:ui';
import 'package:call_log/call_log.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

import 'util.dart';
import 'dialpad.dart';



class HistorySheet extends StatefulWidget {
  HistorySheet({Key? key}) : super(key: key);

  @override
  _HistorySheetState createState() => _HistorySheetState();
}

class _HistorySheetState extends State<HistorySheet> {
  List<CallLogEntry> _logs = [];
  bool _logLoaded = false;

  @override
  void initState() {
    super.initState();
    refreshHistory();
  }

  Future<void> refreshHistory() async {

    if( await requestPermissions(Permission.log) == false)
    {
      setState(() {
        _logLoaded = true;
        _logs = new List.empty();
      });
      return;
    }

    List<CallLogEntry> clx = (await CallLog.get()).toList();

    setState(() {
      _logLoaded = true;
      _logs = clx;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: _logLoaded 
        ? RefreshIndicator(child: 
            ListView.builder(
              itemCount: _logs.length,
              itemBuilder: (BuildContext context, int index) {
                return Column(
                  children: [
                    CallLogTile(cle:  _logs.elementAt(index)),
                    Divider(indent: 5,thickness: 1,height: 25,)
                  ]
                );
              },
            ),
            onRefresh: refreshHistory
          )
        : Center(child: CircularProgressIndicator()),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            showModalBottomSheet(
              isScrollControlled: true,
              context: context,
              builder: (context) => Wrap(children: [Dialpad(primaryColor: Colors.green)]),
            );
          },
          child: const Icon(Icons.dialpad, size: 36),
          backgroundColor: Colors.blueAccent[300],
        ),
        floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
        
    );
  }
}

class CallLogTile extends StatefulWidget {
  CallLogEntry cle;
  CallLogTile({Key? key, required this.cle}) : super(key: key);

  @override
  _CallLogTileSheetState createState() => _CallLogTileSheetState();
}

class _CallLogTileSheetState extends State<CallLogTile> {

  @override
  Widget build(BuildContext context) {
    String? vn = widget.cle.name != null ? widget.cle.name : widget.cle.simDisplayName;
    vn = vn == null ? widget.cle.number : vn;
    DateTime ct = DateTime.fromMillisecondsSinceEpoch(widget.cle.timestamp ?? 0);
    var x = timeago.format(ct, locale: "${window.locale.languageCode}");
    return ListTile(
      contentPadding: EdgeInsets.only( left: 3),
      leading: CircleAvatar(
        radius: 30,
        backgroundColor: getColorByCallType(widget.cle.callType), 
        child: Icon(getIconByCallType(widget.cle.callType))),
      title: Text(vn ?? "", style: TextStyle(fontSize: 20)),
      subtitle: Text(x),
      trailing : ClipOval(
        child: Material(
          color: Colors.transparent, // Button color
          child: InkWell(
              splashColor: Colors.grey, // Splash color
              onTap: ()async{
                if( await requestPermissions(Permission.phone) == false || await requestPermissions(Permission.contacts) == false)
                  return;
                
                try{
                  await callNumber(widget.cle.number ?? "");
                }on Exception catch(e){
                  showInfoDialog(context, AppLocalizations.of(context)!.unexpectedError, e.toString());
                }

                              
              },
              child: SizedBox(width: 56, height: 56, child: Icon(Icons.phone, size: 48)),
            )
            
        ),
      )
    );
  }

  // String getInitials(String? vn) {
  //   try{
  //     List<String> names = vn!.split(" ");

  //     if( names.length < 2){
  //       if( vn.length > 1 )
  //         return vn.substring(0,1);
  //       else 
  //         return " ";
  //     }

  //     if( names[0].length < 1 || names[1].length < 1 )
  //       return " ";

  //     return names[0].substring(0, 1) + names[1].substring(0, 1);
  //   }on Exception{

  //   }
  //   return " ";
  // }

  IconData? getIconByCallType(CallType? callType) {
    switch(callType){
      case CallType.incoming:{
        return Icons.call_received;
      }
      case CallType.outgoing:{
        return Icons.call_made;
      }
      case CallType.missed:{
        return Icons.call_missed;
      }
      case CallType.rejected:{
        return Icons.call_missed;
      }
      default:
        return Icons.call_made;
    }
  }

  getColorByCallType(CallType? callType) {
    switch(callType){
      case CallType.incoming:
      case CallType.outgoing:{
        return Colors.green;
      }
      case CallType.missed:
      case CallType.rejected:{
        return Colors.red;
      }
      default:
        return Colors.green;
    }
  }
}