package com.duesoon.app.domain.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object BackupSerializer {
    /**
     * Standard JSON configuration for portable backup and export.
     * 
     * - prettyPrint = true: Ensures the exported JSON is readable and easy to manually inspect or diff.
     * - ignoreUnknownKeys = true: Forward compatibility. If a newer backup version has new fields, older apps can still parse it safely.
     * - explicitNulls = false: Saves space by omitting null fields. Missing fields fall back to default values during parsing.
     * - encodeDefaults = true: Ensures all boolean flags and default required fields are always present in the JSON contract.
     */
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}
