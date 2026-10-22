import com.livelingo.android.model.CommunityPhrasebook
import com.livelingo.android.model.Conversation
import com.livelingo.android.model.LanguagePairPlan
import com.livelingo.android.model.Languages
import com.livelingo.android.model.LocalGlossary
import com.livelingo.android.model.wordAt

fun main() {
    var assertions = 0
    fun eq(actual: Any?, expected: Any?) { check(actual == expected) { "Expected $expected but got $actual" }; assertions++ }
    eq(Languages.normalize("de-DE"), "de")
    eq(Languages.normalize("NL_nl"), "nl")
    eq(Languages.normalize("zh-Hans"), "zh")
    eq(Languages.choices.size, 59)
    eq(Languages.choices.map { it.code }.toSet().size, 59)
    eq(setOf("ar", "fa", "nl", "uk", "ur", "ru", "tr", "fr", "en").all { Languages.find(it) != null }, true)
    eq(Languages.normalize("ar-SY"), "ar")
    eq(Languages.normalize("ar-PS"), "ar")
    eq(Languages.normalize("fa-IR"), "fa")
    eq(Languages.normalize("prs-AF"), null)
    eq(Languages.find("ps"), null) // Pashto cannot be advertised as supported by ML Kit
    eq(Languages.isCommunityPhrasebook("ps"), true)
    eq(Languages.normalizeTarget("ps-AF"), "ps")
    eq(Languages.searchCommunity("Pasjtoe").single().code, "ps")
    eq(Languages.searchCommunity("koerdisch").map { it.code }.toSet(), setOf("kmr", "ckb"))
    eq(Languages.communityPhrasebooks.size, 3)
    eq(CommunityPhrasebook.nllbTags["kmr"], "kmr_Latn")
    eq(CommunityPhrasebook.nllbTags["ckb"], "ckb_Arab")
    eq(CommunityPhrasebook.nllbTags["ps"], "pbt_Arab")
    eq(CommunityPhrasebook.translate("nl", "kmr", "hallo!").getOrNull(), "Silav")
    eq(CommunityPhrasebook.translate("en", "ckb", "thank you").getOrNull(), "سوپاس")
    eq(CommunityPhrasebook.translate("nl", "ps", "water").getOrNull(), "اوبه")
    eq(CommunityPhrasebook.translate("en", "ps", "long unrecognized phrase").isFailure, true)
    eq(CommunityPhrasebook.translate("fa", "ps", "yes").isFailure, true)
    eq(Languages.requested.any { it.code == "so" }, true)
    eq(Languages.isCommunityPhrasebook("ckb"), true)
    eq(Languages.search("عرب").firstOrNull()?.code, "ar")
    eq(Languages.search("persian").firstOrNull()?.code, "fa")
    eq(Languages.search("Syrië").firstOrNull()?.code, "ar")
    eq(Languages.search("Palestina").firstOrNull()?.code, "ar")
    eq(Languages.search("dutch").firstOrNull()?.code, "nl")
    eq(LanguagePairPlan.starterSources("nl", "de-DE"), listOf("de", "ar", "en"))
    eq(LanguagePairPlan.starterSources("ar", "ar-SY"), listOf("en", "nl"))
    eq(LanguagePairPlan.starterSources("en", "fa-IR"), listOf("fa", "ar", "nl"))
    eq(LanguagePairPlan.starterSources("de", "unsupported"), listOf("ar", "en", "nl"))
    eq(LanguagePairPlan.starterSources("fr", "uk").size <= 3, true)
    eq(wordAt("Do you understand?", 8), "understand")
    eq(wordAt("Verstehst du?", 4), "Verstehst")
    eq(wordAt("l’amour est ici", 3), "l’amour")
    eq(wordAt("hallo, wereld", 5), null)
    eq(wordAt("école!", 0), "école")
    eq(wordAt("test", -1), null)
    eq(wordAt("test", 4), null)
    eq(LocalGlossary.lookup("de-DE", "Wasser")?.illustration, "💧")
    eq(LocalGlossary.lookup("en", "UnderStand")?.definition, "To grasp the meaning of something.")
    eq(LocalGlossary.lookup("en", "quasar"), null) // never make up a definition
    val original = Conversation().partial("Bonjour le monde")
    val detected = original.identifiedLanguage(original.revision, "fr")
    eq(detected.activeLanguage, "fr")
    eq(detected.identifiedLanguage(original.revision - 1, "en"), detected)
    val final = detected.finish("Bonjour le monde", 100, targetLanguage = "nl")
    eq(final.lines.single().sourceLanguage, "fr")
    eq(final.lines.single().targetLanguage, "nl")
    val next = final.finish("Guten Morgen", 200, sourceLanguage = "de", targetLanguage = "en")
    eq(next.lines.map { it.targetLanguage }, listOf("nl", "en")) // old language label preserved after target switch
    val updated = next.finalLanguage(2, "de").finalTranslation(2, "Good morning")
    eq(updated.lines.last().translation, "Good morning")
    eq(updated.lines.first().translation, null)
    val cleared = updated.clear()
    eq(cleared.lines.size, 0)
    eq(cleared.nextId, 3L) // avoid ID reuse by delayed translation callback
    eq(cleared.finish("new", 300).lines.single().id, 3L)
    println("PASS $assertions language, selection, word peek, session and stale-result checks")
}
