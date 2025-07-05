# * * * * * * * KOIN PROGUARD RULES START * * * * * * *
# Keep annotation definitions
-keep class org.koin.core.annotation.** { *; }
# Keep classes annotated with Koin annotations
-keep @org.koin.core.annotation.* class * { *; }
# * * * * * * * KOIN PROGUARD RULES END * * * * * * *
