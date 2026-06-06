package com.example.tikstok.model

import androidx.annotation.DrawableRes
import com.example.tikstok.R

enum class AssetType { STOCK, CRYPTO }

data class Asset(
    val symbol: String,
    val ticker: String,
    val name: String,
    val type: AssetType,
    @param:DrawableRes val iconRes: Int,
)

object Assets {
    val stocks = listOf(
        Asset("AAPL", "AAPL", "Apple", AssetType.STOCK, R.drawable.ic_apple),
        Asset("GOOGL", "GOOGL", "Alphabet", AssetType.STOCK, R.drawable.ic_google),
        Asset("META", "META", "Meta", AssetType.STOCK, R.drawable.ic_meta),
        Asset("NVDA", "NVDA", "NVIDIA", AssetType.STOCK, R.drawable.ic_nvidia),
        Asset("TSLA", "TSLA", "Tesla", AssetType.STOCK, R.drawable.ic_tesla),
        Asset("AMD", "AMD", "AMD", AssetType.STOCK, R.drawable.ic_amd),
        Asset("MCD", "MCD", "McDonald's", AssetType.STOCK, R.drawable.ic_mcdonalds),
    )

    val crypto = listOf(
        Asset("BTC-USD", "BTC", "Bitcoin", AssetType.CRYPTO, R.drawable.ic_bitcoin),
        Asset("ETH-USD", "ETH", "Ethereum", AssetType.CRYPTO, R.drawable.ic_ethereum),
        Asset("DOGE-USD", "DOGE", "Dogecoin", AssetType.CRYPTO, R.drawable.ic_dogecoin),
        Asset("SOL-USD", "SOL", "Solana", AssetType.CRYPTO, R.drawable.ic_solana),
        Asset("XRP-USD", "XRP", "XRP", AssetType.CRYPTO, R.drawable.ic_xrp),
        Asset("ADA-USD", "ADA", "Cardano", AssetType.CRYPTO, R.drawable.ic_cardano),
        Asset("DOT-USD", "DOT", "Polkadot", AssetType.CRYPTO, R.drawable.ic_polkadot),
    )

    val all = stocks + crypto
    val default = stocks.first()

    fun bySymbol(symbol: String): Asset? = all.firstOrNull { it.symbol == symbol }
}
