package com.brandcrafts.erp.feature.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.feature.employee.EmployeeListItemUi
import com.brandcrafts.erp.feature.employee.EmployeeManagementUiEvent
import com.brandcrafts.erp.feature.employee.EmployeeManagementUiState
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet
import com.brandcrafts.erp.ui.LocalBottomChromeVisible
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView

private enum class PeopleStatusFilter { ALL, ACTIVE, INACTIVE }

@Composable
fun PeopleScreen(
    contactState: ContactsUiState,
    employeeState: EmployeeManagementUiState,
    role: UserRole?,
    selectedTab: PeopleTab,
    onTabSelected: (PeopleTab) -> Unit,
    onContactEvent: (ContactsUiEvent) -> Unit,
    onEmployeeEvent: (EmployeeManagementUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var filterOpen by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf(PeopleStatusFilter.ALL) }
    val employeesAllowed = role == UserRole.ADMIN
    val availableTabs = if (employeesAllowed) PeopleTab.entries else listOf(PeopleTab.CUSTOMERS, PeopleTab.SUPPLIERS)
    val activeTab = selectedTab.takeIf { it in availableTabs } ?: PeopleTab.CUSTOMERS

    Box(modifier = modifier.fillMaxSize().background(peoplePageColor())) {
        Column(modifier = Modifier.fillMaxSize()) {
            PeopleTabs(
                tabs = availableTabs,
                selectedTab = activeTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )
            PeopleSearchRow(
                tab = activeTab,
                contactQuery = contactState.searchQuery,
                employeeQuery = employeeState.searchQuery,
                onQueryChange = { query ->
                    if (activeTab == PeopleTab.EMPLOYEES) {
                        onEmployeeEvent(EmployeeManagementUiEvent.SearchChanged(query))
                    } else {
                        onContactEvent(ContactsUiEvent.SearchChanged(query))
                    }
                },
                onFilterClick = { filterOpen = true },
            )
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    PeopleTab.CUSTOMERS, PeopleTab.SUPPLIERS -> PeopleContactsContent(
                        state = contactState,
                        role = role,
                        statusFilter = statusFilter,
                        onRetry = { onContactEvent(ContactsUiEvent.RetryClicked) },
                        onEdit = { onContactEvent(ContactsUiEvent.EditClicked(it)) },
                    )
                    PeopleTab.EMPLOYEES -> PeopleEmployeesContent(
                        state = employeeState,
                        statusFilter = statusFilter,
                        onRetry = { onEmployeeEvent(EmployeeManagementUiEvent.RetryClicked) },
                        onEdit = { onEmployeeEvent(EmployeeManagementUiEvent.EditEmployeeRequested(it)) },
                    )
                }
            }
        }
        PeopleFab(
            tab = activeTab,
            visible = when (activeTab) {
                PeopleTab.CUSTOMERS -> true
                PeopleTab.SUPPLIERS -> role == UserRole.ADMIN
                PeopleTab.EMPLOYEES -> employeesAllowed
            },
            onClick = {
                when (activeTab) {
                    PeopleTab.CUSTOMERS, PeopleTab.SUPPLIERS -> onContactEvent(ContactsUiEvent.AddClicked)
                    PeopleTab.EMPLOYEES -> onEmployeeEvent(EmployeeManagementUiEvent.AddEmployeeClicked)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp),
        )
    }
    if (filterOpen) {
        PeopleFilterSheet(
            selectedFilter = statusFilter,
            onApply = { statusFilter = it; filterOpen = false },
            onDismissRequest = { filterOpen = false },
        )
    }
}

@Composable
private fun PeopleTabs(
    tabs: List<PeopleTab>,
    selectedTab: PeopleTab,
    onTabSelected: (PeopleTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val isDark = peopleDark()
    val selectedTint = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) .25f else .13f)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isDark) Color(0xFF111A25) else Color(0xFFF9F4F0))
            .padding(3.dp),
    ) {
        val itemGap = 4.dp
        val segmentWidth = (maxWidth - itemGap * (tabs.size - 1)) / tabs.size
        val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
        val selectedOffset by animateDpAsState(
            targetValue = (segmentWidth + itemGap) * selectedIndex,
            animationSpec = tween(durationMillis = 180),
            label = "peopleTabSlide",
        )
        Box(modifier = Modifier.fillMaxWidth().height(34.dp)) {
            Box(
                modifier = Modifier
                    .offset(x = selectedOffset)
                    .width(segmentWidth)
                    .height(34.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(selectedTint),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(itemGap),
            ) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else peopleSecondaryColor(),
                        label = "peopleTabText",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(tab.labelRes()),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleSearchRow(
    tab: PeopleTab,
    contactQuery: String,
    employeeQuery: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    val query = if (tab == PeopleTab.EMPLOYEES) employeeQuery else contactQuery
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(if (peopleDark()) 0.dp else 3.dp, shape, clip = false)
                .clip(shape),
            singleLine = true,
            shape = shape,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    stringResource(tab.searchRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = peopleSecondaryColor(),
                )
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, null, tint = peopleSecondaryColor(), modifier = Modifier.size(19.dp))
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = peopleCardColor(),
                unfocusedContainerColor = peopleCardColor(),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(if (peopleDark()) 0.dp else 3.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(peopleCardColor())
                .border(1.dp, peopleOutline(), CircleShape)
                .clickable(onClick = onFilterClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.FilterList, stringResource(R.string.people_filter), tint = peopleSecondaryColor())
        }
    }
}

@Composable
private fun PeopleContactsContent(
    state: ContactsUiState,
    role: UserRole?,
    statusFilter: PeopleStatusFilter,
    onRetry: () -> Unit,
    onEdit: (Contact) -> Unit,
) {
    when (state.content) {
        ContactsUiState.Content.Loading -> LoadingView(message = stringResource(R.string.contacts_loading))
        ContactsUiState.Content.Error -> ErrorState(
            title = stringResource(R.string.contacts_error),
            description = stringResource(R.string.contacts_error_description),
            retryLabel = stringResource(R.string.retry),
            onRetry = onRetry,
        )
        ContactsUiState.Content.Empty -> PeopleEmptyState(state.selectedType == com.brandcrafts.erp.domain.model.ContactType.CUSTOMER)
        ContactsUiState.Content.Loaded -> {
            val contacts = state.visibleContacts.filter { it.matches(statusFilter) }
            if (contacts.isEmpty()) PeopleEmptyState(state.selectedType == com.brandcrafts.erp.domain.model.ContactType.CUSTOMER)
            else LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(contacts, key = Contact::id) { contact ->
                    PeopleContactCard(
                        contact = contact,
                        canEdit = role == UserRole.ADMIN ||
                            contact.type == com.brandcrafts.erp.domain.model.ContactType.CUSTOMER,
                        onEdit = { onEdit(contact) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PeopleEmployeesContent(
    state: EmployeeManagementUiState,
    statusFilter: PeopleStatusFilter,
    onRetry: () -> Unit,
    onEdit: (EmployeeListItemUi) -> Unit,
) {
    when (state.content) {
        EmployeeManagementUiState.Content.Loading -> LoadingView(message = stringResource(R.string.employee_management_loading))
        EmployeeManagementUiState.Content.Error -> ErrorState(
            title = stringResource(R.string.employee_management_error),
            description = stringResource(R.string.employee_management_error_description),
            retryLabel = stringResource(R.string.retry),
            onRetry = onRetry,
        )
        EmployeeManagementUiState.Content.Empty -> PeopleEmployeeEmptyState()
        EmployeeManagementUiState.Content.Loaded -> {
            val employees = state.visibleEmployees.filter { it.matches(statusFilter) }
            if (employees.isEmpty()) PeopleEmployeeEmptyState()
            else LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(employees, key = EmployeeListItemUi::uid) { employee ->
                    PeopleEmployeeCard(employee = employee, onEdit = { onEdit(employee) })
                }
            }
        }
    }
}

@Composable
private fun PeopleContactCard(contact: Contact, canEdit: Boolean, onEdit: () -> Unit) {
    PeopleCard(
        name = contact.name,
        subtitle = contact.company,
        phone = contact.phone,
        email = contact.email,
        active = contact.active,
        canEdit = canEdit,
        onEdit = onEdit,
    )
}

@Composable
private fun PeopleEmployeeCard(employee: EmployeeListItemUi, onEdit: () -> Unit) {
    PeopleCard(
        name = employee.name,
        subtitle = employee.designation.ifBlank { stringResource(employee.role.peopleLabelRes()) },
        phone = employee.phone,
        email = employee.email,
        active = employee.active,
        canEdit = true,
        onEdit = onEdit,
    )
}

@Composable
private fun PeopleCard(
    name: String,
    subtitle: String,
    phone: String,
    email: String,
    active: Boolean,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (peopleDark()) 0.dp else 3.dp, shape, clip = false)
            .clip(shape)
            .background(peopleCardColor())
            .border(1.dp, peopleOutline(), shape),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PeopleAvatar(name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = peopleSecondaryColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (phone.isNotBlank()) PeopleMetadata(Icons.Outlined.Phone, phone)
                if (email.isNotBlank()) PeopleMetadata(Icons.Outlined.Email, email)
            }
            PeopleActiveIndicator(active)
        }
        if (canEdit) {
            HorizontalDivider(color = peopleOutline())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable(onClick = onEdit),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Box(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.contacts_edit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PeopleAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (peopleDark()) Color(0xFF4A2A17) else Color(0xFFFFE8D8)),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.initials(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PeopleActiveIndicator(active: Boolean) {
    val color = if (active) Color(0xFF168A4B) else Color(0xFFC33A3A)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (peopleDark()) .24f else .12f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(
            stringResource(if (active) R.string.contacts_active else R.string.contacts_inactive),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun PeopleMetadata(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = peopleSecondaryColor())
        Text(value, style = MaterialTheme.typography.labelSmall, color = peopleSecondaryColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PeopleFab(
    tab: PeopleTab,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val shape = RoundedCornerShape(16.dp)
    AnimatedVisibility(
        visible = LocalBottomChromeVisible.current,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp, bottom = 92.dp)
            .height(44.dp)
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(19.dp))
            Text(stringResource(tab.addRes()), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
    }
}

@Composable
private fun PeopleFilterSheet(
    selectedFilter: PeopleStatusFilter,
    onApply: (PeopleStatusFilter) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var pending by remember(selectedFilter) { mutableStateOf(selectedFilter) }
    BrandBottomSheet(
        title = stringResource(R.string.people_apply_filter),
        onDismissRequest = onDismissRequest,
        containerColor = peopleCardColor(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PeopleStatusFilter.entries.forEach { filter ->
                val selected = filter == pending
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = if (peopleDark()) .70f else .45f),
                            RoundedCornerShape(12.dp),
                        )
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = if (peopleDark()) .22f else .1f) else Color.Transparent)
                        .clickable { pending = filter },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(filter.labelRes()),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
                    .clickable { onApply(pending) },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.people_apply_filter), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}

@Composable
private fun PeopleEmptyState(customers: Boolean) = EmptyState(
    title = stringResource(if (customers) R.string.contacts_empty_customers else R.string.contacts_empty_suppliers),
    description = stringResource(R.string.contacts_empty_description),
)

@Composable
private fun PeopleEmployeeEmptyState() = EmptyState(
    title = stringResource(R.string.employee_management_empty),
    description = stringResource(R.string.employee_management_empty_description),
)

private fun Contact.matches(filter: PeopleStatusFilter): Boolean = when (filter) {
    PeopleStatusFilter.ALL -> true
    PeopleStatusFilter.ACTIVE -> active
    PeopleStatusFilter.INACTIVE -> !active
}

private fun EmployeeListItemUi.matches(filter: PeopleStatusFilter): Boolean = when (filter) {
    PeopleStatusFilter.ALL -> true
    PeopleStatusFilter.ACTIVE -> active
    PeopleStatusFilter.INACTIVE -> !active
}

private fun PeopleTab.labelRes(): Int = when (this) {
    PeopleTab.CUSTOMERS -> R.string.contacts_customers
    PeopleTab.SUPPLIERS -> R.string.contacts_suppliers
    PeopleTab.EMPLOYEES -> R.string.people_employees
}

private fun PeopleTab.searchRes(): Int = when (this) {
    PeopleTab.CUSTOMERS -> R.string.people_search_customers
    PeopleTab.SUPPLIERS -> R.string.people_search_suppliers
    PeopleTab.EMPLOYEES -> R.string.people_search_employees
}

private fun PeopleTab.addRes(): Int = when (this) {
    PeopleTab.CUSTOMERS -> R.string.people_add_customer
    PeopleTab.SUPPLIERS -> R.string.people_add_supplier
    PeopleTab.EMPLOYEES -> R.string.people_add_employee
}

private fun PeopleStatusFilter.labelRes(): Int = when (this) {
    PeopleStatusFilter.ALL -> R.string.people_filter_all
    PeopleStatusFilter.ACTIVE -> R.string.contacts_active
    PeopleStatusFilter.INACTIVE -> R.string.contacts_inactive
}

private fun UserRole.peopleLabelRes(): Int = when (this) {
    UserRole.ADMIN -> R.string.role_admin
    UserRole.EMPLOYEE -> R.string.role_employee
}

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .joinToString(separator = "") { it.first().uppercase() }
    .ifBlank { "?" }

@Composable
private fun peopleDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

@Composable
private fun peoplePageColor(): Color = if (peopleDark()) Color(0xFF070D14) else Color(0xFFFFFCFA)

@Composable
private fun peopleCardColor(): Color = if (peopleDark()) Color(0xFF111A25) else Color.White

@Composable
private fun peopleOutline(): Color = if (peopleDark()) Color(0xFF283646) else Color(0xFFEEE8E3)

@Composable
private fun peopleSecondaryColor(): Color = if (peopleDark()) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)
