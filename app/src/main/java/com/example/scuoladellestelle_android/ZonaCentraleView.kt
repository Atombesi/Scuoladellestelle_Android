package com.example.scuoladellestelle_android

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onErrore: () -> Unit,
    onModificaFoglio: (String) -> Unit
) {
    // STATO LOCALE: Registra il testo quando il server risponde
    var jsonFoglioContenuto by remember { mutableStateOf<String?>(null) }

    // RECUPERO ID HARDWARE REALE
    val contestoAndroid = LocalContext.current
    val dispositivoHardwareId = remember(contestoAndroid) {
        Settings.Secure.getString(contestoAndroid.contentResolver, Settings.Secure.ANDROID_ID) ?: "ATTREZZO_SCONOSCIUTO"
    }

    // Aggancio del listener di rete allineato all'interno dello scope dei parametri
    LaunchedEffect(Unit) {
        ApiService.onContenutoFoglioRicevuto = { jsonRicevuto ->
            if (jsonRicevuto != null && jsonRicevuto != "ERRORE" && jsonRicevuto.isNotBlank()) {

                // INTERCETTAZIONE CONFERMA: Se contiene la conferma di salvataggio da Windows
                if (jsonRicevuto.contains("OK_SALVATO")) {
                    jsonFoglioContenuto = null // Azzera lo stato locale chiudendo la vista di compilazione
                    onDownloadCompletato("OK_SALVATO") // Spinge il segnale di reset verso la MainActivity
                } else {
                    jsonFoglioContenuto = jsonRicevuto
                    onDownloadCompletato(jsonRicevuto)
                }

                try {
                    if (!jsonRicevuto.contains("OK_SALVATO")) {
                        val root = org.json.JSONObject(jsonRicevuto)
                        var idQuestionarioEstratto = root.optInt("ID_Questionario", 0)
                        if (idQuestionarioEstratto == 0) {
                            idQuestionarioEstratto = root.optInt("ID_questionario", 0)
                        }

                        if (idQuestionarioEstratto > 0) {
                            val pacchettoConferma = org.json.JSONObject().apply {
                                put("type", "CONFERMA_RICEZIONE_FOGLIO")
                                put("id_questionario", idQuestionarioEstratto)
                                put("device_id", dispositivoHardwareId)
                            }
                            ApiService.inviaComandoAlPC(pacchettoConferma.toString())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                onErrore()
            }
        }
    }

    Box(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (jsonFoglioContenuto != null) {
            CompilazioneGiornalieroView(
                jsonGrezzoString = jsonFoglioContenuto!!,
                // 🎯 FIX DEFINITIVO: Ripristinato il nome del parametro corretto atteso dal tuo modulo
                indiceSelezionatoEsterno = selectedId ?: 0,
                onSalvaEProcedi = { jsonAggiornato ->
                    jsonFoglioContenuto = jsonAggiornato
                    onModificaFoglio(jsonAggiornato)
                }
            )
        } else {
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
                            onDownloadCompletato = { _ -> },
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
}
