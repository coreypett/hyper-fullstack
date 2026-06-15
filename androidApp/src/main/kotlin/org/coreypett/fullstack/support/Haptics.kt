package org.coreypett.fullstack.support

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

@Composable
fun rememberTapHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val performHaptic = rememberTapHaptic()
    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
    ) {
        performHaptic()
        onClick()
    }
}
