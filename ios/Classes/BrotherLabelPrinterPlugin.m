#import "BrotherLabelPrinterPlugin.h"
#if __has_include(<brother_label_printer/brother_label_printer-Swift.h>)
#import <brother_label_printer/brother_label_printer-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "brother_label_printer-Swift.h"
#endif

@implementation BrotherLabelPrinterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBrotherLabelPrinterPlugin registerWithRegistrar:registrar];
}
@end
