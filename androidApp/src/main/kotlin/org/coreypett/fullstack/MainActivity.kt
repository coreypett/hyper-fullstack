package org.coreypett.fullstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.coreypett.fullstack.di.SharedDependencyGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        SharedDependencyGraph.start()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val orderBookFeature = SharedDependencyGraph.orderBookFeature()
        setContent {
            App(orderBookFeature = orderBookFeature)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
