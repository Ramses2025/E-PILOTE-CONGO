package cg.epilote.shared.data.sync

import com.couchbase.lite.Conflict
import com.couchbase.lite.ConflictResolver
import com.couchbase.lite.Document
import com.couchbase.lite.MutableDocument

class EpiloteConflictResolver : ConflictResolver {

    override fun resolve(conflict: Conflict): Document? {
        val local  = conflict.localDocument
        val remote = conflict.remoteDocument

        // Document supprimé dans un des deux → garder la version existante
        if (local == null)  return remote
        if (remote == null) return local

        // Documents verrouillés → la version verrouillée gagne toujours
        if (remote.getBoolean("_locked")) return remote
        if (local.getBoolean("_locked"))  return local

        val localTs  = local.getLong("updatedAt")
        val remoteTs = remote.getLong("updatedAt")

        return when {
            // Timestamps identiques = conflit réel → marquer pour revue
            localTs == remoteTs -> markForReview(local)
            // Last Write Wins
            localTs > remoteTs  -> local
            else                -> remote
        }
    }

    private fun markForReview(doc: Document): MutableDocument {
        return doc.toMutable().apply {
            setBoolean("requiresReview", true)
        }
    }
}
