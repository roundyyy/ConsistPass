package com.example.consistpass

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences

import java.security.MessageDigest
import java.util.concurrent.Executor
import kotlin.experimental.xor
import kotlin.system.exitProcess


import androidx.security.crypto.MasterKey

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val seed = "unique_seed_value_enter_here_anything_you_want"
    private val ENCRYPTED_PREF_FILE_NAME = "SecureStorage"
    private val SECRET_KEY_PREF_NAME = "encrypted_secret_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeBiometricPrompt()
    }

    private fun initializeBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                showSecretKeyDialog()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Please unlock your device")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showSecretKeyDialog() {
        val secretKeyPrompt = SecretKeyPrompt(this) { enteredKey ->
            storeSecretKey(enteredKey)
            proceedWithApp()
        }
        secretKeyPrompt.show()
    }

    private fun getEncryptedSharedPreferences(): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            applicationContext,
            ENCRYPTED_PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    private fun storeSecretKey(secretKey: String) {
        try {
            val encryptedSharedPreferences = getEncryptedSharedPreferences()
            encryptedSharedPreferences.edit().putString(SECRET_KEY_PREF_NAME, secretKey).apply()
        } catch (e: Exception) {
            // Handle exceptions (e.g., KeyStoreException, IOException, NoSuchAlgorithmException)
            Toast.makeText(this, "Error storing secret key: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getSecretKey(): String? {
        return try {
            val encryptedSharedPreferences = getEncryptedSharedPreferences()
            encryptedSharedPreferences.getString(SECRET_KEY_PREF_NAME, null)
        } catch (e: Exception) {
            // Handle exceptions
            Toast.makeText(this, "Error retrieving secret key: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun proceedWithApp() {
        val editTextWebsite: EditText = findViewById(R.id.editTextWebsite)
        val buttonGenerate: Button = findViewById(R.id.buttonGenerate)
        val textViewPassword: TextView = findViewById(R.id.textViewPassword)
        val buttonCopy: Button = findViewById(R.id.buttonCopy)
        val buttonExit: Button = findViewById(R.id.buttonExit)

        buttonGenerate.setOnClickListener {
            val website = editTextWebsite.text.toString()
            val key = getSecretKey() ?: return@setOnClickListener

            val password = generatePassword(website, key, seed)
            textViewPassword.text = password
        }

        buttonCopy.setOnClickListener {
            val password = textViewPassword.text.toString()
            if (password.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("password", password)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No password to copy", Toast.LENGTH_SHORT).show()
            }
        }

        buttonExit.setOnClickListener {
            clearSecretKey()
            finish()
        }
    }

    private fun clearSecretKey() {
        try {
            val encryptedSharedPreferences = getEncryptedSharedPreferences()
            encryptedSharedPreferences.edit().remove(SECRET_KEY_PREF_NAME).apply()
        } catch (e: Exception) {
            // Handle exceptions
            Toast.makeText(this, "Error clearing secret key: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    // The generatePassword function remains the same
    private fun generatePassword(website: String, secretKey: String, seed: String): String {
        // Step 1: First SHA-256 hash of secretKey + website
        val initialInput = website + secretKey
        val firstHash = sha256(initialInput)

        // Step 2: Second SHA-256 hash of seed + firstHash
        val firstHashAsHex = firstHash.joinToString("") { "%02x".format(it) } // Convert first hash to a hex string
        val secondInput = seed + firstHashAsHex
        val secondHash = sha256(secondInput)

        // Step 3: XOR the first 16 bytes with the last 16 bytes of the second hash
        val xorBytes = xorFirstAndLast16Bytes(secondHash)

        // Step 4: Derive bit rotation amount from the first byte of the second hash
        val rotationAmount = (secondHash[0].toInt() and 0xFF) % 8

        // Step 5: Perform bitwise rotation on the XOR result
        val rotatedXorBytes = xorBytes.map { rotateLeft(it, rotationAmount) }.toByteArray()

        // Step 6: Third SHA-256 hash of the rotated XOR result
        val thirdHash = sha256(rotatedXorBytes)

        // Step 7: Final XOR of the first 16 bytes with the last 16 bytes of the third hash
        val finalXorBytes = xorFirstAndLast16Bytes(thirdHash)



        // Convert the final XOR bytes to a password string
        val password = convertBytesToPassword(finalXorBytes)



        return convertBytesToPassword(finalXorBytes)

    }

    // Helper function: SHA-256 hashing
    private fun sha256(input: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    }

    // Overloaded helper function: SHA-256 for ByteArray input
    private fun sha256(input: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input)
    }

    // Helper function: XOR first 16 bytes with last 16 bytes
    private fun xorFirstAndLast16Bytes(hash: ByteArray): ByteArray {
        val xorResult = ByteArray(16)
        for (i in 0 until 16) {
            xorResult[i] = (hash[i] xor hash[hash.size - 16 + i])
        }
        return xorResult
    }

    // Helper function: Rotate bits of a byte to the left
    private fun rotateLeft(byte: Byte, shift: Int): Byte {
        return ((byte.toInt() shl shift) or (byte.toInt() ushr (8 - shift))).toByte()
    }

    // Helper function: Convert bytes to password string
    private fun convertBytesToPassword(xorBytes: ByteArray): String {
        val mixedPassword = StringBuilder()

        // Convert each byte deterministically into a mix of character types
        for (i in xorBytes.indices) {
            when (i % 4) {
                0 -> mixedPassword.append((65 + (xorBytes[i].toInt() and 0xFF) % 26).toChar()) // Uppercase letter
                1 -> mixedPassword.append((97 + (xorBytes[i].toInt() and 0xFF) % 26).toChar()) // Lowercase letter
                2 -> mixedPassword.append((xorBytes[i].toInt() and 0xFF) % 10) // Digit
                3 -> mixedPassword.append(specialCharacterFromByte(xorBytes[i])) // Special character
            }
        }

        return mixedPassword.toString()
    }

    // Helper function: Determine special character from a byte
    private fun specialCharacterFromByte(byte: Byte): Char {
        val specialChars = "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/\\`~"
        return specialChars[(byte.toInt() and 0xFF) % specialChars.length]
    }


    override fun onPause() {
        super.onPause()
        clearSecretKey()
        finishAffinity()
        exitProcess(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearSecretKey()
        finishAffinity()
        exitProcess(0)

    }
}