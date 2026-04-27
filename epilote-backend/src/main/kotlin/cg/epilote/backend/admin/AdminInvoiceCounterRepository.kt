package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneId

/**
 * Compteur atomique de numérotation de facture.
 *
 * Un document counter par année (`config::invoice_counter::{YYYY}`) dans la collection `config`.
 * L'opération d'incrément s'appuie sur `Binary.increment()` du SDK Kotlin Couchbase, qui est
 * atomique côté cluster (pas de race condition même sous charge concurrente).
 *
 * Référence officielle : https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#counters
 */
@Repository
class AdminInvoiceCounterRepository(private val bucket: Bucket) {
    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val DOC_PREFIX = "config::invoice_counter::"
    }

    private fun col(name: String): Collection = runBlocking { scope.collection(name) }

    private fun currentYear(): Int =
        Instant.now().atZone(ZoneId.of("UTC")).year

    /**
     * Incrémente atomiquement le compteur annuel et retourne la valeur obtenue.
     * Si le document n'existe pas encore, il est initialisé à 1.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun nextSequence(year: Int = currentYear()): Long {
        val docId = "$DOC_PREFIX$year"
        // Avec `initialValue`, le SDK crée le document avec cette valeur s'il n'existe pas,
        // puis incrémente de `delta`. C'est atomique côté cluster.
        val result = col(CONFIG_COLLECTION).binary.increment(
            id = docId,
            delta = 1UL,
            initialValue = 1UL
        )
        return result.content.toLong()
    }

    /**
     * Applique le format défini dans `PlatformIdentity.invoiceNumberFormat` en remplaçant
     * `{YYYY}` par l'année courante et `{NNNNNN}` (ou tout autre bloc `{N+}`) par la valeur
     * du compteur avec padding zéro.
     *
     * Défense en profondeur : si un format ne contient pas de bloc `{N+}` (données
     * obsolètes en base avant validation en écriture), on retombe sur le format par
     * défaut pour garantir l'unicité de la référence — deux factures ne peuvent
     * JAMAIS porter la même référence.
     */
    fun formatReference(format: String, year: Int, sequence: Long): String {
        val sequencePlaceholder = Regex("\\{N+}")
        val safeFormat = format.takeIf {
            it.isNotBlank() && sequencePlaceholder.containsMatchIn(it)
        } ?: "FAC-{YYYY}-{NNNNNN}"
        val withYear = safeFormat.replace("{YYYY}", year.toString())
        return sequencePlaceholder.replace(withYear) { match ->
            val width = match.value.length - 2
            sequence.toString().padStart(width, '0')
        }
    }

    suspend fun nextReference(format: String): String {
        val year = currentYear()
        val seq = nextSequence(year)
        return formatReference(format, year, seq)
    }
}
