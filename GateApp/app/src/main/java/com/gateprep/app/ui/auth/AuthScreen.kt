package com.gateprep.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.AppViewModel
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.theme.LocalExtra

/** The two authentication destinations share one polished form and animate when the user switches mode. */
enum class AuthMode { LOGIN, SIGNUP }

@Composable
fun AuthScreen(
    vm: AppViewModel,
    mode: AuthMode,
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    val ex = LocalExtra.current
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mode) { vm.clearAuthError() }

    val emailOk = email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
    val passwordOk = password.length in 8..72
    val canSubmit = emailOk && passwordOk && (mode == AuthMode.LOGIN || (name.trim().length >= 2 && confirm == password))

    Column(
        Modifier.fillMaxSize().background(ex.bg).verticalScroll(rememberScrollState()).navigationBarsPadding(),
    ) {
        Box(
            Modifier.fillMaxWidth().height(286.dp).background(ex.brand).statusBarsPadding(),
        ) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 42.dp, y = (-42).dp).size(190.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.11f)))
            Box(Modifier.align(Alignment.BottomStart).offset(x = (-58).dp, y = 68.dp).size(190.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))

            Box(
                Modifier.padding(start = 20.dp, top = 14.dp).size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }

            Column(Modifier.align(Alignment.Center).padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(78.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.School, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
                Spacer(Modifier.height(14.dp))
                Text("GATE CS Practice", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                Text(
                    if (mode == AuthMode.LOGIN) "Welcome back. Continue where you stopped." else "Create your account and sync your preparation.",
                    color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
            }
        }

        Surface(
            Modifier.fillMaxWidth().offset(y = (-28).dp).padding(horizontal = 20.dp).animateContentSize(),
            shape = RoundedCornerShape(28.dp), color = ex.card, shadowElevation = 8.dp,
            border = BorderStroke(1.dp, ex.outline),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        val forward = targetState == AuthMode.SIGNUP
                        (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(240)) { if (forward) -it / 2 else it / 2 } + fadeOut(tween(180)))
                    }, label = "auth-mode",
                ) { shownMode ->
                    Column {
                        Text(
                            if (shownMode == AuthMode.LOGIN) "Sign in" else "Create account",
                            fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (shownMode == AuthMode.LOGIN) "Your questions, progress and mocks are waiting." else "One account keeps your progress available across devices.",
                            fontSize = 13.sp, lineHeight = 19.sp, color = ex.muted,
                        )
                        Spacer(Modifier.height(20.dp))

                        if (shownMode == AuthMode.SIGNUP) {
                            AuthField(
                                value = name, onValueChange = { name = it.take(80); vm.clearAuthError() },
                                label = "Full name", icon = Icons.Rounded.Person,
                                keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        AuthField(
                            value = email, onValueChange = { email = it.trim().take(255); vm.clearAuthError() },
                            label = "Email address", icon = Icons.Rounded.Email,
                            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                        )
                        Spacer(Modifier.height(12.dp))
                        AuthField(
                            value = password, onValueChange = { password = it.take(72); vm.clearAuthError() },
                            label = "Password", icon = Icons.Rounded.Lock,
                            keyboardType = KeyboardType.Password,
                            imeAction = if (shownMode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
                            password = true, visible = showPassword, onToggleVisibility = { showPassword = !showPassword },
                        )
                        if (shownMode == AuthMode.SIGNUP) {
                            Spacer(Modifier.height(12.dp))
                            AuthField(
                                value = confirm, onValueChange = { confirm = it.take(72); vm.clearAuthError() },
                                label = "Confirm password", icon = Icons.Rounded.Lock,
                                keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                                password = true, visible = showConfirm, onToggleVisibility = { showConfirm = !showConfirm },
                            )
                            if (confirm.isNotEmpty() && confirm != password) {
                                Text("Passwords do not match", color = ex.danger, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 5.dp))
                            }
                        }
                        if (password.isNotEmpty() && !passwordOk) {
                            Text("Password must contain 8–72 characters", color = ex.danger, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 5.dp))
                        }
                    }
                }

                vm.authError?.let { message ->
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ex.dangerBg).padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.Shield, null, tint = ex.danger, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.size(9.dp))
                        Text(message, color = ex.danger, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                        .background(if (canSubmit) ex.brand else Brush.linearGradient(listOf(ex.cardAlt, ex.cardAlt)))
                        .clickableScale {
                            if (canSubmit && !vm.authBusy) {
                                if (mode == AuthMode.LOGIN) vm.signIn(email, password, onAuthenticated)
                                else vm.signUp(name, email, password, onAuthenticated)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    if (vm.authBusy) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text(if (mode == AuthMode.LOGIN) "Sign in" else "Create account", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (canSubmit) Color.White else ex.muted)
                        Spacer(Modifier.size(7.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = if (canSubmit) Color.White else ex.muted, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(if (mode == AuthMode.LOGIN) "New to GATE Prep?" else "Already have an account?", color = ex.muted, fontSize = 13.sp)
                    TextButton(onClick = { if (!vm.authBusy) onSwitchMode() }) {
                        Text(if (mode == AuthMode.LOGIN) "Create account" else "Sign in", color = ex.brandStart, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp).offset(y = (-14).dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.CheckCircle, null, tint = ex.success, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(7.dp))
            Text("Secure session · protected content · synchronized progress", color = ex.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    password: Boolean = false,
    visible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
) {
    val ex = LocalExtra.current
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(label) }, leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (password) {{
            IconButton(onClick = { onToggleVisibility?.invoke() }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (visible) "Hide password" else "Show password")
            }
        }} else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ex.brandStart, focusedLabelColor = ex.brandStart, cursorColor = ex.brandStart,
            unfocusedBorderColor = ex.outline,
        ),
    )
}
