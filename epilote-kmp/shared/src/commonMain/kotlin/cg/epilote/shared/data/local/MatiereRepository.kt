package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.Matiere
import com.couchbase.lite.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MatiereRepository(private val db: Database) {

    private val collection: Collection
        get() = db.getCollection("matieres") ?: db.createCollection("matieres")

    fun save(matiere: Matiere) {
        val doc = MutableDocument(matiere.id).apply {
            setString("type",         "matiere")
            setString("ecoleId",      matiere.ecoleId)
            setString("classeId",     matiere.classeId)
            setString("nom",          matiere.nom)
            setInt("coefficient",     matiere.coefficient)
            setString("enseignantId", matiere.enseignantId)
            setLong("updatedAt",      matiere.updatedAt)
        }
        collection.save(doc)
    }

    fun saveAll(matieres: List<Matiere>) {
        db.inBatch { matieres.forEach { save(it) } }
    }

    fun getByClasse(ecoleId: String, classeId: String): List<Matiere> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
            )
            .orderBy(Ordering.property("nom"))

        return query.execute().allResults().mapNotNull { it.toMatiere() }
    }

    fun observeByClasse(ecoleId: String, classeId: String): Flow<List<Matiere>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                        .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
                )
                .orderBy(Ordering.property("nom"))

            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toMatiere() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    fun getByEnseignant(ecoleId: String, enseignantId: String): List<Matiere> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("enseignantId").equalTo(Expression.string(enseignantId)))
            )
            .orderBy(Ordering.property("classeId"), Ordering.property("nom"))

        return query.execute().allResults().mapNotNull { it.toMatiere() }
    }

    private fun Result.toMatiere(): Matiere? {
        val dict = getDictionary("matieres") ?: return null
        val id   = getString("id") ?: return null
        return Matiere(
            id            = id,
            ecoleId       = dict.getString("ecoleId") ?: return null,
            classeId      = dict.getString("classeId") ?: "",
            nom           = dict.getString("nom") ?: "",
            coefficient   = dict.getInt("coefficient").takeIf { it > 0 } ?: 1,
            enseignantId  = dict.getString("enseignantId") ?: "",
            updatedAt     = dict.getLong("updatedAt")
        )
    }
}
