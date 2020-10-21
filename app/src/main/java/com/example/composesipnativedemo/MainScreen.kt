package com.example.composesipnativedemo

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.example.composesipnativedemo.ui.ComposeSipNativeDemoTheme

@Composable
fun MainScreen(
    currentUser: String = "",
    logs: List<String> = emptyList(),
    needsToScroll: Boolean = false,
    changeUser: () -> Unit = {},
    start: () -> Unit = {},
    stop: () -> Unit = {},
    resetNeedsToScroll: () -> Unit = {},
    clearLogs: () -> Unit = {},
    makeCall: () -> Unit = {},
) {
    ComposeSipNativeDemoTheme {
        Scaffold {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(text = "Current user: $currentUser", modifier = Modifier.padding(8.dp))
                    Button(onClick = changeUser, modifier = Modifier.padding(8.dp)) {
                        Text(text = "Change User")
                    }
                }

                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = start, modifier = Modifier.padding(8.dp)) {
                        Text(text = "Start")
                    }
                    Button(onClick = stop, modifier = Modifier.padding(8.dp)) {
                        Text(text = "Stop")
                    }
                }


                Button(onClick = makeCall, modifier = Modifier.padding(8.dp)) {
                    Text(text = "Call")
                }

                LogsZone(
                    logs = logs,
                    needsToScroll = needsToScroll,
                    resetNeedsToScroll = resetNeedsToScroll,
                    clearLogs = clearLogs,
                    modifier = Modifier.fillMaxHeight(fraction = 20f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LogsZone(
    logs: List<String>,
    needsToScroll: Boolean,
    resetNeedsToScroll: () -> Unit,
    clearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Logs", modifier = Modifier.padding(8.dp))
            Button(onClick = clearLogs, modifier = Modifier.padding(8.dp)) {
                Text(text = "Clear")
            }
        }
        Divider(modifier = Modifier.fillMaxWidth(), color = Color.Black, thickness = 4.dp)
        Logs(
            logs = logs,
            needsToScroll = needsToScroll,
            resetNeedsToScroll = resetNeedsToScroll,
        )
    }
}

@Composable
fun Logs(
    logs: List<String>,
    needsToScroll: Boolean,
    resetNeedsToScroll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    ScrollableColumn(scrollState = scrollState, modifier = modifier) {
        logs.forEach { log ->
            Text(log)
        }
    }
    if (needsToScroll) {
        scrollState.smoothScrollTo(logs.size.toFloat() * 100)
        resetNeedsToScroll()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}