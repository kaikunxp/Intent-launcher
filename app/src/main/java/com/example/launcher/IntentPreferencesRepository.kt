package com.example.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.intentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "remote_launcher_prefs",
)

class IntentPreferencesRepository(private val context: Context) {

    val configFlow: Flow<TargetIntentConfig> = context.intentDataStore.data.map { prefs ->
        mapPreferencesToConfig(prefs)
    }

    suspend fun getConfig(): TargetIntentConfig {
        val prefs = context.intentDataStore.data.first()
        return mapPreferencesToConfig(prefs)
    }

    suspend fun saveConfig(config: TargetIntentConfig) {
        context.intentDataStore.edit { prefs ->
            prefs[KEY_LABEL] = config.label.trim()
            prefs[KEY_PACKAGE] = config.packageName.trim()
            prefs[KEY_ACTIVITY] = config.activityClass.trim()
            prefs[KEY_ACTION] = config.action.trim()
            prefs[KEY_DATA_URI] = config.dataUri.trim()
            prefs[KEY_AUTO_LAUNCH] = config.autoLaunchEnabled
            prefs[KEY_PREFER_PRIVILEGED] = config.preferPrivilegedLaunch
            prefs[KEY_TARGET_KIND] = config.kind.name
            prefs[KEY_HAS_SAVED_CONFIG] = true
        }
    }

    private fun mapPreferencesToConfig(prefs: Preferences): TargetIntentConfig {
        val hasSaved = prefs[KEY_HAS_SAVED_CONFIG] ?: false
        if (!hasSaved) {
            return TargetIntentConfig.Default
        }
        val kind = runCatching {
            TargetKind.valueOf(prefs[KEY_TARGET_KIND] ?: TargetKind.ACTIVITY.name)
        }.getOrDefault(TargetKind.ACTIVITY)

        return TargetIntentConfig(
            label = prefs[KEY_LABEL].orEmpty(),
            packageName = prefs[KEY_PACKAGE].orEmpty(),
            activityClass = prefs[KEY_ACTIVITY].orEmpty(),
            action = prefs[KEY_ACTION].orEmpty(),
            dataUri = prefs[KEY_DATA_URI].orEmpty(),
            autoLaunchEnabled = prefs[KEY_AUTO_LAUNCH] ?: true,
            preferPrivilegedLaunch = prefs[KEY_PREFER_PRIVILEGED] ?: true,
            kind = kind,
        )
    }

    private companion object {
        val KEY_HAS_SAVED_CONFIG = booleanPreferencesKey("has_saved_config")
        val KEY_LABEL = stringPreferencesKey("target_label")
        val KEY_PACKAGE = stringPreferencesKey("target_package")
        val KEY_ACTIVITY = stringPreferencesKey("target_activity")
        val KEY_ACTION = stringPreferencesKey("target_action")
        val KEY_DATA_URI = stringPreferencesKey("target_data_uri")
        val KEY_AUTO_LAUNCH = booleanPreferencesKey("auto_launch_enabled")
        val KEY_PREFER_PRIVILEGED = booleanPreferencesKey("prefer_privileged_launch")
        val KEY_TARGET_KIND = stringPreferencesKey("target_kind")
    }
}
