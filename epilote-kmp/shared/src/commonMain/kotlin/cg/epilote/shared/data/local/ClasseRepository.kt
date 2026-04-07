package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.Classe
import com.couchbase.lite.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ClasseRepository(private val db: Database) {

    private val collection: Collection
        get() = db.getCollection("classes") ?: db.createCollection("classes")

    fun save(classe: Classe) {
        val doc = MutableDocument(classe.id).apply {
            setString("type",      "classe")
            setString("ecoleId",   classe.ecoleId)
            setString("nom",       classe.nom)
            setString("niveau",    classe.niveau)
            setString("annee",     classe.annee)
            setLong("updatedAt",   classe.updatedAt)
        }
        collection.save(doc)
    }

    fun saveAll(classes: List<Classe>) {
        db.inBatch { classes.forEach { save(it) } }
    }

    fun getByEcole(ecoleId: String): List<Classe> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(Expression.property("ecoleId").equalTo(Expression.string(ecoleId)))
            .orderBy(Ordering.property("nom"))

        return query.execute().allResults().mapNotNull { it.toClasse() }
    }

    fun observeByEcole(ecoleId: String): Flow<List<Classe>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(Expression.property("ecoleId").equalTo(Expression.string(ecoleId)))
                .orderBy(Ordering.property("nom"))

            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toClasse() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    private fun Result.toClasse(): Classe? {
        val dict = getDictionary("classes") ?: return null
        val id   = getString("id") ?: return null
        return Classe(
            id        = id,
            ecoleId   = dict.getString("ecoleId") ?: return null,
            nom       = dict.getString("nom") ?: "",
            niveau    = dict.getString("niveau") ?: "",
            annee     = dict.getString("annee") ?: "",
            updatedAt = dict.getLong("updatedAt")
        )
    }
}
