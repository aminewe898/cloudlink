package com.cloudlink.app.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cloudlink.app.ui.components.CloudLinkBackdrop
import com.cloudlink.app.ui.components.CloudPanel

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAuthenticating by remember { mutableStateOf(false) }
    var authenticationMessage by remember { mutableStateOf<String?>(null) }
    val allowedAuthenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    }

    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticating = false
        if (result.resultCode == Activity.RESULT_OK) {
            authenticationMessage = null
            onUnlock()
        } else {
            authenticationMessage = "Device authentication was cancelled."
        }
    }

    fun launchDeviceCredential() {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock CloudLink",
            "Enter your device PIN, pattern, or password"
        )
        if (keyguard.isDeviceSecure && intent != null) {
            authenticationMessage = null
            isAuthenticating = false
            deviceCredentialLauncher.launch(intent)
        } else {
            isAuthenticating = false
            authenticationMessage = "Set up a secure device lock before using CloudLink."
        }
    }

    fun beginAuthentication() {
        val availability = BiometricManager.from(context).canAuthenticate(allowedAuthenticators)
        if (availability == BiometricManager.BIOMETRIC_SUCCESS) {
            authenticationMessage = null
            isAuthenticating = true
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            launchDeviceCredential()
        } else {
            authenticationMessage = when (availability) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "Set up a device PIN, pattern, password, or biometric first."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "This device does not provide a supported authenticator."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "Authentication hardware is temporarily unavailable."
                else -> "Device authentication is unavailable."
            }
        }
    }

    LaunchedEffect(isAuthenticating) {
        if (!isAuthenticating) return@LaunchedEffect
        val activity = context as? FragmentActivity
        if (activity == null) {
            isAuthenticating = false
            authenticationMessage = "Authentication is unavailable in the current activity."
            return@LaunchedEffect
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isAuthenticating = false
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        launchDeviceCredential()
                    } else {
                        authenticationMessage = errString.toString()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticating = false
                    authenticationMessage = null
                    onUnlock()
                }

                override fun onAuthenticationFailed() {
                    authenticationMessage = "Authentication was not recognized. Try again."
                }
            }
        )
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock CloudLink")
            .setSubtitle("Confirm your identity to access saved infrastructure")
            .setAllowedAuthenticators(allowedAuthenticators)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) builder.setNegativeButtonText("Use device PIN")
        prompt.authenticate(builder.build())
    }

    CloudLinkBackdrop(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CloudPanel(Modifier.widthIn(max = 430.dp).fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("CloudLink is locked", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your saved servers and credentials stay protected until you confirm your identity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    authenticationMessage?.let { message ->
                        Spacer(Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                                .padding(12.dp)
                        ) {
                            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = ::beginAuthentication,
                        enabled = !isAuthenticating,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Waiting for authentication")
                        } else {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Unlock securely")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Uses your device security",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
