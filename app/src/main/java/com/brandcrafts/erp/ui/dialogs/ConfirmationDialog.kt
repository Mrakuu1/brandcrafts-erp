package com.brandcrafts.erp.ui.dialogs

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
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
    destructive: Boolean = false,
) {
    BrandDialog(
        onDismissRequest = { if (!confirmLoading) onDismiss() },
        title = title,
        description = description,
        confirmButton = {
            if (destructive) {
                Button(
                    onClick = onConfirm,
                    enabled = !confirmLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                    ),
                ) { Text(confirmLabel) }
            } else {
                PrimaryButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    loading = confirmLoading,
                )
            }
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
