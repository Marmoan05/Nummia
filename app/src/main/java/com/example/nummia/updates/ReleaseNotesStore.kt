package com.example.nummia.updates

import android.content.SharedPreferences
import java.io.IOException

/** Guarda localmente las notas de una actualización hasta que se hayan mostrado. */
internal object ReleaseNotesStore {
    private const val PENDING_VERSION = "pending_release_notes_version"
    private const val PENDING_TITLE = "pending_release_notes_title"
    private const val PENDING_BODY = "pending_release_notes_body"
    private const val PENDING_PUBLISHED_AT = "pending_release_notes_published_at"
    private const val LAST_SEEN_VERSION = "last_whats_new_version"

    fun savePending(preferences: SharedPreferences, release: AppRelease) {
        val saved = preferences.edit()
            .putString(PENDING_VERSION, release.versionName)
            .putString(PENDING_TITLE, release.title)
            .putString(PENDING_BODY, release.releaseNotes)
            .putString(PENDING_PUBLISHED_AT, release.publishedAt)
            .commit()
        if (!saved) throw IOException("No se pudieron guardar las notas pendientes de la actualización.")
    }

    fun savePending(preferences: SharedPreferences, update: AppUpdate) {
        savePending(
            preferences,
            AppRelease(
                versionName = update.versionName,
                title = update.releaseTitle,
                releaseNotes = update.releaseNotes,
                publishedAt = update.publishedAt
            )
        )
    }

    fun pendingForInstalledVersion(
        preferences: SharedPreferences,
        installedVersion: String
    ): AppRelease? {
        if (preferences.getString(PENDING_VERSION, null) != installedVersion ||
            preferences.getString(LAST_SEEN_VERSION, null) == installedVersion
        ) {
            return null
        }
        return AppRelease(
            versionName = installedVersion,
            title = preferences.getString(PENDING_TITLE, null)
                ?: "Nummia $installedVersion",
            releaseNotes = preferences.getString(PENDING_BODY, null).orEmpty(),
            publishedAt = preferences.getString(PENDING_PUBLISHED_AT, null).orEmpty()
        )
    }

    fun hasPending(preferences: SharedPreferences): Boolean =
        preferences.contains(PENDING_VERSION)

    fun lastSeenVersion(preferences: SharedPreferences): String? =
        preferences.getString(LAST_SEEN_VERSION, null)

    fun shouldShowAfterUpdate(
        preferences: SharedPreferences,
        version: String,
        firstInstallTime: Long,
        lastUpdateTime: Long
    ): Boolean =
        isUpdatedInstall(firstInstallTime, lastUpdateTime) &&
            lastSeenVersion(preferences) != version

    fun markSeen(preferences: SharedPreferences, version: String) {
        val editor = preferences.edit().putString(LAST_SEEN_VERSION, version)
        if (preferences.getString(PENDING_VERSION, null) == version) {
            editor.remove(PENDING_VERSION)
                .remove(PENDING_TITLE)
                .remove(PENDING_BODY)
                .remove(PENDING_PUBLISHED_AT)
        }
        editor.apply()
    }

    fun clearPending(preferences: SharedPreferences, version: String) {
        if (preferences.getString(PENDING_VERSION, null) != version) return
        preferences.edit()
            .remove(PENDING_VERSION)
            .remove(PENDING_TITLE)
            .remove(PENDING_BODY)
            .remove(PENDING_PUBLISHED_AT)
            .apply()
    }

    fun isUpdatedInstall(firstInstallTime: Long, lastUpdateTime: Long): Boolean =
        lastUpdateTime > firstInstallTime
}
