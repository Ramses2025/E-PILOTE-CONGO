package cg.epilote.backend.admin

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/super-admin/communications")
class AdminCommunicationController(
    private val communicationRepo: AdminCommunicationRepository
) {
    private fun Authentication.userId() = principal as String

    @GetMapping("/announcements")
    fun listAnnouncements(): ResponseEntity<List<AnnouncementResponse>> =
        runBlocking { ResponseEntity.ok(communicationRepo.listAnnouncements()) }

    @PostMapping("/announcements")
    fun createAnnouncement(
        @Valid @RequestBody req: CreateAnnouncementRequest,
        auth: Authentication
    ): ResponseEntity<AnnouncementResponse> = runBlocking {
        ResponseEntity.status(HttpStatus.CREATED).body(communicationRepo.createAnnouncement(req, auth.userId()))
    }

    @GetMapping("/messages")
    fun listMessages(): ResponseEntity<List<AdminMessageResponse>> =
        runBlocking { ResponseEntity.ok(communicationRepo.listMessages()) }

    @PostMapping("/messages")
    fun createMessage(
        @Valid @RequestBody req: CreateAdminMessageRequest,
        auth: Authentication
    ): ResponseEntity<AdminMessageResponse> = runBlocking {
        communicationRepo.createMessage(req, auth.userId())?.let {
            ResponseEntity.status(HttpStatus.CREATED).body(it)
        } ?: ResponseEntity.badRequest().build()
    }

    @PutMapping("/messages/{messageId}/status")
    fun updateMessageStatus(
        @PathVariable messageId: String,
        @RequestParam status: String
    ): ResponseEntity<AdminMessageResponse> = runBlocking {
        communicationRepo.updateMessageStatus(messageId, status)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.badRequest().build()
    }
}
