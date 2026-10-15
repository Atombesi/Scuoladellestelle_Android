package com.example.scuoladellestelle_android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun CompilazioneGiornalieroView(
    jsonGrezzoString: String,
    onSalvaEProcedi: (String) -> Unit
) {
    val rootJson = remember { JSONObject(jsonGrezzoString) }
    val vociArray = remember { rootJson.getJSONArray("VociDiUnFoglio") }

    val listaVoci = remember { mutableStateListOf<JSONObject>().apply {
        for (i in 0 until vociArray.length()) { add(vociArray.getJSONObject(i)) }
    }}

    var indiceSelezionato by remember { mutableStateOf(0) }
    val voceAttiva = if (listaVoci.isNotEmpty()) listaVoci[indiceSelezionato] else null

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {

        // ===================================================================
        // LATO SINISTRO (40%): Elenco delle Voci Scorrevole
        // ===================================================================
        Box(modifier = Modifier.fillMaxHeight().weight(0.40f).border(1.dp, Color(0xFFE4E7EC))) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                itemsIndexed(listaVoci) { idx, voce ->
                    val isCompilato = voce.optBoolean("COMPILATO", false)
                    val isCorrente = idx == indiceSelezionato

                    val colSfondo = when {
                        isCorrente -> Color(0xFFE0F2FE)
                        isCompilato -> Color(0xFFECFDF5)
                        else -> Color.White
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(colSfondo, RoundedCornerShape(6.dp))
                            .border(1.dp, if (isCorrente) Color(0xFF007AFF) else Color(0xFFF2F4F7), RoundedCornerShape(6.dp))
                            .clickable { indiceSelezionato = idx }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isCompilato) "✅ " else "📄 ", fontSize = 13.sp)
                        Text(
                            text = voce.optString("Testo", "Voce senza testo").trim(),
                            fontSize = 12.sp,
                            fontWeight = if (isCorrente) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCorrente) Color(0xFF007AFF) else Color(0xFF344054),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ===================================================================
        // LATO DESTRO (60%): Griglia ultra-stretta a sinistra, Tastierino a destra
        // ===================================================================
        if (voceAttiva != null) {
            val isValutativo = voceAttiva.optBoolean("VALUTATIVO", false)

            var t1Pos by remember(indiceSelezionato) { mutableStateOf(voceAttiva.optInt("TOT1_POS", 0).toString()) }
            var t1Neg by remember(indiceSelezionato) { mutableStateOf(voceAttiva.optInt("TOT1_NEG", 0).toString()) }
            var t1Prp by remember(indiceSelezionato) { mutableStateOf(voceAttiva.optInt("TOT1_PROMPT", 0).toString()) }
            var t2Pos by remember(indiceSelezionato) { mutableStateOf(voceAttiva.optInt("TOT2_POS", 0).toString()) }
            var t2Neg by remember(indiceSelezionato) { mutableStateOf(voceAttiva.optInt("TOT2_NEG", 0).toString()) }
            var t2Prp by remember(indiceSelezionato) { mutableStateOf(voceAttiva.optInt("TOT2_PROMPT", 0).toString()) }

            var campoAttivoId by remember(indiceSelezionato) { mutableStateOf(1) }

            val tot1 = (t1Pos.toIntOrNull() ?: 0) + (t1Neg.toIntOrNull() ?: 0) + (t1Prp.toIntOrNull() ?: 0)
            val tot2 = (t2Pos.toIntOrNull() ?: 0) + (t2Neg.toIntOrNull() ?: 0) + (t2Prp.toIntOrNull() ?: 0)

            val t1Errore = !isValutativo && tot1 != 20 && tot1 != 0
            val t2Errore = !isValutativo && tot2 != 20 && tot2 != 0

            Column(modifier = Modifier.fillMaxSize().weight(0.60f).padding(10.dp)) {

                Text(
                    text = "Voce attiva: " + voceAttiva.optString("Testo").trim(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D2939),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    // 📝 1. GRIGLIA DATI COMPATTA (210dp)
                    Card(
                        modifier = Modifier.width(210.dp).wrapContentHeight(), // wrapContent per non allungarsi a vuoto
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE4E7EC))
                    ) {
                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("", modifier = Modifier.width(40.dp))
                                Text("S1", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475467), textAlign = TextAlign.Center)
                                Text("S2", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475467), textAlign = TextAlign.Center)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("POS", modifier = Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344054))
                                BoxCasellaFinta(testo = t1Pos, isAttiva = campoAttivoId == 1, errore = t1Errore, modifier = Modifier.weight(1f).clickable { campoAttivoId = 1 })
                                BoxCasellaFinta(testo = t2Pos, isAttiva = campoAttivoId == 4, errore = t2Errore, modifier = Modifier.weight(1f).clickable { campoAttivoId = 4 })
                            }

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("NEG", modifier = Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344054))
                                BoxCasellaFinta(testo = t1Neg, isAttiva = campoAttivoId == 2, errore = t1Errore, modifier = Modifier.weight(1f).clickable { campoAttivoId = 2 })
                                BoxCasellaFinta(testo = t2Neg, isAttiva = campoAttivoId == 5, errore = t2Errore, modifier = Modifier.weight(1f).clickable { campoAttivoId = 5 })
                            }

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("PRP", modifier = Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344054))
                                BoxCasellaFinta(testo = t1Prp, isAttiva = campoAttivoId == 3, errore = t1Errore, modifier = Modifier.weight(1f).clickable { campoAttivoId = 3 })
                                BoxCasellaFinta(testo = t2Prp, isAttiva = campoAttivoId == 6, errore = t2Errore, modifier = Modifier.weight(1f).clickable { campoAttivoId = 6 })
                            }

                            Divider(modifier = Modifier.padding(vertical = 2.dp), color = Color(0xFFE4E7EC))

                            // 🎯 I TOTALI ORA SONO PERFETTAMENTE PARLANTI SOTTO LE COLONNE
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("TOT", modifier = Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667085))
                                Text(text = tot1.toString(), modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (t1Errore) Color.Red else Color(0xFF12B76A), textAlign = TextAlign.Center)
                                Text(text = tot2.toString(), modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (t2Errore) Color.Red else Color(0xFF12B76A), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // 🔢 2. TASTIERINO NUMERICO SUL FIANCO (240dp)
                    Card(
                        modifier = Modifier.width(240.dp).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F7)),
                        border = BorderStroke(1.dp, Color(0xFFD0D5DD))
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val pulsantiTastierino = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("⌫", "0", "INVIO")
                            )
                            pulsantiTastierino.forEach { rigaTasti ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rigaTasti.forEach { carattereTasto ->
                                        val isInvio = carattereTasto == "INVIO"
                                        val isCancella = carattereTasto == "⌫"
                                        val colSfondoTasto = when {
                                            isInvio -> Color(0xFF007AFF)
                                            isCancella -> Color(0xFFF04438)
                                            else -> Color.White
                                        }
                                        val colTestoTasto = if (isInvio || isCancella) Color.White else Color(0xFF1D2939)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(colSfondoTasto, RoundedCornerShape(6.dp))
                                                .border(1.dp, if (isInvio || isCancella) Color.Transparent else Color(0xFFD0D5DD), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    var valoreCorrente = when (campoAttivoId) {
                                                        1 -> t1Pos 2 -> t1Neg 3 -> t1Prp
                                                        4 -> t2Pos 5 -> t2Neg else -> t2Prp
                                                    }
                                                    if (isCancella) {
                                                        valoreCorrente = if (valoreCorrente.length > 1) valoreCorrente.dropLast(1) else "0"
                                                        when (campoAttivoId) {
                                                            1 -> t1Pos = valoreCorrente 2 -> t1Neg = valoreCorrente 3 -> t1Prp = valoreCorrente
                                                            4 -> t2Pos = valoreCorrente 5 -> t2Neg = valoreCorrente 6 -> t2Prp = valoreCorrente
                                                        }
                                                    } else if (isInvio) {
                                                        // 🎯 GESTIONE INVIO MANUALE (Rimasto intatto)
                                                        if (campoAttivoId < 6) {
                                                            campoAttivoId++
                                                        } else {
                                                            if (!t1Errore && !t2Errore) {
                                                                voceAttiva.put("TOT1_POS", t1Pos.toIntOrNull() ?: 0)
                                                                voceAttiva.put("TOT1_NEG", t1Neg.toIntOrNull() ?: 0)
                                                                voceAttiva.put("TOT1_PROMPT", t1Prp.toIntOrNull() ?: 0)
                                                                voceAttiva.put("TOT1", tot1)
                                                                voceAttiva.put("TOT2_POS", t2Pos.toIntOrNull() ?: 0)
                                                                voceAttiva.put("TOT2_NEG", t2Neg.toIntOrNull() ?: 0)
                                                                voceAttiva.put("TOT2_PROMPT", t2Prp.toIntOrNull() ?: 0)
                                                                voceAttiva.put("TOT2", tot2)
                                                                voceAttiva.put("COMPILATO", true)
                                                                if (indiceSelezionato < listaVoci.size - 1) {
                                                                    indiceSelezionato++
                                                                    campoAttivoId = 1
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        // 🎯 NUOVA LOGICA: DIGITAZIONE NUMERICA CON AVANZAMENTO AUTOMATICO A 2 CIFRE
                                                        if (valoreCorrente == "0") valoreCorrente = ""
                                                        if (valoreCorrente.length < 2) {
                                                            valoreCorrente += carattereTasto

                                                            // Assegniamo la cifra digitata alla casella corretta
                                                            when (campoAttivoId) {
                                                                1 -> t1Pos = valoreCorrente 2 -> t1Neg = valoreCorrente 3 -> t1Prp = valoreCorrente
                                                                4 -> t2Pos = valoreCorrente 5 -> t2Neg = valoreCorrente 6 -> t2Prp = valoreCorrente
                                                            }

                                                            // Calcolo dei totali temporanei immediati per la validazione al volo
                                                            val v1 = (if(campoAttivoId==1) valoreCorrente else t1Pos).toIntOrNull() ?: 0
                                                            val v2 = (if(campoAttivoId==2) valoreCorrente else t1Neg).toIntOrNull() ?: 0
                                                            val v3 = (if(campoAttivoId==3) valoreCorrente else t1Prp).toIntOrNull() ?: 0
                                                            val v4 = (if(campoAttivoId==4) valoreCorrente else t2Pos).toIntOrNull() ?: 0
                                                            val v5 = (if(campoAttivoId==5) valoreCorrente else t2Neg).toIntOrNull() ?: 0
                                                            val v6 = (if(campoAttivoId==6) valoreCorrente else t2Prp).toIntOrNull() ?: 0

                                                            val checkTot1 = v1 + v2 + v3
                                                            val checkTot2 = v4 + v5 + v6
                                                            val err1 = !isValutativo && checkTot1 != 20 && checkTot1 != 0
                                                            val err2 = !isValutativo && checkTot2 != 20 && checkTot2 != 0

                                                            // Se la casella ha raggiunto le 2 cifre, esegue il salto automatico hardware
                                                            if (valoreCorrente.length == 2) {
                                                                if (campoAttivoId < 6) {
                                                                    campoAttivoId++
                                                                } else {
                                                                    // Ultimo campo (6): se le quadrature passano (20 o 0), archivia e cambia riga bambino
                                                                    if (!err1 && !err2) {
                                                                        voceAttiva.put("TOT1_POS", v1)
                                                                        voceAttiva.put("TOT1_NEG", v2)
                                                                        voceAttiva.put("TOT1_PROMPT", v3)
                                                                        voceAttiva.put("TOT1", checkTot1)
                                                                        voceAttiva.put("TOT2_POS", v4)
                                                                        voceAttiva.put("TOT2_NEG", v5)
                                                                        voceAttiva.put("TOT2_PROMPT", v6)
                                                                        voceAttiva.put("TOT2", checkTot2)
                                                                        voceAttiva.put("COMPILATO", true)

                                                                        if (indiceSelezionato < listaVoci.size - 1) {
                                                                            indiceSelezionato++
                                                                            campoAttivoId = 1
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }


                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = carattereTasto, fontSize = if (isInvio) 12.sp else 16.sp, fontWeight = FontWeight.Bold, color = colTestoTasto)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { onSalvaEProcedi(rootJson.toString()) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344054)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Salva e Procedi alle Routine ➔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
@Composable
fun BoxCasellaFinta(testo: String, isAttiva: Boolean, errore: Boolean, modifier: Modifier = Modifier) {
    val bordoColore = when {
        isAttiva -> Color(0xFF007AFF)
        errore -> Color.Red
        else -> Color(0xFFD0D5DD)
    }
    val colSfondo = if (isAttiva) Color(0xFFF0F7FF) else Color(0xFFF9FAFB)
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .height(34.dp) // 🎯 CASELLA COMPATTATA IN ALTEZZA (Da 38 a 34)
            .background(colSfondo, RoundedCornerShape(4.dp))
            .border(if (isAttiva) 2.dp else 1.dp, bordoColore, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = testo.ifBlank { "0" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (errore && !isAttiva) Color.Red else Color(0xFF1D2939)
        )
    }
}