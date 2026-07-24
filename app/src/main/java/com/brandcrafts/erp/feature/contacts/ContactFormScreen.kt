package com.brandcrafts.erp.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun ContactFormScreen(
    uiState: ContactFormUiState,
    onEvent: (ContactFormUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isInitialLoading -> LoadingView(
            message = stringResource(R.string.contact_form_loading),
            modifier = modifier,
        )

        uiState.loadError != null -> ErrorState(
            title = stringResource(R.string.contact_form_load_error_title),
            description = stringResource(uiState.loadError.descriptionRes()),
            retryLabel = stringResource(R.string.retry),
            onRetry = { onEvent(ContactFormUiEvent.RetryLoadClicked) },
            secondaryActionLabel = stringResource(R.string.cancel),
            onSecondaryAction = { onEvent(ContactFormUiEvent.CancelClicked) },
            modifier = modifier,
        )

        else -> UniversalFormSheet(
            title = stringResource(uiState.titleRes()),
            primaryActionLabel = stringResource(uiState.saveLabelRes()),
            onPrimaryAction = { onEvent(ContactFormUiEvent.SaveClicked) },
            onDismissRequest = { onEvent(ContactFormUiEvent.CancelClicked) },
            primaryActionLoading = uiState.isSaving,
            primaryActionEnabled = !uiState.isSaving,
            cancelActionLabel = stringResource(R.string.cancel),
            modifier = modifier,
        ) {
            ContactFormFields(uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun ContactFormFields(
    uiState: ContactFormUiState,
    onEvent: (ContactFormUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.saveError?.let { error ->
            Text(
                text = stringResource(error.descriptionRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AppTextField(
            value = stringResource(uiState.type.typeRes()),
            onValueChange = {},
            label = stringResource(R.string.contact_form_type_label),
            readOnly = true,
            enabled = false,
        )
        AppTextField(
            value = uiState.name,
            onValueChange = { onEvent(ContactFormUiEvent.NameChanged(it)) },
            label = stringResource(R.string.contact_form_name_label),
            errorMessage = uiState.errors.name?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        AppTextField(
            value = uiState.company,
            onValueChange = { onEvent(ContactFormUiEvent.CompanyChanged(it)) },
            label = stringResource(R.string.contact_form_company_label),
            enabled = !uiState.isSaving,
        )
        AppTextField(
            value = uiState.phone,
            onValueChange = { onEvent(ContactFormUiEvent.PhoneChanged(it)) },
            label = stringResource(R.string.contact_form_phone_label),
            errorMessage = uiState.errors.phone?.let { stringResource(it) },
            enabled = !uiState.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        AppTextField(
            value = uiState.email,
            onValueChange = { onEvent(ContactFormUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.contact_form_email_label),
            errorMessage = uiState.errors.email?.let { stringResource(it) },
            enabled = !uiState.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        AppTextField(
            value = uiState.address,
            onValueChange = { onEvent(ContactFormUiEvent.AddressChanged(it)) },
            label = stringResource(R.string.contact_form_address_label),
            enabled = !uiState.isSaving,
            singleLine = false,
        )
        AppTextField(
            value = uiState.gstNumber,
            onValueChange = { onEvent(ContactFormUiEvent.GstNumberChanged(it)) },
            label = stringResource(R.string.contact_form_gst_label),
            enabled = !uiState.isSaving,
        )
        AppTextField(
            value = uiState.city,
            onValueChange = { onEvent(ContactFormUiEvent.CityChanged(it)) },
            label = stringResource(R.string.contact_form_city_label),
            enabled = !uiState.isSaving,
        )
        AppTextField(
            value = uiState.state,
            onValueChange = { onEvent(ContactFormUiEvent.StateChanged(it)) },
            label = stringResource(R.string.contact_form_state_label),
            enabled = !uiState.isSaving,
        )
        AppTextField(
            value = uiState.pincode,
            onValueChange = { onEvent(ContactFormUiEvent.PincodeChanged(it)) },
            label = stringResource(R.string.contact_form_pincode_label),
            enabled = !uiState.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        AppTextField(
            value = uiState.notes,
            onValueChange = { onEvent(ContactFormUiEvent.NotesChanged(it)) },
            label = stringResource(R.string.contact_form_notes_label),
            enabled = !uiState.isSaving,
            singleLine = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.contact_form_active_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = uiState.active,
                onCheckedChange = { onEvent(ContactFormUiEvent.ActiveChanged(it)) },
                enabled = !uiState.isSaving,
            )
        }
    }
}

private fun ContactFormUiState.titleRes(): Int = when (mode) {
    ContactFormMode.CREATE -> when (type) {
        ContactType.CUSTOMER -> R.string.contact_form_add_customer_title
        ContactType.SUPPLIER -> R.string.contact_form_add_supplier_title
        null -> R.string.contact_form_title
    }
    ContactFormMode.EDIT -> when (type) {
        ContactType.CUSTOMER -> R.string.contact_form_edit_customer_title
        ContactType.SUPPLIER -> R.string.contact_form_edit_supplier_title
        null -> R.string.contact_form_title
    }
}

private fun ContactFormUiState.saveLabelRes(): Int = when (mode) {
    ContactFormMode.CREATE -> R.string.contact_form_create_action
    ContactFormMode.EDIT -> R.string.contact_form_save_action
}

private fun ContactType?.typeRes(): Int = when (this) {
    ContactType.CUSTOMER -> R.string.contacts_customer
    ContactType.SUPPLIER -> R.string.contacts_supplier
    null -> R.string.contact_form_type_unknown
}

private fun ContactFormError.descriptionRes(): Int = when (this) {
    ContactFormError.UNAUTHORIZED -> R.string.contact_form_error_unauthorized
    ContactFormError.DUPLICATE_PHONE -> R.string.contact_form_error_duplicate_phone
    ContactFormError.DUPLICATE_EMAIL -> R.string.contact_form_error_duplicate_email
    ContactFormError.CONTACT_NOT_FOUND -> R.string.contact_form_error_not_found
    ContactFormError.NETWORK -> R.string.contact_form_error_network
    ContactFormError.UNKNOWN -> R.string.contact_form_error_unknown
}

@Preview(showBackground = true)
@Composable
private fun AddCustomerContactFormPreview() {
    BrandCraftsTheme {
        ContactFormScreen(previewContactFormState(type = ContactType.CUSTOMER), onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EditCustomerContactFormPreview() {
    BrandCraftsTheme {
        ContactFormScreen(
            previewContactFormState(type = ContactType.CUSTOMER).copy(
                mode = ContactFormMode.EDIT,
                contactId = "contact-preview",
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddSupplierContactFormPreview() {
    BrandCraftsTheme(darkTheme = true) {
        ContactFormScreen(previewContactFormState(type = ContactType.SUPPLIER), onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactFormValidationPreview() {
    BrandCraftsTheme {
        ContactFormScreen(
            previewContactFormState(type = ContactType.CUSTOMER).copy(
                errors = ContactFormFieldErrors(
                    name = R.string.contact_form_name_required,
                    phone = R.string.contact_form_phone_invalid,
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactFormSavingPreview() {
    BrandCraftsTheme {
        ContactFormScreen(
            previewContactFormState(type = ContactType.CUSTOMER).copy(isSaving = true),
            onEvent = {},
        )
    }
}

private fun previewContactFormState(type: ContactType) = ContactFormUiState(
    type = type,
    name = "Avery Shah",
    company = "BrandCrafts Studio",
    phone = "+91 98765 43210",
    email = "avery@example.com",
    address = "12 Market Road",
    city = "Ahmedabad",
    state = "Gujarat",
    pincode = "380001",
    isInitialLoading = false,
)
