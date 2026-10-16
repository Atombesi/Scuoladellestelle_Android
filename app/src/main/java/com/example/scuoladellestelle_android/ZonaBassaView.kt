package com.example.scuoladellestelle_android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZonaBassaView(
    totaliG: Int, compilatiG: Int,
    totaliRT: Int, compilatiRT: Int,
    totaliCP: Int, compilatiCP: Int,
    onSaltoCategoria: (String) -> Unit,
    onInviaClick: () -> Unit
) {
    val tuttoCompleto = (totaliG > 0 && compilatiG == totaliG) &&
            (totaliRT > 0 && compilatiRT == totaliRT) &&
            (totaliCP > 0 && compilatiCP == totaliCP)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E7EC))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 📊 LATO SINISTRO: Le tre pillole cliccabili dei contatori
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillolaStato(
                etichetta = "Voci", current = compilatiG, total = totaliG,
                onClick = { onSaltoCategoria("GIORNALIERO") }
            )
            PillolaStato(
                etichetta = "RT", current = compilatiRT, total = totaliRT,
                onClick = { onSaltoCategoria("ROUTINE") }
            )
            PillolaStato(
                etichetta = "CP", current = compilatiCP, total = totaliCP,
                onClick = { onSaltoCategoria("COMPORTAMENTALE") }
            )
        }

        // ➔ LATO DESTRA: Pulsante di invio intelligente
        Button(
            onClick = { onInviaClick() },
            modifier = Modifier.height(38.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (tuttoCompleto) Color(0xFF12B76A) else Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (tuttoCompleto) "Invia Dati al PC ➔" else "Salva Sessione",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun PillolaStato(etichetta: String, current: Int, total: Int, onClick: () -> Unit) {
    val isCompleto = total > 0 && current == total
    val colSfondo = if (isCompleto) Color(0xFFECFDF5) else Color(0xFFF2F4F7)
    val colBordo = if (isCompleto) Color(0xFF12B76A) else Color(0xFFD0D5DD)
    val colTesto = if (isCompleto) Color(0xFF12B76A) else Color(0xFF344054)

    Row(
        modifier = Modifier
            .background(colSfondo, RoundedCornerShape(20.dp))
            .border(1.dp, colBordo, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isCompleto) "🟢" else "🔵",
            fontSize = 11.sp
        )
        Text(
            text = "$etichetta: $current/$total",
            color = colTesto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
