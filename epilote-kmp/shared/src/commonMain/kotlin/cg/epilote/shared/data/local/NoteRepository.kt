package cg.epilote.shared.data.local

import cg.epilote.shared.data.schema.SchemaContract
import cg.epilote.shared.domain.model.Note
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

class NoteRepository(private val db: Database) {

    private val collection: com.couchbase.lite.Collection
        get() = db.getCollection("grades") ?: db.createCollection("grades")

    fun save(note: Note) {
        val doc = MutableDocument(note.id).apply {
            setInt(SchemaContract.FIELD_SCHEMA_VERSION, SchemaContract.CURRENT_SCHEMA_VERSION)
            setString("type",            "grade")
            setString("schoolId",        note.schoolId)
            setString("anneeId",         note.anneeId)
            setString("classeId",        note.classeId)
            setString("eleveId",         note.eleveId)
            setString("eleveName",       note.eleveName)
            setString("matiereId",       note.matiereId)
            setString("matiereNom",      note.matiereNom)
            setString("enseignantId",    note.enseignantId)
            setString("evaluationId",    note.evaluationId ?: "")
            setString("typeEval",        note.typeEval)
            setString("periode",         note.periode)
            setDouble("valeur",          note.valeur)
            setDouble("valeurMax",       note.valeurMax)
            setInt("coefficient",        note.coefficient)
            setString("commentaire",     note.commentaire ?: "")
            setBoolean("locked",         note.locked)
            setBoolean("requiresReview", note.requiresReview)
            setLong("updatedAt",         note.updatedAt)
        }
        collection.save(doc)
    }

    fun getByClasse(schoolId: String, classeId: String, periode: String): List<Note> =
        QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("schoolId").equalTo(Expression.string(schoolId))
                    .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                    .and(Expression.property("periode").equalTo(Expression.string(periode)))
            )
            .orderBy(Ordering.property("eleveId"))
            .execute().allResults().mapNotNull { it.toNote() }

    fun getByEleve(schoolId: String, eleveId: String, periode: String): List<Note> =
        QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("schoolId").equalTo(Expression.string(schoolId))
                    .and(Expression.property("eleveId").equalTo(Expression.string(eleveId)))
                    .and(Expression.property("periode").equalTo(Expression.string(periode)))
            )
            .execute().allResults().mapNotNull { it.toNote() }

    fun observeByClasse(schoolId: String, classeId: String, periode: String): Flow<List<Note>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("schoolId").equalTo(Expression.string(schoolId))
                        .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                        .and(Expression.property("periode").equalTo(Expression.string(periode)))
                )
            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toNote() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    fun getPendingReview(schoolId: String): List<Note> =
        QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("schoolId").equalTo(Expression.string(schoolId))
                    .and(Expression.property("requiresReview").equalTo(Expression.booleanValue(true)))
            )
            .execute().allResults().mapNotNull { it.toNote() }

    private fun Result.toNote(): Note? {
        val dict = getDictionary("grades") ?: return null
        val id   = getString("id") ?: return null
        return Note(
            id             = id,
            schoolId = dict.getString("schoolId")    ?: return null,
            anneeId        = dict.getString("anneeId")     ?: "",
            classeId       = dict.getString("classeId")    ?: return null,
            eleveId        = dict.getString("eleveId")     ?: return null,
            eleveName      = dict.getString("eleveName")   ?: "",
            matiereId      = dict.getString("matiereId")   ?: return null,
            matiereNom     = dict.getString("matiereNom")  ?: "",
            enseignantId   = dict.getString("enseignantId")?: "",
            evaluationId   = dict.getString("evaluationId").takeIf { it?.isNotBlank() == true },
            typeEval       = dict.getString("typeEval")    ?: "devoir",
            periode        = dict.getString("periode")     ?: return null,
            valeur         = dict.getDouble("valeur"),
            valeurMax      = dict.getDouble("valeurMax").takeIf { it > 0 } ?: 20.0,
            coefficient    = dict.getInt("coefficient").takeIf { it > 0 } ?: 1,
            commentaire    = dict.getString("commentaire").takeIf { it?.isNotBlank() == true },
            locked         = dict.getBoolean("locked"),
            requiresReview = dict.getBoolean("requiresReview"),
            updatedAt      = dict.getLong("updatedAt")
        )
    }
}
