package com.pedrohrsm.diariodebordo.localizacao

/** Uma leitura de posição. Instante em milissegundos UTC; precisão em metros quando o GPS informa. */
data class Ponto(val latitude: Double, val longitude: Double, val instante: Long, val precisaoM: Float? = null)

class SemPermissaoDeLocalizacaoException : IllegalStateException("Permissão de localização não concedida")

class LocalIndisponivelException : IllegalStateException("O GPS não respondeu a tempo")
