package com.brandcrafts.erp.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProfileMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isAdmin: Boolean,
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
        DropdownMenuItem(text = { Text(profileLabel) }, onClick = onProfileClick)
        DropdownMenuItem(text = { Text(changePasswordLabel) }, onClick = onChangePasswordClick)
        if (isAdmin) {
            DropdownMenuItem(text = { Text(employeeManagementLabel) }, onClick = onEmployeeManagementClick)
            DropdownMenuItem(text = { Text(settingsLabel) }, onClick = onSettingsClick)
        }
        DropdownMenuItem(text = { Text(logoutLabel) }, onClick = onLogoutClick)
    }
}
