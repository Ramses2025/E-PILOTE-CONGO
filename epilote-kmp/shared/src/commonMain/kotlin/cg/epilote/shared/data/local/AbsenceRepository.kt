package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.Absence
import com.couchbase.lite.DataSource
import com.couchbase.lite.Database
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.Result
import com.couchbase.lite.SelectResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AbsenceRepository(private val db: Database) {

    private val collection: com.couchbase.lite.Collection
        get() = db.getCollection("attendances") ?: db.createCollection("attendances")

    fun save(absence: Absence) {
        val doc = MutableDocument(absence.id).apply {
            setString("type",        "attendance")
            setString("ecoleId",     absence.ecoleId)
            setString("anneeId",     absence.anneeId)
            setString("classeId",    absence.classeId)
            setString("eleveId",     absence.eleveId)
            setString("eleveName",   absence.eleveName)
            setString("date",        absence.date)
            setBoolean("justifiee",  absence.justifiee)
            setString("motif",       absence.motif ?: "")
            setString("saisieParId", absence.saisieParId)
            setLong("updatedAt",     absence.updatedAt)
        }
        collection.save(doc)
    }

    fun getByEleve(ecoleId: String, eleveId: String): List<Absence> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("eleveId").equalTo(Expression.string(eleveId)))
            )
            .orderBy(Ordering.property("date").descending())

        return query.execute().allResults().mapNotNull { it.toAbsence() }
    }

    fun observeByDate(ecoleId: String, date: String): Flow<List<Absence>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                        .and(Expression.property("date").equalTo(Expression.string(date)))
                )
            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toAbsence() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    private fun Result.toAbsence(): Absence? {
        val dict = getDictionary("attendances") ?: return null
        val id   = getString("id") ?: return null
        return Absence(
            id          = id,
            ecoleId     = dict.getString("ecoleId")   ?: return null,
            anneeId     = dict.getString("anneeId")   ?: "",
            classeId    = dict.getString("classeId")  ?: "",
            eleveId     = dict.getString("eleveId")   ?: return null,
            eleveName   = dict.getString("eleveName") ?: "",
            date        = dict.getString("date")       ?: return null,
            justifiee   = dict.getBoolean("justifiee"),
            motif       = dict.getString("motif").takeIf { it?.isNotBlank() == true },
            saisieParId = dict.getString("saisieParId") ?: "",
            updatedAt   = dict.getLong("updatedAt")
        )
    }
}
