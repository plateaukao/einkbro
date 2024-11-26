package info.plateaukao.einkbro.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DomainConfigurationDao {
    @Query("SELECT * FROM domain_configuration")
    suspend fun getAllDomainConfigurations(): List<DomainConfiguration>

    @Query("SELECT * FROM domain_configuration WHERE domain = :domain")
    suspend fun getDomainConfiguration(domain: String): DomainConfiguration?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun addDomainConfiguration(domainConfiguration: DomainConfiguration)

    @Update
    suspend fun updateDomainConfiguration(domainConfiguration: DomainConfiguration)
}