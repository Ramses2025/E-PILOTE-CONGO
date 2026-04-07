package cg.epilote.backend.config

import com.couchbase.client.kotlin.Cluster
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.Duration.Companion.seconds

@Configuration
class CouchbaseConfig {

    @Value("\${couchbase.connection-string}")
    private lateinit var connectionString: String

    @Value("\${couchbase.username}")
    private lateinit var username: String

    @Value("\${couchbase.password}")
    private lateinit var password: String

    @Value("\${couchbase.bucket}")
    private lateinit var bucketName: String

    @Bean
    fun couchbaseCluster(): Cluster = runBlocking {
        Cluster.connect(
            connectionString = connectionString,
            username = username,
            password = password,
        ) {
            io { kvConnectTimeout = 10.seconds }
        }
    }

    @Bean
    fun couchbaseBucket(cluster: Cluster) = runBlocking {
        cluster.bucket(bucketName)
    }
}
