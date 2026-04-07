package cg.epilote.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EpiloteApplication

fun main(args: Array<String>) {
    runApplication<EpiloteApplication>(*args)
}
