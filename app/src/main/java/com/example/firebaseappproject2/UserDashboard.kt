package com.example.firebaseappproject2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


class UserDashboard : AppCompatActivity() {

    // UI elements and FirebaseAuth instance
    private lateinit var btnOpenCamera: Button
    private lateinit var btnLogout: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up click listeners for buttons
        btnOpenCamera = findViewById(R.id.btnOpenCamera)
        btnLogout = findViewById(R.id.btnLogout)

        // Open Camera button listener
        btnOpenCamera.setOnClickListener {
            // Start CameraActivity
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // Logout button listener
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            // Clear back stack and start MainActivity as a new task to prevent returning to dashboard after logout
            /* FLAG_ACTIVITY_NEW_TASK
            Start the new activity (MainActivity) as if it were the root of a new task.
            * FLAG_ACTIVITY_CLEAR_TASK
            Removes all previous activities from the stack, clears previous browsing history. */
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}