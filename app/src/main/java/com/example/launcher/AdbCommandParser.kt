package com.example.launcher

import android.content.Intent

data class ParsedAdbResult(
    val config: TargetIntentConfig?,
    val summary: String,
    val error: String? = null,
)

object AdbCommandParser {

    fun parse(
        rawInput: String,
        currentAutoLaunch: Boolean = true,
        currentPreferPrivileged: Boolean = true,
    ): ParsedAdbResult {
        val cleaned = rawInput.trim()
            .replace("\\\n", " ")
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()
        if (cleaned.isBlank()) {
            return ParsedAdbResult(
                config = null,
                summary = "",
                error = "Paste an ADB command, component (pkg/activity), intent URI, or action.",
            )
        }

        if (cleaned.startsWith("intent:", ignoreCase = true)) {
            return parseIntentUri(cleaned, currentAutoLaunch, currentPreferPrivileged)
        }

        val tokens = tokenizeShellArgs(cleaned)
        if (tokens.isEmpty()) {
            return ParsedAdbResult(
                config = null,
                summary = "",
                error = "Could not parse any tokens from input.",
            )
        }

        val normalizedTokens = stripAdbShellPrefix(tokens)
        if (normalizedTokens.isEmpty()) {
            return ParsedAdbResult(
                config = null,
                summary = "",
                error = "Incomplete adb shell command.",
            )
        }

        if (normalizedTokens.size >= 4 &&
            normalizedTokens[0] == "cmd" &&
            normalizedTokens[1] == "statusbar" &&
            normalizedTokens[2] == "click-tile"
        ) {
            val componentToken = normalizedTokens[3]
            val (pkg, cls) = splitComponent(componentToken)
            if (pkg.isBlank() || cls.isBlank()) {
                return ParsedAdbResult(
                    config = null,
                    summary = "",
                    error = "click-tile requires package/TileServiceClass format.",
                )
            }
            val label = cls.substringAfterLast('.')
            val config = TargetIntentConfig(
                label = "$label (QS Tile)",
                packageName = pkg,
                activityClass = cls,
                autoLaunchEnabled = currentAutoLaunch,
                preferPrivilegedLaunch = true,
                kind = TargetKind.QS_TILE,
                isConfigured = true,
            )
            return ParsedAdbResult(
                config = config,
                summary = "QS Tile: $pkg/$cls",
            )
        }

        if (normalizedTokens.firstOrNull() == "am") {
            val subCmd = normalizedTokens.getOrNull(1)
            if (subCmd != "start" && subCmd != "start-activity") {
                return ParsedAdbResult(
                    config = null,
                    summary = "",
                    error = "Only 'am start', 'am start-activity', or 'cmd statusbar click-tile' commands are supported.",
                )
            }
            return parseAmStartTokens(
                args = normalizedTokens.drop(2),
                currentAutoLaunch = currentAutoLaunch,
                currentPreferPrivileged = currentPreferPrivileged,
            )
        }

        if (normalizedTokens.firstOrNull() == "start" || normalizedTokens.firstOrNull() == "start-activity") {
            return parseAmStartTokens(
                args = normalizedTokens.drop(1),
                currentAutoLaunch = currentAutoLaunch,
                currentPreferPrivileged = currentPreferPrivileged,
            )
        }

        if (normalizedTokens.size == 1) {
            val single = normalizedTokens.first()
            return parseSingleToken(single, currentAutoLaunch, currentPreferPrivileged)
        }

        if (normalizedTokens.any { it.startsWith("-") }) {
            return parseAmStartTokens(
                args = normalizedTokens,
                currentAutoLaunch = currentAutoLaunch,
                currentPreferPrivileged = currentPreferPrivileged,
            )
        }

        return ParsedAdbResult(
            config = null,
            summary = "",
            error = "Unrecognized format. Try 'am start -n pkg/activity', 'pkg/activity', or a package name.",
        )
    }

    private fun parseIntentUri(
        uriText: String,
        currentAutoLaunch: Boolean,
        currentPreferPrivileged: Boolean,
    ): ParsedAdbResult {
        return try {
            val parsed = Intent.parseUri(uriText, Intent.URI_INTENT_SCHEME)
            val component = parsed.component
            val pkg = component?.packageName ?: parsed.`package`.orEmpty()
            val cls = component?.className.orEmpty()
            val action = parsed.action.orEmpty()
            val dataUri = parsed.dataString.orEmpty()
            val category = parsed.categories?.firstOrNull().orEmpty()
            val mimeType = parsed.type.orEmpty()

            if (pkg.isBlank() && action.isBlank() && dataUri.isBlank()) {
                return ParsedAdbResult(
                    config = null,
                    summary = "",
                    error = "Intent URI did not specify a package, action, or data URI.",
                )
            }

            val label = cls.substringAfterLast('.').ifBlank {
                pkg.ifBlank { action.substringAfterLast('.').ifBlank { "Custom Intent" } }
            }
            val config = TargetIntentConfig(
                label = label,
                packageName = pkg,
                activityClass = cls,
                action = action,
                dataUri = dataUri,
                category = category,
                mimeType = mimeType,
                autoLaunchEnabled = currentAutoLaunch,
                preferPrivilegedLaunch = currentPreferPrivileged,
                kind = TargetKind.ACTIVITY,
                isConfigured = true,
            )
            ParsedAdbResult(config = config, summary = "Parsed intent URI: ${config.displayTarget}")
        } catch (e: Exception) {
            ParsedAdbResult(
                config = null,
                summary = "",
                error = "Invalid intent URI: ${e.localizedMessage ?: "parse error"}",
            )
        }
    }

    private fun parseSingleToken(
        token: String,
        currentAutoLaunch: Boolean,
        currentPreferPrivileged: Boolean,
    ): ParsedAdbResult {
        if (token.contains("://")) {
            val config = TargetIntentConfig(
                label = token.substringBefore("://").uppercase() + " Link",
                action = Intent.ACTION_VIEW,
                dataUri = token,
                autoLaunchEnabled = currentAutoLaunch,
                preferPrivilegedLaunch = currentPreferPrivileged,
                kind = TargetKind.ACTIVITY,
                isConfigured = true,
            )
            return ParsedAdbResult(config = config, summary = "Deep link URI: $token")
        }

        if (token.contains("/")) {
            val (pkg, cls) = splitComponent(token)
            if (pkg.isBlank() || cls.isBlank()) {
                return ParsedAdbResult(
                    config = null,
                    summary = "",
                    error = "Component must be in package/activity format.",
                )
            }
            val config = TargetIntentConfig(
                label = cls.substringAfterLast('.'),
                packageName = pkg,
                activityClass = cls,
                autoLaunchEnabled = currentAutoLaunch,
                preferPrivilegedLaunch = currentPreferPrivileged,
                kind = TargetKind.ACTIVITY,
                isConfigured = true,
            )
            return ParsedAdbResult(config = config, summary = "Component: $pkg/$cls")
        }

        val isLikelyAction = token.contains(".action.") ||
            token.startsWith("android.settings.") ||
            token.startsWith("android.intent.") ||
            token == token.uppercase()
        if (isLikelyAction) {
            val label = token.substringAfterLast('.').replace('_', ' ').lowercase()
                .replaceFirstChar { it.uppercase() }
            val config = TargetIntentConfig(
                label = label,
                action = token,
                autoLaunchEnabled = currentAutoLaunch,
                preferPrivilegedLaunch = currentPreferPrivileged,
                kind = TargetKind.ACTIVITY,
                isConfigured = true,
            )
            return ParsedAdbResult(config = config, summary = "Action: $token")
        }

        val config = TargetIntentConfig(
            label = token.substringAfterLast('.'),
            packageName = token,
            autoLaunchEnabled = currentAutoLaunch,
            preferPrivilegedLaunch = currentPreferPrivileged,
            kind = TargetKind.ACTIVITY,
            isConfigured = true,
        )
        return ParsedAdbResult(config = config, summary = "Package: $token")
    }

    private fun parseAmStartTokens(
        args: List<String>,
        currentAutoLaunch: Boolean,
        currentPreferPrivileged: Boolean,
    ): ParsedAdbResult {
        var pkg = ""
        var cls = ""
        var action = ""
        var dataUri = ""
        var category = ""
        var mimeType = ""
        val extraArgs = mutableListOf<String>()

        var i = 0
        while (i < args.size) {
            val flag = args[i]
            when (flag) {
                "-n", "--component" -> {
                    val value = args.getOrNull(i + 1).orEmpty()
                    if (value.contains("/")) {
                        val (parsedPkg, parsedCls) = splitComponent(value)
                        pkg = parsedPkg
                        cls = parsedCls
                    } else if (value.isNotBlank()) {
                        pkg = value
                    }
                    i += 2
                }

                "-a", "--action" -> {
                    action = args.getOrNull(i + 1).orEmpty()
                    i += 2
                }

                "-d", "--data" -> {
                    dataUri = args.getOrNull(i + 1).orEmpty()
                    i += 2
                }

                "-p", "--package" -> {
                    pkg = args.getOrNull(i + 1).orEmpty()
                    i += 2
                }

                "-c", "--category" -> {
                    val cat = args.getOrNull(i + 1).orEmpty()
                    if (category.isBlank()) {
                        category = cat
                    } else {
                        extraArgs.add("-c")
                        extraArgs.add(cat)
                    }
                    i += 2
                }

                "-t", "--type" -> {
                    mimeType = args.getOrNull(i + 1).orEmpty()
                    i += 2
                }

                "--user" -> {
                    // Ignore adb --user 0 wrapper so our UserService runs under its own UID cleanly
                    i += 2
                }

                "-W", "-S", "-R", "-D", "-N", "-P", "--activity-clear-task", "--activity-new-task",
                "--activity-single-top", "--activity-clear-top", "--activity-no-history",
                -> {
                    if (flag == "-R" || flag == "-P") {
                        i += 2
                    } else {
                        i += 1
                    }
                }

                "-f", "--flags", "--es", "--ez", "--ei", "--el", "--ef", "--eu", "--ecn",
                "--eia", "--ela", "--efa", "--grant-read-uri-permission", "--grant-write-uri-permission",
                -> {
                    if (flag.startsWith("--grant-")) {
                        extraArgs.add(flag)
                        i += 1
                    } else if (flag == "-f" || flag == "--flags") {
                        val v = args.getOrNull(i + 1)
                        if (v != null) {
                            extraArgs.add(flag)
                            extraArgs.add(v)
                        }
                        i += 2
                    } else {
                        val key = args.getOrNull(i + 1)
                        val value = args.getOrNull(i + 2)
                        if (key != null && value != null) {
                            extraArgs.add(flag)
                            extraArgs.add(quoteIfNeeded(key))
                            extraArgs.add(quoteIfNeeded(value))
                        }
                        i += 3
                    }
                }

                else -> {
                    if (!flag.startsWith("-")) {
                        if (flag.contains("/") && pkg.isBlank() && cls.isBlank()) {
                            val (parsedPkg, parsedCls) = splitComponent(flag)
                            pkg = parsedPkg
                            cls = parsedCls
                        } else if (flag.contains("://") && dataUri.isBlank()) {
                            dataUri = flag
                            if (action.isBlank()) action = Intent.ACTION_VIEW
                        } else if (pkg.isBlank() && flag.contains(".")) {
                            pkg = flag
                        }
                    }
                    i += 1
                }
            }
        }

        if (pkg.isBlank() && action.isBlank() && dataUri.isBlank()) {
            return ParsedAdbResult(
                config = null,
                summary = "",
                error = "Could not find a package (-p / -n), action (-a), or data URI (-d).",
            )
        }

        val label = cls.substringAfterLast('.').ifBlank {
            action.substringAfterLast('.').ifBlank {
                pkg.substringAfterLast('.').ifBlank { "Custom Intent" }
            }
        }
        val config = TargetIntentConfig(
            label = label,
            packageName = pkg,
            activityClass = cls,
            action = action,
            dataUri = dataUri,
            category = category,
            mimeType = mimeType,
            extraArgs = extraArgs.joinToString(" "),
            autoLaunchEnabled = currentAutoLaunch,
            preferPrivilegedLaunch = currentPreferPrivileged,
            kind = TargetKind.ACTIVITY,
            isConfigured = true,
        )
        return ParsedAdbResult(
            config = config,
            summary = "Parsed am start: ${config.displayTarget}",
        )
    }

    private fun stripAdbShellPrefix(tokens: List<String>): List<String> {
        var idx = 0
        if (tokens.getOrNull(idx) == "adb") {
            idx++
            while (idx < tokens.size) {
                val tok = tokens[idx]
                if (tok == "-s" || tok == "-t" || tok == "-H" || tok == "-P") {
                    idx += 2
                } else if (tok == "-d" || tok == "-e" || tok == "-a") {
                    idx += 1
                } else {
                    break
                }
            }
        }
        if (tokens.getOrNull(idx) == "shell") {
            idx++
        }
        return tokens.drop(idx)
    }

    private fun splitComponent(raw: String): Pair<String, String> {
        val pkg = raw.substringBefore('/').trim()
        val rawCls = raw.substringAfter('/', "").trim()
        val resolvedCls = when {
            rawCls.isBlank() -> ""
            rawCls.startsWith(".") && pkg.isNotBlank() -> "$pkg$rawCls"
            else -> rawCls
        }
        return pkg to resolvedCls
    }

    fun tokenizeShellArgs(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaping = false

        for (ch in input) {
            if (escaping) {
                current.append(ch)
                escaping = false
                continue
            }
            when (ch) {
                '\\' -> {
                    if (inSingleQuote) {
                        current.append(ch)
                    } else {
                        escaping = true
                    }
                }
                '\'' -> {
                    if (!inDoubleQuote) {
                        inSingleQuote = !inSingleQuote
                    } else {
                        current.append(ch)
                    }
                }
                '"' -> {
                    if (!inSingleQuote) {
                        inDoubleQuote = !inDoubleQuote
                    } else {
                        current.append(ch)
                    }
                }
                else -> {
                    if (!inSingleQuote && !inDoubleQuote && ch.isWhitespace()) {
                        if (current.isNotEmpty()) {
                            tokens.add(current.toString())
                            current.clear()
                        }
                    } else {
                        current.append(ch)
                    }
                }
            }
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    private fun quoteIfNeeded(token: String): String {
        return if (token.any { it.isWhitespace() || it == '"' || it == '\'' }) {
            "\"${token.replace("\"", "\\\"")}\""
        } else {
            token
        }
    }
}
