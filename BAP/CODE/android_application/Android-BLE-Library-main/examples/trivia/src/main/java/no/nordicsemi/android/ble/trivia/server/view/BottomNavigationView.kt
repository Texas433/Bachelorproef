package no.nordicsemi.android.ble.trivia.server.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.ble.trivia.R
import no.nordicsemi.android.common.theme.NordicTheme

@Composable
fun BottomNavigationView(
    onNextClick: () -> Unit,
    isTimerRunning: Boolean
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = onNextClick,
        enabled = !isTimerRunning
    ) {
        Text(text = stringResource(id = R.string.next))
    }
}

@Preview
@Composable
fun BottomNavigationView_Preview() {
    NordicTheme {
        BottomNavigationView(
            onNextClick = { },
            isTimerRunning = true,
        )
    }
}