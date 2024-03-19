package com.elee.eleeuilab

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.elee.eleeuilab.widget.ShakeableLayerDemoActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.tv_shakeable_layer_entry).setOnClickListener {
            startActivity(Intent(this, ShakeableLayerDemoActivity::class.java))
        }
    }
}