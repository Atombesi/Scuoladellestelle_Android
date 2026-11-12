package com.example.scuoladellestelle_android

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scuoladellestelle_android.ui.theme.CompilazioneGiornalieroView

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
    onModificaFoglio: (String) -> Unit,
    // 🎯 NUOVI PARAMETRI AGGIUNTI PER PASSARE LE FUNZIONI DI MAIN_ACTIVITY
    onStartLoading: (String) -> Unit,
    onStatusChange: (Boolean, String, List<StructElencofogliJson>) -> Unit
) {
    // STATO LOCALE: Registra il testo quando il server risponde
    var jsonFoglioContenuto by remember { mutableStateOf<String?>(null) }
    var primoElencoRicevuto by remember { mutableStateOf(false) }

    // RECUPERO ID HARDWARE REALE
    val contestoAndroid = LocalContext.current
    val dispositivoHardwareId = remember(contestoAndroid) {
        Settings.Secure.getString(contestoAndroid.contentResolver, Settings.Secure.ANDROID_ID) ?: "ATTREZZO_SCONOSCIUTO"
    }

    // Carichiamo le preferenze locali per poter usare la Nota Utente nel pulsante Connetti spostato qui
    val prefs = remember { contestoAndroid.getSharedPreferences("config_rete", android.content.Context.MODE_PRIVATE) }
    val notaUtenteSalvata = prefs.getString("nota_utente", "Tablet Aula 1") ?: "Tablet Aula 1"

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
    // 🎯 INCOLLALO ESATTAMENTE QUI, SUBITO SOTTO IL CHIUDI GRAFFA DEL PRECEDENTE LAUNCHED_EFFECT
    LaunchedEffect(selectedId) {
        if (selectedId == null) {
            jsonFoglioContenuto = null
        }
    }
    Box(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (jsonFoglioContenuto != null) {
            CompilazioneGiornalieroView(
                jsonGrezzoString = jsonFoglioContenuto!!,
                indiceSelezionatoEsterno = selectedId ?: 0,
                onSalvaEProcedi = { jsonAggiornato: String ->
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

                    // 🎯 RISTRUTTURAZIONE IN ROW PER INSERIRE IL PULSANTE SULLA DESTRA
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lato Sinistro: Testi informativi dinamici
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (fogliList.isEmpty()) "Connetti al Server" else "Seleziona il Foglio da Compilare",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D2939),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = statusMessage,
                                fontSize = 14.sp,
                                color = Color(0xFF667085)
                            )
                        }

                        // Lato Destro: Il pulsante Connetti integrato permanentemente qui
                        Button(
                            onClick = {
                                // 1. Attiva lo stato di caricamento sulla MainActivity
                                onStartLoading("Connessione ed estrazione elenco in corso...")

                                // 2. Lancia la connessione reale sul tunnel radio
                                ApiService.connettiAlServer(
                                    context = contestoAndroid,
                                    notaUtente = notaUtenteSalvata,
                                    onStatoConnessione = { connesso, messaggio ->
                                        // Notifica la MainActivity del cambio di stato (passa lista vuota all'inizio)
                                        onStatusChange(connesso, messaggio, emptyList())
                                    },
                                    onElencoFogli = { listaRicevuta ->
                                        primoElencoRicevuto = true
                                        // Spinge l'elenco dei fogli finale alla MainActivity che aggiorna la griglia
                                        onStatusChange(true, "Elenco caricato con successo.", listaRicevuta)
                                    },
                                    onFoglioDati = { _ -> }
                                )
                            },
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            shape = RoundedCornerShape(6.dp),
                            enabled = !isLoading
                        ) {
                            Text(if (primoElencoRicevuto) "Reload" else "Connetti", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }



                    }

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
                            if (statusMessage == "Nessun foglio disponibile da compilare") {
                                // 🎯 MESSAGGIO AL CENTRO, GRANDE E BEN VISIBILE
                                Text(
                                    text = statusMessage,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFF04438), // Rosso avviso coerente con i tuoi popup
                                    fontSize = 22.sp,          // Dimensione maggiorata
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                // Testo di default iniziale all'apertura dell'app
                                Text(
                                    text = "Nessun foglio in memoria.\nPremi 'Connetti' a destra per iniziare.",
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
}
