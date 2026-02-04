package dev.knsalvaterra.kezir

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.datatransport.BuildConfig
import dev.knsalvaterra.kezir.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // For dev mode
    private val DEV_EVENT_ID = "664544741697781760"
    private val DEV_PIN = "4728"

    private val debug = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dev mode: Long-press on the title to auto-login
        if (debug) {
            binding.titleTextView.setOnLongClickListener {
                Toast.makeText(this, "Dev Login", Toast.LENGTH_SHORT).show()
                login(DEV_PIN, DEV_EVENT_ID)
                true // Consume the long click
            }
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

    private fun login(pin: String, eventId: String) {
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
                    Toast.makeText(this@LoginActivity, getString(R.string.invalid_pin), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Auth", "Login failed", e)
                Toast.makeText(this@LoginActivity, getString(R.string.login_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
