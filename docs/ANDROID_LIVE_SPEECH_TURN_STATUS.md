# Live Android-spraakrondes: veilige afsluiting en zichtbare status

## Werkelijke app-core-functionaliteit

Androids on-device `SpeechRecognizer` produceert verschillende callbacks
voor één herkenningsronde. Een `VoiceSessionGeneration` schermt oudere
recognizers al af, maar **dezelfde** recognizer kan bijvoorbeeld tweemaal
`onResults` of vlak na `onResults` alsnog `onError` afgeven.

Tuddy gebruikt nu per nieuw gemaakte recognizer een tijdelijke
`SpeechTurnCompletionGate`. Daarin kan slechts één terminaal resultaat
winnen: óf `onResults`, óf `onError`. Dubbele terminale meldingen
kunnen daardoor niet nog een identieke gespreksregel, dubbele haptische
feedback, onjuiste sessietellers of een tweede timer/restart aanmaken.
Een achteraf binnengekomen fout van dezelfde reeds afgeronde ronde kan
een geslaagd gesproken resultaat niet alsnog stoppen. De bestaande
session-epoch, levenscyclus- en toestemmingscontroles blijven bovendien
actief. Er vindt nadrukkelijk **geen** tekstgebaseerde deduplicatie
plaats: als iemand in twee echte rondes dezelfde zin uitspreekt, zijn
het twee echte gespreksregels.

Ook late tussenteksten, taalwisselvoorstellen, begin- en eindmeldingen
van een al afgesloten ronde mogen de weergegeven tekst of gekozen taal
niet opnieuw beïnvloeden.

## Herkenbare luisterstatus

Onder de hoofdbediening én in de fullscreen leesmodus staat dezelfde
uitsluitend tijdelijke Android-herkenningsstatus:

- Microfoon uit;
- offline spraakdienst starten;
- klaar voor de volgende zin;
- spraak ontvangen;
- zin afronden/verwerken;
- afgeronde zin ontvangen, lokale tekstvertaling loopt afzonderlijk;
- bij een normale stilte of geen herkenbare match lokaal opnieuw proberen;
- bij een terminale spraakfout gestopt, met bestaande detailmelding.

De status beschrijft **callbackvoortgang** en is geen bewijs van juist
herkende tekst of een volledig zonder internet functionerend toestel.
Bij Stop, achtergrond en nieuwe rondes wordt deze status via dezelfde
lifecycle gereset. Gewone uitval blijft zonder cloudfallback; alleen
de bestaande beperkte retry voor `NO_MATCH` en `SPEECH_TIMEOUT`
mag de microfoon opnieuw starten.

## Gates

Deze mijlpaal vereist de bestaande Android GitHub-build en source-head
checks, maar geen nieuwe testsuites. De voltooide build en een source merge
zijn nadrukkelijk niet hetzelfde als een fysieke Android 12+
vliegtuigmodusproef, een native-speaker-review, een rechtengecontroleerd
Kurmanji/Sorani/Pasjtoe-spraakmodel of een ondertekende productie-release.
