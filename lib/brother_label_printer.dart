import 'dart:async';

import 'package:flutter/services.dart';

class BrotherLabelPrinter {
  static const MethodChannel _channel =
      const MethodChannel('brother_label_printer');

  static Future<String> search(String macAddress, bool resetConnection) async {
    final String result = await _channel.invokeMethod('search',
        {'macAddress': macAddress, 'resetConnection': resetConnection});
    return result;
  }

  static Future<String> transferTemplate(String filePath) async {
    final String result = await _channel.invokeMethod('transferTemplate',
        {'filePath': filePath});
    return result;
  }

  static Future<String> printTemplate(
      int templateId, Map<String, String> replacers, int numberOfCopies) async {
    final String result = await _channel.invokeMethod('printTemplate', {
      'templateId': templateId,
      'replacers': replacers,
      'numberOfCopies': numberOfCopies
    });
    return result;
  }
}
