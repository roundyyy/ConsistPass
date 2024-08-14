package com.example.consistpass

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

class SecretKeyPrompt(private val context: Context, private val onSecretKeyEntered: (String) -> Unit) {

    // Function to show the dialog
    fun show() {
        val editText = EditText(context)
        editText.hint = "Enter your secret key"

        val dialog = AlertDialog.Builder(context)
            .setTitle("Enter Secret Key")
            .setMessage("Please enter your secret key to proceed")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val secretKey = editText.text.toString()
                onSecretKeyEntered(secretKey)
            }
            .setNegativeButton("Exit") { _, _ ->
                // Close the app if the user does not want to enter the key
                (context as MainActivity).finish()
            }
            .setCancelable(false)  // Prevent dismissal by tapping outside
            .create()

        dialog.show()
    }
}
