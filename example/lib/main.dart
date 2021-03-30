import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:brother_label_printer/brother_label_printer.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    // initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> testSearch() async {
    String platformVersion;
    try {
      platformVersion = await BrotherLabelPrinter.search('', resetConnection: true);
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> testUpload() async {}

  Future<void> testPrint() async {
    var res = await BrotherLabelPrinter.printTemplate(
        2,
        {
          'SKU': '1098765432',
          'item_brand': 'BV',
          'item_size': 'XL',
          'item_descr': 'borsa bottega veneta',
          'item_mpc': '6548-655-987',
          'new_price': '60,00'
        },
        numberOfCopies: 2,
        ip: '192.168.1.151',
        macAddress: '40:5B:D8:A0:72:A4');

    print(res);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: ListView(
          children: [
            Center(
              child: Text('Result: $_platformVersion\n'),
            ),
            FlatButton(onPressed: testSearch, child: Text('Cerca')),
            FlatButton(onPressed: testPrint, child: Text('Stampa')),
            FlatButton(onPressed: testUpload, child: Text('Template'))
          ],
        ),
      ),
    );
  }
}
