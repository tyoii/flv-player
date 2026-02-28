package com.tyoii.flvplayer.data.model

data class DriveFile(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: Long,
    val mimeType: String,
    val parentId: String?,
    val isFolder: Boolean
) {
    /** Parse bililive-go filename: [date time][host][title].flv */
    val parsedInfo: RecordingInfo? by lazy {
        val regex = """\[(\d{4}-\d{2}-\d{2} \d{2}-\d{2}-\d{2})\]\[(.+?)\]\[(.+?)\]\.flv""".toRegex()
        regex.find(name)?.let { match ->
            RecordingInfo(
                date = match.groupValues[1],
                hostName = match.groupValues[2],
                title = match.groupValues[3]
            )
        }
    }

    val displayName: String
        get() = parsedInfo?.let { "${it.hostName} - ${it.title}" } ?: name

    val sizeFormatted: String
        get() {
            val mb = size / (1024.0 * 1024.0)
            return if (mb >= 1024) String.format("%.1f GB", mb / 1024)
            else String.format("%.0f MB", mb)
        }
}

data class RecordingInfo(
    val date: String,
    val hostName: String,
    val title: String
)
