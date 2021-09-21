import 'package:contacts_service/contacts_service.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:simple_dialer/call_log.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:timeago/timeago.dart' as timeago;

import 'phone_contacts.dart';
import 'settings.dart';
import 'dialpad.dart';
import 'util.dart';

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
  List<String> _tabs = [];
  PhoneContatcsSheet? pcs;
  HistorySheet? cls;
  int selectedIndex = 0;

  _MyHomePageState(){
    setTimeAgoMessages();
    readSettings();
  }

  @override
  Widget build(BuildContext context) {  
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.contacts),
        actions: <Widget>[
          PopupMenuButton<String>(
            onSelected:(index) async {
              switch (index) {
                case "Settings":
                  await Navigator.of(context).push(MaterialPageRoute(builder: (context) => SettingsPage()));
                  readSettings();
                break;
              }
            },
            itemBuilder: (BuildContext context) {
              return [
                PopupMenuItem<String>(
                   value: "Settings",
                   child: Text(AppLocalizations.of(context)!.settings),
                )
              ];

            },
          ),
        ],
      ),
      body: getTabsByIndex(selectedIndex),
      bottomNavigationBar: buildNavBar(),

      floatingActionButton: FloatingActionButton(
        onPressed: () {
          showModalBottomSheet(
            isScrollControlled: true,
            context: context,
            builder: (context) => Wrap(children: [Dialpad(primaryColor: Colors.blue)]),
          );
        },
        heroTag: null,
        child: const Icon(Icons.dialpad, size: 36),
        backgroundColor: Colors.blueAccent[300],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }

  Widget? getTabsByIndex(int index){
    if(index == 0){
      return (pcs = pcs != null ? pcs : PhoneContatcsSheet());
    }else if(index == 1){
      return (cls = cls != null ? cls: HistorySheet());
    }
  }

  void readSettings() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    bool activateRecents = prefs.getBool("activateRecents") ?? false;
       
    if(!(await isDefaultDialer())){


      setState(() {
        prefs.setBool("activateRecents", false);
        activateRecents = false;
      });
    }else{
    
    }

    _tabs.clear();
    _tabs.add("Contacts");
    if( activateRecents ){
      _tabs.add("Recents");
    }
    selectedIndex = 0;
    setState(() {
    });
  }

  BottomNavigationBar? buildNavBar(){
    if(_tabs.length < 2)
      return null;

    List<BottomNavigationBarItem> bnbi = [BottomNavigationBarItem(icon: Icon(Icons.contacts), label: AppLocalizations.of(context)!.contacts)];
    
    if( _tabs.contains("Recents")){
      bnbi.add(BottomNavigationBarItem(icon: Icon(Icons.history), label: AppLocalizations.of(context)!.recents));
    }

    BottomNavigationBar bnb = BottomNavigationBar(
      items: bnbi,
      currentIndex: selectedIndex,
      selectedItemColor: Colors.blue,
      onTap: (int index) {
        setState(() {
          selectedIndex = index;
        });
      },    
    );
    return bnb;
  }

  void setTimeAgoMessages() {
    timeago.setLocaleMessages('ar', timeago.ArMessages());
    timeago.setLocaleMessages('ar_short', timeago.ArShortMessages());
    timeago.setLocaleMessages('az', timeago.AzMessages());
    timeago.setLocaleMessages('az_short', timeago.AzShortMessages());
    timeago.setLocaleMessages('ca', timeago.CaMessages());
    timeago.setLocaleMessages('ca_short', timeago.CaShortMessages());
    timeago.setLocaleMessages('cs', timeago.CsMessages());
    timeago.setLocaleMessages('cs_short', timeago.CsShortMessages());
    timeago.setLocaleMessages('da', timeago.DaMessages());
    timeago.setLocaleMessages('da_short', timeago.DaShortMessages());
    timeago.setLocaleMessages('de', timeago.DeMessages());
    timeago.setLocaleMessages('de_short', timeago.DeShortMessages());
    timeago.setLocaleMessages('dv', timeago.DvMessages());
    timeago.setLocaleMessages('dv_short', timeago.DvShortMessages());
    timeago.setLocaleMessages('en', timeago.EnMessages());
    timeago.setLocaleMessages('en_short', timeago.EnShortMessages());
    timeago.setLocaleMessages('es', timeago.EsMessages());
    timeago.setLocaleMessages('es_short', timeago.EsShortMessages());
    timeago.setLocaleMessages('fa', timeago.FaMessages());
    timeago.setLocaleMessages('fr', timeago.FrMessages());
    timeago.setLocaleMessages('fr_short', timeago.FrShortMessages());
    timeago.setLocaleMessages('gr', timeago.GrMessages());
    timeago.setLocaleMessages('gr_short', timeago.GrShortMessages());
    timeago.setLocaleMessages('he', timeago.HeMessages());
    timeago.setLocaleMessages('he_short', timeago.HeShortMessages());
    timeago.setLocaleMessages('he', timeago.HeMessages());
    timeago.setLocaleMessages('he_short', timeago.HeShortMessages());
    timeago.setLocaleMessages('hi', timeago.HiMessages());
    timeago.setLocaleMessages('hi_short', timeago.HiShortMessages());
    timeago.setLocaleMessages('id', timeago.IdMessages());
    timeago.setLocaleMessages('it', timeago.ItMessages());
    timeago.setLocaleMessages('it_short', timeago.ItShortMessages());
    timeago.setLocaleMessages('ja', timeago.JaMessages());
    timeago.setLocaleMessages('km', timeago.KmMessages());
    timeago.setLocaleMessages('km_short', timeago.KmShortMessages());
    timeago.setLocaleMessages('ko', timeago.KoMessages());
    timeago.setLocaleMessages('ku', timeago.KuMessages());
    timeago.setLocaleMessages('ku_short', timeago.KuShortMessages());
    timeago.setLocaleMessages('mn', timeago.MnMessages());
    timeago.setLocaleMessages('mn_short', timeago.MnShortMessages());
    timeago.setLocaleMessages('ms_MY', timeago.MsMyMessages());
    timeago.setLocaleMessages('ms_MY_short', timeago.MsMyShortMessages());
    timeago.setLocaleMessages('nb_NO', timeago.NbNoMessages());
    timeago.setLocaleMessages('nb_NO_short', timeago.NbNoShortMessages());
    timeago.setLocaleMessages('nl', timeago.NlMessages());
    timeago.setLocaleMessages('nl_short', timeago.NlShortMessages());
    timeago.setLocaleMessages('nn_NO', timeago.NnNoMessages());
    timeago.setLocaleMessages('nn_NO_short', timeago.NnNoShortMessages());
    timeago.setLocaleMessages('pl', timeago.PlMessages());
    timeago.setLocaleMessages('pt_BR', timeago.PtBrMessages());
    timeago.setLocaleMessages('pt_BR_short', timeago.PtBrShortMessages());
    timeago.setLocaleMessages('ro', timeago.RoMessages());
    timeago.setLocaleMessages('ro_short', timeago.RoShortMessages());
    timeago.setLocaleMessages('ru', timeago.RuMessages());
    timeago.setLocaleMessages('ru_short', timeago.RuShortMessages());
    timeago.setLocaleMessages('rw', timeago.RwMessages());
    timeago.setLocaleMessages('rw_short', timeago.RwShortMessages());
    timeago.setLocaleMessages('sv', timeago.SvMessages());
    timeago.setLocaleMessages('sv_short', timeago.SvShortMessages());
    timeago.setLocaleMessages('ta', timeago.TaMessages());
    timeago.setLocaleMessages('th', timeago.ThMessages());
    timeago.setLocaleMessages('th_short', timeago.ThShortMessages());
    timeago.setLocaleMessages('tr', timeago.TrMessages());
    timeago.setLocaleMessages('uk', timeago.UkMessages());
    timeago.setLocaleMessages('uk_short', timeago.UkShortMessages());
    timeago.setLocaleMessages('vi', timeago.ViMessages());
    timeago.setLocaleMessages('vi_short', timeago.ViShortMessages());
    timeago.setLocaleMessages('zh_CN', timeago.ZhCnMessages());
    timeago.setLocaleMessages('zh', timeago.ZhMessages());
  }
}





