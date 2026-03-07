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

        binding.searchEventEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 3) {
                    searchEvents(query)
                } else {
                    eventAdapter.submitList(emptyList())
                }
            }
        })
    }

    private fun searchEvents(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300)
            try {
                val response = ApiClient.api.searchEvents(query)
                if (response.isSuccessful) {
                    eventAdapter.submitList(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}