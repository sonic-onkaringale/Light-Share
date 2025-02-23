package utils

// Sanitize filenames
fun String.sanitizeFileName(): String
{
    return this.replace("[^A-Za-z0-9._-]".toRegex(), "_")
}