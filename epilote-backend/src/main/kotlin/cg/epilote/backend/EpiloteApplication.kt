package cg.epilote.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class EpiloteApplication

fun main(args: Array<String>) {
    runApplication<EpiloteApplication>(*args)
}
