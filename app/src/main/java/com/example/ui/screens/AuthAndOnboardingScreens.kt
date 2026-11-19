package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.BrandViolet
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activityContext = context as? Activity
            if (activityContext == null) {
                onAuthError("Sign-in requires an active screen context.")
                return@launch
            }
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isCheckingSilent by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        BrandViolet.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_onboarding_focus),
                    contentDescription = "Study Time Workspace Illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(BrandViolet)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "STUDY TIME",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandViolet,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Focus Deeper.\nLearn Together.",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Pomodoro tracking, smart study planning, real-time study groups, global XP leaderboards, and Gemini AI coaching.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            FeatureHighlightRow(Icons.Filled.Timer, "Verified Pomodoro & Deep Focus Timers")
            FeatureHighlightRow(Icons.Filled.Groups, "Live Study Groups, Chat & Challenges")
            FeatureHighlightRow(Icons.Filled.AutoAwesome, "Gemini AI Study Coach & Smart Plans")

            Spacer(Modifier.height(26.dp))

            if (isCheckingSilent) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("Restoring session...", style = MaterialTheme.typography.labelMedium)
            } else {
                Button(
                    onClick = {
                        errorMessage = null
                        isSigningIn = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isSigningIn = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isSigningIn = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isSigningIn = false
                            }
                        )
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Signing in...", color = Color.White)
                    } else {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null, enter = fadeIn()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Secured with Google Identity & Cloud Sync",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureHighlightRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandViolet.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandViolet, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun OnboardingSetupScreen(
    displayName: String,
    onComplete: (dailyGoalMinutes: Long, country: String, seedDemo: Boolean) -> Unit
) {
    var goalMinutes by remember { mutableFloatStateOf(120f) }
    var country by remember { mutableStateOf("Global") }
    var seedDemo by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome, ${displayName.ifBlank { "Scholar" }}!",
                style = MaterialTheme.typography.displayMedium,
                color = BrandViolet
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Personalize your daily focus target and starter workspace.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Daily Study Goal: ${goalMinutes.toInt()} min (${goalMinutes.toInt() / 60}h ${goalMinutes.toInt() % 60}m)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = goalMinutes,
                        onValueChange = { goalMinutes = it },
                        valueRange = 30f..360f,
                        steps = 10,
                        modifier = Modifier.testTag("onboarding_goal_slider")
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country / Region (for leaderboards)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_country_input")
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = seedDemo,
                            onCheckedChange = { seedDemo = it },
                            modifier = Modifier.testTag("onboarding_seed_checkbox")
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Include Starter Study Kit",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Adds 4 sample subjects, exam countdown, and Big-O note",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onComplete(goalMinutes.toLong(), country, seedDemo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_finish_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Enter Study Time", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
