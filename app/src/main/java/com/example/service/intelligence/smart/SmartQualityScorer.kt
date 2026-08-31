package com.example.service.intelligence.smart

/**
 * Step 83: Smart Quality Scorer.
 * 
 * Computes an internal content quality and completeness score (0 to 100).
 * Prevents low-quality, incomplete, malformed, or suspicious posts from being auto-published.
 */
object SmartQualityScorer {

    const val MINIMUM_AUTO_PUBLISH_SCORE = 50

    /**
     * Calculates the composite quality score for a processed content item.
     */
    fun calculateScore(
        title: String,
        confidence: SmartConfidenceScore,
        postDate: String?,
        lastDate: String?,
        examDate: String?,
        organization: String?,
        links: List<SmartLink>,
        hasConflict: Boolean,
        hasInvalidDate: Boolean
    ): SmartQualityScore {
        var titleScore = 0
        var confidenceScore = 0
        var sourceDateScore = 0
        var importantDatesScore = 0
        var linksScore = 0
        var orgScore = 0
        var integrityBonus = 0
        val penalties = mutableListOf<String>()

        // 1. Title Score (max 15)
        if (title.isNotBlank()) {
            if (title.length >= 10 && !title.equals("Official Update", ignoreCase = true)) {
                titleScore = 15
            } else {
                titleScore = 5
                penalties.add("Short or generic title")
            }
        } else {
            penalties.add("Missing title")
        }

        // 2. Category Confidence Score (max 25)
        confidenceScore = (confidence.score * 25).toInt().coerceIn(0, 25)
        if (confidence.level == SmartConfidenceLevel.LOW) {
            penalties.add("Low category confidence (${confidence.score})")
        }

        // 3. Source Date Score (max 15)
        if (!postDate.isNullOrBlank() && SmartDateIntelligence.isValidDate(postDate)) {
            val cutoff = SmartDateIntelligence.isEligibleByCutoff(postDate)
            if (cutoff == true) {
                sourceDateScore = 15
            } else if (cutoff == false) {
                sourceDateScore = 0
                penalties.add("Post date is before historical cutoff")
            } else {
                sourceDateScore = 5
            }
        } else {
            penalties.add("Missing or unverified source date")
        }

        // 4. Important Dates Score (max 15)
        if (!lastDate.isNullOrBlank() && SmartDateIntelligence.isValidDate(lastDate)) {
            importantDatesScore += 10
        }
        if (!examDate.isNullOrBlank()) {
            importantDatesScore += 5
        }
        if (hasInvalidDate) {
            importantDatesScore = 0
            penalties.add("Invalid/impossible date detected")
        }

        // 5. Verified Links Score (max 15)
        val hasOfficial = links.any { it.linkType == SmartLinkType.OFFICIAL }
        val hasActionable = links.any { 
            it.linkType == SmartLinkType.APPLY || it.linkType == SmartLinkType.RESULT || 
            it.linkType == SmartLinkType.ADMIT_CARD || it.linkType == SmartLinkType.ANSWER_KEY || 
            it.linkType == SmartLinkType.PDF 
        }
        if (hasActionable) linksScore += 10
        if (hasOfficial) linksScore += 5

        // 6. Organization Score (max 10)
        if (!organization.isNullOrBlank()) {
            orgScore = 10
        } else {
            penalties.add("Organization not explicitly detected")
        }

        // 7. Integrity / Conflict Bonus (max 5)
        if (!hasConflict && !hasInvalidDate && penalties.isEmpty()) {
            integrityBonus = 5
        } else if (hasConflict) {
            penalties.add("Factual data conflict with another source")
        }

        val total = (titleScore + confidenceScore + sourceDateScore + importantDatesScore + linksScore + orgScore + integrityBonus)
            .coerceIn(0, 100)

        val isEligible = total >= MINIMUM_AUTO_PUBLISH_SCORE && 
                         confidence.level != SmartConfidenceLevel.LOW && 
                         !hasConflict && 
                         !hasInvalidDate && 
                         title.isNotBlank()

        return SmartQualityScore(
            totalScore = total,
            titleScore = titleScore,
            categoryConfidenceScore = confidenceScore,
            sourceDateScore = sourceDateScore,
            importantDatesScore = importantDatesScore,
            verifiedLinksScore = linksScore,
            organizationScore = orgScore,
            integrityBonus = integrityBonus,
            isEligibleForAutoPublish = isEligible,
            penaltyReasons = penalties
        )
    }
}
