package com.brandcrafts.erp.feature.quotation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import java.io.File
import kotlinx.coroutines.CancellationException

@Composable
fun QuotationDetailsRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: QuotationDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentBack by rememberUpdatedState(onBack)
    val currentEdit by rememberUpdatedState(onEdit)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.effects.collect { effect -> when(effect) {
        QuotationDetailsUiEffect.NavigateBack -> currentBack()
        is QuotationDetailsUiEffect.NavigateToEdit -> currentEdit(effect.quotationId)
        is QuotationDetailsUiEffect.PreviewPdf -> openQuotationPdf(context,effect.cacheFileName,true){snackbar.showSnackbar(resources.getString(R.string.quotation_pdf_preview_error))}
        is QuotationDetailsUiEffect.SharePdf -> openQuotationPdf(context,effect.cacheFileName,false){snackbar.showSnackbar(resources.getString(R.string.quotation_pdf_share_error))}
        is QuotationDetailsUiEffect.ShowMessage -> snackbar.showSnackbar(resources.getString(effect.messageRes))
    } } }
    QuotationDetailsScreen(state, viewModel::onEvent, snackbar)
}

private suspend fun openQuotationPdf(context: android.content.Context, cacheFileName:String, preview:Boolean, onFailure:suspend()->Unit) {
    val file=File(File(context.cacheDir,"pdf"),cacheFileName)
    if(file.name!=cacheFileName || !file.isFile){onFailure();return}
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
    val intent=Intent(if(preview) Intent.ACTION_VIEW else Intent.ACTION_SEND).apply { if(preview)setDataAndType(uri,"application/pdf") else {type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);clipData=ClipData.newRawUri(cacheFileName,uri)};addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    try{context.startActivity(if(preview)intent else Intent.createChooser(intent,null))}catch(_:ActivityNotFoundException){onFailure()}catch(_:SecurityException){onFailure()}catch(e:CancellationException){throw e}catch(_:Throwable){onFailure()}
}
