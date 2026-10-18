# Privé SRT/WebVTT-editor: stapsgewijs terug en opnieuw

De bestaande offline ondertiteleditor ondersteunt nu **↶ Terug** en **↷ Opnieuw**, met maximaal twintig eerder geslaagde documentbewerkingen in tijdelijk Android-schermgeheugen. De volgende handelingen zijn herstelbaar:

- Corrigeren van één tekstregel.
- Aanpassen van begin- of eindtijd en verschuiven van alle tijdcodes.
- Eén cue splitsen, naastliggende cues samenvoegen of een cue verwijderen.
- De al naar main gemergde, expliciet bevestigde **Zoek en vervang…**-batch van [Tuddy #140](https://github.com/Zennay/Tuddy/pull/140), in **één** undo-stap.

De knoppen zijn uitgeschakeld zonder relevante geschiedenis. Elke nieuwe wijziging na Terug verwijdert de oude opnieuw-tak; **Herstel** gooit alle wijzigingen weg en wist beide geschiedenissen. De bestaande zoek-en-vervangdialoog blijft de enige batchtool: geen parallelle UI of alternatieve parser.

## Privacy en release

- Alle geschiedenis blijft uitsluitend tijdens het geopende document in RAM; maximaal twintig eerder complete cue-lijsten. Sluiten/verlaten vernietigt de historie.
- Het origineel op de gekozen Android-documentlocatie wordt nooit bijgewerkt. Een gewijzigde ondertitel blijft pas beschikbaar buiten Tuddy na **Bewaar kopie SRT…** of **Bewaar kopie VTT…** en aparte bewust gekozen opslaglocatie.
- Geen metadata, extra logs, microfoon, netwerkaanroepen, native-speakercontrole, modeldownload of nieuwe permissies.
- Deze productuitbreiding gebruikt de bestaande `PrivateSubtitleDraftEditor` en `PrivateSubtitleBatchReplace` van main, zonder hun veiligheidsgrenzen te verzwakken.
- Alleen `app/src/main/java/com/livelingo/android/ui/PrivateSubtitleFileDialog.kt` verandert aan productgedrag. De bestaande hosted Android build-, apparaat-/licentie-, signing- en releasegates blijven ongewijzigd; een bronmerge is geen fysieke deviceacceptatie of APK-productierelease.
