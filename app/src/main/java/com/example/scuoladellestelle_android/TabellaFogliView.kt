package com.example.scuoladellestelle_android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabellaFogliView(
    fogliList: List<StructElencofogliJson>,
    selectedId: Int?,
    onFoglioSelezionato: (Int) -> Unit,
    onDownloadCompletato: (String) -> Unit, // Mantenuto per compatibilità con l'interfaccia
    onErrore: () -> Unit                    // Mantenuto per compatibilità con l'interfaccia
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFFE4E7EC), RoundedCornerShape(8.dp))
    ) {
        // 📊 INTESTAZIONE COLONNE (Aggiunta colonna Note)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text("Bambino", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
            Text("Data", modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
            Text("ID Foglio", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
            Text("Note", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
        }
        HorizontalDivider(color = Color(0xFFE4E7EC))

        // Corpo della tabella
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(fogliList) { item ->
                // 🎯 INTERCETTAZIONE LOGICA NATIVA: Leggiamo i campi combinati immuni al Case-Sensitive
                val valoreDisponibile = item.disponibileReale
                val testoNota = item.notaReale

                val isDisponibile = valoreDisponibile != 0
                val isSelected = item.ID_Questionario == selectedId

                // 🎨 CALCOLO DINAMICO DEI COLORI: Rosso pastello se bloccato, Azzurro se selezionato, Bianco di base
                val colSfondo = when {
                    !isDisponibile -> Color(0xFFFEE4E2) // Rosso pastello opaco di avviso
                    isSelected -> Color(0xFFE0F0FF)     // Azzurro di selezione
                    else -> Color.White
                }

                val colTesto = when {
                    !isDisponibile -> Color(0xFFB42318) // Rosso scuro per massima leggibilità
                    isSelected -> Color(0xFF004EEB)     // Blu scuro di selezione
                    else -> Color(0xFF475467)           // Grigio scuro standard
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colSfondo)
                        // 🎯 BLOCCO HARDWARE DEL CLICK: Se il foglio è occupato, il click viene disattivato alla radice
                        .clickable(enabled = isDisponibile) {
                            onFoglioSelezionato(item.ID_Questionario)
                        }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.Bambino,
                        modifier = Modifier.weight(1.5f),
                        color = colTesto,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = item.Data_foglio,
                        modifier = Modifier.width(110.dp),
                        color = colTesto
                    )
                    Text(
                        text = item.ID_foglio.toString(),
                        modifier = Modifier.width(90.dp),
                        color = colTesto
                    )
                    // 🎯 VISUALIZZAZIONE NOTE SICURA: Stampa il testo esatto inviato da VB.NET
                    Text(
                        text = testoNota,
                        modifier = Modifier.weight(1f),
                        color = colTesto,
                        fontSize = 13.sp,
                        fontWeight = if (!isDisponibile) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                HorizontalDivider(color = Color(0xFFF2F4F7))
            }
        }
    }
}
