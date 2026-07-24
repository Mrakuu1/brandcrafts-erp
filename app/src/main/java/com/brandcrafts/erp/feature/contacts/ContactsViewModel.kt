package com.brandcrafts.erp.feature.contacts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
@HiltViewModel class ContactsViewModel @Inject constructor(private val observe:ObserveContactsUseCase,private val session:SessionManager):ViewModel(){private val _state=MutableStateFlow(ContactsUiState());val state=_state.asStateFlow();private val _effects=Channel<ContactsUiEffect>(Channel.BUFFERED);val effects=_effects.receiveAsFlow();private var job:Job?=null;init{collect()};fun onEvent(e:ContactsUiEvent){when(e){is ContactsUiEvent.SearchChanged->{_state.value=_state.value.copy(searchQuery=e.query);filter()};is ContactsUiEvent.TypeSelected->{_state.value=_state.value.copy(selectedType=e.type);filter()};ContactsUiEvent.RetryClicked->collect();ContactsUiEvent.AddClicked->add();is ContactsUiEvent.EditClicked->edit(e.contact);is ContactsUiEvent.ContactClicked->emit(ContactsUiEffect.ShowUnavailableFeature);ContactsUiEvent.ErrorConsumed->Unit}}private fun collect(){job?.cancel();val role=(session.currentUser.value as? CurrentUserState.Authenticated)?.user?.role;_state.value=_state.value.copy(content=ContactsUiState.Content.Loading,role=role);job=viewModelScope.launch{observe().collect{r->r.fold({items->_state.value=_state.value.copy(allContacts=items,role=role);filter()},{_state.value=_state.value.copy(content=ContactsUiState.Content.Error)})}}}private fun filter(){val s=_state.value;val q=s.searchQuery.trim();val v=s.allContacts.filter{it.type==s.selectedType&&(q.isBlank()||listOf(it.name,it.company,it.phone,it.email).any{x->x.contains(q,true)})};_state.value=s.copy(visibleContacts=v,content=if(v.isEmpty())ContactsUiState.Content.Empty else ContactsUiState.Content.Loaded)}private fun add(){if(_state.value.role==UserRole.ADMIN||_state.value.selectedType==ContactType.CUSTOMER)emit(if(_state.value.selectedType==ContactType.CUSTOMER)ContactsUiEffect.RequestAddCustomer else ContactsUiEffect.RequestAddSupplier)}private fun edit(c:Contact){if(_state.value.role==UserRole.ADMIN||c.type==ContactType.CUSTOMER)emit(if(c.type==ContactType.CUSTOMER)ContactsUiEffect.RequestEditCustomer(c.id)else ContactsUiEffect.RequestEditSupplier(c.id))}private fun emit(e:ContactsUiEffect){viewModelScope.launch{_effects.send(e)}}}
