package io.opentelemetry.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.theme.DemoAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<CoordinatesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            Modifier.padding(all = 20.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CenterText("OpenTelemetry Demo", fontSize = 40.sp)
                        }
//                        Distance(viewModel.distanceState)
                        SessionId(viewModel.sessionIdState)
                        MainOtelButton(
                            painterResource(id = R.drawable.otel_icon),
                            "Click to Begin",
                            onClick = {}
                        )
//                        Disposition()
                    }
                }
            }
        }
    }

}


