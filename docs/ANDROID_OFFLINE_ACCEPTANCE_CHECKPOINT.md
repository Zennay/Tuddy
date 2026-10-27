# Android 12+ fysieke offline-proef: tijdelijk concept opslaan

Tuddy blijft in de projectfase **Incubate / Verifying**. Het bestaande
**Offline startcheck → Leg fysieke offline-waarnemingen vast**-formulier
heeft een echte operator nodig voor tien NL→EN- en tien EN→NL-pogingen.

## Nieuw: een onafgerond formulier onderbreken en heropenen

De operator kan tijdens het invullen expliciet op **Bewaar voorlopig
proefconcept (zonder transcript)** tikken. Androids documentkiezer
bepaalt waar het bestand terechtkomt. Het bestand bevat maximaal
twintig handmatige uitkomsten (spraak herkend ja/nee, lokaal vertaald
ja/nee, eventuele gemeten duur), plus de verwachte en geïnstalleerde
APK SHA-256. **Geen uitgesproken woorden, vertaling, audio, Android-ID,
vrije tekst, automatisch ingestelde acceptatievinkjes of verborgen
achtergrondopslag.** De gekozen externe Android-provider kan het
document zelf synchroniseren; kies zelf een betrouwbare lokale map.

Later opent de operator de Offline startcheck en gebruikt
**Heropen eerder handmatig proefconcept**. Tuddy leest maximaal 4096
bytes uit precies het geselecteerde document, weigert ongeldige of
uitgebreide inhoud en vergelijkt de oorspronkelijke digest met de
**nu geïnstalleerde** APK. Bij een andere of onbekende APK worden
geen eerdere uitkomsten geladen. Alle pogingen blijven in RAM
totdat de operator opnieuw expliciet exporteert of het scherm sluit.

De import bewaart **geen eerdere toestemmings- en veiligheidsvinkjes**:
de operator moet de vliegtuigmodus en echte radiostatus, microfoon-Stop,
cloudfallback en privacy na terugkeer opnieuw zelf controleren.
Een heropend concept staat zichtbaar gemarkeerd in het uiteindelijke
rapport als *uit eerdere sessie geïmporteerde menselijke uitkomsten*.
Daarmee wordt **nooit** beweerd dat twintig pogingen binnen één
ononderbroken offline-sessie plaatsvonden.

## Foutafhandeling en beperkingen

- Een bestaand, nog niet opgeslagen concept wordt nooit stilzwijgend
  overschreven bij import; maak het eerst bewust leeg of bewaar het.
- Annuleren van de export of niet kunnen schrijven vernietigt
  ingevoerde waarnemingen niet.
- Ongeldige richting, meer dan tien pogingen per richting, vertaling
  zonder spraakherkenning, duur zonder vertaling of ongeldige tijd
  zorgt voor een importweigering.
- Het checkpoint is een **werkconcept**, geen officieel
  acceptatierapport of automatisch bewijs. De definitieve rapportknop
  blijft apart en het rapport noemt alle resterende reviewgates.

Een onafhankelijke reviewer moet nog steeds de fysieke Android 12+-
proef, de exacte geïnstalleerde APK, echte netwerkisolatie, de
spraak-/vertalingsuitkomsten en een ondertekende release beoordelen.
GitHub source-merge en CI vormen daarvoor op zichzelf geen bewijs.

## Handmatig rapport naar een onafhankelijke reviewer delen

Na het expliciet controleren van de observaties kan de operator nu via
**Bekijk en deel rapport met beoordelaar…** een tweede, volledige preview
openen. Het rapport is identiek aan de bestaande tekstexport. Pas na een
afzonderlijke toestemming opent Android Delen; Tuddy kiest nooit zelf een
ontvanger en uploadt niets. Vlak voor het deelvenster controleert de app
de geïnstalleerde APK-/apparaatstaat en radiosnapshot opnieuw. Als die
status wijzigt, wordt delen geweigerd en blijft het proefconcept bestaan.

Het gedeelde bericht bevat toestelstatus, APK-checksum en alleen de
handmatig geregistreerde uitkomstaggregaten, geen gesproken woorden
of audio. De gekozen ontvangende app kan de informatie wel opslaan,
synchroniseren of verzenden. Verlaat je Tuddy voor dat deelvenster,
dan wordt de rapportpreview gesloten. Het openen van een andere app
onderbreekt de lopende proef; de operator mag geen doorlopende fysieke
radiostilte of onafhankelijke review claimen op grond van dit delen.
Het formulier blijft beschikbaar na annuleren of terugkeren uit het
deelvenster. Een mislukte verzending geeft geen fictieve succesmelding.
