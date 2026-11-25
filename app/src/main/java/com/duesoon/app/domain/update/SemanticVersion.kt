package com.duesoon.app.domain.update

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // Stable release is newer than a pre-release of the same major.minor.patch
        if (preRelease == null && other.preRelease != null) return 1
        if (preRelease != null && other.preRelease == null) return -1
        if (preRelease == null && other.preRelease == null) return 0
        return preRelease!!.compareTo(other.preRelease!!, ignoreCase = true)
    }

    companion object {
        private val SEMVER_REGEX = Regex(
            """^[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.\-]+))?(?:\+[0-9A-Za-z.\-]+)?$"""
        )

        fun parseOrNull(raw: String?): SemanticVersion? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            val match = SEMVER_REGEX.matchEntire(trimmed) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: return null
            val patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return null
            val pre = match.groupValues[4].takeIf { it.isNotBlank() }
            return SemanticVersion(major, minor, patch, pre)
        }

        fun isNewer(candidate: String?, current: String?): Boolean {
            val parsedCandidate = parseOrNull(candidate) ?: return false
            val parsedCurrent = parseOrNull(current) ?: return false
            return parsedCandidate > parsedCurrent
        }
    }
}
