package com.steamclock.versioncheckkotlin.models

import com.steamclock.versioncheckkotlin.VersionCheckException
import java.lang.Exception
import kotlin.math.max

class Version(string: String): Comparable<Version> {

    var marketingComponents: IntArray = intArrayOf()
        private set

    var build: Int? = null
        private set

    var additionalText: String = ""
        private set

    var isDevelopmemnt: Boolean = false
        private set

    val description: String
        get() {
            val version = marketingComponents.joinToString(".") { it.toString() }
            val build = if (build == null) "" else "@$build"
            return "$version$additionalText$build"
        }

    //---------------------------------------------------------------------------------
    // Initialization
    //---------------------------------------------------------------------------------
    init {
        initWith(string)
    }

    @Throws(VersionCheckException::class)
    private fun initWith(marketing: String?, buildStr: String?) {
        // Build
        if (buildStr == debugBuildString) {
            isDevelopmemnt = true
            build = null
        } else {
            isDevelopmemnt = false
            build = buildStr?.toIntOrNull()
        }

        // Cannot parse any further if we have no marketing string
        if (marketing.isNullOrEmpty()) {
            if (build == null) {
                // If we also have no build, then this is a failed parse.
                throw VersionCheckException.InvalidVersionString()
            } else {
                return
            }
        }

        // Attempt to pull of additional text from the end of the string
        val splitMarketing = marketingRegex.matchEntire(marketing)
        val numericMarketing = splitMarketing?.groupValues?.getOrNull(1)
        additionalText = splitMarketing?.groupValues?.getOrNull(2) ?: ""

        if (numericMarketing == null || numericMarketing.startsWith(".") || numericMarketing.endsWith(".")) {
            throw VersionCheckException.InvalidVersionString()
        } else {
            marketingComponents = numericMarketing
                .split(".")
                .map { segment ->
                    try {
                        segment.toInt()
                    } catch (e: Exception) {
                        throw VersionCheckException.InvalidVersionString(e)
                    }
                }.toIntArray()
        }
    }

    @Throws(VersionCheckException::class)
    private fun initWith(string: String) {
        val parts = string.split("@")
        if (parts.size > 2) {
            throw VersionCheckException.InvalidVersionString()
        }

        initWith(parts.getOrNull(0), parts.getOrNull(1))
    }

    //---------------------------------------------------------------------------------
    // equals and hashCode overrides
    //---------------------------------------------------------------------------------
    // Auto generated and then slightly modified to "pad" marketing components;
    // if changing class properties please regenerate again.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Version

        // Pad out arrays with 0s
        val paddedArrays = createPaddedArrays(this.marketingComponents, other.marketingComponents)

        // marketingComponents and build must be equal.
        if (!paddedArrays.first.contentEquals(paddedArrays.second)) return false
        if (build != other.build) return false

        return true
    }

    // Auto generated; if changing class properties please regenerate again.
    override fun hashCode(): Int {
        var result = marketingComponents.contentHashCode()
        result = 31 * result + (build ?: 0)
        return result
    }

    //---------------------------------------------------------------------------------
    // Comparable implementation
    //---------------------------------------------------------------------------------
    /**
     * All comparisons are translated into calls to compareTo, which returns an Int.
     * a > b	a.compareTo(b) > 0
     * a < b	a.compareTo(b) < 0
     * a >= b	a.compareTo(b) >= 0
     * a <= b	a.compareTo(b) <= 0
     */
    override fun compareTo(other: Version): Int {
        val thisBuild = build ?: 0
        val otherBuild = other.build ?: 0

        // Handle case where Version only contains build number.
        if (marketingComponents.isEmpty() && other.marketingComponents.isEmpty()) {
            return thisBuild.compareTo(otherBuild)
        }

        // First check pairwise marketing components
        // Pad out arrays with 0s
        val zipPaddedArrays = createZipPaddedArrays(this.marketingComponents, other.marketingComponents)
        zipPaddedArrays.forEach {
            if (it.first > it.second) return 1
            if (it.first < it.second) return -1
        }

        // If here, then marketing components are equal.
        // Development builds are always considered to be "higher" then any
        // specific numerical builds, so check of the "development-ness" if the
        // versions doesn't match and handle separately.
        if (!isDevelopmemnt && other.isDevelopmemnt) return -1
        if (isDevelopmemnt && !other.isDevelopmemnt) return 1

        // Finally, if marketing values the same, and neither are development builds, then tie
        // breaker comes down to build number.
        return thisBuild.compareTo(otherBuild)
    }

// todo iOS overrides ~=
    //    // Pattern match against a version, use the first argument as a pattern, value will match if any components are equal,
//    // OR are nil in the pattern,
//    public static func ~= (_ pattern: Version, _ value: Version) -> Bool {
//        // assume that all missing components in value are zeros
//        let components = Zip2WithNilPadding(pattern.marketingComponents, value.marketingComponents).map { ($0.0, $0.1 ?? 0) }
//
//        // check that all marketing components are equal, or misisng in the patterns
//        let marketingMatches = components.reduce(true) {
//            return $0 && (($1.0 == $1.1) || ($1.0 == nil))
//        }
//
//        // overall equality is marketing equality and build equality
//        return marketingMatches && ((pattern.build == value.build) || (pattern.build == nil))
//    }


    //---------------------------------------------------------------------------------
    // Private helpers
    //---------------------------------------------------------------------------------
    private fun createPaddedArrays(first: IntArray, second: IntArray): Pair<IntArray, IntArray> {
        val maxLength = max(first.size, second.size)
        return Pair(first.copyOf(maxLength), second.copyOf(maxLength))
    }

    // Couldn't seem to find a zip2 alternative in Android, possibly googling wrong search terms.
    private fun createZipPaddedArrays(first: IntArray, second: IntArray): List<Pair<Int, Int>> {
        val maxLength = max(first.size, second.size)
        val firstPadded = first.copyOf(maxLength)
        val secondPadded = second.copyOf(maxLength)
        return firstPadded.mapIndexed { index, firstInt -> Pair(firstInt, secondPadded[index]) }
    }

    companion object {
        const val debugBuildString = "0"

        /**
         * In Swift, NSRegularExpression(pattern:#"([0-9\.]+)(.*)"#)
         * But in Kotlin we do not appear to need the backslash escape character
         */
        val marketingRegex = "([0-9.]+)(.*)".toRegex()
    }
}