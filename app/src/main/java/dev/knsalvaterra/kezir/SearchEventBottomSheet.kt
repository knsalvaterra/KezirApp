package dev.knsalvaterra.kezir

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.knsalvaterra.kezir.api.ApiClient
import dev.knsalvaterra.kezir.api.Event
import dev.knsalvaterra.kezir.databinding.LayoutSearchEventBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchEventBottomSheet(private val onEventSelected: (Event) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: LayoutSearchEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutSearchEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        eventAdapter = EventAdapter { event ->
            onEventSelected(event)
            dismiss()
        }
        binding.eventsRecyclerView.adapter = eventAdapter

        // Ensure test events are displayed immediately when ready
        binding.eventsRecyclerView.post {
            showTestEvents()
        }

        binding.searchEventEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


            }
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 3) {
                    searchEvents(query)
                } else if (query.isEmpty()) {
                    showTestEvents()
                }
            }
        })
    }

    private fun showTestEvents() {
        val testEvents = listOf(
            Event("664544741697781760", "Evento de Teste 1", "20 Mai 2024", "Lisboa, Portugal"),
            Event("2", "Concerto de Verão", "15 Jun 2024", "Porto, Portugal"),
            Event("3", "Conferência Tech", "10 Jul 2024", "S. Tomé"),
            Event("4", "Festival de Arte", "05 Ago 2024", "Cascais, Portugal"),
            Event("5", "Maratona Kezir", "12 Set 2024", "Funchal, Madeira")
        )
        eventAdapter.submitList(testEvents)
    }

    private fun searchEvents(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300)
            try {
                val response = ApiClient.api.searchEvents(query)
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    eventAdapter.submitList(results)
                }
            } catch (e: Exception) {
                // Keep showing previous or test events on error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}