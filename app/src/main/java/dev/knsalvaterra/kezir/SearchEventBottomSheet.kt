package dev.knsalvaterra.kezir

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.knsalvaterra.kezir.api.Event
import dev.knsalvaterra.kezir.databinding.LayoutSearchEventBinding
import dev.knsalvaterra.kezir.viewmodel.SearchViewModel

class SearchEventBottomSheet(private val onEventSelected: (Event) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: LayoutSearchEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private val viewModel: SearchViewModel by viewModels()

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
        observeViewModel()
        
        viewModel.loadEvents()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            vibrate()
            onEventSelected(event)
            dismiss()
        }
        binding.eventsRecyclerView.apply {
            adapter = eventAdapter
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            displayEvents(events)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

//mayube make a util cuz i already havbe this in main
    private fun vibrate() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun setupSearchLogic() {
        binding.searchEventEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterEvents(s.toString().trim())
            }
        })
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
        /**
         * pre-fetches all events from the API.
         */
        fun preloadEvents() {
            SearchViewModel.preloadEvents()
        }
    }
}
