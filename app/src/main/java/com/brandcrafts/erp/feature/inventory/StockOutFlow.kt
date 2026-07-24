package com.brandcrafts.erp.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.core.result.StockOutError
import com.brandcrafts.erp.core.result.StockOutResult
import com.brandcrafts.erp.domain.model.StockOutInput
import com.brandcrafts.erp.domain.usecase.GetInventoryItemUseCase
import com.brandcrafts.erp.domain.usecase.StockOutUseCase
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

data class StockOutUiState(val materialId: String, val name: String = "", val quantity: String = "", val reason: String = "", val remarks: String = "", val quantityError: Int? = null, val loading: Boolean = true, val saving: Boolean = false, val error: Int? = null)
sealed interface StockOutUiEvent { data class Quantity(val value: String): StockOutUiEvent; data class Reason(val value: String): StockOutUiEvent; data class Remarks(val value: String): StockOutUiEvent; data object Save: StockOutUiEvent; data object Cancel: StockOutUiEvent; data object Retry: StockOutUiEvent }
sealed interface StockOutUiEffect { data object Back: StockOutUiEffect; data object Saved: StockOutUiEffect }

@HiltViewModel class StockOutViewModel @Inject constructor(saved: SavedStateHandle, private val getItem: GetInventoryItemUseCase, private val stockOut: StockOutUseCase): ViewModel() {
    private val id = checkNotNull<String>(saved["materialId"]); private val _state = MutableStateFlow(StockOutUiState(id)); val state = _state.asStateFlow(); private val _effects = Channel<StockOutUiEffect>(Channel.BUFFERED); val effects = _effects.receiveAsFlow()
    init { load() }
    fun onEvent(e: StockOutUiEvent) { when(e) { is StockOutUiEvent.Quantity -> update { copy(quantity=e.value,quantityError=null,error=null) }; is StockOutUiEvent.Reason -> update { copy(reason=e.value,error=null) }; is StockOutUiEvent.Remarks -> update { copy(remarks=e.value,error=null) }; StockOutUiEvent.Save -> save(); StockOutUiEvent.Cancel -> emit(StockOutUiEffect.Back); StockOutUiEvent.Retry -> load() } }
    private fun load() = viewModelScope.launch { when(val r=getItem(id)) { is InventoryResult.Success -> if(r.data.active) update { copy(name=r.data.name,loading=false) } else update { copy(loading=false,error=R.string.stock_out_error_inactive) }; is InventoryResult.Error -> update { copy(loading=false,error=R.string.stock_out_error_material) } } }
    private fun save() { val s=_state.value; if(s.saving||s.loading)return; val q=s.quantity.toDoubleOrNull(); if(q==null||q<=0){update{copy(quantityError=R.string.stock_out_quantity_invalid)};return}; if(s.reason.isBlank()){update{copy(error=R.string.stock_out_reason_required)};return}; update{copy(saving=true,error=null)}; viewModelScope.launch { when(val r=stockOut(StockOutInput(id,q,s.reason,s.remarks))) { is StockOutResult.Success->emit(StockOutUiEffect.Saved); is StockOutResult.Error->update{copy(saving=false,error=r.error.res())} } } }
    private fun update(f: StockOutUiState.()->StockOutUiState){_state.value=_state.value.f()}; private fun emit(e:StockOutUiEffect){viewModelScope.launch{_effects.send(e)}}
}
private fun StockOutError.res()=when(this){StockOutError.INSUFFICIENT_STOCK->R.string.stock_out_error_insufficient;StockOutError.UNAUTHORIZED->R.string.stock_out_error_unauthorized;StockOutError.MATERIAL_INACTIVE->R.string.stock_out_error_inactive;StockOutError.MATERIAL_NOT_FOUND->R.string.stock_out_error_material;StockOutError.NETWORK_UNAVAILABLE->R.string.stock_out_error_network;else->R.string.stock_out_error_unknown}
@Composable fun StockOutRoute(back:()->Unit,saved:()->Unit,vm:StockOutViewModel=hiltViewModel()){val s by vm.state.collectAsStateWithLifecycle();LaunchedEffect(vm){vm.effects.collect{if(it==StockOutUiEffect.Saved)saved() else back()}};StockOutScreen(s,vm::onEvent)}
@Composable fun StockOutScreen(s:StockOutUiState,event:(StockOutUiEvent)->Unit){when{ s.loading->LoadingView(message=stringResource(R.string.stock_out_loading));s.name.isBlank()->ErrorState(stringResource(R.string.stock_out_error_title),stringResource(s.error?:R.string.stock_out_error_material),stringResource(R.string.retry),{event(StockOutUiEvent.Retry)},secondaryActionLabel=stringResource(R.string.cancel),onSecondaryAction={event(StockOutUiEvent.Cancel)});else->UniversalFormSheet(stringResource(R.string.stock_out_title),stringResource(R.string.stock_out_save),{event(StockOutUiEvent.Save)},{event(StockOutUiEvent.Cancel)},primaryActionLoading=s.saving,cancelActionLabel=stringResource(R.string.cancel)){Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){s.error?.let{Text(stringResource(it),color=MaterialTheme.colorScheme.error)};AppTextField(s.name,{},stringResource(R.string.stock_out_material),readOnly=true);AppTextField(s.quantity,{event(StockOutUiEvent.Quantity(it))},stringResource(R.string.stock_out_quantity),errorMessage=s.quantityError?.let{stringResource(it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal));AppTextField(s.reason,{event(StockOutUiEvent.Reason(it))},stringResource(R.string.stock_out_reason));AppTextField(s.remarks,{event(StockOutUiEvent.Remarks(it))},stringResource(R.string.stock_out_remarks),singleLine=false)}}}}
