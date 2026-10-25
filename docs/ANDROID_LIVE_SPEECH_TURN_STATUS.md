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

## Nieuwe keuze: doorlopend of één zin

In het hoofdscherm en in de fullscreen leesmodus kan de gebruiker
**Luistermodus** wisselen terwijl de microfoon uit staat. De keuze
wordt lokaal in app-private instellingen bewaard; uitsluitend de
modusnaam, **nooit** een gesproken zin, opname of gesprek.

- **Doorlopend** is het bestaande standaardgedrag. Na een afgeronde
  zin, een normale stille poging of een beperkte timeout-herstelactie
  mag de app met de bestaande privacy-, model- en lifecyclechecks een
  nieuwe Android on-device recognizer starten.
- **Eén zin** is precies **één** bewust gestarte herkenningspoging:
  na een geldig resultaat wordt eerst de tijdelijke gespreksregel
  aangemaakt en lokaal vertaald; de microfoon wordt meteen gestopt
  en er wordt géén volgende herkenner gepland. De al gestarte
  ML Kit-tekstvertaling kan onafhankelijk afronden, mits de regel
  nog bestaat en ongewijzigd is.
- Bij **geen spraakmatch**, **spraak-timeout** of het uitblijven van
  elke Android-terminal-callback stopt Eén zin expliciet. De gebruiker
  kan opnieuw op **Luister** tikken; de app probeert niet stiekem een
  tweede keer op te nemen. De bestaande 60-seconden watchdog blijft
  ook voor deze afzonderlijke poging geldig.
- Wisselen is alleen mogelijk als de microfoon uit staat. De
  afzonderlijke opnametijdlimiet, toestemming, microfoon/privacy-
  bewaking, strikte vliegtuigmodus, geïnstalleerd speechpakket,
  herkennergeneratie en stop-bij-achtergrond blijven behouden.

Het statuslabel meldt in Eén zin duidelijk dat de microfoon gestopt
is, en maakt geen claim dat de vertaling al klaar is. Deze modus is
bedoeld voor een korte bewuste invoer, niet als garantie over
herkenningskwaliteit of echte vliegtuigmodusisolatie.

## Transcript-only luisteren zonder live vertaling

De gebruiker kan tijdens het doorlopende luisteren expliciet op
**Pauzeer live vertalen · houd origineel** tikken. De Android on-device
SpeechRecognizer blijft actief en legt nieuwe herkenningsregels alleen
als tijdelijk origineel vast. De app verstuurt nieuwe gesproken zinnen
dan **niet** naar de lokale ML Kit-tekstvertaler. Ook de voorlopige
tussentekstvertaling is uitgeschakeld. Dat geldt ook voor een
bewust beoordeelde **één-zin**-spraakopname: de originele tekst blijft
zichtbaar maar wordt niet automatisch vertaald.

Bij het pauzeren krijgen bestaande, nog wachtende **spraakafkomstige**
regels expliciet de status **Gepauzeerd**. Hun oorspronkelijke tekst,
taalrichting, correctie- en sprekerlabels blijven bewaard. Reeds
gestarte ML Kit-modeltaken kunnen op de achtergrond nog intern afronden:
Android biedt hier geen gegarandeerde afbreekmogelijkheid. Hun oude
callbacks mogen de gepauzeerde regel niet meer wijzigen: de app
verhoogt eerst de regelrevisie en annuleert daarna uitsluitend voor die
spraakregels de bijbehorende wachtrij- en deadlineverzoeken.
Een daarnaast door de gebruiker **expliciet getypte vertaling** blijft
doorlopen en wordt niet automatisch geannuleerd of gewijzigd.
Dit is een **weergave- en nieuwe-vertaalaanvragen-stop**, geen
onbewijsbare belofte dat eerdere lokale verwerking onmiddellijk stopt.

Met **Hervat live vertalen** worden *alleen nieuw herkende* zinnen
weer lokaal vertaald. Eerder overgeslagen zinnen worden niet stilzwijgend
ingehaald; voor een expliciete hervertaling moet de microfoon gestopt
zijn en kan de gebruiker bij de originele regel op
**▶ Vertaal deze zin alsnog** tikken. De bestaande read-only
modelbeschikbaarheidscontrole en begrensde vertaalwachtrij blijven voor
die actie van kracht. Gewone handmatig getypte vertalingen blijven ook
tijdens de pauze afzonderlijk mogelijk, want daarbij kiest de gebruiker
zelf expliciet de vertaalactie.

De pauzeknop is beschikbaar in het normale gesprek en de fullscreen
leesmodus; hij start of stopt de microfoon niet en verandert geen
Android-vliegtuigmodus of offline spraakpakketten. De keuze leeft
alleen tijdens deze app-sessie, slaat geen tekst op en maakt geen
cloudfallback of download mogelijk.

## Menselijke review bij door Android laag beoordeelde spraak

Androids on-device SpeechRecognizer kan naast de herkende tekst ook
optionele `CONFIDENCE_SCORES` teruggeven. Tuddy koppelt uitsluitend
een **geldige score op dezelfde positie als de daadwerkelijk gekozen
herkenningshypothese** aan de tijdelijke gespreksregel. Een ontbrekende
score, een Android-onbekendwaarde (-1), een niet-eindig getal of een
score buiten 0–1 wordt niet verzonnen of gebruikt.

Een door Android gerapporteerde score onder **0,50** toont een
zichtbare waarschuwing in de continue transcriptweergave, met een
rechtstreekse **Controleer deze herkende zin**-actie. Wanneer luisteren
gestopt is, opent deze actie de bestaande handmatige correctie, waarna
alle normale revisie- en vertaalbeveiligingen blijven gelden. Na een
eigen tekstcorrectie krijgt de menselijke invoer voorrang op de
oude apparaatwaarschuwing. Ook bij pauze van lokale vertaling blijft
de herkenningswaarschuwing bij de originele zin aanwezig.

In **Eén zin opnemen** verschijnt dezelfde waarschuwing al in het
privéreviewvenster, vóór de gebruiker de herkenningssuggestie
goedkeurt of verwerpt. Het scherm behoudt de bewerkbare originele
tekst, alternatieve Android-hypothesen en expliciete bevestiging.
Een niet-gerapporteerde score wordt niet als positief beoordeeld.

**Belangrijk:** Android/leveranciersscores zijn niet noodzakelijk
gekalibreerd en geen vastgestelde kans dat een zin klopt. Hoge scores
bewijzen niets; lage scores kunnen correct gesproken woorden betreffen.
Tuddy verandert geen bronzin automatisch, weigert geen audio,
markeert geen voltooide vertalingen als gevalideerd en verandert
de modellen niet op basis van een score. De metadata blijven
uitsluitend bij RAM-gespreksregels; export en gedeelde tekst nemen
deze scores niet op. Werkelijke spraakkwaliteit vereist menselijke
toestelproeven en onafhankelijke taalreview.

## Gates

Deze mijlpaal vereist de bestaande Android GitHub-build en source-head
checks, maar geen nieuwe testsuites. De voltooide build en een source merge
zijn nadrukkelijk niet hetzelfde als een fysieke Android 12+
vliegtuigmodusproef, een native-speaker-review, een rechtengecontroleerd
Kurmanji/Sorani/Pasjtoe-spraakmodel of een ondertekende productie-release.
