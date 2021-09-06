import 'package:flutter/material.dart';
import 'package:flutter_phone_direct_caller/flutter_phone_direct_caller.dart';
import 'package:permission_handler/permission_handler.dart';
import 'util.dart';
  
class Dialpad extends StatefulWidget {
  Dialpad({Key? key, required this.primaryColor}) : super(key: key);
  final Color primaryColor;
  
  @override
  _DialpadState createState() => _DialpadState();
}

class _DialpadState extends State<Dialpad> {
  DialText dt = DialText();

  @override
  Widget build(BuildContext context) {
    
    return Container(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisAlignment : MainAxisAlignment.spaceAround,
        children: [
          dt,
          Row(

            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              getOvalButton("1", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("2", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("3", textColor: Colors.black, surfaceColor: widget.primaryColor)
            ],
          ),
          Divider(height: 5),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              getOvalButton("4", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("5", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("6", textColor: Colors.black, surfaceColor: widget.primaryColor)
            ],
          ),
          Divider(height: 5),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              getOvalButton("7", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("8", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("9", textColor: Colors.black, surfaceColor: widget.primaryColor)

            ],
          ),
          Divider(height: 5),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              getOvalButton("*", textColor: Colors.black, surfaceColor: Colors.white),
              getOvalButton("0", textColor: Colors.black, surfaceColor: widget.primaryColor),
              getOvalButton("#", textColor: Colors.black, surfaceColor: Colors.white),
            ],
          ),
          Divider(height: 5),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              getCallButton()
            ],
          ),
          Divider(height: 5),
        ],
      ),
    );
  }

  Widget getOvalButton(String text, {textColor:Colors.white, surfaceColor: Colors.blue}){
    //CircleAvatar(radius: 35 ,backgroundColor: Colors.amber, child: Text("1"))
    return ElevatedButton(

      onPressed: () {
        dt.setDialText(dt.getDialText()+text);
      },
      child: Text(text, style: TextStyle(color: textColor, fontSize: 30)),
      style: ElevatedButton.styleFrom(
        shape: CircleBorder(),
        padding: EdgeInsets.all(15),
        primary: surfaceColor
      )
    );
  }
  Widget getCallButton({textColor:Colors.white, surfaceColor: Colors.blue}){
    dt.getDialText();

    return ElevatedButton(
      onPressed: () async {
        if( await requestPermissions(Permission.phone) == false )
          return;

        if( dt.getDialText().length < 1)
          return;
          

        await FlutterPhoneDirectCaller.callNumber(dt.getDialText());
      },
      child: Icon(Icons.phone_enabled_sharp),
      style: ElevatedButton.styleFrom(
        shape: CircleBorder(),
        padding: EdgeInsets.all(20),
        primary: Colors.green
      )
    );
  }
}

class DialText extends StatefulWidget {
  final _DialTextState dts = _DialTextState();
  DialText({Key? key}) : super(key: key);
  

  String getDialText(){
    return dts.getDialText();
  }
  void setDialText(String text){
    dts.setDialText(text);
  }
  @override
  _DialTextState createState(){
    return dts;
  } 
}

class _DialTextState extends State<DialText> {
  String dialText = "";
  ScrollController sc = ScrollController();

  @override
  Widget build(BuildContext context) {
      return Column(
        children: [Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
           Expanded( child: 
            Center(
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                controller: sc,
                child: Text(
                  dialText,
                  overflow: TextOverflow.fade,
                  style: TextStyle(color: Colors.black, fontSize: 30)
                )
              )
            )
          ),
          GestureDetector(child: Icon(Icons.backspace_outlined, size: 36),
            onTap: (){
              setDialText(dialText.substring(0, dialText.length > 0 ? dialText.length-1 : 0));
            }
          )
      ],
      ),
      Divider(
        color: Colors.black,
        thickness: 1,
      )]);
  }

  void setDialText(String text){
    setState(() {
      dialText = text;
    });
    sc.animateTo(sc.position.maxScrollExtent+25, duration: Duration(milliseconds: 1), curve: Curves.linear);
  }
  String getDialText(){
    return dialText;
  }
}
