# Tuddy 🎙️

## Nieuw: woord bekijken zolang je het indrukt

De v0.4-functie **vasthouden-en-loslaten** is terug als tijdelijke
offline preview: houd een woord in een ondertitel of getypt
gesprek vast om snel de beperkte ingebouwde betekenis en je
eigen, niet geverifieerde uitleg te zien. Laat los om te
sluiten. **Kort tikken** opent nog steeds de uitgebreide
woordkaart, inclusief bewerken en lokaal voorlezen.
[Bediening en privacy](docs/TRANSIENT_OFFLINE_WORD_PEEK.md).

## Nieuw: bewaarde woorden lokaal horen en oefenen

In **Mijn opgeslagen woorden** kun je elk lokaal bewaard woord
met **🔊 Hoor woord**, **Rustig** en **Stop** uitspreken met een
bestaande geïnstalleerde Android-stem. Dezelfde bediening
zit in **Oefen opgeslagen woorden**. Bij woordwissel,
achtergrond of actieve live microfoon stopt de audio; tijdens
luisteren met de microfoon zijn de knoppen uitgeschakeld.
Geen stemdownload, cloudspraak of nieuwe opslag.
[Werking en beperkingen](docs/OFFLINE_SAVED_WORD_AUDIO.md).

## Nieuw: luister naar je eigen offline oefenkaart

Tijdens **Oefen mijn eigen zinnen** kun je de zichtbare vraag
op verzoek met een lokaal geïnstalleerde Android-stem laten lezen.
Het **antwoord** is pas na **Toon mijn betekenis** afspeelbaar,
normaal of rustig. Kaart-/richtingwissel, beoordelen en
achtergrond sluiten het vorige geluid. Ontbrekende lokale stemmen
betekenen stilte, geen cloudfallback; jouw zinnen zijn niet
automatisch gecontroleerd.
[Bediening en beperkingen](docs/OFFLINE_PRIVATE_PRACTICE_VOICE.md).

## Nieuw: afgeronde ondertitels en handmatig woorden voorlezen

In **▣ Toon deze zin groot** kun je na een tik het afgeronde
origineel of de daadwerkelijk lokaal voltooide vertaling laten
voorlezen. Bij **▸ Lees woord voor woord** kun je ook het
geselecteerde tekstwoord op normaal of rustig tempo laten horen.
Geen automatische audiotiming, extra modeldownload of
netwerkspraak: bij ontbrekende geïnstalleerde offline Android-stem
wordt niets ingesproken. Iedere nieuwe zin/woordselectie en
achtergrondgang stopt het vorige geluid.
[Lees hoe dit werkt](docs/OFFLINE_AUDIO_COMPLETED_CAPTIONS.md).

## Nieuw: offline gesprekskaarten op verzoek voorlezen

De grote eigen-zinnenkaart én de vijf ingebouwde voorbeeldwoorden
bieden nu **🔊 Zin/Bronwoord**, **🔊 Betekenis/Doelwoord**, **Rustig**
en **Stop**. De app gebruikt alleen een stem die Android zelf als
reeds geïnstalleerd en niet-netwerkafhankelijk meldt. Als een
Kurmanji-, Sorani- of Pasjtoe-stem ontbreekt, blijft de knop zonder
online uitwijkroute stil en verschijnt een melding. Wisselen van
kaart, achtergrond en sluiten stoppen het geluid. Eigen betekenissen
zijn **niet geverifieerd**.
[Handleiding en privacy](docs/PRONOUNCE_OFFLINE_PHRASE_CARDS.md).

## Nieuw: handmatig woord voor woord lezen

In een grote, afgeronde gesprekskaart kun je bewust **▸ Lees woord voor woord**
kiezen. Blader met **← Woord** en **Woord →** door de bestaande oorspronkelijke
tekst of een daadwerkelijk lokaal voltooide vertaling. Elke taal begint bij
het eerste tekstwoord, en nieuwe zinnen starten opnieuw. De markering is
**niet** gesynchroniseerd met audio en stelt geen uitgelijnde vertaling of
native-speakercontrole voor.
[Bediening en beperkingen](docs/MANUAL_WORD_STEP_CAPTION.md).

## Nieuw: vijf basiswoorden extra groot presenteren

De vijf ingebouwde offline Kurmanji-, Sorani- en Pasjtoe-woorden
kun je vanuit het basiswoordenbord nu **▣ groot tonen**, met
**Vorige / Volgende**, bronkeuze Nederlands/Engels,
volgordewissel en groter/kleiner schrift. Alleen de bestaande vijf
niet-onafhankelijk-gecontroleerde voorbeelden; geen extra taalmodel
of microfoon. [Meer informatie](docs/FULLSCREEN_OFFLINE_STARTER_CARDS.md).

## Nieuw: mijn offline gesprekskaarten terugvinden

Je kunt eigen Kurmanji-, Sorani- en Pasjtoe-zinnen nu tegelijk
doorzoeken, met meerdere zoekwoorden en optioneel alleen favorieten.
**Toon groot** bladert uitsluitend door de gefilterde resultaten
(inclusief de gekozen taalvolgorde), niet stilzwijgend door andere
privézinnen. De teller laat zien of er meer dan 80 kaarten zijn;
**Toon volgende kaarten** laat die op verzoek zien. Alle betekenissen
blijven door jezelf geschreven en ongeverifieerd.
[Bediening en privacy](docs/PRIVATE_PHRASE_DISCOVERY.md).

## Nieuw: mijn eigen uitlegregels doorzoeken

Heb je in de huidige sessie één of meer eigen betekenissen bij
ondertitels gezet? Met **✎ N eigen uitleg** vind je alle persoonlijke
uitlegregels terug in één tijdelijke, doorzoekbare weergave.
Zoek op herkende tekst, modelvertaling of je eigen betekenis;
open een volledige tweetalige kaart of wijzig/wis de eigen notitie.
Een onvoltooide vertaling wordt niet als afgerond gepresenteerd.

Dit overzicht is alleen een **read-only index van het lopende
gesprek**, geen opgeslagen woordenboek, automatische controle
of nieuwe export. Achtergrond/sluiten beëindigt het overzicht en
de bestaande privacy- en modelgates blijven intact.
[Bediening en privacy](docs/SESSION_MEANING_INDEX.md).

## Nieuw: eigen uitleg naast een afgeronde gesprekszin

Tuddy heeft nu een tweede, onafhankelijke mogelijkheid om een zin te
verduidelijken: na het stoppen van de microfoon kun je bij een
gespreksregel op **✎ Eigen uitleg** drukken en je eigen korte
interpretatie (max. 280 tekens) toevoegen. De originele woorden en de
automatische offline modelvertaling blijven zichtbaar en onveranderd.
Je notitie krijgt overal de waarschuwing **niet geverifieerd**,
ook in de grote tweetalige gesprekskaart en leesmodus. Je kunt haar
later wijzigen of wissen.

Een notitie verdwijnt wanneer je de originele zin corrigeert, die
regel verwijdert of het gesprek wist. Hij blijft alleen in het
tijdelijke sessiegeheugen; er is geen opslag, netwerk, nieuw model,
verificatieclaim of automatische export. **De notitie verschijnt niet
in bestaande gesprek-/SRT-/WebVTT-bestanden of deelacties.**
[Werking en privacygrenzen](docs/SESSION_MANUAL_MEANING.md).


**Tuddy** is the Android-first, privacy-focused realtime translation and language-learning app previously prototyped as **LiveLingo**.

It aims to display speech and its translation together like synchronized lyrics, with a selected target language, automatic source-language detection, scrollable conversation history, and compact word cards. The project prioritizes newcomer and refugee language access.

## Nieuw: een afgeronde gesprekszin groot tonen

Tik in de gesprekstijdlijn bij een afgeronde zin op
**▣ Toon deze zin groot** om zonder delen of exporteren tijdelijk
een leesbare schermvullende kaart te tonen. Kies origineel, vertaling
of beide, grotere tekst en bladeren door andere afgeronde zinnen.
De microfoon wordt gestopt, er wordt niets automatisch bewaard.
[Uitleg en grenzen](docs/FULLSCREEN_SINGLE_CAPTION_CARD.md).

## Nieuw: afdrukbaar offline tweetalig zinnenboek

Bij **Mijn eigen offline zinnen** kan de gebruiker een zelfstandig
HTML-zinnenboek bewaren van zelfgeschreven Kurmanji/Sorani/Pasjtoe-zinnen
en hun eigen NL/EN-betekenissen, als afdrukbare tweetalige kaarten.
Kies bewust één taal of alle talen en de Android-opslaglocatie.
Zinnen en betekenissen worden **niet als geverifieerd gepresenteerd**;
er is geen internet of extra model nodig. Het externe bestand is
geen Tuddy-back-up en valt niet onder het wissen van de app.
[Bediening en privacy](docs/PRIVATE_PRINTABLE_PHRASEBOOK.md).

## Nieuw: eigen gesprekskaarten direct zoeken

Op het hoofdscherm opent **▣ Mijn gesprekskaarten · direct zoeken en tonen**
een snelle, volledig offline zoekweergave voor bewust opgeslagen
Kurmanji-, Sorani- en Pasjtoe-zinnen. Je kunt rechtstreeks een grote
tweetalige kaart laten zien of je eigen zinnen beheren. Dit gebruikt
geen live transcriptopslag, spraakherkenning, cloud of automatische
vertaling. Eigen betekenissen zijn niet geverifieerd.
[Meer informatie](docs/OFFLINE_BILINGUAL_PHRASE_PRESENTER.md).

## Nieuw: eigen offline woordenboek

Naast de kleine ingebouwde woordenlijst kunnen gebruikers nu zelf
**woordbetekenissen** invoeren, zoeken, bewerken en wissen. Een tik op
een ondertitel toont een eigen notitie als deze bestaat en biedt een
expliciete knop om een betekenis toe te voegen. Eigen woorden kunnen
ook met de offline oefenkaarten worden geleerd; de app houdt deze
notities duidelijk gescheiden van de gecontroleerde basiswoorden.
Alles blijft privé op het toestel totdat de gebruiker zelf iets invoert.
[Werking, grenzen en privacy](docs/PRIVATE_OWN_WORD_NOTES.md).

## Nieuw: tweetalige SRT-ondertitelconcepten

Bij **Gesprekstools → Bewaar SRT-ondertitelconcept…** kun je van
voltooide originele en lokaal vertaalde zinnen een SRT-bestand maken.
Dit is alleen na aparte privacytoestemming en via je eigen Android-
bestandskiezer; er is geen automatische export. De cue-tijden zijn
**ruwe schattingen gebaseerd op het moment dat spraakherkenning klaar
was** en absoluut geen audio-/woord-synchronisatie. Controleer de
vertaling en lijn de tijden handmatig uit in je montageprogramma.
[Werking en privacygrenzen](docs/PRIVATE_BILINGUAL_SRT_DRAFTS.md).

## Nieuw: één privéondertitel verwijderen

Per ondertitel kun je met **🗑 Wis deze regel** een afzonderlijke
gespreksregel na expliciete bevestiging uit het tijdelijke geheugen wissen.
Dit bewaart de andere gespreksturns en opgeslagen woorden. De bevestiging
controleert de actuele tekst en revisie voordat de regel wordt verwijderd;
oude lokale vertaalcallbacks kunnen die regel niet herstellen. Nog niet
voltooide export- en deelacties worden ingetrokken, maar bestaande externe
kopieën en klembordinhoud kunnen niet achteraf worden gewist.
[Privacy en bediening](docs/PRIVATE_SINGLE_CAPTION_DELETE.md).

## Geen automatische vertaalmodeldownloads meer

Een nieuwe ondertitel, handmatig getypte zin of correctie probeert
niet langer stilzwijgend een ML Kit-teksttaalmodel op te halen.
Tuddy controleert eerst de werkelijk geïnstalleerde modellen voor
de herkende bron en het gekozen doel. Ontbreekt iets, dan toont
de zin **Model ontbreekt** met de benodigde talen. De gebruiker
kiest daarna zélf of hij het ontbrekende model via wifi voorbereidt.
Ook bij tijdelijk onbekende modelstatus is er geen cloudfallback.
[Exact gedrag en grenzen](docs/EXPLICIT_MODEL_DOWNLOAD_ONLY.md).

## Nieuw: uitspraak oefenen met lokaal Android-voorlezen

Bij elke ondertitel kun je via **🔊 Lokaal voorlezen · kies tempo**
de oorspronkelijke zin of een afgeronde vertaling laten uitspreken,
in normaal of rustig tempo. De app biedt alleen al geïnstalleerde,
niet-netwerkafhankelijke Android TTS-stemmen aan. Ontbreekt een
bruikbare lokale stem, dan blijft voorlezen uit en volgt **geen**
online fallback. Een eigen stopactie beëindigt de uitspraak.
Er wordt niets als audio bewaard.
[Bediening en beperkingen](docs/OFFLINE_PRONUNCIATION_CHOICES.md).

## Nieuw: tweetalig lezen naast elkaar

In **⛶ Leesmodus** komen het werkelijk herkende origineel en de
lokale vertaling op brede schermen (vanaf 600 dp) in twee kolommen
**naast elkaar**. Je kunt daar zelf tussen kolommen of de normale
weergave onder elkaar wisselen. Spraak-/vertalingsstatus, eigen
sprekerlabels en de live-woordfocus blijven zichtbaar; onvoltooide
vertalingen worden niet ingevuld met verzonnen tekst. Op smalle
schermen blijft de vertrouwde verticale layout.
[Uitleg en privacygrenzen](docs/BILINGUAL_PARALLEL_READING.md).

## Nieuw: privé-oefenkaarten zonder internet

Met **🧠 Oefenkaarten** oefen je de woorden die je bewust hebt
bewaard uit de ingebouwde woordenlijst en je eigen notities
voor Kurmanji, Sorani en Pasjtoe. Wissel tussen kaarten voor
woorden, eigen zinnen of alles. Bekijk eerst het antwoord en
geef zelf **Ken ik ✓** of **Nog eens** aan. Kies nu ook
**Antwoord typen** om een woord of eigen zin letterlijk te reconstrueren,
je invoer zonder netwerk met je opgeslagen antwoord te vergelijken,
en moeilijke kaarten te herhalen of over te slaan. Dit beoordeelt
alleen tekstgelijkheid, niet de werkelijke betekenis of uitspraak.
Eigen betekenissen zijn niet door Tuddy vertaald of gecontroleerd.
Geen score of getypt antwoord wordt opgeslagen of verstuurd.
Na het onthullen van een kaart kun je het opgeslagen woord of je eigen zin
ook normaal of rustig **lokaal laten voorlezen** met een al geïnstalleerde
Android-stem; zonder passende offline stem blijft Tuddy stil. Sluiten of
wisselen van kaart stopt de uitspraak.
[Bediening en grenzen](docs/OFFLINE_STUDY_FLASHCARDS.md).

## Nieuw: bewegende woordfocus in het gewone gesprek

De actuele voorlopige spraakregel krijgt nu ook buiten de fullscreen
leesmodus een bewegende nadruk op het **laatste herkende tekstwoord**.
Het hergebruikt dezelfde Unicode-tekstfocus als de grote leesmodus,
maar met compactere letters. Dit is geen exacte audio-tijdsynchronisatie
en markeert de vertaling niet op verzonnen woordposities.
[Bediening en grenzen](docs/INLINE_LIVE_WORD_FOCUS.md).

## Nieuw: 5 offline basiswoorden direct bereikbaar

Het gespreksscherm heeft nu **🌍 5 basiswoorden · Kurmanji, Sorani,
Pasjtoe**. Eén tik toont alle vijf bestaande, exact gebundelde
basiswoorden (hallo, bedankt, ja, nee, water) in de gekozen taal,
met Nederlandse of Engelse brontekst. Dit werkt zonder microfoon,
wifi of taalmodel en biedt een route naar de eigen offline
oefenzinnen. Het is **geen** vrije vertaalmachine, geen bewijs van
spraakondersteuning of moedertaalsprekeracceptatie.
[Overzicht en grenzen](docs/COMMUNITY_STARTER_PHRASEBOARD.md).


## Nieuw: Syrische en Palestijnse Arabische spraakkandidaten

Naast algemeen Arabisch zijn `ar-SY` en `ar-PS` nu apart kiesbaar
als **Android-regiokandidaten** uit het originele LiveLingo-v0.4-
bronarchief. Tuddy controleert per kandidaat de werkelijk aanwezige
lokale Android-spraakpakketten en weigert cloudfallback. ML Kit biedt
nog steeds uitsluitend generiek Arabische tekstvertaling, **geen**
bewezen dialect-specifieke ASR/MT-dekking.
[Bediening en beperkingen](docs/LEVANTINE_SPEECH_CANDIDATES.md).

## Nieuw: optionele limiet voor microfoonopvang

Onder **Taal- en modelinstellingen → Privacy** kun je een
maximale luistersessie van **5, 15 of 30 minuten** kiezen,
of **Geen** voor bestaande werking. De timer loopt door
over automatische herkenner-herstarts en stopt bij
aflopen echt de on-device microfoon; het tijdelijke
gesprek blijft behouden. Een wijziging tijdens luisteren
begint opnieuw vanaf het moment van kiezen.
Alleen de gekozen minutenwaarde wordt privé opgeslagen.
[Werking en grenzen](docs/MICROPHONE_SESSION_LIMIT.md).

## Nieuw: optioneel trilsignaal bij nieuwe spraakzin

Via **Taal- en modelinstellingen → Toegankelijkheid** kun je een
kort trilalarm aanzetten als de lokale Android-herkenner een
**afgeronde** ondertitel oplevert. Maximaal één puls per drie
seconden, niet bij voorlopige woorden of vertaalupdates.
Standaard uit; de voorbeeldtrilling wordt alleen op je eigen
tik afgespeeld. Er worden geen woorden, audio, tijdstempels
of extra persoonlijke gegevens opgeslagen. Android kan haptiek
dempen; dit is geen garantie op geslaagde herkenning.
[Uitleg en grenzen](docs/COMPLETED_CAPTION_HAPTICS.md).

## Nieuw: optionele privéweergave

Via **Taal- en modelinstellingen → Privacy · privéweergave** kun je
Androids schermbeveiliging voor Tuddy aan- of uitzetten. Indien aan,
vraagt de app het besturingssysteem screenshots, standaard
schermopnames en recente-appsvoorbeelden te blokkeren. De keuze
blijft lokaal op het toestel bewaard; geen transcript of audio
wordt voor deze functie opgeslagen. Externe camera's of andere
toestelafwijkingen kunnen nooit volledig worden tegengehouden.
[Bediening en beperkingen](docs/PRIVATE_SCREEN_SHIELD.md).


## Nieuw: offline startcheck vanaf het hoofdscherm

De knop **Offline startcheck** laat zien of de gekozen Android-spreektaal
daadwerkelijk lokaal beschikbaar is én of de benodigde ML Kit-tekstmodellen
al op dit toestel staan. Je kunt beide statussen apart of samen opnieuw
controleren. Ontbrekende pakketten kunnen uitsluitend via een expliciete
wifi-downloadknop worden voorbereid. Het venster leidt ook naar de
taalinstellingen en naar de modus **Typ zin zonder microfoon**.
Android 12 meldt geen verifieerbare spraakpakketlijst en wordt daarom
niet als 'klaar' weergegeven. De controle bewijst geen werkende
vliegtuigmodus of vrij te vertalen Koerdische/Pasjtoe spraak.
[Bediening en beperkingen](docs/OFFLINE_START_CHECK.md).

## Nieuw: lokaal opgeslagen oefengegevens bewust wissen

Onder **Taal- en modelinstellingen → Privacy** kun je na afzonderlijke
bevestiging alle privébasiswoorden, zelfgemaakte oefenzinnen en favoriete
taalparen tegelijk verwijderen. Tuddy meldt per opslagcategorie
eventuele mislukte verwijderingen; het huidige gesprek, de actieve
taalkeuze en eerder gemaakte externe exports blijven intact.
[Bediening en privacygrenzen](docs/PRIVATE_STUDY_DATA_ERASURE.md).

## Nieuw: favoriete taalparen

Gebruik **☆ Favoriete taalparen** om maximaal drie privécombinaties van
spreektaal en vertaaltaal vast te leggen, te kiezen of te verwijderen.
Een wissel stopt de microfoon, houdt het oude gesprek met oorspronkelijke
taalrichting intact en vraagt daarna om een nieuwe controle van spraak-
én tekstmodel. Er wordt niets automatisch gedownload. Alleen de taalcodes
worden privé bewaard, geen transcript of stemgegevens.
[Bediening en grenzen](docs/FAVORITE_LANGUAGE_PAIRS.md).

## Nieuw: tekst vanuit een andere Android-app delen

**Delen → Tuddy** verschijnt voor één platte tekst in het Android-deelmenu.
Ook via **tekst selecteren → Tuddy** kan geselecteerde tekst uit andere
Android-apps via de systeemactie 'Tekst verwerken' worden geopend.
De gedeelde tekst wordt eerst als **bewerkbaar, tijdelijk concept** getoond;
pas na **Vertaal lokaal** volgt de bestaande offline ML Kit-tekstvertaling.
Annuleren of de app verlaten wist het open concept. Ongeldige en te lange
inhoud wordt geweigerd; geen automatische URL-/bijlagenimport, cloudspraak
of externe bestandsopslag.
[Privacy, bediening en beperkingen](docs/ANDROID_TEXT_SHARING.md).

## Nieuw: offline tekstmodelopslag beheren

Via **Taal- en modelinstellingen → Beheer offline tekstmodelopslag**
zie je de echt geïnstalleerde ML Kit-tekstmodellen. Ongebruikte modellen
kun je **per taal, na afzonderlijke bevestiging** verwijderen. Het actuele
bron-/doeltaalpaar blijft beschermd. De actie is uitgeschakeld bij luisteren,
modelvoorbereiding of een al lopende opslagactie; er is geen automatische
opruiming en geen netwerkspraak. Je huidige gesprek en privéwoorden blijven
staan, maar later gebruik kan opnieuw een modeldownload via wifi vragen.
Dit beheert geen Android-spraakpakketten en belooft geen gemeten opslagwinst.
[Uitleg en beperkingen](docs/OFFLINE_TEXT_MODEL_STORAGE.md).
Nog geen fysiek toestelbewijs of getekende release.

## Nieuw: zinnen handmatig corrigeren

Bij een definitieve gespreksregel kun je met **✎ Corrigeer tekst**
de oorspronkelijke zin in het tijdelijke gesprek herstellen en de
tekst lokaal opnieuw laten vertalen. Voor regels met **Fout** of
**Model ontbreekt** is er ook **↻ Probeer opnieuw**, zonder de tekst
opnieuw in te typen. Oude, vertraagde resultaten kunnen de nieuwere
correctie niet overschrijven; een handmatige correctie krijgt een
zichtbaar label. De microfoon moet uit staan. Dit is geen echte
spraakmodelverbetering of menselijke tolkvalidatie.
[Bediening, privacy en grenzen](docs/CAPTION_CORRECTIONS.md).

## Nieuw: zin typen zonder microfoon

Tik in het gesprekscherm op **✍ Typ zin · zonder microfoon**. Hiermee open
je direct een privacyvriendelijk invoervenster zonder Android-microfoon,
zelfs op telefoons zonder on-device spraakherkenning. Je kiest eerst in de
instellingen de bron- en doeltaal. Een ingevoerde zin wordt exact volgens
de bestaande invoergrenzen als originele ondertitel in het tijdelijke
gesprek toegevoegd. De app probeert met de bestaande ML Kit-pijplijn
**lokale tekstvertaling** voor dat taalpaar; indien het model nog ontbreekt
kan de eenmalige download eerst wifi vragen. Mislukt de vertaling, dan
blijft de invoer als onvoltooide regel herkenbaar.

Het invoervenster kan alleen worden geopend wanneer de microfoon uitstaat.
Annuleren, gesprek wissen of verlaten van de app verwijdert de tijdelijke
concepttekst. Er is geen stilzwijgende server-spraakherkenning, opslag van
de concepttekst of onbewezen vrije Koerdische/Pasjtoe-vertaling. De optie
**Zin typen** onder de modelinstellingen gebruikt dezelfde invoerfunctie.
Een medische of juridische vertaling blijft ongeschikt zonder menselijke
controle. Fysieke Android-acceptatie en release/signing staan apart.

## Nieuw: mobiel transcript-eerst gesprekscherm

Op kleinere telefoons blijft de live ondertiteltijdlijn zichtbaar: **Luister,
Wis gesprek, Volg live, ⛶ Leesmodus en ⇄ Wissel richting** blijven bereikbaar.
De knop **Gesprekstools · export, sprekers en archief** opent een begrensd,
intern scrollbaar paneel voor de bestaande spreker-navigatie, eigen TXT/JSON-
export, privéarchief en eventueel apart gelabelde export, plus nieuwe
archiefacties. Het paneel is standaard gesloten; ook de taal- en
modelinstellingen blijven afzonderlijk uitklapbaar. Beide grote panelen
sluiten elkaar om niet tegelijk de ruimte boven de live tekst op te eisen.
Alle bestaande privacybevestigingen, documentkiezers en rechten blijven
onveranderd. Dit is broncode, geen fysiek geïnstalleerde/release-APK.

## Nieuw: schermvullend live meelezen

Open **⛶ Leesmodus · schermvullende ondertitels** voor een rustige weergave
van de oorspronkelijke spraak en, zodra werkelijk lokaal voltooid, de
vertaling in grote kaarten. Live tussenresultaten blijven expliciet
onafgerond. Wanneer je zelf omhoog scrolt, stopt het automatisch volgen;
met **Volg live** spring je terug naar de nieuwste regel. **■ Stop
microfoon** stopt de reeds actieve herkenning ook vanuit deze weergave;
**Sluiten** keert terug zonder bestaande ondertitels te wissen.
In de leesmodus staat bij elke afgeronde regel het handmatig gekozen
label **A/B/onbekend**. Via **Alle sprekers** kun je de tijdelijke tijdlijn
filteren op A, B of nog onbekend; de originele live tussenregel verschijnt
uitsluitend in de ongefilterde tijdlijn, want die heeft nog geen stabiel
sprekerlabel. Alle labels en filters blijven RAM-only; ze zijn geen
automatische stem- of persoonsidentificatie.
Het scherm gebruikt dezelfde gekozen ondertitelgrootte, bewaart geen
gesprekken, kent geen automatische speaker-ID en vraagt geen extra
Android-rechten. Een fysieke toestelproef en getekende release blijven open.

## Nieuw: veilige tweeweg-gesprekken

Met **⇄ Wissel richting** kun je bij ondersteunde spreektaalparen zonder
gespreksverlies de oorspronkelijke spreek- en vertaaltaal omdraaien.
De microfoon stopt eerst; de nieuwe richting vereist opnieuw controle van
de echte offline spraak- en tekstmodellen op het apparaat. Iedere voltooide
ondertitel blijft zijn **eigen** taalrichting tonen. 59 tekstdoelen zijn
niet hetzelfde als 59 on-device spraaktalen: de knop is alleen beschikbaar
voor dertien expliciet kiesbare regionale spraakkandidaten: elf oorspronkelijke talen (NL/EN/DE/FR/ES/AR/FA/UK/TR/RU/UR), met voor Arabisch ook de afzonderlijke historische regiocodes ar-SY en ar-PS. Android moet het exacte lokale spraakpakket voor iedere kandidaat daadwerkelijk melden; de picker bewijst dat niet. Zie
[de handleiding voor tweeweg-gesprekken](docs/TWO_WAY_OFFLINE_CONVERSATION.md).
Dit bewijst geen telefoonacceptatie of Koerdische/Pasjtoe vrije vertaling.

## Nieuw: één regel uit een privé-archief kopiëren of delen

In het eigen **read-only JSON-archief** kunnen gebruikers na selectie
van een **afgeronde** tweetalige regel kiezen voor
**Gebruik deze archiefregel…**. Een aparte toestemming is vereist voordat
die ene regel naar het Android-klembord of de deelkiezer kan gaan.
De payload meldt dat de inhoud afkomstig is uit een **niet onafhankelijk
geverifieerd, door de gebruiker gekozen archiefbestand**.
De huidige live-sessie wordt niet gewijzigd; de actie wordt geannuleerd
bij achtergrond, gesprek wissen of archief sluiten.
[Privacy en bediening](docs/PRIVATE_ARCHIVE_EXCERPTS.md).

## Nieuw: privégesprek exporteren als JSON

Naast de bestaande tekstexport heeft het gesprekscherm nu **Bewaar JSON…**.
Je kiest zelf via Android waar de JSON met maximaal 500 afgeronde regels
wordt opgeslagen.
Met **Open JSON-archief…** kun je een zelf gekozen, eerder opgeslagen bestand
begrensd en alleen-lezen bekijken, zonder het lopende gesprek te vervangen.
Het archief heeft nu ook een **tweetalige zoekbalk**, resultaatentelling en
per uitspraak datum/tijd; zoekopdrachten blijven volledig lokaal in RAM.
Ongeldige, gewijzigde of te grote archieven worden geweigerd; de inhoud
is door de gebruiker aangeleverd en geen bewijs van echte spraakopname. Per regel blijven originele taal, doeltaal, tijdstempel,
originele tekst en een eventuele **voltooide** vertaling gekoppeld.
Nog lopende of mislukte vertalingen worden expliciet `null`, niet stilzwijgend
als voltooid beschouwd. Export is opt-in; er wordt niets automatisch
gesynchroniseerd of bewaard. Het gekozen bestandsplatform kan **wel**
privégesprekken buiten Tuddy synchroniseren. Lees
[de JSON-format- en privacyuitleg](docs/PRIVATE_TRANSCRIPT_JSON.md).
Dit is broncode-functionaliteit, geen bewijs van een toestelinstallatie.

## Status — 2026-10-17

**Native Android debug APK available; prototype NOT device-validated or released.**
Source `main@b1a298a907f8ff2f7459b9c934f56fafb462f502` passed hosted
[Android APK CI #39064114978](https://github.com/Zennay/Tuddy/actions/runs/39064114978)
(`testDebugUnitTest` + `assembleDebug`).
Artifact **`Tuddy-debug-apk`**, GitHub ID **12112063195**, contains the debug APK,
its SHA256 receipt and the exact source-commit receipt. **APK SHA256:**
`fdc2dc8d4ec7de7293afca9ce3ed1aad832e21c3d0ba1aedef2210bc3b120bf6`.
This checksum is for `app-debug.apk`, not for the enclosing ZIP.
A successful hosted build does **not** prove Android 12+ airplane-mode behavior,
microphone usability, speech model coverage, device performance or release readiness.

### Implemented and validated in source

- Native Android on-device-only `SpeechRecognizer`, continuous captioning across
  sentences with bounded retries for silence and fail-closed permission/language
  errors. Background, stop and session clearing cancel queued restarts.
- ML Kit **on-device text** translation for 59 supported target languages
  after required model downloads. Offline speech recognition coverage remains
  independent and device-/speech-pack-specific.
- Bilingual live/interim/final captions; 'Volg live' respects the user's pause,
  follows growing interim text and works with a 500-line RAM-only history.
- The chosen spoken source and text target survive process restarts as two
  validated private preference codes (restoring unknown/stale codes falls back
  to reviewed defaults); microphone audio, captions and word lookups remain
  RAM-only and are never restored from storage.
- Small, offline, explicitly **five-phrase** NL/EN → Kurmanji/Sorani/Pashto
  preview. This is NOT arbitrary-sentence translation or offline recognition.
- Tap-to-define bilingual text with accessible manual word entry, Unicode
  selection and a small curated offline NL/EN/DE/FR/ES glossary. Unknown words,
  Arabic/Kurdish/Pashto dictionary coverage and sensitive medical/legal text
  never receive invented explanations. Captions preserve per-line language IDs.
- Source integration PRs [#9](https://github.com/Zennay/Tuddy/pull/9),
  [#10](https://github.com/Zennay/Tuddy/pull/10),
  [#11](https://github.com/Zennay/Tuddy/pull/11),
  [#12](https://github.com/Zennay/Tuddy/pull/12),
  [#13](https://github.com/Zennay/Tuddy/pull/13),
  [#14](https://github.com/Zennay/Tuddy/pull/14),
  [#15](https://github.com/Zennay/Tuddy/pull/15),
  [#16](https://github.com/Zennay/Tuddy/pull/16) and
  [#17](https://github.com/Zennay/Tuddy/pull/17). Current integrated main
  has **29/29** Python quality checks passing in an isolated checkout.
  These complement, not replace, hosted Android unit tests.

### New source feature: on-device speech-pack preparation (P1 #2 partial)

- Android 13+ now has a user-initiated **Controleer spraak** action
  reporting Android's actual installed/pending/downloadable/online-only
  on-device source language packs. Unavailable coverage stays unknown.
- A missing supported pack gets an explicit **Download via wifi** action.
  The device recognizer manages model provisioning; Android 14+ progress
  is visible, Android 13 requests must be checked again. Status is
  invalidated after a language switch or background lifecycle.
- It is still **not** free-form Kurmanji, Sorani or Pashto ASR/translation.
  Those languages remain visibly limited to the five-phrase preview.
  See [offline speech pack guide](docs/OFFLINE_SPEECH_PACKS.md).
- Device execution and end-to-end airplane-mode speech remain **unverified**;
  issue #2 stays open until independently licensed local ASR + MT actually
  works for all three communities.

### Additional offline setup: actual ML Kit text-model inventory

A second operator action, **Controleer tekst**, now queries the on-device
ML Kit downloaded translation models for the selected pair. The screen
shows exactly which non-English models are missing. After **Model via wifi**
Tuddy automatically reads the inventory again; completing a download
request is not by itself a readiness claim. Stale callbacks are canceled
on Stop, backgrounding and language changes. Model inventory and speech
readiness remain independent.

See [offline text readiness](docs/OFFLINE_TEXT_MODEL_READINESS.md).

### Open release and product gates

1. Use [docs/DEVICE_VALIDATION.md](docs/DEVICE_VALIDATION.md) with the exact
   APK checksum above on a **physical Android 12+ device**. Record ten
   airplane-mode utterances, multiple continuous restarts, model setup,
   language/permission lifecycle, TalkBack word card and session privacy;
   do not claim a device result until that evidence exists.
2. Finish full historical Android v0.4 source integration. The original
   ZIP remains in ChatGPT Library at
   `/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`;
   the imported conversation and phrasebook modules do NOT constitute a
   wholesale migration of its previous UI/viewmodel/speech architecture.
3. Deliver separately licensed and on-device-tested free-form Kurdish
   Kurmanji, Sorani and Pashto **speech recognition AND translation**
   with native-speaker/medical/legal accuracy review. Five beginner phrases
   are not that implementation. Keep [issue #2](https://github.com/Zennay/Tuddy/issues/2)
   open and labels honest.
4. Keep [issue #1](https://github.com/Zennay/Tuddy/issues/1) and zCloud production
   admission/release holds open until an actual device acceptance and owner
   approval; no automatic VPS workload or signed production deployment.

Notion project: https://app.notion.com/p/3f89e19ac955810aa4a5e53871966f8f

Earlier project tracking: https://github.com/Zennay/zCloud/issues/1344 and https://github.com/Zennay/zCloud/issues/1350

## Privacy and accuracy

No cloud transcription fallback is intended. Text/model downloads may require internet once. Accuracy for legal, asylum, and medical contexts is not guaranteed; a qualified interpreter or native-speaker review remains essential.


### Offline woorden bewaren en oefenen (nieuw)

Tik op een woord in een afgeronde oorspronkelijke of vertaalde ondertitel om de
**lokale woordkaart** te openen. Alleen als de kleine, handmatig gecontroleerde
NL/EN/DE/FR/ES woordenlijst de betekenis daadwerkelijk bevat, verschijnt
**Bewaar woord**. Bewaren is uitdrukkelijk opt-in. Via **Mijn woorden** kun je
woorden offline terugvinden, jezelf oefenen door betekenissen te onthullen,
woorden afzonderlijk verwijderen of na bevestiging de hele lijst wissen.

Alleen maximaal 120 unieke `taal|woord`-sleutels worden opgeslagen in private
Android appvoorkeuren; de betekenis komt telkens opnieuw uit de ingebouwde
woordenlijst. Er wordt **geen gesprek, audio, contextzin, providerantwoord,
persoonlijke identiteit of gespreksgeschiedenis** mee opgeslagen, en er is geen
cloud-woordenboek of netwerkverzoek. Android-backup blijft uitgeschakeld.
Gesprekken blijven standaard RAM-only; de bestaande expliciete export is een
afzonderlijke gebruikersactie. Niet bekende woorden en Koerdische/Pasjtoe
uitdrukkingen mogen geen verzonnen definities krijgen. Apparaatacceptatie en
native-speaker review zijn nog niet bewezen.


### Instelbare, beter leesbare ondertitels

Onder **Taal- en modelinstellingen → Ondertitelgrootte** kies je
**Normaal / Groot / Extra groot**. Dit past onmiddellijk de live
oorspronkelijke tekst én vertaling, het leesbare privé-archief en de
tijdelijk gemarkeerde zinnen aan. Alleen de schermvoorkeur wordt
privé onthouden, nooit het transcript of audio. De modeluitkomst blijft
gelijk. [Gebruik en privacy](docs/CAPTION_READING_SIZE.md). Echte
Android-toegankelijkheidsacceptatie blijft nog afzonderlijk vereist.

### Tijdelijke markeringen voor een belangrijk gespreksmoment

Je kunt nu een afgeronde tweetalige uitspraak **☆ Markeer** geven,
via **★ gemarkeerd** direct terugvinden, naar de oorspronkelijke
tijdlijnpositie springen en de geselecteerde regels bewust als TXT
opslaan. Maximaal 80 markeringen horen alleen bij dit gesprek en
verdwijnen bij **Wis gesprek**, als de oudste zin uit het geheugen
rolt of bij app-afsluiting. Er is geen automatische opslag of netwerk,
en lopende vertalingen zijn nooit als afgerond gelabeld.
[Lees de privacy- en gebruiksinstructies](docs/SESSION_CAPTION_BOOKMARKS.md).

### Zelfgekozen leesbaar woordenblad

Via **Mijn woorden → Bewaar leesbaar woordenblad…** kun je na een
expliciete Android-bestandskeuze een TXT maken met uitsluitend
zelf opgeslagen, door het ingebouwde NL/EN/DE/FR/ES-miniwoordenboek
herkende woorden en hun reeds aanwezige betekenissen. Dit is een
**lees- en printbestand**, geen vervanging van de JSON-herstelback-up.
Het bevat geen gesprek, audio, zelfgeschreven oefenzinnen of
leerresultaten. Een gekozen externe bestandsprovider kan synchroniseren.
[Uitleg en privacy](docs/PRIVATE_VOCABULARY_STUDY_SHEET.md).

### Nieuwe offline woordoefening

Via **Mijn woorden → Oefen opgeslagen woorden** kun je nu echt oefenen
met maximaal 20 eerder bewaarde, lokaal gecontroleerde woorden per ronde.
Na **Toon betekenis** beoordeel je zelf **Ik wist het** of **Nog oefenen**;
lastige woorden keren maximaal tweemaal terug. Je ziet je voortgang en
eindoverzicht, kunt een nieuwe ronde doen of de volgende set oefenen.
Oefenresultaten blijven alleen in RAM gedurende het oefenvenster;
gesprekken, audio, scores en leerhistorie gaan nooit naar een server
of het bestaande JSON-back-upbestand. Dit is geen automatische
spaced-repetition-over-dagen of formele taaltoets.
[Uitleg en beperkingen](docs/OFFLINE_WORD_PRACTICE.md).

### Woorden oefenen én back-uppen op eigen verzoek

De bestaande knop **Mijn woorden** biedt nu ook **Bewaar back-up…** en
**Herstel back-up…** voor alleen de zelf opgeslagen taal/woord-sleutels
uit het kleine lokale, handmatig gecontroleerde woordenboek.
Herstel controleert eerst bestandsversie, bekende woorden, dubbelen en
UTF-8-inhoud, toont hoeveel woorden geladen worden en vraagt vervolgens
apart om de bestaande lijst werkelijk te vervangen. Annuleren raakt de
woorden niet. Definities komen altijd uit de lokaal geïnstalleerde
woordenlijst, niet uit het gekozen bestand. Geen gesprekken, audio of
persoonlijke zinnen worden geëxporteerd of geïmporteerd. Android vraagt
zelf om een opslaglocatie; een externe bestandsprovider kan wel
synchroniseren. Zie [woordback-uphandleiding](docs/PRIVATE_WORD_BACKUP.md).
Fysieke toestelacceptatie en release-APK blijven afzonderlijk vereist.

### Voorlezen van afgeronde vertalingen (op eigen verzoek)

Bij een definitieve lokaal vertaalde regel kun je nu op
**Spreek vertaling uit** tikken. Tuddy vraagt pas dan de standaard
Android TextToSpeech-dienst om **reeds geïnstalleerde** stemmen te tonen.
De app accepteert uitsluitend stemmen die die dienst zelf als
`isNetworkConnectionRequired=false` en niet als 'not installed' markeert,
voor de herkende doeltaal. Als geen passende stem beschikbaar is, wordt
er niets afgespeeld; Tuddy start geen stemdownload, server-TTS of
stilzwijgende online fallback. De microfoon moet uitstaan voor de knop
werkt. **Stop stem**, Stop, achtergrond en app-afsluiting ruimen de
spreekdienst op.

Dit is een **Android-gerapporteerde offline-capaciteit**, geen
onafhankelijk bewijs dat alle apparaat-/fabrikantspraakdiensten volledig
zonder netwerk werken. Een fysiek toestel en vliegtuigmodus zijn nog
niet gevalideerd. Niet alle 59 ML Kit-teksttalen hebben lokaal een
geïnstalleerde Android-stem; Kurmanji/Sorani/Pasjtoe krijgen geen
verzonnen tekst- of spraakondersteuning. TTS-uitspraak is ongeschikt als
enige basis voor gevoelige medische, juridische of asielbesluiten.


### Eigen Kurmanji-, Sorani- en Pasjtoe-zinnen offline bewaren

Open onder **Offline basiswoorden** de knop
**Mijn eigen Kurmanji/Sorani/Pasjtoe zinnen**. Je kunt daar maximaal
80 persoonlijke zinnen met je **eigen** Nederlandse of Engelse
betekenis typen, bewust opslaan, terugzoeken, individueel verwijderen
of na bevestiging allemaal wissen. Kurmanji gebruikt Latijns schrift;
Sorani en Pasjtoe krijgen expliciet RTL-tekstweergave. De enige
permanente data zijn door jou bewust ingevoerde zinnen in de
app-private Android-opslag; Android-backup blijft uitgeschakeld.
Uit het gesprek wordt **niets automatisch opgeslagen** of naar deze
lijst overgenomen; de app verstuurt geen van deze zinnen naar een server.

Deze zelfgeschreven en niet-gecontroleerde notities worden **nooit
voorgesteld als automatisch vertaalde resultaten**. De apart gelabelde
vijf ingebouwde beginnerzinnen blijven dezelfde beperkte,
handmatig gereviewde preview. Er worden geen Koerdische/Pasjtoe ASR-
of ML Kit-vertaalmodellen gedownload of voorgewend. Vrije offline
spraak en vertaling voor alle drie talen, native-speaker controle,
modellicenties en toestelacceptatie uit
[issue #2](https://github.com/Zennay/Tuddy/issues/2) blijven open.
Gebruik eigen notities zonder menselijke tolk niet voor gevoelige
medische, juridische of asielbeslissingen.


### Privé back-up en herstel voor eigen zinnen

In **Mijn eigen offline zinnen** kun je nu expliciet **Bewaar back-up…**
kiezen. Android vraagt via de Storage Access Framework-bestandskiezer
welke locatie het JSON-bestand krijgt; de app schrijft nergens anders.
**Herstel back-up…** accepteert alleen het eigen
versie-1-formaat, maximaal 50 KiB / 80 zinnen, strikte UTF-8,
de bekende taal- en betekenisvelden en slechts de originele zelf
ingevoerde teksten. Beschadigde, onbekende en gedupliceerde gegevens
worden geweigerd voordat er opslag verandert. Een geldig bestand laat
eerst het aantal ingelezen zinnen zien en vraagt apart of bestaande
privézinnen werkelijk moeten worden vervangen; annuleren verandert niets.

Geen extra opslag- of internetmachtiging en geen automatische
achtergrondback-up. Een door jou gekozen cloudbestandsprovider kan
de back-up buiten Tuddy synchroniseren. Exporteer geen gevoelige
persoonsgegevens. De zelf ingevulde betekenissen zijn en blijven
niet-gecontroleerde persoonlijke notities, **geen** nieuwe vertaalmodellen.


### Eén afgeronde tweetalige zin bewust delen

Bij een afgeronde lokaal vertaalde ondertitel verschijnt nu **Deel deze zin…**
naast de bestaande kopieerknop. Eerst volgt een afzonderlijke
privacybevestiging: de getoonde zin kan daarna buiten Tuddy terechtkomen.
Pas na **Open deelvenster** geeft Tuddy alleen die ene originele tekst
plus de daadwerkelijk afgeronde vertaling aan het standaard
Android-deelvenster door. Geen volledig gespreksarchief, transcript
in opbouw, onvoltooide vertaling of automatische achtergrondverzending.
Als de zin of sessie intussen verandert, vervalt de toestemming en moet
je opnieuw kiezen. Na weggaan uit de app verdwijnt het openstaande
toestemmingsvenster. De gekozen externe app kan tekst zelf opslaan,
doorsturen of synchroniseren: deel geen vertrouwelijke persoonsgegevens.
Het openen van Androids chooser bewijst niet dat er werkelijk iets is
verzonden. Deze wijziging levert uitsluitend broncode; fysieke
toestelacceptatie, taalmodelbeschikbaarheid en getekende APK blijven apart.


### Handmatig sprekers onderscheiden in het live gesprek

In de live ondertiteltijdlijn staat bij iedere afgeronde regel **Spreker: onbekend**.
Met **Wijzig spreker** doorloop je handmatig **A → B → onbekend**. Dit is geen
automatische sprekerherkenning: Tuddy beweert niet dat een stem aan een persoon
gekoppeld is. Boven het gesprek kun je met **Volgende A** en **Volgende B**
langs de gemarkeerde gespreksregels navigeren zonder de live opname te resetten.
De labels bestaan uitsluitend in het geheugen van de huidige sessie,
verdwijnen bij **Wis gesprek** of wanneer een oude zin uit de tijdlijn
verdwijnt, en worden niet toegevoegd aan bestaande tekst-/JSON-exporten.
Een ander account, cloud, sprekeridentificatie of nieuwe Android-rechten
is hiervoor niet nodig. Dit is uitsluitend broncode: device- en releasegates
blijven gelden.


### Volledig gesprek met zelfgekozen A/B-sprekers bewaren

De bestaande tekst- en JSON-export blijven **zonder sprekerlabels**. Wie een
afgeronde gespreksregel handmatig als A of B heeft gemarkeerd, krijgt daarnaast
**Bewaar gesprek met sprekerlabels…**. De app stelt een afzonderlijke
privacybevestiging vóór het Android-bestandsscherm verplicht; de gebruiker
kiest zelf het opslagdoel. Het begrensde tekstbestand bevat uitsluitend
afgeronde gesprekkenregels, inclusief A/B/onbekend-labels uit de huidige
tijdelijke sessie. Een gewijzigd transcript of label tussen voorbereiden
en bevestigen weigert de verouderde export. Sprekerlabels zijn nooit
biometrisch herkend, geen echte persoonsidentiteit, niet gesynchroniseerd
door Tuddy en niet in het normale archief opgeslagen. Een gekozen externe
bestandsprovider kan documenten wel opslaan of synchroniseren.
