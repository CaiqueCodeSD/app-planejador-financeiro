package com.example.planejadorfinanceiro.data.security

data class MaterialPinProtegido(
    val iv: ByteArray,
    val conteudo: ByteArray
)

interface PinMaterialProtector {

    fun proteger(dados: ByteArray): MaterialPinProtegido

    fun desproteger(material: MaterialPinProtegido): ByteArray
}