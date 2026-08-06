package com.scs3311.smart_home_monitoring_app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.scs3311.smart_home_monitoring_app.ui.navigation.SmartHomeNavGraph
import com.scs3311.smart_home_monitoring_app.ui.theme.SmarthomemonitoringappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val testReference = FirebaseDatabase.getInstance()
            .getReference("connectionTest")

        testReference.child("lastAndroidWrite")
            .setValue(System.currentTimeMillis())

        testReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseTest", "Data received: ${snapshot.value}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseTest", "Firebase error: ${error.message}")
            }
        })

        setContent {
            SmarthomemonitoringappTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartHomeNavGraph()
                }
            }
        }
    }
}
