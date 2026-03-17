package com.example.apktask.data

import com.example.apktask.model.FriendProgress
import com.example.apktask.model.Task
import com.example.apktask.model.UserProfile

/**
 * Interface de synchronisation distante.
 *
 * Implémentations disponibles :
 *  - [MockRemoteRepository]    : données fictives, fonctionne sans backend
 *  - FirebaseRemoteRepository  : production (à créer après setup Firebase)
 *
 * ══════════════════════════════════════════════════════════════════════
 *  GUIDE D'INTÉGRATION FIREBASE — À faire de ton côté
 * ══════════════════════════════════════════════════════════════════════
 *
 *  ÉTAPE 1 — Créer le projet Firebase
 *    → https://console.firebase.google.com
 *    → Nouveau projet > nom "DoIt" > continuer
 *
 *  ÉTAPE 2 — Ajouter l'app Android
 *    → "Ajouter une app" > Android
 *    → Package name : com.example.apktask
 *    → Télécharger google-services.json
 *    → Copier dans : app/google-services.json
 *
 *  ÉTAPE 3 — Activer Authentication
 *    → Firebase Console > Authentication > Sign-in method
 *    → Activer : Google + Email/Password
 *
 *  ÉTAPE 4 — Créer la base Firestore
 *    → Firebase Console > Firestore Database > Créer
 *    → Mode Production (règles sécurisées)
 *    → Copier ces règles dans Firestore Rules :
 *
 *       rules_version = '2';
 *       service cloud.firestore {
 *         match /databases/{database}/documents {
 *           match /users/{userId} {
 *             allow read: if request.auth != null
 *               && (resource.data.isPublic == true
 *                   || request.auth.uid == userId);
 *             allow write: if request.auth.uid == userId;
 *           }
 *           match /users/{userId}/days/{date} {
 *             allow read: if request.auth != null
 *               && get(/databases/$(database)/documents/users/$(userId)).data.isPublic == true;
 *             allow write: if request.auth.uid == userId;
 *           }
 *         }
 *       }
 *
 *  ÉTAPE 5 — Décommenter dans libs.versions.toml et app/build.gradle
 *    → Versions Firebase, dépendances firebase-*, plugin google-services
 *
 *  ÉTAPE 6 — Implémenter FirebaseRemoteRepository
 *    → Créer data/FirebaseRemoteRepository.kt qui implémente RemoteRepository
 *    → Injecter dans TaskRepository et UserRepository à la place de MockRemoteRepository
 *
 *  ÉTAPE 7 — Structure Firestore attendue
 *    /users/{userId}
 *      - displayName: String
 *      - avatarColorIndex: Int
 *      - isPublic: Boolean
 *      - streak: Int
 *    /users/{userId}/days/{YYYY-MM-DD}
 *      - completedCount: Int
 *      - totalCount: Int
 *      - registeredAt: Timestamp
 *
 * ══════════════════════════════════════════════════════════════════════
 */
interface RemoteRepository {

    /** Synchronise les tâches d'une journée vers le cloud. */
    suspend fun syncDayTasks(date: String, tasks: List<Task>): Result<Unit>

    /** Recherche un utilisateur par son friendCode (8 premiers chars de userId). */
    suspend fun findUserByCode(friendCode: String): Result<UserProfile?>

    /** Récupère les compteurs journaliers d'un ami (pas les tâches individuelles). */
    suspend fun getFriendProgress(friendId: String, date: String): Result<FriendProgress?>

    /** Met à jour le profil public de l'utilisateur courant. */
    suspend fun updatePublicProfile(profile: UserProfile): Result<Unit>

    /** Indique si Firebase est configuré et disponible. */
    fun isAvailable(): Boolean
}
