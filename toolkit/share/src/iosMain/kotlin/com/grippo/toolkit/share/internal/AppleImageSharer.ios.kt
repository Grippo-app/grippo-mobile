package com.grippo.toolkit.share.internal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.grippo.toolkit.share.ImageSharer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIActivityTypeSaveToCameraRoll
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

internal class AppleImageSharer : ImageSharer {

    override suspend fun share(image: ImageBitmap, fileName: String) {
        requirePngFileName(fileName)

        val uiImage = withContext(Dispatchers.Default) {
            val png = encodePng(image)
            decodeUIImage(png)
        }

        withContext(Dispatchers.Main) {
            val presenter = topViewController()
                ?: throw IllegalStateException("No view controller to present the share sheet")

            val controller = UIActivityViewController(
                activityItems = listOf(uiImage),
                applicationActivities = null,
            )
            // iOS terminates an app saving to Photos without NSPhotoLibraryAddUsageDescription.
            if (!declaresPhotoLibraryAddUsage()) {
                controller.excludedActivityTypes = listOf(UIActivityTypeSaveToCameraRoll)
            }
            // iPad requires a popover anchor; iPhone ignores it.
            controller.popoverPresentationController?.let { popover ->
                val bounds = presenter.view.bounds
                popover.sourceView = presenter.view
                popover.sourceRect = CGRectMake(
                    x = CGRectGetMidX(bounds),
                    y = CGRectGetMidY(bounds),
                    width = 0.0,
                    height = 0.0,
                )
                popover.permittedArrowDirections = 0u
            }

            presenter.presentViewController(
                viewControllerToPresent = controller,
                animated = true,
                completion = null,
            )
        }
    }

    private fun encodePng(image: ImageBitmap): ByteArray {
        val bitmap = image.asSkiaBitmap()
        return Image.makeFromBitmap(bitmap).use { skiaImage ->
            val data = skiaImage.encodeToData(EncodedImageFormat.PNG)
                ?: throw IllegalStateException("Unable to encode shared image as PNG")
            data.use { encoded -> encoded.bytes }
        }
    }

    private fun decodeUIImage(png: ByteArray): UIImage {
        val data = png.usePinned { pinned ->
            NSData.dataWithBytes(bytes = pinned.addressOf(0), length = png.size.toULong())
        }
        return UIImage.imageWithData(data)
            ?: throw IllegalStateException("Unable to decode shared PNG into UIImage")
    }

    private fun declaresPhotoLibraryAddUsage(): Boolean {
        return NSBundle.mainBundle.objectForInfoDictionaryKey(PHOTO_LIBRARY_ADD_USAGE_KEY) != null
    }

    private fun topViewController(): UIViewController? {
        var controller = currentWindow()?.rootViewController ?: return null
        while (true) {
            controller = controller.presentedViewController ?: return controller
        }
    }

    private fun currentWindow(): UIWindow? {
        val scenes = UIApplication.sharedApplication.connectedScenes.mapNotNull { it as? UIWindowScene }
        val scene = scenes.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?: scenes.firstOrNull()
        return scene?.keyWindow
    }

    private companion object {
        const val PHOTO_LIBRARY_ADD_USAGE_KEY = "NSPhotoLibraryAddUsageDescription"
    }
}
