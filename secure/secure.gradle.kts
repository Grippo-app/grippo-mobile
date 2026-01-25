import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class SyncSecureConfigs : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val googleAuthProps: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val androidFirebaseSource: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iosFirebaseSource: RegularFileProperty

    @get:OutputFile
    abstract val androidFirebaseTarget: RegularFileProperty

    @get:OutputFile
    abstract val iosFirebaseTarget: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val androidAppGradle: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iosInfoPlist: RegularFileProperty

    @TaskAction
    fun sync() {
        logger.lifecycle("secure: start")

        val propsFile = googleAuthProps.get().asFile
        if (!propsFile.exists()) {
            error("secure: ❌ Missing Google auth properties file: ${propsFile.path}")
        }
        logger.lifecycle("secure: ✅ using ${propsFile.path}")
        val googleProps = loadProps(propsFile)

        val androidFirebaseSourceFile = androidFirebaseSource.get().asFile
        val androidFirebaseTargetFile = androidFirebaseTarget.get().asFile
        if (!androidFirebaseSourceFile.exists()) {
            error("secure: ❌ Missing Android firebase config: ${androidFirebaseSourceFile.path}")
        }
        androidFirebaseSourceFile.copyTo(androidFirebaseTargetFile, overwrite = true)
        logger.lifecycle("secure: ✅ firebase android -> ${androidFirebaseTargetFile.path}")

        val iosFirebaseSourceFile = iosFirebaseSource.get().asFile
        val iosFirebaseTargetFile = iosFirebaseTarget.get().asFile
        if (!iosFirebaseSourceFile.exists()) {
            error("secure: ❌ Missing iOS firebase config: ${iosFirebaseSourceFile.path}")
        }
        iosFirebaseSourceFile.copyTo(iosFirebaseTargetFile, overwrite = true)
        logger.lifecycle("secure: ✅ firebase ios -> ${iosFirebaseTargetFile.path}")

        logger.lifecycle("secure: ✅ applying Google Sign-In identifiers")

        val webClientId = requireProp(googleProps, Keys.webClientId, propsFile)
        val iosClientId = requireProp(googleProps, Keys.iosClientId, propsFile)
        requireProp(googleProps, Keys.androidClientId, propsFile)

        val gidServerClientId = webClientId
        val gidRedirectUri = buildGidRedirectUri(iosClientId)
        val gidReversedClientId = buildGidReversedClientId(iosClientId)

        val androidAppGradleFile = androidAppGradle.get().asFile
        replaceOrError(
            file = androidAppGradleFile,
            regex = Patterns.googleServerClientId,
            label = "manifestPlaceholders[\"GOOGLE_SERVER_CLIENT_ID\"]"
        ) { match ->
            "${match.groupValues[1]}$webClientId${match.groupValues[3]}"
        }

        val iosInfoPlistFile = iosInfoPlist.get().asFile

        replacePlistKeyStringOrError(
            file = iosInfoPlistFile,
            keyName = "GIDClientID",
            value = iosClientId
        )

        replacePlistKeyStringOrError(
            file = iosInfoPlistFile,
            keyName = "GIDServerClientID",
            value = gidServerClientId
        )

        replacePlistKeyStringOrError(
            file = iosInfoPlistFile,
            keyName = "GIDRedirectURI",
            value = gidRedirectUri
        )

        replacePlistFirstUrlSchemeOrError(
            file = iosInfoPlistFile,
            schemeValue = gidReversedClientId
        )

        logger.lifecycle("secure: ✅ updated androidApp/build.gradle.kts and iosApp/iosApp/Info.plist")
        logger.lifecycle("secure: ✅ done")
    }

    private fun loadProps(path: File): Properties =
        Properties().apply { path.inputStream().use { load(it) } }

    private fun requireProp(props: Properties, key: String, propsFile: File): String =
        props.getProperty(key)?.trim()?.takeIf { it.isNotBlank() }
            ?: error("Missing $key in ${propsFile.path}")

    private fun buildGidRedirectUri(clientId: String): String {
        val suffix = ".apps.googleusercontent.com"
        val baseId = clientId.removeSuffix(suffix)
        return "com.googleusercontent.apps.$baseId:/oauthredirect"
    }

    private fun buildGidReversedClientId(clientId: String): String {
        val suffix = ".apps.googleusercontent.com"
        val baseId = clientId.removeSuffix(suffix)
        return "com.googleusercontent.apps.$baseId"
    }

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

    private fun replacePlistKeyStringOrError(
        file: File,
        keyName: String,
        value: String
    ) {
        val content = file.readText()

        val keyRegex = Regex(
            "(<key>${Regex.escape(keyName)}</key>\\s*<string>)([\\s\\S]*?)(</string>)",
            RegexOption.DOT_MATCHES_ALL
        )

        val match = keyRegex.find(content) ?: error("Missing $keyName in ${file.path}")

        val updated = content.replaceRange(
            match.range,
            "${match.groupValues[1]}${value.trim()}${match.groupValues[3]}"
        )

        file.writeText(updated)
    }

    private fun replacePlistFirstUrlSchemeOrError(
        file: File,
        schemeValue: String
    ) {
        val content = file.readText()

        val schemeRegex = Regex(
            "(<key>CFBundleURLSchemes</key>[\\s\\S]*?<array>[\\s\\S]*?<string>)([\\s\\S]*?)(</string>)",
            RegexOption.DOT_MATCHES_ALL
        )

        val match = schemeRegex.find(content) ?: error("Missing CFBundleURLSchemes in ${file.path}")

        val updated = content.replaceRange(
            match.range,
            "${match.groupValues[1]}${schemeValue.trim()}${match.groupValues[3]}"
        )

        file.writeText(updated)
    }

    private object Keys {
        const val webClientId = "GOOGLE_CLIENT_ID_WEB"
        const val androidClientId = "GOOGLE_CLIENT_ID_ANDROID"
        const val iosClientId = "GOOGLE_CLIENT_ID_IOS"
    }

    private object Patterns {
        val googleServerClientId =
            Regex("(manifestPlaceholders\\[\\\"GOOGLE_SERVER_CLIENT_ID\\\"\\]\\s*=\\s*\\\")([^\"]*)(\\\")")
    }
}

tasks.register<SyncSecureConfigs>("syncSecureConfigs") {
    group = "secure"
    description = "Copies Firebase configs and Google Sign-In identifiers into app files."

    googleAuthProps.set(layout.projectDirectory.file("secure/google/google-auth.properties"))
    androidFirebaseSource.set(layout.projectDirectory.file("secure/firebase/google-services.json"))
    iosFirebaseSource.set(layout.projectDirectory.file("secure/firebase/GoogleService-Info.plist"))
    androidFirebaseTarget.set(layout.projectDirectory.file("androidApp/google-services.json"))
    iosFirebaseTarget.set(layout.projectDirectory.file("iosApp/GoogleService-Info.plist"))
    androidAppGradle.set(layout.projectDirectory.file("androidApp/build.gradle.kts"))
    iosInfoPlist.set(layout.projectDirectory.file("iosApp/iosApp/Info.plist"))
}
