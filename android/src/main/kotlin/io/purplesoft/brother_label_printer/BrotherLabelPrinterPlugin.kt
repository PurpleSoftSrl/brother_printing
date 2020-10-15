package io.purplesoft.brother_label_printer

import androidx.annotation.NonNull
import com.brother.ptouch.sdk.NetPrinter
import com.brother.ptouch.sdk.NetworkDiscovery
import com.brother.ptouch.sdk.Printer
import com.brother.ptouch.sdk.PrinterInfo
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** BrotherPrintingPlugin */
class BrotherLabelPrinterPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "brother_label_printer")
        channel.setMethodCallHandler(this)
    }

    var brotherPrinterDiscovery: NetworkDiscovery? = null
    var brotherPrinter: NetPrinter? = null
    var brotherPrinterBase: Printer? = null
    var brotherPrinterDiscoveryInProgress = false
    var brotherPrinterPrintingInProgress = false
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d("BrotherPrinting", "Method Called: ${call.method}")

        if (call.method == "search") {
            if (brotherPrinterDiscoveryInProgress) {
                result.error("DSCAGNT001", "Discover Agent already running...", "Try later")
            } else {
                val nodeName = call.argument<String>("macAddress")
                val resetConnection = call.argument<Boolean>("resetConnection") ?: false

                val delay = call.argument<Long>("delay") ?: 1000
                val timeOut = call.argument<Long>("timeOut") ?: 60000
                if (resetConnection) {
                    brotherPrinter = null;
                    brotherPrinterBase = null
                    brotherPrinterDiscovery = null
                    brotherPrinterDiscoveryInProgress = false
                    brotherPrinterPrintingInProgress = false
                }
                if (brotherPrinter == null) {
                    brotherPrinterDiscoveryInProgress = true
                    var foundPrinter = "No printers found"
                    Log.d("BrotherPrinting", "Arguments: [MacAddress: $nodeName - Delay: $delay]")
                    brotherPrinterDiscovery = NetworkDiscovery { netPrinter ->
                        foundPrinter = "Printer Found:\r\nModelName: ${netPrinter.modelName}\r\nNodeName: ${netPrinter.nodeName}\r\nLocation: ${netPrinter.location}\r\nIPAddress: ${netPrinter.ipAddress}\r\nMacAddress: ${netPrinter.macAddress}\r\nSerialNumber: ${netPrinter.serNo}"
                        Log.d("Discover Agent", foundPrinter)
                        if (nodeName.isNullOrBlank() || nodeName == netPrinter.macAddress) {
                            brotherPrinter = netPrinter
                            brotherPrinterDiscovery!!.stop()
                            brotherPrinterDiscoveryInProgress = false
                            Log.d("Discover Agent", "Stopped by Result")
                        }
                    }

                    brotherPrinterDiscovery!!.start()
                    Log.d("Discover Agent", "Started")

                    val maxLoopNr = timeOut / delay
                    var currentLoopNr = 0
                    while (brotherPrinterDiscoveryInProgress) {
                        Log.d("Discover Agent", "($currentLoopNr) Working...")
                        if (currentLoopNr > maxLoopNr) {
                            Log.d("Discover Agent", "Timeout reached: $timeOut")
                            brotherPrinterDiscovery!!.stop()
                            brotherPrinterDiscoveryInProgress = false
                            Log.d("Discover Agent", "Stopped by Timeout")
                        }
                        currentLoopNr += 1
                        Thread.sleep(delay)
                    }
                }
                result.success("${brotherPrinter!!.ipAddress} - ${brotherPrinter!!.macAddress}")
            }
        }
        else if (call.method == "printTemplate") {

            if (brotherPrinter == null) {
                result.error("BRPRNOTSET", "Printer not set", "Search the printer first")
            } else {
                val templateId = call.argument<Int>("templateId") ?: 1
                val noc = call.argument<Int>("numberOfCopies") ?: 1
                val labelReplacers = call.argument<Map<String, String>>("replacers")
                Log.d("BrotherPrinting", "- TemplateId: $templateId - Replacers Counter: ${labelReplacers?.count()}")
                /*    while (brotherPrinterPrintingInProgress) {
                        Log.d("BrotherPrinting", "Print already in progress... sleep 500ms")
                        Thread.sleep(500)
                    }*/
                brotherPrinterPrintingInProgress = true
                if (brotherPrinterBase == null) {
                    // Specify Printer
                    brotherPrinterBase = Printer()
                    val settings = brotherPrinterBase!!.printerInfo
                    settings.printerModel = PrinterInfo.Model.QL_820NWB
                    settings.ipAddress = brotherPrinter!!.ipAddress
                    settings.printMode = PrinterInfo.PrintMode.FIT_TO_PAGE
                    settings.numberOfCopies = noc
                    //settings.macAddress = brotherPrinter.macAddress
                    settings.port = PrinterInfo.Port.NET
                    brotherPrinterBase!!.printerInfo = settings
                }
                runBlocking {
                    launch(Dispatchers.Default) {
                        if (brotherPrinterBase!!.startCommunication()) {
                            // Specify the template key and the printer encode
                            if (brotherPrinterBase!!.startPTTPrint(templateId, null)) {
                                if (!labelReplacers.isNullOrEmpty()) {
                                    for (replacer in labelReplacers) {
                                        brotherPrinterBase!!.replaceTextName(replacer.value, replacer.key)
                                    }
                                }

                                // Start print
                                val templatePrintResult = brotherPrinterBase!!.flushPTTPrint()
                                if (templatePrintResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                                    Log.d("TAG", "ERROR - " + templatePrintResult.errorCode)
                                }
                            }
                            brotherPrinterBase!!.endCommunication()
                            brotherPrinterPrintingInProgress = false
                        }
                    }
                }
            }
        }
        else if (call.method == "transferTemplate") {
            if (brotherPrinter == null) {
                result.error("BRPRNOTSET", "Printer not set", "Search the printer first")
            } else {
                brotherPrinterPrintingInProgress = true
                if (brotherPrinterBase == null) {
                    // Specify Printer
                    brotherPrinterBase = Printer()
                    val settings = brotherPrinterBase!!.printerInfo
                    settings.printerModel = PrinterInfo.Model.QL_820NWB
                    settings.ipAddress = brotherPrinter!!.ipAddress
                    settings.printMode = PrinterInfo.PrintMode.FIT_TO_PAGE
                    settings.port = PrinterInfo.Port.NET
                    brotherPrinterBase!!.printerInfo = settings
                }
                runBlocking {
                    launch(Dispatchers.Default) {
                        if (brotherPrinterBase!!.startCommunication()) {
                            val filePath = call.argument<String>("filePath")
                            val templatePrintResult = brotherPrinterBase!!.transfer(filePath)
                            if (templatePrintResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                                Log.d("TAG", "ERROR - " + templatePrintResult.errorCode)
                            }
                            brotherPrinterBase!!.endCommunication()
                            brotherPrinterPrintingInProgress = false
                            Log.d("TAG", "STATUS - " + templatePrintResult.errorCode)
                            //result.success(templatePrintResult.errorCode.toString())
                        }
                    }
                }
            }
        }
        else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}