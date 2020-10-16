package io.purplesoft.brother_label_printer

import android.app.Activity
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
    private val TAG = "brother_label_printer"
    private lateinit var activity: Activity
    var brotherPrinterDiscovery: NetworkDiscovery? = null
    var brotherPrinter: NetPrinter? = null
    var brotherPrinterBase: Printer? = null
    var brotherPrinterDiscoveryInProgress = false
    var brotherPrinterPrintingInProgress = false

    /// The MethodChannel that will the communication between Flutter and native Android
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "brother_label_printer")
        channel.setMethodCallHandler(this)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d(TAG, "Method Called: ${call.method}")
        val args = call.arguments as Map<Any, Any?>

        if (call.method == "search") {
            searchPrinter(args, false, result)
        } else if (call.method == "printTemplate") {
            printTemplate(args, result)
        } else if (call.method == "transferTemplate") {
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
                    settings.isLabelEndCut = true
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
        } else {
            result.notImplemented()
        }
    }

    private fun searchPrinter(arguments: Map<Any, Any?>, privateSearch: Boolean, result: Result) {
        if (brotherPrinterDiscoveryInProgress) {
            if (!privateSearch)
                result.error("DSCAGNT001", "Discover Agent already running...", "Try later")
            //return false
        } else {
            brotherPrinterDiscoveryInProgress = true
            val nodeName: String = (arguments["macAddress"] ?: "") as String
            val resetConnection: Boolean = (arguments["resetConnection"] ?: false) as Boolean

            if (resetConnection) {
                brotherPrinter = null;
                brotherPrinterBase = null
                brotherPrinterDiscovery = null
                brotherPrinterDiscoveryInProgress = false
                brotherPrinterPrintingInProgress = false
            }
            if (this.brotherPrinter == null) {
                brotherPrinterDiscoveryInProgress = true

                Log.d(TAG, "Discovery Init")
                brotherPrinterDiscovery = NetworkDiscovery { netPrinter ->
                    val foundPrinter = "Printer Found:\r\nModelName: ${netPrinter.modelName}\r\nNodeName: ${netPrinter.nodeName}\r\nLocation: ${netPrinter.location}\r\nIPAddress: ${netPrinter.ipAddress}\r\nMacAddress: ${netPrinter.macAddress}\r\nSerialNumber: ${netPrinter.serNo}"
                    Log.d(TAG, foundPrinter)
                    if (nodeName.isNullOrEmpty() || nodeName.equals(netPrinter.macAddress, true)) {
                        Log.d(TAG, "Setting current printer")
                        brotherPrinter = netPrinter
                        Log.d(TAG, "Stop discovery")
                        brotherPrinterDiscoveryInProgress = false
                        brotherPrinterDiscovery?.stop()
                    }
                }
                brotherPrinterDiscovery?.start()

                while (brotherPrinterDiscoveryInProgress) {
                    Thread.sleep(500)
                }


                Log.d(TAG, "Stopped discovery")

                if (!privateSearch) {
                    if (brotherPrinter != null) {
                        result.success("${brotherPrinter!!.ipAddress} - ${brotherPrinter!!.macAddress}")
                    } else {
                        result.error("NOPRTFOUND", "Discover Agent unable to find printers", "Houston we have a problem")
                    }
                }

            }
        }
    }

    private fun printTemplate(arguments: Map<Any, Any?>, result: Result) {
        val macAddress = arguments["macAddress"] as String?

        if (brotherPrinter == null) {
            searchPrinter(arguments, true, result)
            while (brotherPrinterDiscoveryInProgress) {
                Thread.sleep(200)
            }
        }

        if (brotherPrinter == null) {
            result.error("BRPRNOTSET", "Printer not set", "Search the printer first")
        } else {
            val templateId = (arguments["templateId"] ?: 1) as Int
            val noc = (arguments["numberOfCopies"] ?: 1) as Int
            val labelReplacers = arguments["replacers"] as Map<String, String>
            Log.d(TAG, "TemplateId: $templateId - Replacers Counter: ${labelReplacers.count()}")

            brotherPrinterPrintingInProgress = true
            if (brotherPrinterBase == null) {
                // Specify Printer
                brotherPrinterBase = Printer()
                val settings = brotherPrinterBase!!.printerInfo
                settings.printerModel = PrinterInfo.Model.QL_820NWB
                settings.ipAddress = brotherPrinter!!.ipAddress
                settings.printMode = PrinterInfo.PrintMode.FIT_TO_PAGE
                settings.numberOfCopies = noc
                settings.port = PrinterInfo.Port.NET
                brotherPrinterBase!!.printerInfo = settings
            }
            brotherPrinterBase!!.printerInfo.numberOfCopies = noc
            var resultOk = ""
            var resultKOErrorCode = ""
            var resultKOMessage = ""

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

                                resultKOErrorCode = "templatePrintResult.errorCode.toString()"
                                resultKOMessage = "ERROR - $resultKOErrorCode"
                                //resultHandler?.error(templatePrintResult.errorCode.toString(), "ERROR - " + templatePrintResult.errorCode, templateId)

                            } else {
                                resultOk = "label printed"
                            }
                        }
                        brotherPrinterBase!!.endCommunication()
                        brotherPrinterPrintingInProgress = false

                        //resultHandler?.success("label printed")

                    }
                }
            }
            if (resultOk.isBlank()) {
                result.error(resultKOErrorCode, resultKOMessage, templateId)
            } else {
                result.success(resultOk)
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

}