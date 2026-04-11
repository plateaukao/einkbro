package info.plateaukao.einkbro.browser

interface DomainInterface {
    suspend fun getDomains(): List<String>
    suspend fun addDomain(domain: String)
    suspend fun deleteDomain(domain: String)
    suspend fun deleteAllDomains()
}