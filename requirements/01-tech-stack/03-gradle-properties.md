# `gradle.properties`

This file is **mandatory** at the project root. Each setting is intentional. Document any deviation in a code review.

```properties
# Gradle
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.vfs.watch=true

# Limit parallelism to reduce peak memory during Native release linking
org.gradle.workers.max=1

# Kotlin
kotlin.code.style=official

# Kotlin/Native (iOS)
kotlin.native.binary.smallBinary=true
kotlin.incremental.native=true

# Concurrent Mark and Sweep GC. Reduces stop-the-world pauses during scrolling/animations
# on iOS. Becomes the default in Kotlin 2.4.0-Beta2; we are on 2.3.21, so opt in explicitly.
kotlin.native.binary.gc=cms

# Increase heap for Kotlin/Native compiler (konanc runs on JVM); default is small (~3g)
kotlin.native.jvmArgs=-Xmx6g -XX:+HeapDumpOnOutOfMemoryError

# Kotlin daemon heap (JVM/analysis tasks)
kotlin.daemon.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true

# KSP
ksp.useKSP2=true
ksp.verbose=false
```

## Rationale per setting

### Gradle

- **`org.gradle.jvmargs=-Xmx8g`** — Compose Multiplatform + Kotlin/Native + Room/Koin KSP routinely peak above 4 GB. 8 GB is safe headroom.
- **`org.gradle.caching=true`** — local build cache; saves significant time on incremental builds.
- **`org.gradle.configuration-cache=true`** — configuration cache; further speeds up incremental builds. **Note:** convention plugins must be configuration-cache-compatible (no eager `project.afterEvaluate` access to other projects' state).
- **`org.gradle.parallel=true`** — runs tasks across modules in parallel where the task graph allows.
- **`org.gradle.workers.max=1`** — **counter-intuitive but required**. Kotlin/Native release linking can consume 4–6 GB per worker. With parallel + multiple iOS targets, peak memory exceeds typical CI machines. Limit one worker → linking serializes → peak stays manageable.

### Kotlin/Native

- **`kotlin.native.binary.smallBinary=true`** — strips dead code more aggressively. Reduces iOS framework size.
- **`kotlin.native.binary.gc=cms`** — concurrent mark+sweep. Reduces stop-the-world pauses during scrolling/animations on iOS. Default in Kotlin 2.4.0-Beta2; opt in until then.
- **`kotlin.incremental.native=true`** — incremental Kotlin/Native compilation. Faster Debug iteration.
- **`kotlin.native.jvmArgs=-Xmx6g`** — `konanc` runs on the JVM; default heap is too small for a project of this size.

### Kotlin daemon

- **`kotlin.daemon.jvmargs=-Xmx2g`** — analysis tasks and Compose metrics generation need more than the default. 2 GB is enough.

### Android

- **`android.useAndroidX=true`** — mandatory; the project uses AndroidX.
- **`android.nonTransitiveRClass=true`** — each module gets its own R class; reduces APK size and resource conflicts.

### KSP

- **`ksp.useKSP2=true`** — KSP2 is the current generation (faster, better Kotlin compiler integration).
- **`ksp.verbose=false`** — reduces noise in build output.

## When to deviate

| Situation | Allowed change |
|---|---|
| CI machine has < 16 GB RAM | Drop `org.gradle.jvmargs` to `-Xmx4g`; expect slower builds |
| Local dev only on iOS | `kotlin.native.binary.gc=cms` is still recommended |
| Build is hitting "OutOfMemoryError" in `konanc` | Bump `kotlin.native.jvmArgs` to `-Xmx8g` |
| Build is hitting OOM in Kotlin daemon | Bump `kotlin.daemon.jvmargs` to `-Xmx4g` |
| Configuration cache breaks after adding a plugin | Fix the plugin (don't disable the cache); see Gradle's CC docs |

Never disable `configuration-cache` to "fix" a problem — find the offending task or plugin.
