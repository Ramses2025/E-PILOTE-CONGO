package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.Note
import com.couchbase.lite.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NoteRepository(private val db: Database) {

    private val collection: Collection
        get() = db.getCollection("notes") ?: db.createCollection("notes")

    fun save(note: Note) {
        val doc = MutableDocument(note.id).apply {
            setString("type",          "note")
            setString("ecoleId",       note.ecoleId)
            setString("eleveId",       note.eleveId)
            setString("classeId",      note.classeId)
            setString("matiereId",     note.matiereId)
            setString("periode",       note.periode)
            setDouble("valeur",        note.valeur)
            setString("auteurId",      note.auteurId)
            setBoolean("_locked",      note.locked)
            setBoolean("requiresReview", note.requiresReview)
            setLong("updatedAt",       note.updatedAt)
        }
        collection.save(doc)
    }

    fun getByClasse(ecoleId: String, classeId: String, periode: String): List<Note> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                    .and(Expression.property("periode").equalTo(Expression.string(periode)))
            )
            .orderBy(Ordering.property("eleveId"))

        return query.execute().allResults().mapNotNull { it.toNote() }
    }

    fun observeByClasse(ecoleId: String, classeId: String, periode: String): Flow<List<Note>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                        .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                        .and(Expression.property("periode").equalTo(Expression.string(periode)))
                )

            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toNote() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    fun getPendingReview(ecoleId: String): List<Note> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("requiresReview").equalTo(Expression.booleanValue(true)))
            )
        return query.execute().allResults().mapNotNull { it.toNote() }
    }

    private fun Result.toNote(): Note? {
        val dict = getDictionary("notes") ?: return null
        val id   = getString("id") ?: return null
        return Note(
            id             = id,
            ecoleId        = dict.getString("ecoleId") ?: return null,
            eleveId        = dict.getString("eleveId") ?: return null,
            classeId       = dict.getString("classeId") ?: return null,
            matiereId      = dict.getString("matiereId") ?: return null,
            periode        = dict.getString("periode") ?: return null,
            valeur         = dict.getDouble("valeur"),
            auteurId       = dict.getString("auteurId") ?: "",
            locked         = dict.getBoolean("_locked"),
            requiresReview = dict.getBoolean("requiresReview"),
            updatedAt      = dict.getLong("updatedAt")
        )
    }
}
