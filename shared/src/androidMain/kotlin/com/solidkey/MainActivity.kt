package com.solidkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.solidkey.demo.DemoApp
import com.solidkey.demo.DemoAssets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            DemoAssets.vecLion20k = assets.open("vec_lion_20k.json").bufferedReader().use { it.readText() }
            DemoAssets.vecLion11k = assets.open("vec_lion_11k.json").bufferedReader().use { it.readText() }
        }
        setContent {
            DemoApp()
        }
    }
}
