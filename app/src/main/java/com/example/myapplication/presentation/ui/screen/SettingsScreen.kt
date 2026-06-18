package com.example.myapplication.presentation.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.presentation.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val logoFile = File(context.filesDir, "company_logo.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                logoFile.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.updateLogoPath(logoFile.absolutePath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("🏢 DATOS DE LA EMPRESA")

            SectionTitle("🔧 RUBRO")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("CERRAMIENTOS" to "Carpintería", "HERRERIA" to "Herrería").forEach { (value, label) ->
                    val selected = uiState.businessType == value
                    Button(
                        onClick = { viewModel.updateBusinessType(value) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            SectionTitle("🖼️ LOGO")

            if (uiState.logoPath.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeFile(uiState.logoPath)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Logo de la empresa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin logo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = { logoPickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.logoPath.isNotEmpty()) "Cambiar logo" else "Seleccionar logo")
            }

            SectionTitle("📋 INFORMACIÓN")

            FormTextField(
                label = "Nombre de la empresa",
                value = uiState.companyName,
                onValueChange = viewModel::updateCompanyName,
                enabled = !uiState.isSaving
            )
            FormTextField(
                label = "CUIT",
                value = uiState.companyCuit,
                onValueChange = viewModel::updateCompanyCuit,
                enabled = !uiState.isSaving
            )
            FormTextField(
                label = "Dirección",
                value = uiState.companyAddress,
                onValueChange = viewModel::updateCompanyAddress,
                enabled = !uiState.isSaving
            )
            FormTextField(
                label = "Ciudad",
                value = uiState.companyCity,
                onValueChange = viewModel::updateCompanyCity,
                enabled = !uiState.isSaving
            )
            FormTextField(
                label = "Teléfono",
                value = uiState.companyPhone,
                onValueChange = viewModel::updateCompanyPhone,
                enabled = !uiState.isSaving
            )
            FormTextField(
                label = "Email",
                value = uiState.companyEmail,
                onValueChange = viewModel::updateCompanyEmail,
                enabled = !uiState.isSaving
            )

            SectionTitle("📄 TÉRMINOS Y CONDICIONES")

            Text(
                text = "Texto que aparecerá al pie del presupuesto PDF",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.termsConditions,
                onValueChange = viewModel::updateTermsConditions,
                label = { Text("Términos y condiciones", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                minLines = 4,
                enabled = !uiState.isSaving,
                textStyle = TextStyle(fontSize = 11.sp)
            )

            if (uiState.error != null) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            if (uiState.isSaved) {
                Text(
                    text = "Configuración guardada correctamente",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text("Guardar configuración")
            }
        }
    }
}
