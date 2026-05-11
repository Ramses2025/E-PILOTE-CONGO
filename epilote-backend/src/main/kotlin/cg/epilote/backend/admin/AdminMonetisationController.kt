package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
class AdminMonetisationController(
    private val repo: AdminRepository,
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val invoicePdfService: AdminInvoicePdfService,
    private val paymentReceiptRepo: AdminPaymentReceiptRepository,
    private val paymentClaimRepo: AdminPaymentClaimRepository,
    private val auditHelper: AdminAuditHelper
) {
    private val log = LoggerFactory.getLogger(AdminMonetisationController::class.java)
    private val allowedInvoiceStatuses = setOf("draft", "sent", "paid", "overdue", "cancelled")

    private fun Authentication.userId() = principal as String
    @Suppress("UNCHECKED_CAST")
    private fun Authentication.details() = details as? Map<String, String> ?: emptyMap()
    private fun Authentication.role(): String? = authorities.firstOrNull()?.authority?.removePrefix("ROLE_")
    private fun Authentication.displayName(): String {
        val d = details()
        val full = "${d["firstName"].orEmpty().trim()} ${d["lastName"].orEmpty().trim()}".trim()
        return full.ifEmpty { d["email"] ?: userId() }
    }

    // ── Super Admin : Factures ───────────────────────────────────

    @DeleteMapping("/api/super-admin/invoices/{invoiceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun deleteInvoice(
        @PathVariable invoiceId: String,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<Any> = runBlocking {
        val existing = repo.getInvoiceById(invoiceId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val linkedReceipt = paymentReceiptRepo.listAll().firstOrNull { it.invoiceId == invoiceId }
        if (linkedReceipt != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cette facture est liée au reçu ${linkedReceipt.id}. Supprime d'abord le paiement de test correspondant."
            )
        }
        return@runBlocking if (repo.deleteInvoice(invoiceId)) {
            auditHelper.audit(AuditAction.INVOICE_DELETED, auth, httpReq,
                targetType = "invoice", targetId = invoiceId, targetLabel = existing.reference,
                message = "Facture ${existing.reference.ifBlank { invoiceId }} supprimée",
                metadata = mapOf("groupeId" to existing.groupeId, "subscriptionId" to existing.subscriptionId))
            ResponseEntity.noContent().build<Any>()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/api/super-admin/invoices")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun createInvoice(
        @Valid @RequestBody req: CreateInvoiceRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<InvoiceResponse> = runBlocking {
        val subscription = subscriptionRepo.getSubscriptionById(req.subscriptionId)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        if (subscription.groupeId != req.groupeId) return@runBlocking ResponseEntity.badRequest().build()
        val now = System.currentTimeMillis()
        val isPending = subscription.statut == "pending"
        if (subscription.statut == "cancelled" || (!isPending && subscription.dateFin > 0L && subscription.dateFin < now)) {
            return@runBlocking ResponseEntity.badRequest().build()
        }
        val invoice = repo.createInvoice(req)
        auditHelper.audit(AuditAction.INVOICE_CREATED, auth, httpReq,
            targetType = "invoice", targetId = invoice.id, targetLabel = invoice.reference,
            message = "Facture ${invoice.reference} émise pour groupe ${req.groupeId}",
            metadata = mapOf("montantXAF" to invoice.montantXAF, "groupeId" to invoice.groupeId))
        ResponseEntity.status(HttpStatus.CREATED).body(invoice)
    }

    @GetMapping("/api/super-admin/invoices")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listInvoices(): ResponseEntity<List<InvoiceResponse>> =
        runBlocking { ResponseEntity.ok(repo.listInvoices()) }

    @GetMapping("/api/super-admin/invoices/{invoiceId}/pdf")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun downloadInvoicePdf(@PathVariable invoiceId: String): ResponseEntity<ByteArrayResource> = runBlocking {
        val pdf = invoicePdfService.generateInvoicePdf(invoiceId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val resource = ByteArrayResource(pdf.bytes)
        ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(pdf.fileName).build().toString())
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdf.bytes.size.toLong())
            .body(resource)
    }

    @PutMapping("/api/super-admin/invoices/{invoiceId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun updateInvoiceStatus(
        @PathVariable invoiceId: String,
        @RequestParam statut: String,
        @RequestParam(required = false) datePaiement: Long?,
        @RequestParam(required = false) montantXAF: Long?,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<InvoiceResponse> = runBlocking {
        val normalizedStatus = statut.trim().lowercase()
        if (normalizedStatus !in allowedInvoiceStatuses) return@runBlocking ResponseEntity.badRequest().build()
        val updated = repo.updateInvoiceStatus(invoiceId, normalizedStatus, datePaiement, montantXAF = montantXAF)
        if (updated != null) {
            auditHelper.audit(AuditAction.INVOICE_STATUS_CHANGED, auth, httpReq,
                targetType = "invoice", targetId = invoiceId, targetLabel = updated.reference,
                message = "Facture ${updated.reference} → ${updated.statut}",
                metadata = mapOf("newStatus" to updated.statut))
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }

    // ── Super Admin : Abonnements ────────────────────────────────

    @PostMapping("/api/super-admin/subscriptions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun createSubscription(@Valid @RequestBody req: CreateSubscriptionRequest): ResponseEntity<SubscriptionResponse> = runBlocking {
        val plan = planRepo.getPlanById(req.planId) ?: return@runBlocking ResponseEntity.badRequest().build()
        subscriptionRepo.createSubscription(req.copy(planId = plan.id))
            ?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            ?: ResponseEntity.badRequest().build()
    }

    @GetMapping("/api/super-admin/subscriptions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listSubscriptions(): ResponseEntity<List<SubscriptionResponse>> =
        runBlocking { ResponseEntity.ok(subscriptionRepo.listSubscriptions()) }

    @PutMapping("/api/super-admin/subscriptions/{subId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun updateSubscriptionStatus(
        @PathVariable subId: String,
        @RequestParam statut: String,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<SubscriptionResponse> = runBlocking {
        val updated = subscriptionRepo.updateSubscriptionStatus(subId, statut)
        if (updated != null) {
            auditHelper.audit(AuditAction.SUBSCRIPTION_STATUS_CHANGED, auth, httpReq,
                targetType = "subscription", targetId = subId, targetLabel = updated.groupeId,
                message = "Statut abonnement $subId → ${updated.statut}",
                metadata = mapOf("newStatus" to updated.statut, "groupeId" to updated.groupeId))
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }

    // ── Super Admin : Paiements présentiels ──────────────────────

    @GetMapping("/api/super-admin/payment-methods")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listPaymentMethods(): ResponseEntity<List<Map<String, Any>>> =
        ResponseEntity.ok(PaymentMethod.values().map {
            mapOf("code" to it.code, "label" to it.label, "enabled" to it.enabled)
        })

    @PostMapping("/api/super-admin/payment-receipts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun recordPayment(
        @Valid @RequestBody req: RecordPaymentRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<PaymentReceiptResponse> = runBlocking {
        val method = PaymentMethod.fromCode(req.paymentMethod)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        if (!method.enabled) return@runBlocking ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
        val subscription = subscriptionRepo.getSubscriptionById(req.subscriptionId)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        if (subscription.groupeId != req.groupeId) return@runBlocking ResponseEntity.badRequest().build()

        val idempotencyKey = req.idempotencyKey?.takeIf { it.isNotBlank() }
        var claimToken: String? = null
        if (idempotencyKey != null) {
            when (val outcome = paymentClaimRepo.claim(idempotencyKey)) {
                is ClaimOutcome.Acquired -> claimToken = outcome.claimToken
                is ClaimOutcome.AlreadyDone -> {
                    val cachedReceipt = outcome.receiptId?.let { paymentReceiptRepo.getById(it) }
                    return@runBlocking if (cachedReceipt != null) ResponseEntity.ok(cachedReceipt)
                    else ResponseEntity.status(HttpStatus.CONFLICT).build()
                }
                is ClaimOutcome.InProgress ->
                    return@runBlocking ResponseEntity.status(HttpStatus.CONFLICT).header("Retry-After", "2").build()
                is ClaimOutcome.PreviouslyFailed -> {
                    val msg = outcome.errorMessage ?: "Le paiement initial avec cette clé d'idempotence a échoué."
                    throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "$msg Générer une nouvelle clé d'idempotence pour retenter.")
                }
            }
        }

        val paidAt = System.currentTimeMillis()
        val (accessStart, accessEnd) = subscriptionRepo.computeRenewalPeriod(req.durationMonths)
        var createdInvoiceId: String? = null
        var createdReceiptId: String? = null
        val receipt: PaymentReceiptResponse = try {
            val invoice = if (req.invoiceId != null) {
                val existing = repo.getInvoiceById(req.invoiceId)
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Facture ${req.invoiceId} introuvable.")
                if (existing.groupeId != req.groupeId)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture n'appartient pas au groupe indiqué.")
                repo.updateInvoiceStatus(req.invoiceId, "paid", paidAt, montantXAF = req.montantXAF)
                    ?: throw IllegalStateException("Impossible de marquer la facture ${req.invoiceId} comme payée.")
            } else {
                repo.createInvoice(
                    CreateInvoiceRequest(
                        groupeId = req.groupeId,
                        subscriptionId = req.subscriptionId,
                        montantXAF = req.montantXAF,
                        dateEcheance = accessEnd,
                        notes = listOfNotNull(
                            "Paiement présentiel — mode : ${method.label}",
                            req.externalReference?.let { "Réf. externe : $it" },
                            req.paidBy?.let { "Payé par : $it" },
                            req.notes.takeIf { it.isNotBlank() }
                        ).joinToString("\n"),
                        initialStatus = "paid",
                        datePaiement = paidAt
                    )
                )
            }
            createdInvoiceId = invoice.id.takeIf { req.invoiceId == null }
            val rec = paymentReceiptRepo.record(
                groupeId = req.groupeId, subscriptionId = req.subscriptionId,
                invoiceId = invoice.id, montantXAF = req.montantXAF, method = method,
                externalReference = req.externalReference, paidBy = req.paidBy,
                receivedBy = "${auth.displayName()} (${auth.role() ?: "Administrateur"})",
                notes = req.notes, accessStart = accessStart, accessEnd = accessEnd,
                idempotencyKey = idempotencyKey
            )
            createdReceiptId = rec.id
            val activated = subscriptionRepo.applyPaidPeriod(req.subscriptionId, accessStart, accessEnd)
                ?: throw IllegalStateException("Application de la période d'accès impossible (${req.subscriptionId})")
            auditHelper.audit(AuditAction.PAYMENT_RECORDED, auth, httpReq,
                targetType = "subscription", targetId = req.subscriptionId, targetLabel = req.groupeId,
                message = "Paiement ${req.montantXAF} XAF (${method.label}) enregistré pour groupe ${req.groupeId}",
                metadata = mapOf("montantXAF" to req.montantXAF, "paymentMethod" to method.code,
                    "durationMonths" to req.durationMonths, "invoiceId" to invoice.id,
                    "invoiceReference" to invoice.reference, "accessEnd" to activated.dateFin))
            auditHelper.audit(AuditAction.SUBSCRIPTION_RENEWED, auth, httpReq,
                targetType = "subscription", targetId = req.subscriptionId, targetLabel = req.groupeId,
                message = "Abonnement renouvelé jusqu'au ${java.time.Instant.ofEpochMilli(activated.dateFin)}",
                metadata = mapOf("newEndDate" to activated.dateFin, "durationMonths" to req.durationMonths))
            rec
        } catch (e: Exception) {
            log.warn("Échec recordPayment (sub=${req.subscriptionId}, key=$idempotencyKey) : ${e.message}", e)
            createdReceiptId?.let { paymentReceiptRepo.deleteById(it) }
            createdInvoiceId?.let { repo.deleteInvoice(it) }
            if (idempotencyKey != null && claimToken != null)
                runCatching { paymentClaimRepo.markFailed(idempotencyKey, claimToken, e.message) }
            auditHelper.audit(AuditAction.PAYMENT_RECORDED, auth, httpReq,
                outcome = AuditOutcome.FAILURE,
                targetType = "subscription", targetId = req.subscriptionId, targetLabel = req.groupeId,
                message = "Échec enregistrement paiement : ${e.message}",
                metadata = mapOf("idempotencyKey" to idempotencyKey,
                    "error" to (e.message ?: e::class.simpleName ?: "unknown")))
            val status = if (e is ResponseStatusException) e.statusCode else HttpStatus.UNPROCESSABLE_ENTITY
            val reason = if (e is ResponseStatusException) e.reason ?: "Échec lors de l'enregistrement du paiement."
            else e.message ?: "Échec lors de l'enregistrement du paiement."
            throw ResponseStatusException(status, reason, e)
        }

        if (idempotencyKey != null && claimToken != null)
            runCatching { paymentClaimRepo.markDone(idempotencyKey, claimToken, receipt.id) }
        ResponseEntity.status(HttpStatus.CREATED).body(receipt)
    }

    @GetMapping("/api/super-admin/payment-receipts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listPaymentReceipts(): ResponseEntity<List<PaymentReceiptResponse>> =
        runBlocking { ResponseEntity.ok(paymentReceiptRepo.listAll()) }

    @GetMapping("/api/super-admin/groupes/{groupeId}/payment-receipts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listGroupePaymentReceipts(@PathVariable groupeId: String): ResponseEntity<List<PaymentReceiptResponse>> =
        runBlocking { ResponseEntity.ok(paymentReceiptRepo.listByGroupe(groupeId)) }

    @DeleteMapping("/api/super-admin/payment-receipts/{receiptId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun deletePaymentReceipt(
        @PathVariable receiptId: String,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<Any> = runBlocking {
        val existing = paymentReceiptRepo.getById(receiptId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        if (!paymentReceiptRepo.deleteById(receiptId)) return@runBlocking ResponseEntity.notFound().build()
        existing.invoiceId?.let { runCatching { repo.deleteInvoice(it) } }
        auditHelper.audit(AuditAction.PAYMENT_DELETED, auth, httpReq,
            targetType = "payment_receipt", targetId = receiptId, targetLabel = existing.groupeId,
            message = "Paiement $receiptId supprimé",
            metadata = mapOf("groupeId" to existing.groupeId,
                "subscriptionId" to existing.subscriptionId,
                "invoiceId" to (existing.invoiceId ?: "")))
        ResponseEntity.noContent().build<Any>()
    }
}
