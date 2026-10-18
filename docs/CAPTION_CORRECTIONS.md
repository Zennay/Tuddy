# Gesproken tekst herstellen en een lokale vertaling opnieuw proberen

In de gespreksgeschiedenis verschijnt bij elke definitieve ondertitel **✎ Corrigeer tekst**.
Stop eerst **Luister**. In het bewerkvenster kun je een fout herkende of zelf getypte
oorspronkelijke zin verbeteren. **Corrigeer en vertaal** vervangt alleen die regel
in het lopende gesprek en vraagt voor die regel opnieuw de bestaande lokale
ML Kit-tekstvertaling aan. De zelf aangepaste regel krijgt een duidelijk label;
de app beweert niet dat een mens of een model de juistheid heeft gecontroleerd.

Bij **Fout** of **Model ontbreekt** verschijnt **↻ Probeer opnieuw**. Daarmee
wordt de bestaande originele regel opnieuw vertaald zonder de tekst of andere
regels te veranderen. Eventuele eerdere mislukte modeldownloads worden niet
automatisch opnieuw uitgevoerd buiten de door de gebruiker gestarte handeling.

## Veiligheid en privacy

- Geen externe spraakherkenning, geen cloud-API, geen automatisch gesprekarchief.
  Voor lokale vertaalkoppels kan de bestaande ML Kit-route via wifi een
  ontbrekend **tekstmodel** downloaden; dat is geen online tekstvertaling.
- Correcties, tijdelijke concepten, zelf-gecorrigeerd-markering en retries
  blijven uitsluitend in RAM. App-verlaten/wissen verwijdert het open concept.
- Een nieuwe poging krijgt per regel een nieuwe interne revisie. Oude
  asynchrone taalherkenning en vertaalaanvragen mogen de nieuwere versie
  niet overschrijven, ook niet wanneer de tekst later weer hetzelfde is.
- Handmatige sprekerlabels, markeringen en tijdstempels blijven op de regel
  behouden. Een mogelijk nog niet verstuurde deeltoestemming of transcript-
  exportsnapshot wordt bij wijzigingen onbruikbaar gemaakt.
- Een afgeronde vertaling kan pas weer worden gedeeld/voorgelezen na een
  nieuwe afronding. In exports is de oorspronkelijke tekst de inmiddels door
  de gebruiker aangepaste tekst; een extern document wordt nooit in stilte
  bijgewerkt. Bewaar eerst een nieuw bestand wanneer je de correctie wilt delen.
- De knoppen zijn uitgeschakeld terwijl de microfoon luistert. Ze voeren
  geen aanpassing uit in vooraf opgeslagen read-only JSON-archieven.

**Beperking:** dit corrigeert alleen getoonde tekst. Het is geen audiobewerking,
automatische speaker-diarization of bewijs van nauwkeurige offline spraak-
vertaling voor Kurmanji, Sorani of Pasjtoe. Android-toestelacceptatie, model-
rechten en productie-/APK-release blijven aparte gates.
