package com.grippo.toolkit.image.loader.internal

import coil3.SingletonImageLoader

internal class ImageLoaderInitializer(factory: SingletonImageLoader.Factory) {
    init {
        SingletonImageLoader.setSafe(factory)
    }
}