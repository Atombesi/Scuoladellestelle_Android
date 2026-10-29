package com.example.scuoladellestelle_android

data class Struct_foglio_Json(
    val ID_Questionario: Int = 0,
    val Data_creazione: String? = "",
    val Bambino: String = "",
    val ID_foglio: Int = 0,
    val ID_Bambino: Int = 0,
    val Data_foglio: String? = "",
    var Stato: String? = "",
    val VociDiUnFoglio: List<Class_GIORNALIERO> = listOf(),
    val RT: List<Class_Giornaliero_ROUTINE> = listOf(),
    val CP: List<Class_Giornaliero_CP> = listOf()
)

data class Class_GIORNALIERO(
    val ID: Int = 0,
    val ID_FOGLIO: Int = 0,
    val DATA: String? = "",
    val VALUTATIVO: Boolean = false,
    val TUTTELERISPOSTE: Boolean = false,
    val Codice: String = "",
    val Testo: String? = "",
    var TOT1_POS: Int = 0,
    var TOT1_NEG: Int = 0,
    var TOT1_PROMPT: Int = 0,
    var TOT1: Int = 0,
    var TOT2_POS: Int = 0,
    var TOT2_NEG: Int = 0,
    var TOT2_PROMPT: Int = 0,
    var TOT2: Int = 0,
    var COMPILATO: Boolean = false,
    var TERMINE: Boolean = false
)

data class Class_Giornaliero_ROUTINE(
    val ID: Int = 0,
    val ID_VOCE: Int = 0,
    val ID_FOGLIO: Int = 0,
    var NOTA: String? = "",
    val TESTO: String? = "",
    var TOT: Int = 0,
    val DATA: String = "",
    var COMPILATO: Int = 0,
    val N_voce: Int = 0
)

data class Class_Giornaliero_CP(
    val ID: Int = 0,
    val ID_VOCE: Int = 0,
    val ID_FOGLIO: Int = 0,
    var NOTA: String? = "",
    val TESTO: String? = "",
    var TOT: Int = 0,
    val DATA: String = "",
    var COMPILATO: Int = 0
)
