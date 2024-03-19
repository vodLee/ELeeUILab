package com.elee.eleeuilab.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.elee.eleeuilab.R

class ShakeableLayerDemoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shakeable_layer_demo)
        lifecycle.addObserver(findViewById<ShakeableLayerLayout>(R.id.layout_shake))
    }
}