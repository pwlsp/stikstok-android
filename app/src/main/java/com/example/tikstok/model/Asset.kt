package com.example.tikstok.model

enum class AssetType { STOCK, CRYPTO }

/**
 * A tradeable asset.
 *
 * @param symbol Yahoo Finance ticker used for API calls, e.g. "AAPL" or "BTC-USD".
 * @param ticker Short ticker shown in the UI, e.g. "AAPL" or "BTC".
 * @param name Full display name, e.g. "Apple" or "Bitcoin".
 */
data class Asset(
    val symbol: String,
    val ticker: String,
    val name: String,
    val type: AssetType,
)

/** Static catalog of popular US-market assets the sandbox lets you trade. */
object Assets {
    val stocks = listOf(
        Asset("AAPL", "AAPL", "Apple", AssetType.STOCK),
        Asset("AMZN", "AMZN", "Amazon", AssetType.STOCK),
        Asset("GOOGL", "GOOGL", "Alphabet", AssetType.STOCK),
        Asset("MSFT", "MSFT", "Microsoft", AssetType.STOCK),
        Asset("TSLA", "TSLA", "Tesla", AssetType.STOCK),
        Asset("NVDA", "NVDA", "NVIDIA", AssetType.STOCK),
        Asset("META", "META", "Meta", AssetType.STOCK),
    )

    val crypto = listOf(
        Asset("BTC-USD", "BTC", "Bitcoin", AssetType.CRYPTO),
        Asset("ETH-USD", "ETH", "Ethereum", AssetType.CRYPTO),
        Asset("DOGE-USD", "DOGE", "Dogecoin", AssetType.CRYPTO),
        Asset("SOL-USD", "SOL", "Solana", AssetType.CRYPTO),
    )

    val all = stocks + crypto
    val default = stocks.first()
}
