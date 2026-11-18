package com.example.mobiledev.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedAddressDao {

    @Query("SELECT * FROM cached_addresses WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CachedAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: CachedAddress)
}
