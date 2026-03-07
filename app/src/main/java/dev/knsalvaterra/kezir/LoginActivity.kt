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
//TODO  clean up eventually
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    //todo remove
    private val test_eventid = "765354571144429568"
    //todo remove
    private val test_pin = "7968"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.getInMemorySession()?.let { (id, cookie) ->
            openMainScreen(id, cookie)
            return //skip login screen auto and open main screen
        }



        initializeUI()
        //https://kezir.app/auth?id=684430880843759616&pin=4966 if (intentUri != null && intentUri.path?.contains("login") == true)
        val intentUri = intent?.data
        if (validLink(intentUri)) {
            val linkEventId = intentUri?.getQueryParameter("id")
           // val linkPin = intentUri?.getQueryParameter("pin")

            //if (!linkEventId.isNullOrEmpty() && !linkPin.isNullOrEmpty()) {
            if (!linkEventId.isNullOrEmpty() ) {
            //   performLogin(linkPin, linkEventId)
                binding.eventIdEditText.setText(linkEventId)
                return
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

         //   if (result is LoginResult.Success) {
         //       AuthManager.saveSessionInMemory(result.eventId, result.sessionCookie)
         //       openMainScreen(result.eventId, result.sessionCookie)
         //   } else {
         //       setupLoginScreen()
         //       Toast.makeText(this@LoginActivity, "Ocorreu um erro ao fazer login", Toast.LENGTH_SHORT).show()
         //   }
        }
    }

    private fun initializeUI() {
        if (::binding.isInitialized) return // avoid double inflation
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


      //  binding.titleTextView.setOnLongClickListener { //DEBUG
      //      binding.eventIdEditText.setText(test_eventid)
      //      binding.pinEditText.setText(test_pin)
      //      true
      //  }

        binding.loginButton.setOnClickListener {
            if (isNetworkAvailable()) {
                val eventId = binding.eventIdEditText.text.toString()
                val pin = binding.pinEditText.text.toString()

                if (eventId.isNotBlank() && pin.isNotBlank()) {
                   performLogin(eventId, pin)
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

    private fun openMainScreen(eventId: String, sessionCookie: String) { //todo not necessary sunce it's already saved in AuthManager
        val intent = Intent(this, MainActivity::class.java).apply {
            // Even if saved in AuthManager, passing as extras is fine for explicit navigation
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
                if (binding.root.parent == null) setContentView(binding.root)
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
