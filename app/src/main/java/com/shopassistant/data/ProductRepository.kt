package com.shopassistant.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.shopassistant.model.PackagingLevel
import com.shopassistant.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ProductRepository(private val helper: ShopDatabase) {

    private val _changes = MutableStateFlow(0)

    /** Emits a new value every time data is saved or deleted, so screens can reload. */
    val changes: StateFlow<Int> = _changes

    suspend fun getAll(): List<Product> = withContext(Dispatchers.IO) {
        readProducts(helper.readableDatabase, null)
    }

    suspend fun get(id: Long): Product? = withContext(Dispatchers.IO) {
        readProducts(helper.readableDatabase, id).firstOrNull()
    }

    /** Inserts when [Product.id] is 0, otherwise updates. Returns the product id. */
    suspend fun save(product: Product): Long = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        var savedId = product.id
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("name", product.name)
                put("purchase_price", product.purchasePrice.toPlainString())
                if (product.sellingPrice == null) putNull("selling_price")
                else put("selling_price", product.sellingPrice.toPlainString())
                put("updated_at", now)
            }
            if (product.id == 0L) {
                values.put("created_at", now)
                savedId = db.insertOrThrow("products", null, values)
            } else {
                db.update("products", values, "id = ?", arrayOf(product.id.toString()))
                db.delete("product_levels", "product_id = ?", arrayOf(product.id.toString()))
            }
            product.levels.forEachIndexed { index, level ->
                val row = ContentValues().apply {
                    put("product_id", savedId)
                    put("position", index)
                    put("name", level.name)
                    put("quantity", level.quantityPerParent.toPlainString())
                }
                db.insertOrThrow("product_levels", null, row)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _changes.update { it + 1 }
        savedId
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("products", "id = ?", arrayOf(id.toString()))
        _changes.update { it + 1 }
        Unit
    }

    private fun readLevels(db: SQLiteDatabase, productId: Long?): Map<Long, List<PackagingLevel>> {
        val map = LinkedHashMap<Long, MutableList<PackagingLevel>>()
        val selection = if (productId != null) "product_id = ?" else null
        val args = if (productId != null) arrayOf(productId.toString()) else null
        db.query(
            "product_levels",
            arrayOf("product_id", "name", "quantity"),
            selection, args, null, null,
            "product_id ASC, position ASC"
        ).use { c ->
            while (c.moveToNext()) {
                map.getOrPut(c.getLong(0)) { mutableListOf() }
                    .add(PackagingLevel(c.getString(1), BigDecimal(c.getString(2))))
            }
        }
        return map
    }

    private fun readProducts(db: SQLiteDatabase, id: Long?): List<Product> {
        val levels = readLevels(db, id)
        val selection = if (id != null) "id = ?" else null
        val args = if (id != null) arrayOf(id.toString()) else null
        val result = mutableListOf<Product>()
        db.query(
            "products",
            arrayOf("id", "name", "purchase_price", "selling_price"),
            selection, args, null, null,
            "name COLLATE NOCASE ASC"
        ).use { c ->
            while (c.moveToNext()) {
                val productId = c.getLong(0)
                val productLevels = levels[productId] ?: continue
                result.add(
                    Product(
                        id = productId,
                        name = c.getString(1),
                        purchasePrice = BigDecimal(c.getString(2)),
                        levels = productLevels,
                        sellingPrice = if (c.isNull(3)) null else BigDecimal(c.getString(3))
                    )
                )
            }
        }
        return result
    }
}
