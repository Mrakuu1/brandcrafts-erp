package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
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

/** Full-height navigation drawer used by the shell menu control. */
@Composable
fun ProfileSideMenu(
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileAvatar(
            initials = displayName.initials(),
            contentDescription = avatarContentDescription,
        )
        Text(text = displayName)
        Text(text = email)
        Text(text = roleLabel)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        ProfileDrawerItem(profileLabel, Icons.Outlined.ManageAccounts, onProfileClick)
        ProfileDrawerItem(changePasswordLabel, Icons.Outlined.Key, onChangePasswordClick)
        if (isAdmin) {
            ProfileDrawerItem(employeeManagementLabel, Icons.Outlined.People, onEmployeeManagementClick)
            ProfileDrawerItem(settingsLabel, Icons.Outlined.Settings, onSettingsClick)
        }
        ProfileDrawerItem(logoutLabel, Icons.Outlined.Logout, onLogoutClick)
    }
}

@Composable
private fun ProfileDrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        icon = { Icon(imageVector = icon, contentDescription = null) },
    )
}

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .joinToString(separator = "") { it.first().uppercase() }
