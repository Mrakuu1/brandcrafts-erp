package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isAdmin: Boolean,
    displayName: String,
    email: String,
    roleLabel: String,
    avatarContentDescription: String,
    profileLabel: String,
    changePasswordLabel: String,
    employeeManagementLabel: String,
    settingsLabel: String,
    logoutLabel: String,
    onProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onEmployeeManagementClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProfileAvatar(
                initials = displayName.initials(),
                contentDescription = avatarContentDescription,
            )
            Text(text = displayName)
            Text(text = email)
            Text(text = roleLabel)
        }
        DropdownMenuItem(text = { Text(profileLabel) }, onClick = onProfileClick)
        DropdownMenuItem(text = { Text(changePasswordLabel) }, onClick = onChangePasswordClick)
        if (isAdmin) {
            DropdownMenuItem(text = { Text(employeeManagementLabel) }, onClick = onEmployeeManagementClick)
            DropdownMenuItem(text = { Text(settingsLabel) }, onClick = onSettingsClick)
        }
        DropdownMenuItem(text = { Text(logoutLabel) }, onClick = onLogoutClick)
    }
}

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .joinToString(separator = "") { it.first().uppercase() }
