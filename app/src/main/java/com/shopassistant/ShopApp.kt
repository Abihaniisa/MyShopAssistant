package com.shopassistant

import android.app.Application
import com.shopassistant.data.ProductRepository
import com.shopassistant.data.ShopDatabase

class ShopApp : Application() {
    val repository: ProductRepository by lazy { ProductRepository(ShopDatabase(this)) }
}
