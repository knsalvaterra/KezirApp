package dev.knsalvaterra.kezir

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.knsalvaterra.kezir.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    /**
     * Set this flag to `false` to disable the login screen and use dev credentials.
     * Set to `true` to show the login screen.
     */
    private val LOGIN_ENABLED = false // <-- TOGGLE FEATURE HERE

    // For dev mode
    private val DEV_EVENT_ID = "664544741697781760"
    private val DEV_PIN = "4728"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (LOGIN_ENABLED) {
            // Setup the UI for manual login
            setupManualLoginUI()
        } else {
            // Bypass login UI and use dev credentials
            bypassLogin()
        }
    }

    private fun bypassLogin() {
        // Show a loading screen immediately to improve perceived startup time
        setContentView(R.layout.activity_loading)
        // Attempt to log in with dev credentials in the background
        login(DEV_PIN, DEV_EVENT_ID, isBypass = true)
    }

    private fun setupManualLoginUI() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dev mode helper: Long-press on the title to auto-login
        binding.titleTextView.setOnLongClickListener {
            Toast.makeText(this, "Dev Login", Toast.LENGTH_SHORT).show()
            login(DEV_PIN, DEV_EVENT_ID)
            true // Consume the long click
        }

        binding.loginButton.setOnClickListener {
            val eventId = binding.eventIdEditText.text.toString()
            val pin = binding.pinEditText.text.toString()

            if (eventId.isNotBlank() && pin.isNotBlank()) {
                login(pin, eventId)
            } else {
                Toast.makeText(this, "Please enter Event ID and PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun login(pin: String, eventId: String, isBypass: Boolean = false) {
        lifecycleScope.launch {
            try {
                val request = PinRequest(pin, eventId)
                val response = ApiClient.api.verifyPin(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val sessionCookie = response.headers()["Set-Cookie"]?.split(";")?.get(0)
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("EVENT_ID", eventId)
                        putExtra("SESSION_COOKIE", sessionCookie)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    handleLoginFailure(isBypass)
                }
            } catch (e: Exception) {
                Log.e("Auth", "Login failed", e)
                handleLoginFailure(isBypass)
            }
        }
    }

    private fun handleLoginFailure(isBypass: Boolean) {
        if (isBypass) {
            // If the bypass fails, show the regular login screen as a fallback
            Toast.makeText(this@LoginActivity, "Dev login failed, showing manual login.", Toast.LENGTH_LONG).show()
            runOnUiThread {
                setupManualLoginUI()
            }
        } else {
            Toast.makeText(this@LoginActivity, getString(R.string.invalid_pin), Toast.LENGTH_SHORT).show()
        }
    }
}
