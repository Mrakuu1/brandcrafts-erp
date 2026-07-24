package com.brandcrafts.erp.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun ContactsScreen(
    state: ContactsUiState,
    onEvent: (ContactsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val canAddContact = state.role == UserRole.ADMIN || state.selectedType == ContactType.CUSTOMER

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (canAddContact) {
                FloatingActionButton(
                    onClick = { onEvent(ContactsUiEvent.AddClicked) },
                ) { Text(stringResource(R.string.contacts_add)) }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(
                title = stringResource(R.string.contacts_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            ContactTypeSelector(
                selectedType = state.selectedType,
                onTypeSelected = { onEvent(ContactsUiEvent.TypeSelected(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onEvent(ContactsUiEvent.SearchChanged(it)) },
                placeholder = stringResource(R.string.contacts_search),
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            when (state.content) {
                ContactsUiState.Content.Loading -> LoadingView(
                    message = stringResource(R.string.contacts_loading),
                )

                ContactsUiState.Content.Error -> ErrorState(
                    title = stringResource(R.string.contacts_error),
                    description = stringResource(R.string.contacts_error_description),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { onEvent(ContactsUiEvent.RetryClicked) },
                )

                ContactsUiState.Content.Empty -> EmptyState(
                    title = emptyStateTitle(state),
                    description = stringResource(R.string.contacts_empty_description),
                )

                ContactsUiState.Content.Loaded -> ContactList(
                    contacts = state.visibleContacts,
                    role = state.role,
                    onEditClick = { onEvent(ContactsUiEvent.EditClicked(it)) },
                    onContactClick = { onEvent(ContactsUiEvent.ContactClicked(it)) },
                )
            }
        }
    }
}

@Composable
private fun ContactTypeSelector(
    selectedType: ContactType,
    onTypeSelected: (ContactType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ContactType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ContactType.entries.size),
                label = {
                    Text(
                        text = stringResource(
                            if (type == ContactType.CUSTOMER) {
                                R.string.contacts_customers
                            } else {
                                R.string.contacts_suppliers
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<Contact>,
    role: UserRole?,
    onEditClick: (Contact) -> Unit,
    onContactClick: (Contact) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = contacts, key = Contact::id) { contact ->
            ContactListItem(
                contact = contact,
                canEdit = role == UserRole.ADMIN || contact.type == ContactType.CUSTOMER,
                onEditClick = { onEditClick(contact) },
                onClick = { onContactClick(contact) },
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            headlineContent = { Text(text = contact.name) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (contact.company.isNotBlank()) {
                        Text(text = contact.company)
                    }
                    Text(text = contact.phone)
                    if (contact.email.isNotBlank()) {
                        Text(text = contact.email)
                    }
                }
            },
            trailingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusChip(
                        label = stringResource(
                            if (contact.type == ContactType.CUSTOMER) {
                                R.string.contacts_customer
                            } else {
                                R.string.contacts_supplier
                            },
                        ),
                        tone = StatusTone.INFO,
                    )
                    StatusChip(
                        label = stringResource(
                            if (contact.active) R.string.contacts_active else R.string.contacts_inactive,
                        ),
                        tone = if (contact.active) StatusTone.SUCCESS else StatusTone.NEUTRAL,
                    )
                    if (canEdit) {
                        TextButton(onClick = onEditClick) {
                            Text(text = stringResource(R.string.contacts_edit))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun emptyStateTitle(state: ContactsUiState): String = stringResource(
    when {
        state.searchQuery.isNotBlank() -> R.string.contacts_no_results
        state.selectedType == ContactType.CUSTOMER -> R.string.contacts_empty_customers
        else -> R.string.contacts_empty_suppliers
    },
)

@Preview(showBackground = true)
@Composable
private fun ContactsAdminCustomersPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = previewState(type = ContactType.CUSTOMER, role = UserRole.ADMIN),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsAdminSuppliersPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = previewState(type = ContactType.SUPPLIER, role = UserRole.ADMIN),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsEmployeeCustomersPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = previewState(type = ContactType.CUSTOMER, role = UserRole.EMPLOYEE),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsEmployeeSuppliersDarkPreview() {
    BrandCraftsTheme(darkTheme = true) {
        ContactsScreen(
            state = previewState(type = ContactType.SUPPLIER, role = UserRole.EMPLOYEE),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsLoadingPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = ContactsUiState(content = ContactsUiState.Content.Loading),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsEmptyCustomersPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = ContactsUiState(content = ContactsUiState.Content.Empty),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsEmptySuppliersPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = ContactsUiState(
                content = ContactsUiState.Content.Empty,
                selectedType = ContactType.SUPPLIER,
            ),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsNoResultsPreview() {
    BrandCraftsTheme {
        ContactsScreen(
            state = ContactsUiState(
                content = ContactsUiState.Content.Empty,
                searchQuery = "Sample",
            ),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsErrorPreview() {
    BrandCraftsTheme(darkTheme = true) {
        ContactsScreen(
            state = ContactsUiState(content = ContactsUiState.Content.Error),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

private fun previewState(
    type: ContactType,
    role: UserRole,
): ContactsUiState {
    val contacts = listOf(
        Contact(
            id = "preview-contact",
            type = type,
            name = "Avery Shah",
            company = "BrandCrafts Studio",
            phone = "+91 98765 43210",
            email = "avery@example.com",
            address = "",
            gstNumber = "",
            city = "",
            state = "",
            pincode = "",
            notes = "",
            active = true,
            createdAtMillis = null,
            updatedAtMillis = null,
            createdBy = "",
            updatedBy = "",
        ),
    )
    return ContactsUiState(
        content = ContactsUiState.Content.Loaded,
        selectedType = type,
        allContacts = contacts,
        visibleContacts = contacts,
        role = role,
    )
}
