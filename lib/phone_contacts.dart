import 'dart:typed_data';

import 'package:contacts_service/contacts_service.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'util.dart';
import 'dialpad.dart';

class PhoneContatcsSheet extends StatefulWidget {
  PhoneContatcsSheet({Key? key}) : super(key: key);

  @override
  _PhoneContatcsSheetState createState() => _PhoneContatcsSheetState();
}

class _PhoneContatcsSheetState extends State<PhoneContatcsSheet> {
  List<String> providers = [];

  @override
  void initState() {
    super.initState();
    refreshContacts();
  }

  Future<void> refreshContacts() async {

    if( await requestPermissions(Permission.contacts) == false)
      return;

    //Iterable contacts = await _channel.invokeMethod("addSimContact");
    //var ictx = contacts.map((m) => );

    var cx = (await ContactsService.getContacts(withThumbnails: false, orderByGivenName: true )).toList();

    setState(() {
      contacts = cx;
    });

    for (final contact in contacts) {
      //providers.add(contact.androidAccountTypeRaw ?? "");
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
      body: SafeArea(
        child: contacts.length > 0 
        ? RefreshIndicator(child: 
            ListView.builder(
              itemCount: contacts.length,
              itemBuilder: (BuildContext context, int index) {
                Contact c = contacts.elementAt(index);
                return Column(
                  children: [
                    ContactTile(contact: c),
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

class ContactTile extends StatefulWidget {
  Contact contact;
  ContactTile({Key? key, required this.contact}) : super(key: key);

  @override
  _ContactTileSheetState createState() => _ContactTileSheetState();
}

class _ContactTileSheetState extends State<ContactTile> {

  @override
  Widget build(BuildContext context) {
    Contact c = widget.contact;
    return ListTile(
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
                await callNumber(c.phones!.first.value ?? "");
              },
              child: SizedBox(width: 56, height: 56, child: Icon(Icons.phone, size: 48)),
            )
            : Text(""),
        ),
      )
    );
  }
}