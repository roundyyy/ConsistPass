# ConsistPass - A Simple but Secure Deterministic Password Generator
![Alt Text](icon.png)

**ConsistPass** is an Android application designed to generate secure, deterministic passwords without storing them or requiring an internet connection. The app leverages a unique combination of user-provided inputs and a customizable seed to generate passwords that are consistent and robust. Each user can build the app with their own seed, ensuring that their password generation logic is unique.

## Features

- **Deterministic Password Generation**: The same combination of inputs will always generate the same password, allowing you to regenerate your passwords consistently.
- **Complex Passwords**: The generated passwords are a mix of uppercase letters, lowercase letters, digits, and special characters.
- **Secure Handling of Secret Keys**: The secret key is stored in Android's secure vault (Android Keystore) and is encrypted for additional security.
- **Customizable Seed**: Before building the APK, users can edit the seed, making each build's password generation logic unique.
- **No Password Storage**: Passwords are generated on-the-fly based on user inputs and are not stored anywhere in the app or device.
- **No Internet Connection Required**: The app functions entirely offline, ensuring that your data remains secure on your device.

## How It Works

### 1. Input Data
The password generation process starts with three key pieces of input:
- **Website**: The name of the website or service for which the password is being generated.
- **Secret Key**: A user-defined key that is stored securely in the Android Keystore.
- **Seed**: A value that can be customized by the user before building the APK, ensuring that the generated passwords are unique to their version of the app.

### 2. Password Generation Logic

The password is generated through a multi-step process:

1. **SHA-256 Hashing**: 
   - The app concatenates the `Website` and `Secret Key` and hashes the combination using SHA-256 to produce a 32-byte hash.

2. **Seed Integration and Second Hash**:
   - The app concatenates the `Seed` with the first SHA-256 hash and hashes the result again with SHA-256.

3. **XOR Operation**:
   - The first 16 bytes of the second hash are XORed with the last 16 bytes to scramble the data further.

4. **Bit Rotation**:
   - The app derives a rotation amount from the first byte of the second hash and applies a bitwise rotation to the XOR result.

5. **Final SHA-256 Hash**:
   - The rotated XOR result is hashed once more with SHA-256.

6. **Final XOR and Password Conversion**:
   - The first 16 bytes of the final hash are XORed with the last 16 bytes. This result is then converted into a secure password, ensuring a mix of character types (uppercase, lowercase, digits, and special characters).

### 3. Security Measures

- **Secure Key Storage**: The secret key is stored in the Android Keystore, which leverages hardware-backed security to protect sensitive information. The key is encrypted and never leaves the secure environment.
- **Temporary Key Storage**: The secret key is only held in memory for the duration of the cryptographic operation. It is cleared from memory as soon as the operation is complete. Additionally, the `clearSecretKey()` function is called in the `onPause()` and `onDestroy()` methods to ensure that the key is removed from memory when the app is paused or stopped.
- **Complete App Termination**: When the app is paused or stopped, it terminates fully by calling `finishAffinity()` and `exitProcess(0)`, ensuring that all memory associated with the app is released, including any sensitive data.
- **No Password Storage**: The app does not store any generated passwords, minimizing the risk of exposure. Passwords are generated on-the-fly and discarded after use.
- **Custom Seed**: Each user can set their own seed before building the APK, ensuring that the password generation logic is unique to their version of the app. This makes reverse engineering significantly harder because even if the algorithm is known, the seed adds a layer of complexity that is unique to each build.

### 4. Deterministic Behavior

The app is deterministic, meaning that the same combination of `Website`, `Secret Key`, and `Seed` will always produce the same password. This is crucial for users who need to consistently regenerate the same password without having to store it. The deterministic nature, combined with the secure handling of inputs, ensures that passwords are both reliable and safe.

### Why It's Hard to Reverse Engineer

- **Complex Multi-Step Process**: The password generation involves multiple layers of SHA-256 hashing, XOR operations, and bit rotations. Even if an attacker knows the `Website` and the generated password, deriving the `Secret Key` is extremely difficult due to the non-linear transformations applied throughout the process.
- **Custom Seed**: The customizable seed means that every build of the app can have a unique logic, making it nearly impossible to create a one-size-fits-all reverse engineering tool.
- **Secure Key Handling**: The `Secret Key` is securely stored and encrypted in the Android Keystore, which is designed to resist tampering even on compromised devices.

## Building the App

### Customizing the Seed

Before building the APK, you should customize the seed to make your version of the app unique:

1. Open the project in Android Studio.
2. Navigate to the `MainActivity.kt` file.
3. Locate the following line:
    ```kotlin
    private val seed = "unique_seed_value_enter_here_anything_you_want"
    ```
4. Change the `seed` value to your desired string.
5. Build the APK.

Your version of the app will now use this seed for all password generation, ensuring unique password logic.

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/ConsistPass.git
    ```
2. Open the project in Android Studio.
3. Customize the seed as described above.
4. Build and install the APK on your Android device.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any improvements, new features, or bug fixes.

## License

This project is open-source and available under the MIT License. See the `LICENSE` file for more details.

---

ConsistPass is a simple yet powerful tool for generating secure, deterministic passwords on your Android device. With strong encryption, customizable seeds, and no reliance on cloud storage, it offers a highly secure way to manage your passwords without the need to store them.
