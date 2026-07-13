package cut.the.crap.ui.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.search_cd_search
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.MySearchBar(
    query: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    placeHolder: String? = null,
    onQueryChanged: (String) -> Unit,
    onQuerySubmit: (String) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    shape: Shape = RoundedCornerShape(50),
    handleBackButton: Boolean = true,
) {
    if (handleBackButton) {
        BackHandler(enabled = expanded) {
            onExpandedChanged(false)
            onQueryChanged("")
        }
    }

    if (expanded) {
            ExpandedSearchBar(
                query = query,
                placeHolderText = placeHolder,
                onQueryChanged = onQueryChanged,
                onQuerySubmit = onQuerySubmit,
                onClose = { onExpandedChanged(false) },
                modifier = modifier.weight(1f),
                shape = shape

            )
    } else {
        IconButton(
            onClick = {
                onExpandedChanged(true)
            }
        ) {
            Icon(imageVector = Icons.Filled.Search,
                contentDescription = null)
        }
    }
}

@Composable
private fun ExpandedSearchBar(
    query: String,
    placeHolderText: String? = null,
    shape: Shape,
    onQueryChanged: (String) -> Unit,
    onQuerySubmit: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val focusRequester = remember {
        FocusRequester()
    }
    SideEffect {
        if (query.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier.focusRequester(focusRequester),
        shape = shape,
        textStyle = TextStyle(fontSize = 18.sp),
        singleLine = true,
        placeholder = handlePlaceHolder(placeHolderText),
        leadingIcon = {
            IconButton(
                onClick = {
                    if (query.isEmpty()) {
                        onClose()
                    } else {
                        onQueryChanged("")
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.search_cd_search),
                    modifier = Modifier.clickable { onQueryChanged(query) }
                )
            }


        },
        trailingIcon = {
            IconButton(
                onClick = {
                    if (query.isEmpty()) {
                        onClose()
                    } else {
                        onQueryChanged("")
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (query.isEmpty()) Icons.Default.Close else Icons.Default.Clear,
                    contentDescription = ""
                )
            }
        },
        colors = TextFieldDefaults.colors(
            //containerColor = Color.White,
            focusedIndicatorColor = Color.Transparent, // Remove the underline when focused
            unfocusedIndicatorColor = Color.Transparent // Remove the underline when not focused
        )
    )
}


private fun handlePlaceHolder(string: String?): @Composable (() -> Unit)? {
    return if (string != null) {
        {
            Text(text = string)
        }
    } else {
        null
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview() {
    Column {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            MySearchBar(query = "Search",
                expanded = true, onQueryChanged = {},
                onQuerySubmit = {}, onExpandedChanged ={} )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            MySearchBar(query = "Find",
                expanded = false, onQueryChanged = {},
                onQuerySubmit = {}, onExpandedChanged ={} )
        }

    }

}
