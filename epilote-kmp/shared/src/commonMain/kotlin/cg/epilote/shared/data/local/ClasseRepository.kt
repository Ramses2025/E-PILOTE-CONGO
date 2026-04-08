package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.Classe
import com.couchbase.lite.Collection
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

class ClasseRepository(private val db: Database) {

    private val collection: com.couchbase.lite.Collection
        get() = db.getCollection("academic_config") ?: db.createCollection("academic_config")

    fun save(classe: Classe) {
        val doc = MutableDocument(classe.id).apply {
            setString("type",                    "class")
            setString("ecoleId",                 classe.ecoleId)
            setString("anneeId",                 classe.anneeId)
            setString("nom",                     classe.nom)
            setString("section",                 classe.section ?: "")
            setString("niveauId",                classe.niveauId)
            setString("niveauNom",               classe.niveauNom)
            setString("filiereId",               classe.filiereId ?: "")
            setString("enseignantPrincipalId",   classe.enseignantPrincipalId ?: "")
            setInt("capacite",                   classe.capacite)
            setBoolean("isActive",               classe.isActive)
            setLong("updatedAt",                 classe.updatedAt)
        }
        collection.save(doc)
    }

    fun saveAll(classes: List<Classe>) {
        db.inBatch(UnitOfWork { classes.forEach { save(it) } })
    }

    fun getByEcole(ecoleId: String): List<Classe> =
        QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("type").equalTo(Expression.string("class")))
            )
            .orderBy(Ordering.property("nom"))
            .execute().allResults().mapNotNull { it.toClasse() }

    fun observeByEcole(ecoleId: String): Flow<List<Classe>> =
        callbackFlow {
            val query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                        .and(Expression.property("type").equalTo(Expression.string("class")))
                )
                .orderBy(Ordering.property("nom"))

            val token = query.addChangeListener { change ->
                trySend(change.results?.allResults()?.mapNotNull { it.toClasse() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    private fun Result.toClasse(): Classe? {
        val dict = getDictionary("academic_config") ?: return null
        val id   = getString("id") ?: return null
        return Classe(
            id                     = id,
            ecoleId                = dict.getString("ecoleId")              ?: return null,
            anneeId                = dict.getString("anneeId")              ?: "",
            nom                    = dict.getString("nom")                  ?: "",
            section                = dict.getString("section")?.takeIf { it.isNotEmpty() },
            niveauId               = dict.getString("niveauId")             ?: "",
            niveauNom              = dict.getString("niveauNom")            ?: "",
            filiereId              = dict.getString("filiereId")?.takeIf { it.isNotEmpty() },
            enseignantPrincipalId  = dict.getString("enseignantPrincipalId")?.takeIf { it.isNotEmpty() },
            capacite               = dict.getInt("capacite").takeIf { it > 0 } ?: 50,
            isActive               = dict.getBoolean("isActive"),
            updatedAt              = dict.getLong("updatedAt")
        )
    }
}
