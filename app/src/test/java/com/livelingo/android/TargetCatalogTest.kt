package com.livelingo.android

import org.junit.Assert.*
import org.junit.Test

class TargetCatalogTest {
    @Test fun all59OfflineTextTargetsAreUnique() {
        assertEquals(59, TargetCatalog.codes.size)
        assertEquals(59, TargetCatalog.codes.distinct().size)
    }
    @Test fun dontMisrepresentUnsupportedLanguagesAsMlKitTranslation() {
        assertTrue(setOf("kmr", "ckb", "ps", "so", "ti", "om").none { it in TargetCatalog.codes })
    }
    @Test fun newcomerLanguageAliasesAreSearchable() {
        assertEquals("ar", TargetCatalog.search("palestina").firstOrNull()?.code)
        assertEquals("fa", TargetCatalog.search("perzisch").firstOrNull()?.code)
        assertEquals("uk", TargetCatalog.search("oekraïens").firstOrNull()?.code)
        assertEquals("ur", TargetCatalog.search("pakistan").firstOrNull()?.code)
    }
    @Test fun displayNamesIncludeNativeScripts() {
        assertTrue(TargetCatalog.targets.first { it.code == "ar" }.title.contains("العربية"))
        assertTrue(TargetCatalog.targets.first { it.code == "uk" }.title.contains("Українська"))
    }
}
