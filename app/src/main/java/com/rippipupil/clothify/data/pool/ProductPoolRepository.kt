package com.rippipupil.clothify.data.pool

import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.local.QueryHistoryEntity
import com.rippipupil.clothify.data.local.SeenProductEntity
import com.rippipupil.clothify.data.local.toDomain
import com.rippipupil.clothify.data.local.toEntity
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.sources.ProductSource
import com.rippipupil.clothify.data.sources.SourceFailure
import com.rippipupil.clothify.data.sources.SourceResult
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.QueryPlanner
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source

data class RefillReport(val added: Int, val failures: Map<Source, SourceFailure>)

/** Reserva local de prendas reales por ver: se rellena desde las fuentes y caduca a los 3 días. */
class ProductPoolRepository(
    private val db: ClothifyDatabase,
    private val sources: List<ProductSource>,
    private val planner: QueryPlanner,
    private val profile: ProfileRepository,
    private val settings: SettingsRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val products = db.productDao()
    private val seen = db.seenDao()
    private val history = db.queryHistoryDao()

    suspend fun candidates(): List<Product> = products.unseenFresh(freshSince()).map { it.toDomain() }

    suspend fun needsRefill(): Boolean = products.countUnseenFresh(freshSince()) < REFILL_THRESHOLD

    suspend fun refill(): RefillReport {
        val s = settings.current()
        val queries = planner.plan(
            scores = profile.snapshot(),
            recentTexts = history.textsSince(now() - QUERY_REPEAT_WINDOW_MS).toSet(),
            maxPriceEur = s.maxPriceEur,
            sizes = s.sizes,
            location = s.lastLocation.takeIf { s.locationEnabled },
        )
        val found = mutableListOf<Product>()
        val failures = mutableMapOf<Source, SourceFailure>()
        val succeeded = mutableSetOf<Source>()
        for (query in queries) {
            val (items, failed) = searchAll(query)
            found += items
            failures += failed
            succeeded += items.map { it.source }
        }
        history.upsertAll(queries.map { QueryHistoryEntity(it.text, now()) })
        val known = (seen.allIds() + products.all().map { it.id }).toSet()
        val fresh = found.distinctBy { it.id }.filter { it.id !in known }
        products.upsertAll(fresh.map { it.toEntity() })
        return RefillReport(fresh.size, failures.filterKeys { it !in succeeded })
    }

    /** Busca en todas las fuentes una tras otra, sin guardar en la reserva. */
    suspend fun searchAll(query: SearchQuery): Pair<List<Product>, Map<Source, SourceFailure>> {
        val found = mutableListOf<Product>()
        val failures = mutableMapOf<Source, SourceFailure>()
        for (source in sources) {
            when (val result = source.search(query)) {
                is SourceResult.Ok -> found += result.products
                is SourceResult.Failed -> failures[source.source] = result.reason
            }
        }
        return found.distinctBy { it.id } to failures
    }

    suspend fun markSeen(product: Product, liked: Boolean) =
        seen.insert(SeenProductEntity(product.id, liked, now()))

    suspend fun purgeExpired() = products.deleteOlderThan(freshSince())

    suspend fun resetSeen() = seen.deleteAll()

    suspend fun clearAll() {
        products.deleteAll()
        seen.deleteAll()
        history.deleteAll()
    }

    private fun freshSince() = now() - EXPIRY_MS

    companion object {
        const val REFILL_THRESHOLD = 15
        const val EXPIRY_MS = 3 * 24 * 60 * 60 * 1000L
        const val QUERY_REPEAT_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}
