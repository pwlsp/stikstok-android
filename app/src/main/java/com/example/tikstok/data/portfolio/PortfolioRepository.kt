package com.example.tikstok.data.portfolio

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed persistence for the portfolio — the source of truth for profiles and their
 * history (Room only caches market data). Layout:
 *
 * ```
 * users/{uid}                       nickname, createdAt, activeProfileId
 *   profiles/{profileId}            id, name, startingBalance, cash, createdAt,
 *                                   holdings[], transactions[], cashEntries[]
 * ```
 *
 * Each profile is one document holding its whole state as arrays, so a trade is a single write.
 * Firestore's on-device persistence (enabled by default on Android) makes reads and queued writes
 * work offline; [PortfolioStore] keeps the live in-memory copy the UI reads.
 */
object PortfolioRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun profilesCol(uid: String) = userDoc(uid).collection("profiles")

    /** Everything needed to rebuild the in-memory store for a signed-in user. */
    data class AccountSnapshot(
        val nickname: String?,
        val createdAt: Long?,
        val activeProfileId: Long?,
        val profiles: List<Profile>,
    )

    suspend fun load(uid: String): AccountSnapshot {
        val user = userDoc(uid).get().await()
        val profiles = profilesCol(uid).get().await()
            .documents
            .mapNotNull { it.toProfile() }
            .sortedBy { it.createdAt }
        return AccountSnapshot(
            nickname = user.getString("nickname"),
            createdAt = user.getLong("createdAt"),
            activeProfileId = user.getLong("activeProfileId"),
            profiles = profiles,
        )
    }

    /** Upserts the account-level fields (merged, so it never clobbers the profiles subcollection). */
    suspend fun saveAccount(uid: String, nickname: String, createdAt: Long, activeProfileId: Long) {
        userDoc(uid).set(
            mapOf(
                "nickname" to nickname,
                "createdAt" to createdAt,
                "activeProfileId" to activeProfileId,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun saveProfile(uid: String, profile: Profile) {
        profilesCol(uid).document(profile.id.toString()).set(profile.toMap()).await()
    }

    suspend fun deleteProfile(uid: String, profileId: Long) {
        profilesCol(uid).document(profileId.toString()).delete().await()
    }

    // --- (de)serialization ----------------------------------------------------------------------

    private fun Profile.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "startingBalance" to startingBalance,
        "cash" to cash,
        "createdAt" to createdAt,
        "holdings" to holdings.map { (symbol, h) ->
            mapOf("symbol" to symbol, "quantity" to h.quantity, "avgCost" to h.avgCost)
        },
        "transactions" to transactions.map {
            mapOf(
                "symbol" to it.symbol,
                "type" to it.type.name,
                "quantity" to it.quantity,
                "unitPrice" to it.unitPrice,
                "amount" to it.amount,
                "timestamp" to it.timestamp,
            )
        },
        "cashEntries" to cashEntries.map {
            mapOf("type" to it.type.name, "amount" to it.amount, "timestamp" to it.timestamp)
        },
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toProfile(): Profile? {
        val id = getLong("id") ?: id.toLongOrNull() ?: return null
        val startingBalance = getDouble("startingBalance") ?: 0.0
        val createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        val profile = Profile(
            id = id,
            name = getString("name") ?: "profile",
            startingBalance = startingBalance,
            createdAt = createdAt,
        )
        profile.cash = getDouble("cash") ?: startingBalance

        // The constructor seeds an initial deposit entry; replace it with whatever was persisted.
        profile.holdings.clear()
        (get("holdings") as? List<Map<String, Any?>>)?.forEach { m ->
            val symbol = m["symbol"] as? String ?: return@forEach
            profile.holdings[symbol] = Holding(
                quantity = (m["quantity"] as? Number)?.toDouble() ?: 0.0,
                avgCost = (m["avgCost"] as? Number)?.toDouble() ?: 0.0,
            )
        }
        profile.transactions.clear()
        (get("transactions") as? List<Map<String, Any?>>)?.forEach { m ->
            profile.transactions.add(
                Transaction(
                    symbol = m["symbol"] as? String ?: return@forEach,
                    type = enumValueOrNull<TransactionType>(m["type"]) ?: TransactionType.BUY,
                    quantity = (m["quantity"] as? Number)?.toDouble() ?: 0.0,
                    unitPrice = (m["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    timestamp = (m["timestamp"] as? Number)?.toLong() ?: 0L,
                ),
            )
        }
        profile.cashEntries.clear()
        (get("cashEntries") as? List<Map<String, Any?>>)?.forEach { m ->
            profile.cashEntries.add(
                CashEntry(
                    type = enumValueOrNull<CashFlowType>(m["type"]) ?: CashFlowType.DEPOSIT,
                    amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    timestamp = (m["timestamp"] as? Number)?.toLong() ?: 0L,
                ),
            )
        }
        return profile
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: Any?): T? =
        (value as? String)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
