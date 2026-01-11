import java.util.Properties

// Centralized file locations to keep the task readable.
private class Paths {
    val androidFirebaseTarget = file("androidApp/google-services.json")
    val iosFirebaseTarget = file("iosApp/GoogleService-Info.plist")
    val androidFirebaseSource = file("secure/firebase/google-services.json")
    val iosFirebaseSource = file("secure/firebase/GoogleService-Info.plist")

    val androidAppGradle = file("androidApp/build.gradle.kts")
    val iosInfoPlist = file("iosApp/iosApp/Info.plist")

    val googleAuthProps = file("secure/google/google-auth.properties")
}

private val paths = Paths()

// Property names expected in secure/google/google-auth.properties.
private object Keys {
    const val googleServerClientId = "GOOGLE_SERVER_CLIENT_ID"
    const val gidClientId = "GID_CLIENT_ID"
    const val gidServerClientId = "GID_SERVER_CLIENT_ID"
    const val gidRedirectUri = "GID_REDIRECT_URI"
    const val gidReversedClientId = "GID_REVERSED_CLIENT_ID"
}

// Regex patterns to update values without relying on line numbers.
private object Patterns {
    val googleServerClientId =
        Regex("(manifestPlaceholders\\[\"GOOGLE_SERVER_CLIENT_ID\"\\]\\s*=\\s*\")([^\"]*)(\")")
    val gidClientId =
        Regex("(<key>GIDClientID</key>\\s*<string>)(.*?)(</string>)", RegexOption.DOT_MATCHES_ALL)
    val gidServerClientId =
        Regex(
            "(<key>GIDServerClientID</key>\\s*<string>)(.*?)(</string>)",
            RegexOption.DOT_MATCHES_ALL
        )
    val gidRedirectUri =
        Regex(
            "(<key>GIDRedirectURI</key>\\s*<string>)(.*?)(</string>)",
            RegexOption.DOT_MATCHES_ALL
        )
    val gidReversedClientId =
        Regex(
            "(<key>CFBundleURLSchemes</key>\\s*<array>\\s*<string>)(.*?)(</string>)",
            RegexOption.DOT_MATCHES_ALL
        )
}

private fun loadProps(path: File): Properties =
    Properties().apply { path.inputStream().use { load(it) } }

private fun findProp(props: Properties, key: String): String? =
    props.getProperty(key)?.trim()?.takeIf { it.isNotBlank() }

private fun requireProp(props: Properties, key: String): String =
    findProp(props, key) ?: error("Missing $key in ${paths.googleAuthProps.path}")

private fun replaceOrError(
    file: File,
    regex: Regex,
    label: String,
    replacer: (MatchResult) -> String
) {
    val content = file.readText()
    val match = regex.find(content) ?: error("Missing $label in ${file.path}")
    val updated = content.replaceRange(match.range, replacer(match))
    file.writeText(updated)
}

tasks.register("syncSecureConfigs") {
    group = "secure"
    description = "Copies Firebase configs and Google Sign-In identifiers into app files."

    doLast {
        logger.lifecycle("secure: start")
        if (!paths.googleAuthProps.exists()) {
            error("Missing Google auth properties file: ${paths.googleAuthProps.path}")
        }
        logger.lifecycle("secure: using ${paths.googleAuthProps.path}")
        val googleProps = loadProps(paths.googleAuthProps)

        if (paths.androidFirebaseSource.exists()) {
            paths.androidFirebaseSource.copyTo(paths.androidFirebaseTarget, overwrite = true)
            logger.lifecycle(
                "secure: firebase android -> ${paths.androidFirebaseTarget.path}"
            )
        } else {
            error("Missing Android firebase config: ${paths.androidFirebaseSource.path}")
        }

        if (paths.iosFirebaseSource.exists()) {
            paths.iosFirebaseSource.copyTo(paths.iosFirebaseTarget, overwrite = true)
            logger.lifecycle(
                "secure: firebase ios -> ${paths.iosFirebaseTarget.path}"
            )
        } else {
            error("Missing iOS firebase config: ${paths.iosFirebaseSource.path}")
        }

        logger.lifecycle("secure: applying Google Sign-In identifiers")
        val googleServerClientId = requireProp(googleProps, Keys.googleServerClientId)
        val gidClientId = requireProp(googleProps, Keys.gidClientId)
        val gidServerClientId = requireProp(googleProps, Keys.gidServerClientId)
        val gidRedirectUri = requireProp(googleProps, Keys.gidRedirectUri)
        val gidReversedClientId = requireProp(googleProps, Keys.gidReversedClientId)

        replaceOrError(
            paths.androidAppGradle,
            Patterns.googleServerClientId,
            "manifestPlaceholders[\"GOOGLE_SERVER_CLIENT_ID\"]"
        ) { match ->
            "${match.groupValues[1]}$googleServerClientId${match.groupValues[3]}"
        }

        replaceOrError(
            paths.iosInfoPlist,
            Patterns.gidClientId,
            "GIDClientID"
        ) { match ->
            "${match.groupValues[1]}$gidClientId${match.groupValues[3]}"
        }

        replaceOrError(
            paths.iosInfoPlist,
            Patterns.gidServerClientId,
            "GIDServerClientID"
        ) { match ->
            "${match.groupValues[1]}$gidServerClientId${match.groupValues[3]}"
        }

        replaceOrError(
            paths.iosInfoPlist,
            Patterns.gidRedirectUri,
            "GIDRedirectURI"
        ) { match ->
            "${match.groupValues[1]}$gidRedirectUri${match.groupValues[3]}"
        }

        replaceOrError(
            paths.iosInfoPlist,
            Patterns.gidReversedClientId,
            "CFBundleURLSchemes"
        ) { match ->
            "${match.groupValues[1]}$gidReversedClientId${match.groupValues[3]}"
        }
        logger.lifecycle("secure: updated androidApp/build.gradle.kts and iosApp/iosApp/Info.plist")
        logger.lifecycle("secure: done")
    }
}
