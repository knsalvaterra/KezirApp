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
import kotlinx.coroutines.launch

class SearchEventBottomSheet(private val onEventSelected: (Event) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: LayoutSearchEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private var allEvents: List<Event> = emptyList()

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

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

        setupRecyclerView()
        setupSearchLogic()
        
        // Automatically load all available events on startup
        fetchAllEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            onEventSelected(event)
            dismiss()
        }
        binding.eventsRecyclerView.apply {
            adapter = eventAdapter
            itemAnimator = null 
        }
    }

    private fun setupSearchLogic() {
        binding.searchEventEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterEvents(s.toString().trim())
            }
        })
    }

    private fun fetchAllEvents() {
        showLoading()
        lifecycleScope.launch {
            try {
                // Fetching all events (passing empty query to the API)
                val response = ApiClient.api.searchEvents("")
                hideLoading()
                
                if (response.isSuccessful) {
                    allEvents = response.body()?.events ?: emptyList()
                    eventAdapter.submitList(allEvents)
                    
                    if (allEvents.isEmpty()) {
                        binding.emptyStateTextView.apply {
                            text = getString(R.string.search_no_results)
                            visibility = View.VISIBLE
                        }
                    }
                } else {
                    showError(getString(R.string.search_error))
                }
            } catch (e: Exception) {
                hideLoading()
                showError(getString(R.string.search_no_internet))
            }
        }
    }

    private fun filterEvents(query: String) {
        if (query.isEmpty()) {
            eventAdapter.submitList(allEvents)
            binding.emptyStateTextView.visibility = if (allEvents.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        // Local filtering logic: instantaneous and no network lag
        val filteredResults = allEvents.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.location.contains(query, ignoreCase = true) 
        }
        
        eventAdapter.submitList(filteredResults)
        
        binding.emptyStateTextView.apply {
            text = getString(R.string.search_no_results)
            visibility = if (filteredResults.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showLoading() {
        binding.searchProgressBar.visibility = View.VISIBLE
        binding.emptyStateTextView.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.searchProgressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.emptyStateTextView.apply {
            text = message
            visibility = View.VISIBLE
        }
        eventAdapter.submitList(emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}