package com.ammar.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ammar.cinestream.feature.home.R

/** The signed-in account in the home toolbar. The app draws it; the server knows nothing of it. */
@Composable
internal fun AccountMenu(
    username: String,
    onSignOut: () -> Unit
) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.home_account_menu),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                text = if (username.isBlank()) stringResource(R.string.home_signed_in)
                else stringResource(R.string.home_signed_in_as, username),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_sign_out)) },
                onClick = {
                    open = false
                    onSignOut()
                }
            )
        }
    }
}
