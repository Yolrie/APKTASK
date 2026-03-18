package com.example.apktask.data.db

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom

/**
 * Gestion de la clé de chiffrement de la base de données SQLCipher.
 *
 * Stratégie double couche :
 *  1. Une passphrase aléatoire de 32 octets est générée au premier lancement
 *     via [SecureRandom] (CSPRNG garanti par Android).
 *  2. Cette passphrase est stockée dans [EncryptedSharedPreferences] :
 *     clés chiffrées AES-256-SIV, valeurs chiffrées AES-256-GCM,
 *     protégées par une clé maître dans l'Android Keystore matériel (si disponible).
 *
 * Résultat :
 *  - La base de données SQLCipher est chiffrée AES-256 page-level.
 *  - La passphrase elle-même est chiffrée par le Keystore.
 *  - Même avec un accès root, les données restent protégées sans la clé Keystore.
 *
 * Sécurité :
 *  - [getOrCreatePassphrase] retourne un tableau copié ; l'appelant doit le zeroise
 *    après usage (voir [AppDatabase.buildDatabase]).
 *  - Aucune passphrase en clair dans les logs.
 *  - PREFS_NAME différent de celui de LocalDataSource : isolation des stores.
 */
object DatabaseKeyManager {

    private const val PREFS_NAME = "apktask_db_key_store"
    private const val KEY_PASSPHRASE = "sqlcipher_passphrase"

    /**
     * Retourne la passphrase SQLCipher (32 octets).
     * Crée et persiste une nouvelle passphrase aléatoire au premier appel.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = buildEncryptedPrefs(context)
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) {
            return Base64.decode(stored, Base64.NO_WRAP)
        }
        // Premier lancement : génération d'une passphrase cryptographique aléatoire
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
            .apply()
        return passphrase
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
