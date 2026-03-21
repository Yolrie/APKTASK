package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.Task
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InjectionPrefs
import com.example.apktask.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Façade pour les opérations sur les tâches.
 *
 * Toutes les fonctions sont suspend — les appelants lancent depuis
 * viewModelScope ou syncScope selon les besoins.
 *
 * Injection des tâches récurrentes :
 *  - [loadTasks] déclenche [injectDueRecurringTasksIfNeeded] lorsque [date] == aujourd'hui.
 *  - [injectDueRecurringTasksIfNeeded] est protégé par [InjectionPrefs] (vérification rapide)
 *    et par [LocalDataSource.isRecurringTaskInjectedForDate] (vérification Room définitive).
 *  - [injectDueRecurringTasks] est aussi appelé directement par [MidnightResetWorker]
 *    lors du reset minuit pour pré-peupler la nouvelle journée.
 */
class TaskRepository(
    private val context: Context,
    private val remote: RemoteRepository = MockRemoteRepository()
) {
    private val local = LocalDataSource.getInstance(context)

    // ── Tâches par date ──────────────────────────────────────────────────────

    /**
     * Charge les tâches du [date].
     * Si [date] == aujourd'hui et que l'injection n'a pas encore eu lieu,
     * les tâches récurrentes dues sont automatiquement insérées avant le retour.
     */
    suspend fun loadTasks(date: String = DateUtils.today()): List<Task> {
        if (date == DateUtils.today()) injectDueRecurringTasksIfNeeded(date)
        return local.loadTasks(date)
    }

    suspend fun saveTasks(date: String = DateUtils.today(), tasks: List<Task>) {
        local.saveTasks(date, tasks)
        TaskWidgetProvider.refreshAll(context)
        if (remote.isAvailable()) {
            syncScope.launch {
                runCatching { remote.syncDayTasks(date, tasks) }
            }
        }
    }

    suspend fun loadSessionRegistered(date: String = DateUtils.today()): Boolean =
        local.loadSessionRegistered(date)

    suspend fun saveSessionRegistered(date: String = DateUtils.today(), registered: Boolean) =
        local.saveSessionRegistered(date, registered)

    suspend fun clearDay(date: String = DateUtils.today()) =
        local.clearDay(date)

    suspend fun clearAll() =
        local.clearAll()

    // ── Injection des tâches récurrentes ─────────────────────────────────────

    /**
     * Garde rapide : court-circuite l'injection si elle a déjà eu lieu aujourd'hui.
     * Appelle [injectDueRecurringTasks] uniquement si nécessaire, puis met à jour
     * [InjectionPrefs] pour éviter tout appel redondant lors des prochaines ouvertures.
     */
    suspend fun injectDueRecurringTasksIfNeeded(date: String = DateUtils.today()) {
        if (InjectionPrefs.getLastInjectionDate(context) == date) return
        injectDueRecurringTasks(date)
        InjectionPrefs.setLastInjectionDate(context, date)
    }

    /**
     * Injecte dans [date] les tâches récurrentes dues non encore présentes.
     *
     * Algorithme :
     *  1. Demande à [RecurringTaskRepository.getDueForDate] les templates éligibles
     *     (filtre isDueOn + isRecurringTaskInjectedForDate).
     *  2. Calcule les IDs à partir du max existant pour éviter tout conflit.
     *  3. Fusionne avec les tâches existantes et persiste en une seule transaction.
     *
     * Appelé par [injectDueRecurringTasksIfNeeded] (ouverture de l'app)
     * et par [MidnightResetWorker] (reset automatique à minuit).
     */
    suspend fun injectDueRecurringTasks(date: String = DateUtils.today()) {
        val dueTasks = RecurringTaskRepository(context).getDueForDate(date)
        if (dueTasks.isEmpty()) return

        val existing = local.loadTasks(date)
        val baseId = existing.maxOfOrNull { it.id } ?: 0

        val injected = dueTasks.mapIndexed { index, recurring ->
            Task(
                id = baseId + index + 1,
                title = recurring.title,
                priority = recurring.priority,
                date = date,
                recurringTaskId = recurring.id
            )
        }

        local.saveTasks(date, existing + injected)
        TaskWidgetProvider.refreshAll(context)
    }

    companion object {
        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
