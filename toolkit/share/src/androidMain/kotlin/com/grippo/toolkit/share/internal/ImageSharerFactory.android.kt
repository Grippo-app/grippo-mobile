package com.grippo.toolkit.share.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.share.ImageSharer

internal actual fun NativeContext.getImageSharer(): ImageSharer {
    return AndroidImageSharer(context)
}
