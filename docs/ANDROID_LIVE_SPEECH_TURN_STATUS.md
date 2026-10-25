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

## Herstel als Android geen enkel eindcallback terugstuurt

Sommige vendorimplementaties van Androids **on-device** spraakherkenner
kunnen tijdens een luisterronde volledig blijven hangen: geen
`onResults`, geen `onError` en geen volgende gespreksregel. Een normale
stilte leidt doorgaans tot een Android `NO_MATCH` of
`SPEECH_TIMEOUT`; deze aparte bescherming is alleen voor
**uitblijvende terminale callbacks**.

- Tuddy geeft iedere *nieuwe* herkenner een eigen **60-secondenlimiet**,
  onafhankelijk van tussenteksten. Een herhaalde partial-callback mag
  niet eindeloos de microfoon openhouden.
- Een normaal eindresultaat of echte terminale fout annuleert de timer
  direct. Bestaande privacy-, toestemmings-, radio- en levenscyclusregels
  blijven de beslissende gates.
- Blijft het toestel zonder terminaal antwoord, dan wordt die oude
  herkenningsronde afgesloten en maximaal **twee keer** automatisch
  met een frisse on-device herkenner geprobeerd. De vorige
  `VoiceSessionGeneration` en de per-turn `SpeechTurnCompletionGate`
  voorkomen dat late callbacks van de oude ronde alsnog tekst toevoegen.
- Na de **derde** vastgelopen ronde wordt de microfoon volledig gestopt,
  met uitleg om het lokale spraakpakket te controleren en zelf opnieuw
  op Luister te tikken. Er komt geen onbeperkte herstartlus en
  geen cloudspraakfallback.
- De reeds ingestelde maximale microfoonsessieduur wordt **niet
  verlengd** door deze herstart. Stop, scherm verlaten en achtergrond
  wissen de timer; de oorspronkelijke ondertitels blijven in de
  tijdelijke gespreksgeschiedenis.
- De teller voor vastgelopen rondes blijft alleen tijdelijk in het
  bestaande privacyvriendelijke sessierapport, zonder spraakinhoud,
  persoonlijke apparaatlog of externe diagnostiek.

Ook deze app-logica is geen vervanging voor een geautoriseerde,
**fysieke Android 12+-vliegtuigmodustest** met werkelijke resultaten.

## Toestelinteractie: actieve microfoonprivacybewaking

De bestaande **Start / Stop**, toestemming, on-device spraakdienst en
beperkte herstarts blijven leidend. Daarnaast gebruikt elke nieuwe
Android-herkenningsronde een tijdelijke sessieguard die:

- de bestaande `RECORD_AUDIO`-toestemming opnieuw controleert;
- Androids globale melding **microfoon gedempt** controleert;
- leest of Android naar bel-, beltoon- of VoIP-communicatiemodus ging;
- de drie signalen ongeveer iedere **twee seconden** blijft controleren
  zolang die herkenner actief is.

Deze guard start **vóór** `SpeechRecognizer.startListening`. Kunnen de
actuele Android-signalen niet betrouwbaar worden gelezen of kan de
tijdelijke controle niet worden ingepland, dan wordt de microfoon
**niet geopend**. Als de telefoon daarna een oproep meldt, de microfoon
mute of de toestemming intrekt, beëindigt de app de herkenner en alle
automatische herstartverzoeken, met een leesbare reden. De gebruiker
kan na controle zelf opnieuw op **Luister** tikken; Tuddy opent geen
systeeminstellingen of andere app en vraagt geen nieuwe rechten aan.

Een nieuwe herkenningsronde vervangt de voorgaande monitor. **Stop**,
achtergrond en Activity-afsluiting verwijderen alle callbacks. Het
diagnoserapport telt alleen het *aantal* door Android gemelde
privacyonderbrekingen in RAM, niet gesprekken, belgegevens,
telefoonnummers, audio, transcript, namen of tijdstippen.

**Beperking:** Androids `AudioManager`-status is niet gegarandeerd
identiek aan een fysieke mute-schakelaar, de status van een externe
headsetmicrofoon of iedere OEM-privacyimplementatie. Het interval laat
een reactievertraging toe. Dit is dus extra beperking van blootstelling
aan microfooncapture, geen onafhankelijk bewijs dat een fysiek toestel
te allen tijde niet heeft opgenomen.

## Gates

Deze mijlpaal vereist de bestaande Android GitHub-build en source-head
checks, maar geen nieuwe testsuites. De voltooide build en een source merge
zijn nadrukkelijk niet hetzelfde als een fysieke Android 12+
vliegtuigmodusproef, een native-speaker-review, een rechtengecontroleerd
Kurmanji/Sorani/Pasjtoe-spraakmodel of een ondertekende productie-release.
