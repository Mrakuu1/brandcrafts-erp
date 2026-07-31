package com.brandcrafts.erp.ui.dialogs

import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun ConfirmationDialog(
    title: String,
    description: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLoading: Boolean = false,
) {
    BrandDialog(
        onDismissRequest = { if (!confirmLoading) onDismiss() },
        title = title,
        description = description,
        confirmButton = {
            PrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                loading = confirmLoading,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !confirmLoading) {
                Text(text = dismissLabel)
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    BrandCraftsTheme {
        ConfirmationDialog(
            title = "Sign out?",
            description = "You will need to sign in again to continue.",
            confirmLabel = "Sign out",
            dismissLabel = "Cancel",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
