package com.nkapps.nkmuzik.utils

object MediaTitleParser {

    data class ParsedTrack(
        val title: String,
        val artist: String,
    )

    fun parse(rawTitle: String?, rawUploaderOrArtist: String?): ParsedTrack {
        val titleClean = rawTitle?.trim() ?: ""
        val rawUploader = rawUploaderOrArtist?.trim() ?: ""
        val uploaderClean = cleanUploaderChannelName(rawUploader)

        if (titleClean.isBlank() && uploaderClean.isBlank()) {
            return ParsedTrack("Bilinmeyen Parça", "Sanatçı")
        }

        if (titleClean.isBlank()) {
            val artist = cleanArtistName(uploaderClean)
            return ParsedTrack("Bilinmeyen Parça", if (artist.isBlank() || isUploaderChannelName(artist) || isUnknownArtist(artist)) "Sanatçı" else artist)
        }

        val separators = listOf(" - ", " – ", " — ", " | ", " : ")
        var parts = listOf<String>()
        var found = false

        for (sep in separators) {
            if (titleClean.contains(sep)) {
                parts = titleClean.split(sep, limit = 2)
                found = true
                break
            }
        }

        if (!found) {
            if (titleClean.contains(" -")) {
                parts = titleClean.split(" -", limit = 2)
                found = true
            } else if (titleClean.contains("- ")) {
                parts = titleClean.split("- ", limit = 2)
                found = true
            }
        }

        val candidateArtist: String
        val candidateTitle: String

        if (found && (parts.size == 2)) {
            val part1 = parts[0].trim()
            val part2 = parts[1].trim()

            val p1Clean = cleanArtistName(part1)
            val p1IsChannel = isUploaderChannelName(p1Clean) || (uploaderClean.isNotBlank() && p1Clean.equals(uploaderClean, ignoreCase = true))

            if (p1IsChannel && hasSeparator(part2)) {
                val subParts = splitBySeparator(part2)
                if (subParts.size == 2) {
                    candidateArtist = cleanArtistName(subParts[0])
                    candidateTitle = cleanSongTitle(subParts[1], uploaderClean)
                } else {
                    candidateArtist = cleanArtistName(part1)
                    candidateTitle = cleanSongTitle(part2, uploaderClean)
                }
            } else {
                candidateArtist = cleanArtistName(part1)
                candidateTitle = cleanSongTitle(part2, uploaderClean)
            }
        } else {
            candidateTitle = cleanSongTitle(titleClean, uploaderClean)
            candidateArtist = if (uploaderClean.isNotBlank() && !isDownloadedMarker(uploaderClean)) {
                cleanArtistName(uploaderClean)
            } else {
                inferArtistFromUploader(uploaderClean)
            }
        }

        val finalArtist = when {
            candidateArtist.isNotBlank() && !isUnknownArtist(candidateArtist) -> candidateArtist

            uploaderClean.isNotBlank() &&
            !isUnknownArtist(uploaderClean) &&
            !isDownloadedMarker(uploaderClean) -> uploaderClean

            else -> "Sanatçı"
        }

        val finalTitle = candidateTitle.ifBlank { titleClean }

        return ParsedTrack(title = finalTitle, artist = finalArtist)
    }

    private fun hasSeparator(text: String): Boolean {
        val separators = listOf(" - ", " – ", " — ", " | ", " : ", " -", "- ")
        return separators.any { text.contains(it) }
    }

    private fun splitBySeparator(text: String): List<String> {
        val separators = listOf(" - ", " – ", " — ", " | ", " : ")
        for (sep in separators) {
            if (text.contains(sep)) {
                return text.split(sep, limit = 2).map { it.trim() }
            }
        }
        if (text.contains(" -")) return text.split(" -", limit = 2).map { it.trim() }
        if (text.contains("- ")) return text.split("- ", limit = 2).map { it.trim() }
        return listOf(text)
    }

    private fun cleanSongTitle(title: String, uploaderName: String = ""): String {
        if (title.isBlank()) return ""
        var cleaned = title

        val noisePatterns = listOf(
            Regex("(?i)\\s*\\(official\\s*(music\\s*)?video\\s*\\)"),
            Regex("(?i)\\s*\\[official\\s*(music\\s*)?video\\]"),
            Regex("(?i)\\s*\\(official\\s*audio\\s*\\)"),
            Regex("(?i)\\s*\\[official\\s*audio\\]"),
            Regex("(?i)\\s*\\(official\\s*lyrics?\\s*(video)?\\s*\\)"),
            Regex("(?i)\\s*\\[official\\s*lyrics?\\s*(video)?\\]"),
            Regex("(?i)\\s*\\(lyric\\s*video\\s*\\)"),
            Regex("(?i)\\s*\\[lyric\\s*video\\]"),
            Regex("(?i)\\s*\\(lyrics?\\s*\\)"),
            Regex("(?i)\\s*\\[lyrics?\\]"),
            Regex("(?i)\\s*\\(visualizer\\s*\\)"),
            Regex("(?i)\\s*\\[visualizer\\]"),
            Regex("(?i)\\s*\\(audio\\s*\\)"),
            Regex("(?i)\\s*\\[audio\\]"),
            Regex("(?i)\\s*\\(hq\\s*/\\s*hd\\s*\\)"),
            Regex("(?i)\\s*\\(hd\\s*/\\s*hq\\s*\\)"),
            Regex("(?i)\\s*\\(hd\\s*\\)"),
            Regex("(?i)\\s*\\[hd\\]"),
            Regex("(?i)\\s*\\(hq\\s*\\)"),
            Regex("(?i)\\s*\\[hq\\]"),
            Regex("(?i)\\s*\\(live(\\s*performance)?\\s*\\)"),
            Regex("(?i)\\s*\\[live(\\s*performance)?\\]"),
            Regex("(?i)\\s*\\(video\\s*clip\\)"),
            Regex("(?i)\\s*\\[video\\s*clip\\]"),
            Regex("(?i)\\s*\\(clip\\)"),
            Regex("(?i)\\s*\\[clip\\]"),
            Regex("(?i)\\s*\\(klip\\)"),
            Regex("(?i)\\s*\\[klip\\]"),
            Regex("(?i)\\s*\\(klipsiz\\)"),
            Regex("(?i)\\s*\\[klipsiz\\]"),
            Regex("(?i)\\s*\\(offical\\s*video\\)")
        )
        for (pattern in noisePatterns) {
            cleaned = cleaned.replace(pattern, "")
        }

        if (uploaderName.isNotBlank()) {
            val escapedUploader = Regex.escape(uploaderName)
            cleaned = cleaned.replace(Regex("(?i)\\s*\\(\\s*$escapedUploader\\s*\\)"), "")
            cleaned = cleaned.replace(Regex("(?i)\\s*\\[\\s*$escapedUploader\\s*\\]"), "")
        }

        val publisherKeywords = "müzik|music|records?|media|production|productions?|yapım|film|tv|channel|kanal|distribution|entertainment|label"
        cleaned = cleaned.replace(Regex("(?i)\\s*\\([^)]*\\b($publisherKeywords)\\b[^)]*\\)"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s*\\[[^\\]]*\\b($publisherKeywords)\\b[^\\]]*\\]"), "")

        cleaned = cleaned.replace(Regex("\\(\\s*\\)"), "")
        cleaned = cleaned.replace(Regex("\\[\\s*\\]"), "")

        return cleaned.trim()
    }

    private fun cleanArtistName(rawArtist: String): String {
        var artist = rawArtist.trim()
        artist = cleanUploaderChannelName(artist)
        return artist
    }

    private fun isDownloadedMarker(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.trim().lowercase()
        val legacyTag = String(byteArrayOf(121, 111, 117, 116, 117, 98, 101, 32, 105, 110, 100, 105, 114, 105, 108, 101, 110), Charsets.UTF_8)
        return lower == "çevrimiçi indirilen" ||
                lower == "online indirilen" ||
                lower == "indirilen" ||
                lower == legacyTag
    }

    fun isUploaderChannelName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith("vevo") ||
                lower.contains(" - topic") ||
                lower.endsWith(" topic") ||
                lower.contains(" official channel") ||
                lower.contains(" music channel") ||
                isDownloadedMarker(lower)
    }

    private fun cleanUploaderChannelName(name: String): String {
        if (name.isBlank()) return ""
        var cleaned = name
        cleaned = cleaned.replace(Regex("(?i)\\s*-\\s*Topic\\b"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s+Topic\\b"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s*VEVO\\b"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s*Official\\s*Channel\\b"), "")
        cleaned = cleaned.replace(Regex("(?i)\\s*Official\\s*Music\\s*Channel\\b"), "")
        return cleaned.trim()
    }

    private fun inferArtistFromUploader(uploader: String): String {
        val cleaned = cleanUploaderChannelName(uploader)
        if (cleaned.isBlank() || isUploaderChannelName(cleaned) || isUnknownArtist(cleaned) || isDownloadedMarker(cleaned)) {
            return "Sanatçı"
        }
        return cleaned
    }

    private fun isUnknownArtist(name: String): Boolean {
        val lower = name.trim().lowercase()
        return lower.isBlank() ||
                lower == "<unknown>" ||
                lower == "unknown" ||
                lower == "unknown artist" ||
                lower == "bilinmeyen sanatçı"
    }
}
