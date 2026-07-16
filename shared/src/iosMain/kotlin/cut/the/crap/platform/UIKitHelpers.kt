package cut.the.crap.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * The view controller currently on top of the presentation stack, used to present modal UIKit
 * controllers (share sheet, document picker) from otherwise-Compose code. Walks down from the key
 * window's root through any already-presented controllers.
 */
internal fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
