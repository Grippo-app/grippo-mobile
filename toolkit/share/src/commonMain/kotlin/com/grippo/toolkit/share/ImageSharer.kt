package com.grippo.toolkit.share

import androidx.compose.ui.graphics.ImageBitmap

public interface ImageSharer {

    public suspend fun share(image: ImageBitmap, fileName: String)
}
