package com.example.scuoladellestelle_android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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

data class ElementoUnificato(
    val tipo: String, // "G" = Giornaliero, "CP" = Comportamentale, "RT" = Routine
    val jsonOriginale: JSONObject,
    val indiceOriginale: Int
)

@Composable
fun CompilazioneGiornalieroView(
    jsonGrezzoString: String,
    onSalvaEProcedi: (String) -> Unit
) {
    val rootJson = remember { JSONObject(jsonGrezzoString) }

    val vociArray = remember { rootJson.optJSONArray("VociDiUnFoglio") ?: org.json.JSONArray() }
    val cpArray = remember { rootJson.optJSONArray("CP") ?: org.json.JSONArray() }
    val rtArray = remember { rootJson.optJSONArray("RT") ?: org.json.JSONArray() }

    // Costruiamo la lista unificata globale
    val listaUnificata = remember { mutableStateListOf<ElementoUnificato>().apply {
        for (i in 0 until vociArray.length()) { add(ElementoUnificato("G", vociArray.getJSONObject(i), i)) }
        for (i in 0 until cpArray.length()) { add(ElementoUnificato("CP", cpArray.getJSONObject(i), i)) }
        for (i in 0 until rtArray.length()) { add(ElementoUnificato("RT", rtArray.getJSONObject(i), i)) }
    }}

    var indiceUnificatoSelezionato by remember { mutableStateOf(0) }
    val elementoAttivo = if (listaUnificata.isNotEmpty()) listaUnificata[indiceUnificatoSelezionato] else null

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {

        // ===================================================================
        // LATO SINISTRO (40%): Elenco Scorrevole Sbloccato con Cerchietti Vuoti
        // ===================================================================
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.40f)
                .border(1.dp, Color(0xFFE4E7EC))
                .padding(4.dp)
        ) {
            itemsIndexed(listaUnificata) { idx, elem ->
                val voceObj = elem.jsonOriginale
                val isCompilato = if (elem.tipo == "G") voceObj.optBoolean("COMPILATO", false) else voceObj.optInt("COMPILATO", 0) == 1
                val isCorrente = idx == indiceUnificatoSelezionato

                val colSfondo = if (isCorrente) Color(0xFFE0F2FE) else Color.White

                val coloreTema = when (elem.tipo) {
                    "G" -> Color(0xFF12B76A)   // Verde
                    "CP" -> Color(0xFFF04438)  // Arancione/Rosso
                    else -> Color(0xFF007AFF)  // Blu
                }

                val testoVoce = if (elem.tipo == "G") voceObj.optString("Testo") else voceObj.optString("TESTO")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(colSfondo, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isCorrente) Color(0xFF007AFF) else Color(0xFFF2F4F7), RoundedCornerShape(6.dp))
                        .clickable { indiceUnificatoSelezionato = idx }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 🎯 DISEGNO DEL PALLINO PERSONALIZZATO: Cerchio vuoto se da fare, pieno con spunta se fatto
                    if (isCompilato) {
                        Box(
                            modifier = Modifier.size(16.dp).background(coloreTema, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(16.dp).border(2.dp, coloreTema, CircleShape)
                        )
                    }

                    Text(
                        text = testoVoce.trim(),
                        fontSize = 12.sp,
                        fontWeight = if (isCorrente) FontWeight.Bold else Modifier.let { FontWeight.Medium },
                        color = if (isCorrente) Color(0xFF007AFF) else Color(0xFF344054),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ===================================================================
        // LATO DESTRO (60%): Pannello Immissione Dati Dinamico Rettificato
        // ===================================================================
        if (elementoAttivo != null) {
            val voceObj = elementoAttivo.jsonOriginale

            var t1Pos by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT1_POS", 0).toString()) }
            var t1Neg by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT1_NEG", 0).toString()) }
            var t1Prp by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT1_PROMPT", 0).toString()) }
            var t2Pos by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT2_POS", 0).toString()) }
            var t2Neg by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT2_NEG", 0).toString()) }
            var t2Prp by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT2_PROMPT", 0).toString()) }
            var campoAttivoId by remember(indiceUnificatoSelezionato) { mutableStateOf(1) }

            val tot1 = (t1Pos.toIntOrNull() ?: 0) + (t1Neg.toIntOrNull() ?: 0) + (t1Prp.toIntOrNull() ?: 0)
            val tot2 = (t2Pos.toIntOrNull() ?: 0) + (t2Neg.toIntOrNull() ?: 0) + (t2Prp.toIntOrNull() ?: 0)
            val isValutativo = voceObj.optBoolean("VALUTATIVO", false)
            val t1Errore = elementoAttivo.tipo == "G" && !isValutativo && tot1 != 20 && tot1 != 0
            val t2Errore = elementoAttivo.tipo == "G" && !isValutativo && tot2 != 20 && tot2 != 0

            var totUnicoSingolo by remember(indiceUnificatoSelezionato) { mutableStateOf(voceObj.optInt("TOT", 0).toString()) }

            val testoDellaVoceAttiva = if (elementoAttivo.tipo == "G") voceObj.optString("Testo") else voceObj.optString("TESTO")

            Column(modifier = Modifier.fillMaxSize().weight(0.60f).padding(10.dp)) {

                Text(
                    text = "Voce attiva: " + testoDellaVoceAttiva.trim(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D2939),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 📝 COLONNA PANNELLO INPUT (210dp)
                    Box(modifier = Modifier.width(210.dp).fillMaxHeight()) {
                        if (elementoAttivo.tipo == "G") {
                            Card(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
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
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("TOT", modifier = Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667085))
                                        Text(text = tot1.toString(), modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (t1Errore) Color.Red else Color(0xFF12B76A), textAlign = TextAlign.Center)
                                        Text(text = tot2.toString(), modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (t2Errore) Color.Red else Color(0xFF12B76A), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE4E7EC))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = if (elementoAttivo.tipo == "CP") "IMMISSIONE COMPORTAMENTALE" else "IMMISSIONE ROUTINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("TOTALE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344054))
                                    BoxCasellaFinta(testo = totUnicoSingolo, isAttiva = true, errore = false, modifier = Modifier.width(100.dp))
                                }
                            }
                        }
                    }
// 🔢 2. TASTIERINO NUMERICO FISSO (240dp)
                    Card(
                        modifier = Modifier.width(240.dp).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F7)),
                        border = BorderStroke(1.dp, Color(0xFFD0D5DD))
                    ) {
                        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val pulsantiTastierino = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("⌫", "0", "INVIO")
                            )
                            pulsantiTastierino.forEach { rigaTasti ->
                                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                                    var stringaLavorazione = if (elementoAttivo.tipo == "G") {
                                                        when (campoAttivoId) {
                                                            1 -> t1Pos 2 -> t1Neg 3 -> t1Prp
                                                            4 -> t2Pos 5 -> t2Neg else -> t2Prp
                                                        }
                                                    } else {
                                                        totUnicoSingolo
                                                    }
                                                    if (isCancella) {
                                                        stringaLavorazione = if (stringaLavorazione.length > 1) stringaLavorazione.dropLast(1) else "0"
                                                        if (elementoAttivo.tipo == "G") {
                                                            when (campoAttivoId) {
                                                                1 -> t1Pos = stringaLavorazione 2 -> t1Neg = stringaLavorazione 3 -> t1Prp = stringaLavorazione
                                                                4 -> t2Pos = stringaLavorazione 5 -> t2Neg = stringaLavorazione 6 -> t2Prp = stringaLavorazione
                                                            }
                                                        } else {
                                                            totUnicoSingolo = stringaLavorazione
                                                        }
                                                    } else if (isInvio) {
                                                        if (elementoAttivo.tipo == "G") {
                                                            if (campoAttivoId < 6) campoAttivoId++ else {
                                                                if (!t1Errore && !t2Errore) {
                                                                    voceObj.put("TOT1_POS", t1Pos.toIntOrNull() ?: 0); voceObj.put("TOT1_NEG", t1Neg.toIntOrNull() ?: 0); voceObj.put("TOT1_PROMPT", t1Prp.toIntOrNull() ?: 0); voceObj.put("TOT1", tot1)
                                                                    voceObj.put("TOT2_POS", t2Pos.toIntOrNull() ?: 0); voceObj.put("TOT2_NEG", t2Neg.toIntOrNull() ?: 0); voceObj.put("TOT2_PROMPT", t2Prp.toIntOrNull() ?: 0); voceObj.put("TOT2", tot2)
                                                                    voceObj.put("COMPILATO", true)
                                                                    if (indiceUnificatoSelezionato < listaUnificata.size - 1) { indiceUnificatoSelezionato++; campoAttivoId = 1 }
                                                                }
                                                            }
                                                        } else {
                                                            voceObj.put("TOT", totUnicoSingolo.toIntOrNull() ?: 0)
                                                            voceObj.put("COMPILATO", 1)
                                                            if (indiceUnificatoSelezionato < listaUnificata.size - 1) { indiceUnificatoSelezionato++ }
                                                        }
                                                    } else {
// 🎯 INSERIMENTO CIFRE NUMERICHE 0-9 CON CONTROLLO STRINGA RIGIDO
                                                        val testoPulito = if (stringaLavorazione == "0") "" else stringaLavorazione
                                                        if (testoPulito.length < 2) {
                                                            val stringaRisultante = testoPulito + carattereTasto
                                                            if (elementoAttivo.tipo == "G") {
// Scriviamo fisicamente il numero modificato
                                                                when (campoAttivoId) {
                                                                    1 -> t1Pos = stringaRisultante 2 -> t1Neg = stringaRisultante 3 -> t1Prp = stringaRisultante
                                                                    4 -> t2Pos = stringaRisultante 5 -> t2Neg = stringaRisultante 6 -> t2Prp = stringaRisultante
                                                                }
// 🎯 LA RETTIFICA MATEMATICA: Avanza solo ed esclusivamente se la stringa ha raggiunto 2 caratteri!
                                                                if (stringaRisultante.length == 2) {
                                                                    val v1 = t1Pos.toIntOrNull() ?: 0
                                                                    val v2 = t1Neg.toIntOrNull() ?: 0
                                                                    val v3 = t1Prp.toIntOrNull() ?: 0
                                                                    val v4 = t2Pos.toIntOrNull() ?: 0
                                                                    val v5 = t2Neg.toIntOrNull() ?: 0
                                                                    val v6 = t2Prp.toIntOrNull() ?: 0
                                                                    val ct1 = v1 + v2 + v3
                                                                    val ct2 = v4 + v5 + v6
                                                                    val e1 = !isValutativo && ct1 != 20 && ct1 != 0
                                                                    val e2 = !isValutativo && ct2 != 20 && ct2 != 0
                                                                    if (campoAttivoId < 6) {
                                                                        campoAttivoId++
                                                                    } else {
// Fine riga Giornaliero
                                                                        if (!e1 && !e2) {
                                                                            voceObj.put("TOT1_POS", v1); voceObj.put("TOT1_NEG", v2); voceObj.put("TOT1_PROMPT", v3); voceObj.put("TOT1", ct1)
                                                                            voceObj.put("TOT2_POS", v4); voceObj.put("TOT2_NEG", v5); voceObj.put("TOT2_PROMPT", v6); voceObj.put("TOT2", ct2)
                                                                            voceObj.put("COMPILATO", true)
                                                                            if (indiceUnificatoSelezionato < listaUnificata.size - 1) { indiceUnificatoSelezionato++; campoAttivoId = 1 }
                                                                        }
                                                                    }
                                                                }
                                                            } else {
// Per CP e Routine (Campo singolo)
                                                                totUnicoSingolo = stringaRisultante
                                                                if (stringaRisultante.length == 2) {
                                                                    voceObj.put("TOT", stringaRisultante.toIntOrNull() ?: 0)
                                                                    voceObj.put("COMPILATO", 1)
                                                                    if (indiceUnificatoSelezionato < listaUnificata.size - 1) { indiceUnificatoSelezionato++ }
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
                    Text("Salva ed Invia File Completo al PC ➔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            .height(34.dp)
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

