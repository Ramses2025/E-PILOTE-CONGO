package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.Eleve
import com.couchbase.lite.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class EleveRepository(private val db: Database) {

    private val collection: Collection
        get() = db.getCollection("students") ?: db.createCollection("students")

    fun save(eleve: Eleve) {
        val doc = MutableDocument(eleve.id).apply {
            setString("type",             "student")
            setString("ecoleId",          eleve.ecoleId)
            setString("anneeId",          eleve.anneeId)
            setString("matricule",        eleve.matricule)
            setString("nom",              eleve.nom)
            setString("prenom",           eleve.prenom)
            setString("currentClassId",   eleve.classeId)
            setString("dateNaissance",    eleve.dateNaissance ?: "")
            setString("genre",            eleve.genre ?: "")
            setString("photo",            eleve.photo ?: "")
            setBoolean("isActive",        eleve.isActive)
            setLong("updatedAt",          eleve.updatedAt)
        }
        collection.save(doc)
    }

    fun saveAll(eleves: List<Eleve>) {
        db.inBatch {
            eleves.forEach { save(it) }
        }
    }

    fun getByClasse(ecoleId: String, classeId: String): List<Eleve> {
        val query = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(Expression.property("classeId").equalTo(Expression.string(classeId)))
            )
            .orderBy(Ordering.property("nom"), Ordering.property("prenom"))

        return query.execute().allResults().mapNotNull { it.toEleve() }
    }

    fun observeByClasse(ecoleId: String, classeId: String): Flow<List<Eleve>> =
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
                trySend(change.results?.allResults()?.mapNotNull { it.toEleve() } ?: emptyList())
            }
            query.execute()
            awaitClose { token.remove() }
        }

    fun search(ecoleId: String, query: String): List<Eleve> {
        val like = Expression.string("%${query.uppercase()}%")
        val dbQuery = QueryBuilder
            .select(SelectResult.all(), SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("ecoleId").equalTo(Expression.string(ecoleId))
                    .and(
                        Function.upper(Expression.property("nom")).like(like)
                            .or(Function.upper(Expression.property("prenom")).like(like))
                            .or(Function.upper(Expression.property("matricule")).like(like))
                    )
            )
            .orderBy(Ordering.property("nom"))
            .limit(Expression.intValue(50))

        return dbQuery.execute().allResults().mapNotNull { it.toEleve() }
    }

    private fun Result.toEleve(): Eleve? {
        val dict = getDictionary("students") ?: return null
        val id   = getString("id") ?: return null
        return Eleve(
            id            = id,
            ecoleId       = dict.getString("ecoleId")        ?: return null,
            anneeId       = dict.getString("anneeId")        ?: "",
            matricule     = dict.getString("matricule")      ?: "",
            nom           = dict.getString("nom")            ?: "",
            prenom        = dict.getString("prenom")         ?: "",
            classeId      = dict.getString("currentClassId") ?: "",
            dateNaissance = dict.getString("dateNaissance").takeIf { it?.isNotBlank() == true },
            genre         = dict.getString("genre").takeIf { it?.isNotBlank() == true },
            photo         = dict.getString("photo").takeIf { it?.isNotBlank() == true },
            isActive      = dict.getBoolean("isActive"),
            updatedAt     = dict.getLong("updatedAt")
        )
    }
}
