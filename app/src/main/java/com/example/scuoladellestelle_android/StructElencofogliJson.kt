package com.example.scuoladellestelle_android

/**
 * Struttura dati speculare alla List(Of Struct_Elencofogli_Json) del server VB.NET.
 * Kotlin utilizzerà questa definizione per mappare automaticamente il JSON ricevuto.
 */
data class StructElencofogliJson(
    val ID_Questionario: Int,
    val Data_foglio: String,
    val Bambino: String,
    val ID_foglio: Int,
    val ID_Bambino: Int,
    val Filename: String
)
