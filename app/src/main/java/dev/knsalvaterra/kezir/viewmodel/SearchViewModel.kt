package dev.knsalvaterra.kezir.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.knsalvaterra.kezir.api.ApiClient
import dev.knsalvaterra.kezir.api.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allEvents: List<Event> = emptyList()

    fun loadEvents() {
        if (preloadedEvents.isNotEmpty()) {
            allEvents = preloadedEvents
            _events.value = allEvents
            return
        }
        fetchAllEvents()
    }

    fun fetchAllEvents() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = ApiClient.api.searchEvents("")
                if (response.isSuccessful) {
                    val result = response.body()?.events ?: emptyList()
                    allEvents = result
                    preloadedEvents = result
                    _events.value = allEvents
                } else {
                    _error.value = "Erro ao carregar eventos"
                }
            } catch (e: Exception) {
                _error.value = "Sem conexão com a internet"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterEvents(query: String) {
        if (query.isEmpty()) {
            _events.value = allEvents
            return
        }

        val filtered = allEvents.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
        _events.value = filtered
    }

    companion object {
        private var preloadedEvents: List<Event> = emptyList()

        fun preloadEvents() {
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.api.searchEvents("")
                    if (response.isSuccessful) {
                        preloadedEvents = response.body()?.events ?: emptyList()
                    }
                } catch (e: Exception) {}
            }
        }
    }
}
