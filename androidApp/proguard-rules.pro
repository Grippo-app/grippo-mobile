# * * * * * * * KOIN PROGUARD RULES START * * * * * * *
# Keep annotation definitions
-keep class org.koin.core.annotation.** { *; }
# Keep classes annotated with Koin annotations
-keep @org.koin.core.annotation.* class * { *; }
# * * * * * * * KOIN PROGUARD RULES END * * * * * * *

# Something strange (Maybe Koog)
-dontwarn io.micrometer.context.ContextAccessor
-dontwarn io.netty.util.internal.logging.InternalLogger
-dontwarn io.netty.util.internal.logging.InternalLoggerFactory
-dontwarn javax.enterprise.inject.spi.Extension
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
# * * * * * * * MAYBE KOOG * * * * * * *
