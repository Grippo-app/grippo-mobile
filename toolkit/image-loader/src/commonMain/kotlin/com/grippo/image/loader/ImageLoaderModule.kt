package com.grippo.image.loader

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.grippo.image.loader.internal.ImageLoaderInitializer
import com.grippo.toolkit.http.client.HttpModule
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [HttpModule::class])
@ComponentScan
public class ImageLoaderModule {

    @Single
    internal fun imageLoaderFactory(
        httpClient: HttpClient,
    ): SingletonImageLoader.Factory {
        return SingletonImageLoader.Factory { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory(httpClient)) }
                .build()
        }
    }

    @Single(createdAtStart = true)
    internal fun imageLoaderInitializer(
        factory: SingletonImageLoader.Factory,
    ): ImageLoaderInitializer = ImageLoaderInitializer(factory)
}
