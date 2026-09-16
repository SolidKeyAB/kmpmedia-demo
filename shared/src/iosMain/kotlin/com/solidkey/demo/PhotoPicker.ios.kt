package com.solidkey.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.solidkey.painpoints.image.loading.OGImageFormat
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Largest edge (px) we keep for a picked photo — same budget as the Android picker. A raw gallery
 * shot is often 12 MP+, huge to decode and the source of the in-game flicker; 1280px is crisp on any
 * phone screen while shrinking the file ~10×.
 */
private const val MAX_PICK_DIM = 1280.0

/** JPEG quality for the re-encoded pick — visually lossless for photos, small on disk. */
private const val JPEG_QUALITY = 0.8

/**
 * iOS photo picker — the exact twin of the Android one, so there's **no platform deviation**:
 * `launch()` opens the system photo library ([PHPickerViewController]), then the chosen image is
 * downscaled, re-encoded and saved into the app's Documents dir as `<name>.jpeg`, and handed back as
 * a [PhotoSource.LocalFile] (declaring [OGImageFormat.JPEG] so [com.solidkey.painpoints.image.loading.OGImageFileType]
 * resolves the right path). We re-encode to **JPEG** rather than WebP because iOS can *decode* WebP
 * (since iOS 14) but ships no built-in WebP *encoder*; the library still downsamples, decodes off the
 * main thread and caches, so the picked-photo experience matches Android exactly.
 *
 * Cancellation is a no-op; every failure reports through [onError]. The picker's `delegate` is a weak
 * reference, so we retain [delegate] inside the remembered handle for as long as the screen is shown.
 */
@Composable
actual fun rememberPhotoPicker(
    onPicked: (PhotoSource) -> Unit,
    onError: (String) -> Unit,
): PhotoPickerHandle = remember(onPicked, onError) {
    val delegate = PhotoPickerDelegate(onPicked, onError)
    object : PhotoPickerHandle {
        override fun launch() = delegate.present()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class PhotoPickerDelegate(
    private val onPicked: (PhotoSource) -> Unit,
    private val onError: (String) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    fun present() {
        val config = PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = PHPickerFilter.imagesFilter()
        }
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = this
        val host = topViewController()
        if (host == null) {
            onError("Couldn't open the photo library.")
            return
        }
        host.presentViewController(picker, animated = true, completion = null)
    }

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return // user backed out
        val provider = result.itemProvider

        // Completion runs off the main thread; do the decode/resize/encode there, marshal only the
        // small result callback back to main so we never touch Compose state off the UI thread.
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data: NSData?, _: NSError? ->
            val source = data?.let { savePickedPhoto(it) }
            dispatch_async(dispatch_get_main_queue()) {
                if (source != null) onPicked(source)
                else onError("Couldn't load that photo.")
            }
        }
    }

    /** Downscale, JPEG-encode and write the pick into Documents; returns the [PhotoSource] or null. */
    private fun savePickedPhoto(data: NSData): PhotoSource? {
        val original = UIImage.imageWithData(data) ?: return null
        val scaled = downscaled(original, MAX_PICK_DIM)
        val jpeg = UIImageJPEGRepresentation(scaled, JPEG_QUALITY) ?: return null

        val name = "picked_" + NSUUID().UUIDString
        val dir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return null
        val path = "$dir/$name.${OGImageFormat.JPEG.extension}"
        return if (jpeg.writeToFile(path, atomically = true)) {
            PhotoSource.LocalFile(name, OGImageFormat.JPEG)
        } else null
    }

    /** Redraw [image] so its longest edge is ≤ [maxDim] px (mirrors the library's UIGraphics resize). */
    private fun downscaled(image: UIImage, maxDim: Double): UIImage {
        val (w, h) = image.size.useContents { Pair(width, height) }
        val longest = maxOf(w, h)
        if (longest <= maxDim) return image
        val ratio = maxDim / longest
        val newW = w * ratio
        val newH = h * ratio
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(newW, newH), false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, newW, newH))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return resized ?: image
    }

    /** Top-most presented view controller to host the picker modally. */
    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
        while (top.presentedViewController != null) top = top.presentedViewController!!
        return top
    }
}
