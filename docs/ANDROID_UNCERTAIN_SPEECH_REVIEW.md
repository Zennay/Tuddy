# Onzekere Android-spraakzinnen zelf beoordelen

Tuddy toont soms een waarschuwing wanneer de Android-on-device-herkenner een
lage zekerheid bij een herkende zin rapporteert. Dat getal is **niet**
gekalibreerd en bewijst niet dat de zin fout is. Het is ook niet de
kwaliteitsscore van de lokale ML Kit-vertaling.

## Eén tijdelijke controlelijst

In het hoofdgesprek verschijnt **Controleer N onzekere spraakzinnen…**
wanneer er nog niet bekeken regels zijn. Dezelfde actie staat in de
fullscreen leesmodus. Stop bewust eerst de microfoon; Tuddy herstart die
niet vanuit het reviewvenster. De lijst toont maximaal dertig regels
tegelijk, met bron-/doeltaal, Android-score en oorspronkelijke
herkende tekst. Dit is geen nieuwe herkenner, modeldownload of cloudactie.

Per regel kan de gebruiker:

- **Bekeken · niet bevestigd:** uitsluitend deze regel uit de tijdelijke
  controlelijst wegnemen. De Android-onzekerheidswaarschuwing blijft bij
  de oorspronkelijke gespreksregel zichtbaar, inclusief de melding
  dat geen onafhankelijke verificatie heeft plaatsgevonden. Geen
  gespreksinhoud, vertaald resultaat of taalinstelling verandert.
- **Corrigeer…:** de bestaande menselijke correctiedialoog openen met
  de actuele oorspronkelijke zin en, als Android die gaf, alternatieve
  tekstvoorstellen. Alleen na expliciet opslaan van *gewijzigde* tekst
  krijgt de regel een nieuwe revisie en start de bestaande offline
  vertaalpipeline opnieuw. Er zijn geen uploads of stille correcties.

Tuddy toetst direct vóór beide acties of de originele tekst, taal- en
revisiegegevens en de lage zekerheidswaarde nog bij de actuele
in-memory gespreksregel horen. Verwijderde, inmiddels gecorrigeerde of
door een andere actie gewijzigde regels worden genegeerd. Een nieuwe
microfoonsessie sluit de controlelijst. Alles blijft alleen in RAM en
wordt bij gesprek wissen en Activity-dispose verwijderd.

## Acceptatiegrenzen

Deze functie helpt bij het **menselijk** beoordelen van Android-output,
maar levert geen echte sprekeridentificatie, ASR-nauwkeurigheidstest,
vertalingvalidatie of onafhankelijke native-speaker-review op.
De open projectgates voor fysieke Android 12+-vliegtuigmodus,
rechtmatig inzetbare Kurmanji/Sorani/Pasjtoe ASR + vertaalmodellen,
brede UX-metingen en ondertekende release blijven onafhankelijk.
