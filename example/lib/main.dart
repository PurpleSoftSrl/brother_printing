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
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await BrotherLabelPrinter.search("", true);
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> testPrint() async {
    await BrotherLabelPrinter.printTemplate(
        2,
        {
          'SKU': '1098765432',
          'item_brand': 'BV',
          'item_size': 'XL',
          'item_descr': 'borsa bottega veneta',
          'item_mpc': '6548-655-987',
          'new_price': '60,00'
        },
        3);
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
            FlatButton(onPressed: testPrint, child: Text('Stampa'))
          ],
        ),
      ),
    );
  }
}
