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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchEventBottomSheet(private val onEventSelected: (Event) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: LayoutSearchEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter

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
        
        // Use pre-loaded data or fetch if not available
        if (preloadedEvents.isNotEmpty()) {
            displayEvents(preloadedEvents)
        } else {//prolly not necessary unless by some unknown rrwson fetching from login didnt work
            fetchAllEvents()
        }
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
                applyLocalFilter(s.toString().trim())
            }
        })
    }

    private fun fetchAllEvents() {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.searchEvents("")
                hideLoading()

                if (response.isSuccessful) {
                    preloadedEvents = response.body()?.events ?: emptyList()
                    displayEvents(preloadedEvents)
                } else {
                    showError(getString(R.string.search_error))
                }
            } catch (e: Exception) {
                hideLoading()
                showError(getString(R.string.search_no_internet))
            }
        }
    }

    private fun applyLocalFilter(query: String) {
        if (query.isEmpty()) {
            displayEvents(preloadedEvents) //show all availabnle events
            return
        }

        val filteredResults = preloadedEvents.filter { 
            it.title.contains(query, ignoreCase = true) ||  //search by name or loc name
            it.location.contains(query, ignoreCase = true) 
        }
        
        displayEvents(filteredResults)
    }

    private fun displayEvents(events: List<Event>) {
        eventAdapter.submitList(events)
        updateEmptyState(events.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            binding.emptyStateTextView.text = getString(R.string.search_no_results)
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
        binding.emptyStateTextView.text = message
        binding.emptyStateTextView.visibility = View.VISIBLE
        eventAdapter.submitList(emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private var preloadedEvents: List<Event> = emptyList()

        /**
         * Pre-fetches all events from the API.
         */
        fun preloadEvents() {
            @Suppress("OPT_IN_USAGE")
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.api.searchEvents("")
                    if (response.isSuccessful) {
                        preloadedEvents = response.body()?.events ?: emptyList()
                    }
                } catch (e: Exception) {
                    // Fail silently
                }
            }
        }
    }
}