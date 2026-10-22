package com.livelingo.android

/**
 * Languages allowed in HUMAN-AUTHORED private correction/phrasebook data.
 *
 * This deliberately does not extend TargetCatalog, Android speech locales,
 * ML Kit translation targets, or any runtime/model eligibility decision.
 * The extra three codes only allow a user to store their own text pairs.
 */
internal object SessionCorrectionLanguageCatalog {
    private val manualOnlyTargets = listOf(
        TextTarget("kmr", "Kurmancî · Kurmanji (alleen eigen zinnen; geen model)"),
        TextTarget("ckb", "کوردیی ناوەندی · Sorani (alleen eigen zinnen; geen model)"),
        TextTarget("ps", "پښتو · Pashto (alleen eigen zinnen; geen model)")
    )

    val manualOnlyCodes: Set<String> = manualOnlyTargets.map { it.code }.toSet()
    val targets: List<TextTarget> =
        (TargetCatalog.targets + manualOnlyTargets).distinctBy { it.code }
    val codes: Set<String> = targets.map { it.code }.toSet()
}
