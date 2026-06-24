package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterBottomSheet(
    initialStartTime: Long?,
    initialEndTime: Long?,
    initialDateType: DateType = DateType.START,  // Which tab to show initially
    onDismiss: () -> Unit,
    onConfirm: (startTime: Long?, endTime: Long?) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {

    var selectedTab by remember {
        mutableIntStateOf(
            when (initialDateType) {
                DateType.START -> 0
                DateType.END -> 1
            }
        )
    }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialStartTime
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialEndTime
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.date_filter_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tab Row for Start/End selection
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.date_filter_start_date)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.date_filter_end_date)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker based on selected tab
            when (selectedTab) {
                0 -> {
                    DatePicker(
                        state = startDatePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                1 -> {
                    DatePicker(
                        state = endDatePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Validation error message
            val currentStart = startDatePickerState.selectedDateMillis
            val currentEnd = endDatePickerState.selectedDateMillis

            if (currentStart != null && currentEnd != null && currentEnd < currentStart) {
                Text(
                    text = stringResource(R.string.date_filter_error_end_before_start),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }

                Button(
                    onClick = {
                        val finalStart = startDatePickerState.selectedDateMillis
                        val finalEnd = endDatePickerState.selectedDateMillis
                        onConfirm(finalStart, finalEnd)
                    },
                    enabled = !(currentStart != null && currentEnd != null && currentEnd < currentStart),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
