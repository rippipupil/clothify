package com.rippipupil.clothify.data.pool

import androidx.room.withTransaction
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.local.TagScoreEntity
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.ProfileScorer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Puntuaciones de gustos por etiqueta. */
class ProfileRepository(private val db: ClothifyDatabase) {
    private val dao = db.tagScoreDao()

    val scores: Flow<Map<String, Double>> = dao.observeAll().map { list -> list.associate { it.tagId to it.score } }

    suspend fun snapshot(): Map<String, Double> = dao.all().associate { it.tagId to it.score }

    suspend fun apply(event: ProfileEvent) = db.withTransaction {
        val before = snapshot()
        val after = ProfileScorer.apply(before, event)
        dao.upsertAll(after.filter { (id, score) -> before[id] != score }.map { (id, score) -> TagScoreEntity(id, score) })
    }

    suspend fun clear() = dao.deleteAll()
}
