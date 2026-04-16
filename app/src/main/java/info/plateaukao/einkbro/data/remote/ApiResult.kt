package info.plateaukao.einkbro.data.remote

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(
        val kind: Kind,
        val message: String,
        val retryAfterSeconds: Long? = null,
        val cause: Throwable? = null,
    ) : ApiResult<Nothing>()

    enum class Kind { MissingKey, RateLimited, Network, Parse, ServerError, Unknown }

    fun valueOrNull(): T? = (this as? Success<T>)?.value
}
