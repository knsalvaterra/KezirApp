package dev.knsalvaterra.kezir

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-login if session exists in memory
        AuthManager.getInMemorySession()?.let { (id, cookie) ->
            openMainScreen(id, cookie)
            return
        }

        initializeUI()
        
        // Handle deep links (e.g., kezir.app/auth?id=...)
        val intentUri = intent?.data
        if (validLink(intentUri)) {
            val linkEventId = intentUri?.getQueryParameter("id")
            if (!linkEventId.isNullOrEmpty()) {
                binding.eventIdEditText.setText(linkEventId)
            }
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

        // Set search icon click listener to open the bottom sheet
        binding.eventIdInputLayout.setEndIconOnClickListener {
            val searchSheet = SearchEventBottomSheet { event ->
                binding.eventIdEditText.setText(event.id)
                binding.pinEditText.requestFocus()
            }
            searchSheet.show(supportFragmentManager, "search_event")
        }

        binding.loginButton.setOnClickListener {
            val eventId = binding.eventIdEditText.text.toString().trim()
            val pin = binding.pinEditText.text.toString().trim()

            if (eventId.isNotBlank() && pin.isNotBlank()) {
                if (isNetworkAvailable()) {
                    performLogin(pin, eventId)
                } else {
                    // Try to login with mock session if offline (preserving existing logic)
                    lifecycleScope.launch {
                        val result = AuthManager.login("m836v1d0grchu3mgu5v2e3ne91")
                        handleLoginResult(result)
                    }
                }
            } else {
                Toast.makeText(this, "Preencha os campos Event ID e PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun openMainScreen(eventId: String, sessionCookie: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EVENT_ID", eventId)
            putExtra("SESSION_COOKIE", sessionCookie)
        }
        startActivity(intent)
        finish()
    }

    private fun handleLoginResult(result: LoginResult) {
        when (result) {
            is LoginResult.Success -> {
                AuthManager.saveSessionInMemory(result.eventId, result.sessionCookie)
                openMainScreen(result.eventId, result.sessionCookie)
            }
            is LoginResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}