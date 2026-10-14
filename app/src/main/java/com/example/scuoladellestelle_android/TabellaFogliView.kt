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
        // Intestazione colonne della tabella
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text("Bambino", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
            Text("Data", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
            Text("ID Foglio", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, color = Color(0xFF344054))
        }
        HorizontalDivider(color = Color(0xFFE4E7EC))

        // Corpo della tabella cliccabile
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(fogliList) { item ->
                val isSelected = item.ID_Questionario == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFFE0F0FF) else Color.White)
                        .clickable {
                            // 🌐 ARCHITETTURA DI FLUSSO: Comunichiamo alla MainActivity che il foglio è stato selezionato.
                            // Sarà la MainActivity a spingere il comando JSON dentro il canale WebSocket
                            onFoglioSelezionato(item.ID_Questionario)
                        }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.Bambino, modifier = Modifier.weight(1f), color = Color(0xFF475467))
                    Text(item.Data_foglio, modifier = Modifier.width(120.dp), color = Color(0xFF475467))
                    Text(item.ID_foglio.toString(), modifier = Modifier.width(100.dp), color = Color(0xFF475467))
                }
                HorizontalDivider(color = Color(0xFFF2F4F7))
            }
        }
    }
}
