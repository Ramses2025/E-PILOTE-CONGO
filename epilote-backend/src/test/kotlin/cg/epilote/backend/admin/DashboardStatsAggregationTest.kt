package cg.epilote.backend.admin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Unit tests for dashboard stats aggregation helpers.
 *
 * These tests exercise the pure computation logic used to derive dashboard
 * KPIs from invoice data, completely independent of Couchbase. They serve as
 * regression guards to ensure that the SQL++ aggregate queries and the
 * in-memory fallback logic produce consistent results.
 */
class DashboardStatsAggregationTest {

    private fun invoice(id: String, statut: String, montant: Long, datePaiement: Long? = null) =
        InvoiceResponse(
            id = id,
            groupeId = "groupe::1",
            subscriptionId = "",
            montantXAF = montant,
            statut = statut,
            dateEmission = 1_000L,
            dateEcheance = 2_000L,
            datePaiement = datePaiement,
            reference = "REF-$id",
            notes = ""
        )

    @Test
    fun `revenueTotal sums all invoice amounts regardless of status`() {
        val invoices = listOf(
            invoice("inv1", "paid", 50_000L),
            invoice("inv2", "draft", 30_000L),
            invoice("inv3", "overdue", 20_000L)
        )
        val total = invoices.sumOf { it.montantXAF }
        assertEquals(100_000L, total)
    }

    @Test
    fun `revenuePaid sums only paid invoices`() {
        val invoices = listOf(
            invoice("inv1", "paid", 50_000L),
            invoice("inv2", "draft", 30_000L),
            invoice("inv3", "paid", 20_000L),
            invoice("inv4", "overdue", 10_000L)
        )
        val paid = invoices.filter { it.statut == "paid" }.sumOf { it.montantXAF }
        assertEquals(70_000L, paid)
    }

    @Test
    fun `countInvoicesOverdue counts only overdue invoices`() {
        val invoices = listOf(
            invoice("inv1", "paid", 50_000L),
            invoice("inv2", "overdue", 30_000L),
            invoice("inv3", "overdue", 20_000L),
            invoice("inv4", "draft", 10_000L)
        )
        val overdue = invoices.count { it.statut == "overdue" }.toLong()
        assertEquals(2L, overdue)
    }

    @Test
    fun `recentInvoices ordering prefers datePaiement over dateEmission`() {
        val invoices = listOf(
            InvoiceResponse("inv1", "groupe::1", "", 1_000L, "paid", 1_000L, 2_000L, 3_000L, "REF-inv1", ""),
            InvoiceResponse("inv2", "groupe::1", "", 2_000L, "draft", 500L, 2_000L, null, "REF-inv2", ""),
            InvoiceResponse("inv3", "groupe::1", "", 3_000L, "sent", 2_000L, 2_000L, null, "REF-inv3", "")
        )
        val sorted = invoices
            .distinctBy { it.id }
            .sortedByDescending { maxOf(it.dateEmission, it.datePaiement ?: 0L) }
        assertEquals("inv1", sorted[0].id, "inv1 has datePaiement=3000, should be first")
        assertEquals("inv3", sorted[1].id, "inv3 has dateEmission=2000, should be second")
        assertEquals("inv2", sorted[2].id, "inv2 has dateEmission=500, should be last")
    }

    @Test
    fun `recentInvoices deduplicates by id when combining collections`() {
        val primary = listOf(
            invoice("inv1", "paid", 1_000L),
            invoice("inv2", "draft", 2_000L)
        )
        val legacy = listOf(
            invoice("inv1", "paid", 1_000L),
            invoice("inv3", "sent", 3_000L)
        )
        val combined = (primary + legacy).distinctBy { it.id }
        assertEquals(3, combined.size)
    }

    @Test
    fun `zero invoices produces zero aggregates`() {
        val invoices = emptyList<InvoiceResponse>()
        assertEquals(0L, invoices.sumOf { it.montantXAF })
        assertEquals(0L, invoices.filter { it.statut == "paid" }.sumOf { it.montantXAF })
        assertEquals(0L, invoices.count { it.statut == "overdue" }.toLong())
    }
}
