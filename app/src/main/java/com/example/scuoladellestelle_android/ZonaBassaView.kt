package com.example.scuoladellestelle_android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZonaBassaView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E7EC))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Architettura Nativa Industriale a moduli Kotlin separati attiva.",
            color = Color(0xFF98A2B3),
            fontSize = 14.sp
        )
    }
}
