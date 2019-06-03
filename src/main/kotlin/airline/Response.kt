package pl.sag.airline

sealed class Response<T>

data class SuccessResponse<T>(val value: T) : Response<T>()

data class ErrorResponse<T>(val error: String) : Response<T>()

fun <T> T.asSuccessResponse() = SuccessResponse(this)