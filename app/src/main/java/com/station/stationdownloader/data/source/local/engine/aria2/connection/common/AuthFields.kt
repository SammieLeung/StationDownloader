package com.station.stationdownloader.data.source.local.engine.aria2.connection.common

sealed class AuthFields(val authMethod: AuthMethod){
    object NoneAuthFields: AuthFields(AuthMethod.NONE)
    data class HttpAuthFields(val username:String,val password:String): AuthFields(AuthMethod.HTTP)
    data class TokenAuthFields(val token:String): AuthFields(AuthMethod.TOKEN)
    enum class AuthMethod {
        NONE, HTTP, TOKEN
    }
}

