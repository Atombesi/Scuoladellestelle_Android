package com.example.scuoladellestelle_android

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZonaCentraleView(
    modifier: Modifier = Modifier,
    statusMessage: String,
    isLoading: Boolean,
    fogliList: List<StructElencofogliJson>,
    selectedId: Int?,
    onFoglioSelezionato: (Int) -> Unit,
    onDownloadCompletato: (String) -> Unit,
    onErrore: () -> Unit
) {
    Box(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    text = "Seleziona il Foglio da Compilare",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D2939),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF667085),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp),
                        color = Color(0xFF007AFF)
                    )
                }

                if (fogliList.isNotEmpty()) {
                    TabellaFogliView(
                        fogliList = fogliList,
                        selectedId = selectedId,
                        onFoglioSelezionato = onFoglioSelezionato,
                        onDownloadCompletato = onDownloadCompletato,
                        onErrore = onErrore
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nessun foglio in memoria.\nPremi 'Verifica Connessione' in alto.",
                            textAlign = TextAlign.Center,
                            color = Color(0xFF98A2B3),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
