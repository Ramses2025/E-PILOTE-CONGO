package cg.epilote.shared.data.local

import cg.epilote.shared.data.schema.SchemaContract
import cg.epilote.shared.domain.model.Matiere
import com.couchbase.lite.DataSource
import com.couchbase.lite.UnitOfWork
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

class MatiereRepository(private val db: Database) {

    private val collection: com.couchbase.lite.Collection
        get() = db.getCollection("academic_config") ?: db.createCollection("academic_config")

    fun save(matiere: Matiere) {
        val doc = MutableDocument(matiere.id).apply {
            setInt(SchemaContract.FIELD_SCHEMA_VERSION, SchemaContract.CURRENT_SCHEMA_VERSION)
            setString("type",             "subject")
            setString("schoolId",         matiere.schoolId)
            setString("classeId",         matiere.classeId)
            setString("nom",              matiere.nom)
            setString("code",             matiere.code)
            setInt("coefficient",         matiere.coefficient)
            setDouble("heuresParSemaine", matiere.heuresParSemaine)
            setString("enseignantId",     matiere.enseignantId)
            setString("enseignantNom",    matiere.enseignantNom)
            setBoolean("isActive",        matiere.isActive)
            setLong("updatedAt",          matiere.updatedAt)
        }
        collection.save(doc)
    }

    fun saveAll(matieres: List<Matiere>) {
        db.inBatch(UnitOfWork { matieres.forEach { save(it) } })
    }

    fun getByClasse(schoolId: String, classeId: String): List<Matiere> =
        QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("schoolId").equalTo(Expression.string(schoolId))
                    .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                    .and(Expression.property("type").equalTo(Expression.string("subject")))
            )
            .orderBy(Ordering.property("nom"))
            .execute().allResults().mapNotNull { it.toMatiere() }

    fun observeByClasse(schoolId: String, classeId: String): Flow<List<Matiere>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("schoolId").equalTo(Expression.string(schoolId))
                        .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                        .and(Expression.property("type").equalTo(Expression.string("subject")))
                )
                .orderBy(Ordering.property("nom"))

            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toMatiere() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    fun getByEnseignant(schoolId: String, enseignantId: String): List<Matiere> =
        QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("schoolId").equalTo(Expression.string(schoolId))
                    .and(Expression.property("enseignantId").equalTo(Expression.string(enseignantId)))
                    .and(Expression.property("type").equalTo(Expression.string("subject")))
            )
            .orderBy(Ordering.property("classeId"), Ordering.property("nom"))
            .execute().allResults().mapNotNull { it.toMatiere() }

    private fun Result.toMatiere(): Matiere? {
        val dict = getDictionary("academic_config") ?: return null
        val id   = getString("id") ?: return null
        return Matiere(
            id              = id,
            schoolId = dict.getString("schoolId")        ?: return null,
            classeId        = dict.getString("classeId")        ?: "",
            nom             = dict.getString("nom")             ?: "",
            code            = dict.getString("code")            ?: "",
            coefficient     = dict.getInt("coefficient").takeIf { it > 0 } ?: 1,
            heuresParSemaine = dict.getDouble("heuresParSemaine"),
            enseignantId    = dict.getString("enseignantId")    ?: "",
            enseignantNom   = dict.getString("enseignantNom")   ?: "",
            isActive        = dict.getBoolean("isActive"),
            updatedAt       = dict.getLong("updatedAt")
        )
    }
}
