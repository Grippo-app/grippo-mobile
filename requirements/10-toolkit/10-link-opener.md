# `:toolkit:link-opener` — Open URLs

A trivial multiplatform wrapper for opening URLs in the system browser.

## API

```kotlin
public interface LinkOpener {
    public fun open(url: String): LinkOpenResult
}

public data class LinkOpenResult(
    public val isOpened: Boolean,
    public val resolvedHandler: String? = null,
)
```

`isOpened` reports whether the system actually launched a handler; `resolvedHandler` exposes the Android package that resolved the intent (null on iOS). Most callers ignore the result — it's there so analytics / diagnostics can record which app handled a deeplink.

## Implementations

### Android (`AndroidLinkOpener`)

```kotlin
internal class AndroidLinkOpener(private val context: Context) : LinkOpener {
    override fun open(url: String): LinkOpenResult {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?.takeIf { !it.scheme.isNullOrBlank() }
            ?: return LinkOpenResult(isOpened = false)

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = runCatching {
            intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY)?.packageName
        }.getOrNull()

        return runCatching {
            context.startActivity(intent)
            LinkOpenResult(isOpened = true, resolvedHandler = resolved)
        }.getOrNull() ?: LinkOpenResult(isOpened = false)
    }
}
```

The Android impl takes raw `Context` (not `NativeContext`) — the platform `LinkOpenerFactory.android.kt` reaches into `NativeContext.context` and constructs `AndroidLinkOpener(context)`.

### iOS (`AppleLinkOpener`)

```kotlin
internal class AppleLinkOpener : LinkOpener {
    override fun open(url: String): LinkOpenResult {
        val nsUrl = NSURL(string = url)
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(nsUrl)) return LinkOpenResult(isOpened = false)

        val opened = runCatching {
            app.openURL(url = nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
            true
        }.getOrDefault(false)
        return LinkOpenResult(isOpened = opened)
    }
}
```

## Usage

```kotlin
internal class TermsOfServiceViewModel(
    private val linkOpener: LinkOpener,
) : BaseViewModel<...>(...), TermsOfServiceContract {

    override fun onTermsClick() {
        linkOpener.open("https://<your-product-domain>.com/terms")
    }
}
```

## Rules

- **Open `https://` URLs only.** No custom schemes, no `tel:` / `mailto:` (those have their own helpers).
- **Treat `LinkOpenResult.isOpened == false` as advisory** — the user pressed a link; if nothing handles it, fall back to a Toast/Snackbar at most. Don't escalate to a full `AppError` dialog.
- The Android impl already drops malformed URIs early (empty/missing scheme returns `LinkOpenResult(isOpened = false)`).

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.link.opener" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
    }
}
```

## Anti-patterns

- **Custom in-app browsers** via `WebView`/`WKWebView`. Use the system browser unless there's a strong reason (e.g. OAuth flows handled in `CustomTabs`).
- **Branching loudly on `LinkOpenResult.isOpened`.** Most call sites can ignore it; reserve it for analytics / diagnostics.
