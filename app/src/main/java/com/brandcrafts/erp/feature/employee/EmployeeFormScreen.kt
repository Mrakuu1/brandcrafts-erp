package com.brandcrafts.erp.feature.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.feature.contacts.PeopleFormSheet
import com.brandcrafts.erp.feature.contacts.PeopleFormTextField
import com.brandcrafts.erp.feature.contacts.peopleFormSwitchColors
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun EmployeeFormScreen(state: EmployeeFormUiState, onEvent: (EmployeeFormUiEvent) -> Unit) {
    when {
        state.isInitialLoading -> LoadingView(message = stringResource(R.string.employee_form_loading))
        state.loadError != null -> ErrorState(
            title = stringResource(R.string.employee_form_load_error_title),
            description = stringResource(state.loadError.descriptionRes()),
            retryLabel = stringResource(R.string.retry),
            onRetry = { onEvent(EmployeeFormUiEvent.RetryLoadClicked) },
            secondaryActionLabel = stringResource(R.string.cancel),
            onSecondaryAction = { onEvent(EmployeeFormUiEvent.CancelClicked) },
        )
        else -> PeopleFormSheet(
            title = stringResource(if (state.mode == EmployeeFormMode.CREATE) R.string.employee_form_create_title else R.string.employee_form_edit_title),
            primaryActionLabel = stringResource(if (state.mode == EmployeeFormMode.CREATE) R.string.employee_form_create_action else R.string.employee_form_save_action),
            onPrimaryAction = { onEvent(EmployeeFormUiEvent.SaveClicked) },
            onDismissRequest = { onEvent(EmployeeFormUiEvent.CancelClicked) },
            primaryActionLoading = state.isSaving,
            primaryActionEnabled = !state.isSaving,
            cancelActionLabel = stringResource(R.string.cancel),
        ) { EmployeeFormFields(state, onEvent) }
    }
}

@Composable
private fun EmployeeFormFields(state: EmployeeFormUiState, onEvent: (EmployeeFormUiEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.saveError?.let { Text(stringResource(it.descriptionRes()), color = MaterialTheme.colorScheme.error) }
        PeopleFormTextField(state.name, { onEvent(EmployeeFormUiEvent.NameChanged(it)) }, stringResource(R.string.employee_form_name_label), errorMessage = state.errors.name?.let { stringResource(it) }, enabled = !state.isSaving)
        PeopleFormTextField(state.email, { onEvent(EmployeeFormUiEvent.EmailChanged(it)) }, stringResource(R.string.employee_form_email_label), errorMessage = state.errors.email?.let { stringResource(it) }, enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        PeopleFormTextField(state.phone, { onEvent(EmployeeFormUiEvent.PhoneChanged(it)) }, stringResource(R.string.employee_form_phone_label), errorMessage = state.errors.phone?.let { stringResource(it) }, enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        if (state.mode == EmployeeFormMode.CREATE) PeopleFormTextField(state.temporaryPassword, { onEvent(EmployeeFormUiEvent.TemporaryPasswordChanged(it)) }, stringResource(R.string.employee_form_temporary_password_label), errorMessage = state.errors.temporaryPassword?.let { stringResource(it) }, enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation())
        Text(stringResource(R.string.employee_management_role_label), style = MaterialTheme.typography.titleSmall)
        EmployeeRoleTabs(
            selectedRole = state.role,
            enabled = !state.isSaving,
            onRoleSelected = { onEvent(EmployeeFormUiEvent.RoleChanged(it)) },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.employee_form_active_label), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.active,
                onCheckedChange = { onEvent(EmployeeFormUiEvent.ActiveChanged(it)) },
                enabled = !state.isSaving,
                colors = peopleFormSwitchColors(),
            )
        }
    }
}

@Composable
private fun EmployeeRoleTabs(
    selectedRole: UserRole,
    enabled: Boolean,
    onRoleSelected: (UserRole) -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val outline = if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (dark) Color(0xFF16212E) else Color(0xFFFFFCFA))
            .border(1.dp, outline, MaterialTheme.shapes.small)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        UserRole.entries.forEach { role ->
            val selected = selectedRole == role
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = if (dark) .24f else .12f)
                        else Color.Transparent,
                    )
                    .clickable(enabled = enabled) { onRoleSelected(role) },
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(
                    text = stringResource(role.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun UserRole.labelRes() = if (this == UserRole.ADMIN) R.string.role_admin else R.string.role_employee
private fun EmployeeFormError.descriptionRes() = when (this) {
    EmployeeFormError.UNAUTHORIZED -> R.string.employee_management_unauthorized; EmployeeFormError.NOT_FOUND -> R.string.employee_management_not_found
    EmployeeFormError.DUPLICATE_EMAIL -> R.string.employee_form_duplicate_email; EmployeeFormError.DUPLICATE_PHONE -> R.string.employee_form_duplicate_phone
    EmployeeFormError.NETWORK -> R.string.employee_management_network_error; EmployeeFormError.VALIDATION -> R.string.employee_form_validation_error; EmployeeFormError.UNKNOWN -> R.string.employee_form_save_error
}

@Preview(showBackground = true) @Composable private fun CreateEmployeePreview() { BrandCraftsTheme { EmployeeFormScreen(EmployeeFormUiState(isInitialLoading = false), {}) } }
@Preview(showBackground = true) @Composable private fun EditEmployeePreview() { BrandCraftsTheme { EmployeeFormScreen(EmployeeFormUiState(mode = EmployeeFormMode.EDIT, uid = "preview", name = "Avery", email = "avery@example.com", phone = "+91 9876543210", isInitialLoading = false), {}) } }
@Preview(showBackground = true) @Composable private fun InvalidEmployeePreview() { BrandCraftsTheme(darkTheme = true) { EmployeeFormScreen(EmployeeFormUiState(isInitialLoading = false, errors = EmployeeFormFieldErrors(name = R.string.employee_form_name_required, temporaryPassword = R.string.employee_form_password_invalid)), {}) } }
@Preview(showBackground = true) @Composable private fun SavingEmployeePreview() { BrandCraftsTheme { EmployeeFormScreen(EmployeeFormUiState(isInitialLoading = false, isSaving = true), {}) } }
