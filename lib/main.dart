import 'dart:async';
import 'dart:typed_data';
import 'package:contacts_service/contacts_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_phone_direct_caller/flutter_phone_direct_caller.dart';
import 'package:permission_handler/permission_handler.dart';
import 'dialpad.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

void main() async {

  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    //Locale myLocale = Localizations.localeOf(context);

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Simple Dialer',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key? key}) : super(key: key);

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<Contact> contacts = [];
  @override
  void initState() {
    super.initState();
    refreshContacts();
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
        title: Text(AppLocalizations.of(context)!.contacts),
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
                                await FlutterPhoneDirectCaller.callNumber(c.phones!.first.value ?? "");
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





