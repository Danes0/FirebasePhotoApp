package com.example.firebaseappproject2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import android.util.Log


class MainActivity : AppCompatActivity() {

    // Declare Firebase Auth, Google sign-in client and Facebook callback manager
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    // UI elements
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button
    private lateinit var googleButton: ImageButton
    private lateinit var facebookButton: ImageButton

    // Request code to identify Google sign-in result
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth & Facebook
        auth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()

        // Connect UI elements with layout
        emailField = findViewById(R.id.editTextUsername)
        passwordField = findViewById(R.id.editTextPassword)
        signInButton = findViewById(R.id.buttonSignIn)
        signUpButton = findViewById(R.id.buttonSignUp)
        googleButton = findViewById(R.id.googleButton)
        facebookButton = findViewById(R.id.facebookButton)

        // Google Sign-In configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()

        // Initialize Google sign-in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google button listener
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        // Facebook sign-in button action
        facebookButton.setOnClickListener {
            // Request permissions from Facebook
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
            // Handle Facebook login result
            LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // Get Facebook credential and sign in with Firebase
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Facebook sign in successful",
                                Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, UserDashboard::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, "Facebook sign in failed",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancel() {
                    // Called when the user manually cancels the Facebook login process
                    Toast.makeText(this@MainActivity, "Facebook sign in canceled", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    // Called when an error occurs during login process
                    Toast.makeText(this@MainActivity, "Facebook sign in error", Toast.LENGTH_SHORT).show()
                    // Log the error details for debugging purposes
                    Log.e("FacebookLogin", "Error: ${error.message}", error)
                }
            })
        }

        // Email/Password Sign In
        signInButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            // Check if email and password are not empty
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Try to sign in with Firebase Authentication
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Use "this@MainActivity" to access the context of the activity "task"
                            Toast.makeText(
                                this@MainActivity, "Sign in successful",
                                Toast.LENGTH_SHORT).show()
                            // Redirect to the dashboard after successful login
                            val intent = Intent(this, UserDashboard::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // If sign in fails, display a message to the user
                            Toast.makeText(
                                this@MainActivity, "Sign in failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                // Notify user if fields are empty
                Toast.makeText(
                    this, "Please enter email and password",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Email/Password Sign Up
        signUpButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            // Check that fields are not empty
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Try to create a new user with Firebase Authentication
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this@MainActivity, "Sign up successful",
                                Toast.LENGTH_SHORT).show()
                            // Redirect to the dashboard after registration
                            val intent = Intent(this, UserDashboard::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@MainActivity, "Sign up failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(
                    this, "Please enter email and password",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle activity results for both Google and Facebook logins
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result from Google Sign-In
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                firebaseAuthWithGoogle(account)
            } else {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
        // Result from Facebook Login
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    // Authenticate user in Firebase using Google account
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google sign in successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, UserDashboard::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Firebase auth failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}