package dev.knsalvaterra.kezir

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.knsalvaterra.kezir.api.AuthManager
import dev.knsalvaterra.kezir.api.LoginResult
import dev.knsalvaterra.kezir.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
//todo
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val loginScreen = false


    //todo remove
    private val test_eventid = "664544741697781760"
    //todo remove
    private val test_pin = "4728"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (loginScreen) {
            setupLoginScreen()
        } else {
            openMainScreen(test_eventid, "session-cookie") //ttodo remove
        }
    }

    private fun setupLoginScreen() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleTextView.setOnLongClickListener {
            binding.eventIdEditText.setText(test_eventid)
            binding.pinEditText.setText(test_pin)
            true
        }

        binding.loginButton.setOnClickListener {
            val eventId = binding.eventIdEditText.text.toString()
            val pin = binding.pinEditText.text.toString()

            if (eventId.isNotBlank() && pin.isNotBlank()) {
                lifecycleScope.launch {
                    val result = AuthManager.login(pin, eventId)
                    handleLoginResult(result)
                }
            } else {
                Toast.makeText(this, "falta Event ID e PIN", Toast.LENGTH_SHORT).show()
            }
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
