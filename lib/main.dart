import 'dart:async';
import 'dart:developer';
import 'dart:typed_data';
import 'package:contacts_service/contacts_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_phone_direct_caller/flutter_phone_direct_caller.dart';
import 'package:permission_handler/permission_handler.dart';
import 'dialpad.dart';


void main() async {

  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Simple Dialer',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Contacts'),
    );
  }
}

class MyHomePage extends StatefulWidget {


  MyHomePage({Key? key, required this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<Contact> contacts = [];
  static const platform = MethodChannel('info.pakkudev.simple_dialer/channel');


  @override
  void initState() {
    super.initState();
    refreshContacts();
  }

  Future<List<Contact>> sortList(List<Contact> input) async {
    List<String> slst = [];

    String sulst = "";
    String sslst = "";

    input.forEach((element) { sulst+=(element.displayName ?? "") + ";";});
    
    try {
      sslst = await platform.invokeMethod('sortList', {"arg1" : sulst}); 
    } on PlatformException catch (e) {
    }

    slst = sslst.split(";").toList();

    List<Contact> output = [];
    slst.remove("");
    slst.forEach((element) {
      output.add( input.singleWhere((e) => e.displayName == element ));
    });

    return output;
  }




  Future<void> refreshContacts() async {

    if( await requestPermissions(Permission.contacts) == false)
      return;

    var cx = (await ContactsService.getContacts(withThumbnails: false, orderByGivenName: true )).toList();

    setState(() {
      contacts = cx;
    });

    for (final contact in contacts) {
      ContactsService.getAvatar(contact).then((avatar) {
        if (avatar == null) 
          return;
        setState(() => contact.avatar = avatar);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: SafeArea(
        child: contacts.length > 0 
        ? RefreshIndicator(child: 
            ListView.builder(
              itemCount: contacts.length,
              itemBuilder: (BuildContext context, int index) {
                Contact c = contacts.elementAt(index);
                return Column(
                  children: [
                    ListTile(
                      contentPadding: EdgeInsets.only( left: 3),
                      leading: (c.avatar != null && c.avatar!.length > 0)
                      ? CircleAvatar(radius: 30, backgroundImage: MemoryImage(c.avatar ?? Uint8List.fromList([])))
                      : CircleAvatar(radius: 30 , child: Text(c.initials())),
                      title: Text(c.displayName ?? "", style: TextStyle(fontSize: 20)),
                      trailing : ClipOval(
                        child: Material(
                          color: Colors.transparent, // Button color
                          child: c.phones!.length > 0 
                            ? InkWell(
                              splashColor: Colors.grey, // Splash color
                              onTap: ()async{
                                if( await requestPermissions(Permission.phone) == false )
                                  return;

                                // if( c.phones!.length < 1)
                                //   return;
                                // if( c.phones!.first.value!.length < 1 )
                                //   return;

                                bool res = c.phones!.first.value!.length > 0 
                                ? await FlutterPhoneDirectCaller.callNumber(c.phones!.first.value ?? "")
                                : false;
                              },
                              child: SizedBox(width: 56, height: 56, child: Icon(Icons.phone, size: 48)),
                            )
                            : Text(""),
                        ),
                      )
                    ),
                    Divider(indent: 5,thickness: 1,height: 25,)
                  ]
                );
              },
            ),
            onRefresh: refreshContacts
          )
        : Center(child: CircularProgressIndicator()),
        ),
        floatingActionButton: FloatingActionButton(

          onPressed: () {
            showModalBottomSheet(
              isScrollControlled: true,
              context: context,
              builder: (context) => Wrap(children: [Dialpad()]),
            );
          },
          child: const Icon(Icons.dialpad, size: 36),
          backgroundColor: Colors.blueAccent[300],
        ),
        floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
        
    );
  }
}





