package com.cloudlink.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.cloudlink.app.data.model.AuthType

@Singleton
class CredentialManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_server_credentials",
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(serverId: Int, password: String) {
        require(serverId > 0 && password.isNotEmpty()) { "A valid server and password are required." }
        check(sharedPreferences.edit().putString("pwd_$serverId", password).commit()) {
            "The password could not be stored securely."
        }
    }

    fun getPassword(serverId: Int): String? {
        return sharedPreferences.getString("pwd_$serverId", null)
    }

    fun savePrivateKey(serverId: Int, privateKey: String) {
        require(serverId > 0 && privateKey.isNotEmpty()) { "A valid server and private key are required." }
        check(sharedPreferences.edit().putString("key_$serverId", privateKey).commit()) {
            "The private key could not be stored securely."
        }
    }

    fun getPrivateKey(serverId: Int): String? {
        return sharedPreferences.getString("key_$serverId", null)
    }

    fun deleteCredentials(serverId: Int) {
        check(sharedPreferences.edit()
            .remove("pwd_$serverId")
            .remove("key_$serverId")
            .commit()) { "The saved credential could not be removed securely." }
    }

    fun replaceCredential(serverId: Int, authType: AuthType, credential: String) {
        require(serverId > 0 && credential.isNotEmpty()) { "A credential is required." }
        val editor = sharedPreferences.edit()
            .remove("pwd_$serverId")
            .remove("key_$serverId")
        when (authType) {
            AuthType.PASSWORD -> editor.putString("pwd_$serverId", credential)
            AuthType.KEY -> editor.putString("key_$serverId", credential)
        }
        check(editor.commit()) { "The credential could not be stored securely." }
    }
}
