package com.example.scuoladellestelle_android

/**
 * Struttura dati speculare alla List(Of Struct_Elencofogli_Json) del server VB.NET.
 * I nomi delle variabili interne sono differenziati per evitare il blocco di firma JVM,
 * ma coprono sia il cassetto maiuscolo che minuscolo del motore di parsing JSON.
 */
data class StructElencofogliJson(
    val ID_Questionario: Int,
    val Data_foglio: String,
    val Bambino: String,
    val ID_foglio: Int,
    val ID_Bambino: Int,
    val Filename: String,

    // 🎯 VARIABILI RINOMINATE PER LA JVM: Ora non collidono più nei metodi get()
    val note: String? = "",
    val disponibile: Int? = 1
) {
    // 🎯 ESTRATTORE DI SICUREZZA PER LE NOTE: Controlla in ordine quale cassetto ha riempito il server VB.NET
    val notaReale: String
        get() {
            return when {
                !note.isNullOrBlank() -> note.trim()
                else -> ""
            }
        }

    // 🎯 ESTRATTORE DI SICUREZZA PER LA DISPONIBILITÀ: Ritorna il valore corretto combinando i due campi
    val disponibileReale: Int
        get() {
            return disponibile ?: (disponibile ?: 1)
        }
}
