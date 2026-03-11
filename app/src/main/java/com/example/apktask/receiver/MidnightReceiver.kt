package com.example.apktask.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.apktask.data.TaskRepository

/**
 * Recepteur de diffusion système — réinitialise les tâches au changement de date.
 *
 * Sécurité :
 *  - android:exported="false" dans le manifeste (seul le système peut l'invoquer)
 *  - Utilise applicationContext pour éviter les fuites de mémoire
 *  - Aucune donnée sensible n'est tracée en dehors des builds de débogage
 */
class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_DATE_CHANGED) return

        // Suppression sécurisée via le dépôt (données chiffrées)
        TaskRepository(context.applicationContext).clearAll()
    }
}
