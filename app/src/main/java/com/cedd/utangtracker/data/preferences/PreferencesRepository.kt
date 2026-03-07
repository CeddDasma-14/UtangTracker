package com.cedd.utangtracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "utang_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DARK_MODE_KEY        = booleanPreferencesKey("dark_mode")
    private val ANTHROPIC_API_KEY    = stringPreferencesKey("anthropic_api_key")
    private val BIOMETRIC_KEY        = booleanPreferencesKey("biometric_enabled")
    private val LENDER_NAME_KEY      = stringPreferencesKey("lender_name")
    private val IS_PREMIUM_KEY       = booleanPreferencesKey("is_premium")
    private val HAS_SEEN_ONBOARDING  = booleanPreferencesKey("has_seen_onboarding")
    private val HAS_SEEN_TOUR        = booleanPreferencesKey("has_seen_tour")

    val isDarkMode: Flow<Boolean>          = context.dataStore.data.map { it[DARK_MODE_KEY] ?: false }
    val anthropicApiKey: Flow<String>      = context.dataStore.data.map { it[ANTHROPIC_API_KEY] ?: "" }
    val isBiometricEnabled: Flow<Boolean>  = context.dataStore.data.map { it[BIOMETRIC_KEY] ?: false }
    val lenderName: Flow<String>           = context.dataStore.data.map { it[LENDER_NAME_KEY] ?: "" }
    val isPremium: Flow<Boolean>           = context.dataStore.data.map { it[IS_PREMIUM_KEY] ?: false }
    val hasSeenOnboarding: Flow<Boolean>   = context.dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }
    val hasSeenTour: Flow<Boolean>         = context.dataStore.data.map { it[HAS_SEEN_TOUR] ?: false }

    suspend fun setDarkMode(enabled: Boolean)          { context.dataStore.edit { it[DARK_MODE_KEY] = enabled } }
    suspend fun setAnthropicApiKey(key: String)        { context.dataStore.edit { it[ANTHROPIC_API_KEY] = key } }
    suspend fun setBiometricEnabled(enabled: Boolean)  { context.dataStore.edit { it[BIOMETRIC_KEY] = enabled } }
    suspend fun setLenderName(name: String)            { context.dataStore.edit { it[LENDER_NAME_KEY] = name } }
    suspend fun setPremium(enabled: Boolean)           { context.dataStore.edit { it[IS_PREMIUM_KEY] = enabled } }
    suspend fun setSeenOnboarding()                    { context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = true } }
    suspend fun setSeenTour()                          { context.dataStore.edit { it[HAS_SEEN_TOUR] = true } }
}
