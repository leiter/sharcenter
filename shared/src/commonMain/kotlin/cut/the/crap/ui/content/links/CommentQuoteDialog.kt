package cut.the.crap.ui.content.links

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.links_comment
import cut.the.crap.shared.resources.links_comment_quote_label
import cut.the.crap.shared.resources.links_comment_quote_placeholder
import cut.the.crap.shared.resources.links_comment_quote_title
import cut.the.crap.shared.resources.links_quote
import androidx.compose.ui.window.Dialog
import cut.the.crap.data.domain.ContentLink
//import cut.the.crap.mockedLinkItems
import cut.the.crap.tools.formatTimestampWithLocalizedFormatter
import cut.the.crap.tools.prepareUrlInformation

/**
 * Dialog for creating a comment or quote based on a ContentLink
 */
@Composable
fun CommentQuoteDialog(
    contentLink: ContentLink,
    onDismiss: () -> Unit,
    onComment: (ContentLink, String) -> Unit,
    onQuote: (ContentLink, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textInput by remember { mutableStateOf("") }
    val texts = prepareUrlInformation(contentLink.link)
    val formattedDate = remember(contentLink.added) {
        formatTimestampWithLocalizedFormatter(contentLink.added)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = stringResource(Res.string.links_comment_quote_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reference Card (similar to ContentLinkItem but read-only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Link info display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = texts[0], // Domain
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = texts[1], // Username/path
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = texts[2].takeLast(5), // ID
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        if (contentLink.description.isNotEmpty()) {
                            Text(
                                text = contentLink.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Date
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text Input Field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text(stringResource(Res.string.links_comment_quote_label)) },
                    placeholder = { Text(stringResource(Res.string.links_comment_quote_placeholder)) },
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { onQuote(contentLink, textInput) },
                        enabled = textInput.isNotBlank()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.FormatQuote,
                                contentDescription = stringResource(Res.string.links_quote),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(stringResource(Res.string.links_quote))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { onComment(contentLink, textInput) },
                        enabled = textInput.isNotBlank()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = stringResource(Res.string.links_comment),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(stringResource(Res.string.links_comment))
                        }
                    }
                }
            }
        }
    }
}

///**
// * Preview for CommentQuoteDialog with various states
// */
//@Preview(showBackground = true, device = Devices.PIXEL_4)
//@Composable
//private fun Preview(
//    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
//) {
//    theme {
//        // Render the dialog content directly without Dialog wrapper for preview
//        val contentLink = mockedLinkItems.first()
//        var textInput by remember { mutableStateOf("") }
//        val texts = prepareUrlInformation(contentLink.link)
//        val formattedDate = remember(contentLink.added) {
//            formatTimestampWithLocalizedFormatter(contentLink.added)
//        }
//
//        Surface(
//            modifier = Modifier.fillMaxWidth(),
//            shape = MaterialTheme.shapes.large,
//            tonalElevation = 6.dp,
//            color = MaterialTheme.colorScheme.surface
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(24.dp)
//            ) {
//                // Title
//                Text(
//                    text = stringResource(Res.string.links_comment_quote_title),
//                    style = MaterialTheme.typography.headlineSmall,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Reference Card (similar to ContentLinkItem but read-only)
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(12.dp)
//                    ) {
//                        // Link info display
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                text = texts[0], // Domain
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.primary
//                            )
//                            Text(
//                                text = texts[1], // Username/path
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Text(
//                                text = texts[2].takeLast(5), // ID
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        // Description
//                        if (contentLink.description.isNotEmpty()) {
//                            Text(
//                                text = contentLink.description,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                        }
//
//                        // Date
//                        Text(
//                            text = formattedDate,
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Text Input Field
//                OutlinedTextField(
//                    value = textInput,
//                    onValueChange = { textInput = it },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(120.dp),
//                    label = { Text(stringResource(Res.string.links_comment_quote_label)) },
//                    placeholder = { Text(stringResource(Res.string.links_comment_quote_placeholder)) },
//                    maxLines = 5,
//                    textStyle = MaterialTheme.typography.bodyMedium
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Action Buttons
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    TextButton(onClick = {}) {
//                        Text(stringResource(Res.string.dialog_cancel))
//                    }
//
//                    Spacer(modifier = Modifier.width(8.dp))
//
//                    TextButton(
//                        onClick = {},
//                        enabled = textInput.isNotBlank()
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(4.dp)
//                        ) {
//                            androidx.compose.material3.Icon(
//                                imageVector = Icons.Filled.FormatQuote,
//                                contentDescription = stringResource(Res.string.links_quote),
//                                modifier = Modifier.padding(end = 4.dp)
//                            )
//                            Text(stringResource(Res.string.links_quote))
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.width(8.dp))
//
//                    TextButton(
//                        onClick = {},
//                        enabled = textInput.isNotBlank()
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(4.dp)
//                        ) {
//                            androidx.compose.material3.Icon(
//                                imageVector = Icons.AutoMirrored.Filled.Comment,
//                                contentDescription = stringResource(Res.string.links_comment),
//                                modifier = Modifier.padding(end = 4.dp)
//                            )
//                            Text(stringResource(Res.string.links_comment))
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
