package dev.knsalvaterra.kezir.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.knsalvaterra.kezir.api.TicketManager
import dev.knsalvaterra.kezir.api.TicketResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _ticketResult = MutableLiveData<TicketResult>()
    val ticketResult: LiveData<TicketResult> = _ticketResult

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _shouldScan = MutableLiveData<Boolean>(false)
    val shouldScan: LiveData<Boolean> = _shouldScan

    fun verifyCode(context: Context, pin: String, code: String, eventId: String?) {
        viewModelScope.launch {
            val result = TicketManager.evaluateTicket(context, pin, code, eventId)
            _ticketResult.value = result
        }
    }

    /**
     * Triggers the scanning process with the specified delay.
     */
    fun triggerScan(duration: Long) {
        viewModelScope.launch {
            _isScanning.value = true
            delay(duration)
            _shouldScan.value = true
            _isScanning.value = false
        }
    }

    fun setScanning(scanning: Boolean) {
        _isScanning.postValue(scanning)
    }

    fun setShouldScan(scan: Boolean) {
        _shouldScan.postValue(scan)
    }
}
