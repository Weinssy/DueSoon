package com.duesoon.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duesoon.app.R
import com.duesoon.app.domain.model.CategoryPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList()
) {
    val presetLabels = CategoryPreset.entries.map { it.label.lowercase() }.toSet()
    val trimmedInput = selectedCategory.trim()
    val filteredSuggestions = suggestions
        .map { it.trim() }
        .filter { candidate ->
            candidate.isNotEmpty() &&
                !presetLabels.contains(candidate.lowercase()) &&
                !candidate.equals(trimmedInput, ignoreCase = true) &&
                (trimmedInput.isEmpty() || candidate.contains(trimmedInput, ignoreCase = true))
        }
        .distinctBy { it.lowercase() }
        .take(6)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.label_category_optional),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CategoryPreset.entries.forEach { preset ->
                val isSelected = selectedCategory.equals(preset.label, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCategorySelected(if (isSelected) "" else preset.label)
                    },
                    label = {
                        Text(
                            stringResource(preset.labelResId),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = preset.color.copy(alpha = 0.85f),
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = preset.color,
                        borderColor = MaterialTheme.colorScheme.outline,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }

        OutlinedTextField(
            value = selectedCategory,
            onValueChange = onCategorySelected,
            label = { Text(stringResource(R.string.label_course_or_category_input)) },
            placeholder = { Text(stringResource(R.string.placeholder_course_or_category)) },
            singleLine = true,
            trailingIcon = {
                if (selectedCategory.isNotEmpty()) {
                    IconButton(onClick = { onCategorySelected("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_clear_category)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredSuggestions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.label_course_suggestions),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredSuggestions.forEach { course ->
                        SuggestionChip(
                            onClick = { onCategorySelected(course) },
                            label = { Text(course, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    }
}
