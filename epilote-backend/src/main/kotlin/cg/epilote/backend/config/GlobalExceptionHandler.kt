package cg.epilote.backend.config

import cg.epilote.backend.auth.AuthException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(AuthException::class)
    fun handleAuth(ex: AuthException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("UNAUTHORIZED", ex.message ?: "Non autorisé"))

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NOT_FOUND", ex.message ?: "Ressource introuvable"))

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_ERROR", ex.message ?: "Données invalides"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.map { error ->
            if (error is FieldError) "${error.field}: ${error.defaultMessage}"
            else error.defaultMessage ?: "Invalide"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_ERROR", errors.joinToString("; ")))
    }

    /**
     * Honore les exceptions porteuses d'un statut HTTP explicite (annotation
     * `@ResponseStatus` sur l'exception ou `ResponseStatusException` levée
     * explicitement). Sans ce handler, le `handleGeneric` ci-dessous les
     * convertirait en HTTP 500, masquant les erreurs métier (ex. 409 CONFLICT
     * pour email dupliqué, 400 pour planId invalide).
     *
     * Référence Spring MVC :
     * https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
     */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val code = status.name
        val message = ex.reason ?: ex.message ?: status.reasonPhrase
        return ResponseEntity.status(status).body(ErrorResponse(code, message))
    }

    @ExceptionHandler(ErrorResponseException::class)
    fun handleErrorResponse(ex: ErrorResponseException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val code = status.name
        val message = ex.body.detail ?: ex.message ?: status.reasonPhrase
        return ResponseEntity.status(status).body(ErrorResponse(code, message))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        // Délègue aux exceptions porteuses de @ResponseStatus avant de tomber
        // dans le 500 générique : sinon une exception métier custom (annotée
        // @ResponseStatus(HttpStatus.CONFLICT) par exemple) serait convertie
        // en 500, masquant l'intention du code métier.
        val responseStatusAnnotation = ex.javaClass.getAnnotation(org.springframework.web.bind.annotation.ResponseStatus::class.java)
        if (responseStatusAnnotation != null) {
            val status = responseStatusAnnotation.value
            val code = status.name
            val message = ex.message ?: status.reasonPhrase
            return ResponseEntity.status(status).body(ErrorResponse(code, message))
        }
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "Erreur interne du serveur"))
    }
}

data class ErrorResponse(val code: String, val message: String)

class NotFoundException(message: String) : RuntimeException(message)
class ValidationException(message: String) : RuntimeException(message)
