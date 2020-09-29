import 'dart:async';

import 'package:flutter/services.dart';

class BrotherLabelPrinter {
  static const MethodChannel _channel = const MethodChannel('brother_label_printer');

  static Future<String> search(String macAddress) async {
    final String version = await _channel.invokeMethod('search', {'macAddress': macAddress});
    return version;
  }

  static Future<String> printTemplate(
      int templateId, Map<String, String> replacers, int numberOfCopies) async {
    final String result = await _channel.invokeMethod('printTemplate',
        {'templateId': templateId, 'replacers': replacers, 'numberOfCopies': numberOfCopies});
    return result;
  }
}
