package dev.knsalvaterra.kezir

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.knsalvaterra.kezir.api.AuthManager
import dev.knsalvaterra.kezir.api.LoginResult
import dev.knsalvaterra.kezir.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    

    private val allowManualEventIdInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AuthManager.getInMemorySession()?.let { (id, pin) ->
            openMainScreen(id, pin)
            return
        }

        initializeUI()
        
        //  pre-loading events as soon as the app opens to make searching fast
        SearchEventBottomSheet.preloadEvents()
        
        val intentUri = intent?.data
        if (validLink(intentUri)) {
            val linkEventId = intentUri?.getQueryParameter("id")
            if (!linkEventId.isNullOrEmpty()) {
                binding.eventIdEditText.setText(linkEventId)
            }
        } else {

            openSearch()
        }
    }

    private fun validLink(uri: Uri?): Boolean {
        return uri != null &&
                uri.path?.contains("auth") == true &&
                (uri.scheme == "kezir" || uri.host == "kezir.app")
    }

    private fun performLogin(pin: String, eventId: String) {
        lifecycleScope.launch {
            val result = AuthManager.login(pin, eventId)
            handleLoginResult(result)
        }
    }

    private fun initializeUI() {
        if (::binding.isInitialized) return
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.eventIdEditText.apply {
            isFocusable = allowManualEventIdInput
            isFocusableInTouchMode = allowManualEventIdInput
            isClickable = allowManualEventIdInput
            
            if (!allowManualEventIdInput) {
                setOnClickListener {
                    openSearch()
                }
            }
        }


        binding.eventIdInputLayout.setEndIconOnClickListener {
            openSearch()
        }

        binding.loginButton.setOnClickListener {
            val eventId = binding.eventIdEditText.text.toString().trim()
            val pin = binding.pinEditText.text.toString().trim()

            if (eventId.isNotBlank() && pin.isNotBlank()) {
                if (isNetworkAvailable()) {
                    performLogin(pin, eventId)
                } //else {
                  //  lifecycleScope.launch {
                  //      val result = AuthManager.login("m836v1d0grchu3mgu5v2e3ne91")
                  //      handleLoginResult(result)
                  //  }
                //}
            } else {
                Toast.makeText(this, getString(R.string.login_fill_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSearch() {
        SearchEventBottomSheet { event ->
            binding.eventIdEditText.setText(event.id)
            binding.pinEditText.requestFocus()
            Toast.makeText(this, getString(R.string.login_pin_hint), Toast.LENGTH_SHORT).show()
        }.show(supportFragmentManager, "search_event")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun openMainScreen(eventId: String, pin: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EVENT_ID", eventId)
            putExtra("USER_PIN", pin)
        }
        startActivity(intent)
        finish()
    }

    private fun handleLoginResult(result: LoginResult) {
        when (result) {
            is LoginResult.Success -> {
                AuthManager.saveSessionInMemory(result.eventId, result.pin)
                openMainScreen(result.eventId, result.pin)
            }
            is LoginResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}