package cn.xihan.sign.base

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Upsert

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:37
 * @介绍 :
 */
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: Collection<T>)

    @Upsert
    suspend fun upsert(vararg entity: T)

    @Upsert
    suspend fun upsert(entities: Collection<T>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T): Int
}