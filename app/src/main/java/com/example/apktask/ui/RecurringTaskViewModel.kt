package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apktask.data.RecurringTaskRepository
import com.example.apktask.model.Priority
import com.example.apktask.model.RecurrenceRule
import com.example.apktask.model.RecurringTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecurringTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecurringTaskRepository(application)

    private val _routines = MutableStateFlow<List<RecurringTask>>(emptyList())
    val routines: StateFlow<List<RecurringTask>> = _routines.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _routines.value = repository.getAll()
        }
    }

    fun addRoutine(title: String, rule: RecurrenceRule, priority: Priority) {
        val task = RecurringTask(
            id = 0,
            title = title,
            priority = priority,
            rule = rule,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.add(task)
            _routines.value = repository.getAll()
        }
    }

    fun toggleActive(routine: RecurringTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActive(routine.id, !routine.isActive)
            _routines.value = repository.getAll()
        }
    }

    fun delete(routine: RecurringTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(routine.id)
            _routines.value = repository.getAll()
        }
    }
}
