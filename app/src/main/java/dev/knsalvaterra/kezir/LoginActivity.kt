package dev.knsalvaterra.kezir

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    //todo remove
    private val test_eventid = "675354571144429568"
    //todo remove
    private val test_pin = "7968"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.getSavedSession(this)?.let {
            (id, cookie) ->
            openMainScreen(id, cookie)
            return //skip login screen auto
        }

        val intentUri = intent?.data
        if (intentUri != null && intentUri.path?.contains("login") == true) {
            val linkEventId = intentUri.getQueryParameter("id")
            val linkPin = intentUri.getQueryParameter("pin")

            if (!linkEventId.isNullOrEmpty() && !linkPin.isNullOrEmpty()) {
                autologin(linkPin, linkEventId)
                return
            }
        }
        setupLoginScreen()
    }

    private fun autologin(pin: String, eventId: String) {
        lifecycleScope.launch {
            val result = AuthManager.login(pin, eventId)
            if (result is LoginResult.Success) {
                AuthManager.saveSession(this@LoginActivity, result.eventId, result.sessionCookie)
                openMainScreen(result.eventId, result.sessionCookie)
            } else {
                setupLoginScreen()
                Toast.makeText(this@LoginActivity, "Link de login inválido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLoginScreen() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleTextView.setOnLongClickListener { //DEBUG
            binding.eventIdEditText.setText(test_eventid)
            binding.pinEditText.setText(test_pin)
            true
        }

        binding.loginButton.setOnClickListener {
            if (isNetworkAvailable()) {
                val eventId = binding.eventIdEditText.text.toString()
                val pin = binding.pinEditText.text.toString()

                if (eventId.isNotBlank() && pin.isNotBlank()) {
                    lifecycleScope.launch {
                        val result = AuthManager.login(pin, eventId)
                        handleLoginResult(result)
                    }
                } else {
                    Toast.makeText(this, "Preencha os campos Event ID e PIN", Toast.LENGTH_SHORT).show()
                }
            } else { //offline
                lifecycleScope.launch {
                    val result = AuthManager.login("m836v1d0grchu3mgu5v2e3ne91")
                    handleLoginResult(result)
                }
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
        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
            putExtra("EVENT_ID", eventId)
            putExtra("SESSION_COOKIE", sessionCookie)
        }

        startActivity(intent)
        finish()
    }

    private fun handleLoginResult(result: LoginResult) {
        when (result) {
            is LoginResult.Success -> {
                openMainScreen(result.eventId, result.sessionCookie)
            }
            is LoginResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
