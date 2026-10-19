# Eigen audiomomenten markeren en vrij A–B herhalen

## Product

Tuddy's bestaande native Android scherm **Ondertitels bij eigen audio** kan nu
maximaal acht zelfgekozen audioposities markeren. De gebruiker kan terug naar
zo'n positie springen, een markering verwijderen en uit twee markeringen een
A–B-luisterlus van **0,75 seconde tot en met 5 minuten** selecteren. Deze
luisterlus is **onafhankelijk van SRT/VTT cuegrenzen**: de gebruiker kan
bijvoorbeeld precies een uitspraak tussen twee ondertitelregels herhalen.

Gebruik binnen **▶ Eigen audio met tijdcodes afspelen…**:

1. Open `▾ Markeer audiomomenten en herhaal vrij A–B`.
2. Luister of gebruik de scrubber, kies `＋ Markeer huidige audiopositie`.
   Binnen 0,5 seconde van een bestaande markering wordt niet dubbel gemarkeerd.
   Maximaal acht markeringen, in tijdvolgorde.
3. Met `▶ Naar markering` spring je naar het gekozen audiomoment.
4. Kies bij het gewenste begin `Kies A` en bij het einde `Kies B`.
   Omgekeerde, identieke en te lange intervallen kunnen niet beginnen.
5. `↻ Herhaal tussen mijn audio A- en B-markeringen` start een begrensde
   lus. De afspeelknop blijft verantwoordelijk voor een gepauzeerde speler;
   de lus activeert niet op de achtergrond een microfoon of opname.
6. Stop de lus via dezelfde knop; een sprong buiten het segment stopt de
   lopende lus ook. Individuele markeringen blijven beschikbaar voor
   navigatie tot het scherm wordt afgesloten.

De bestaande cue-herhaling en ondertitel-A–B-oefening schakelen de vrije
audioherhaling uit zodra die herhaalmodus wordt gestart en andersom.

## Privacy en lifecycle

- Geen transcript-/vertalingmodel of woorduitlijning: uitsluitend
  `MediaPlayer.currentPosition` en handmatige gebruikerskeuzes.
- Geen automatische `start()` bij het vastleggen van markeringen.
  Android-levenscyclus pauzeert bestaand geluid bij achtergrondactiviteit.
- Alle markeringen en A/B-keuzes zijn uitsluitend in vluchtige Compose-state.
  Bij het openen van een ander audio-item, sluiten, fout of lifecycle-dispose
  wordt de speler vrijgegeven en de toestand gereset.
- Geen nieuwe bestanden, broadcasts, cloud-API, upload, database, notificatie,
  analytics, account, of runtime/VPS-koppeling. SAF-medialeverancier kan eigen
  netwerktoegang gebruiken; voor werkelijk offline afspelen moet de audio
  lokaal beschikbaar zijn.
- Maximaal zes uur audiolengte volgens bestaande MediaPlayer-admission;
  lusgrenzen zijn streng begrensd, zodat afspeelposities niet overstromen.

## Release-gates

Dit is bronfunctionaliteit, niet een bewezen fysieke Android 12+ acceptatie
of native Kurmanji/Sorani/Pasjtoe ASR/MT. Bestaande verplichte Gradle/Android
CI, review, APK signing en fysieke offline toestel-/licentiegates blijven
van kracht. Geen zelfstandige QA-/regressiesuites toegevoegd of gestart.


## Nieuw: vanuit een ondertitelregel meteen naar je oefenroute

Je hoeft niet langer eerst twee losse audio-A/B-markeringen op het gehoor
te plaatsen als jouw eigen SRT/VTT al bruikbare tijden heeft. Zoek onder
**⌕ Zoek passage** een ondertitel en kies bij die regel
**＋ Regel … in mijn oefenroute bewaren**; of gebruik vanuit de speler
**＋ Huidige ondertitel als A–B-oefenfragment bewaren**. De bronregel
blijft ongewijzigd en audio start niet automatisch.

Tuddy gebruikt exact de begintijd en eindtijd uit jouw SRT/VTT, inclusief
alleen de op dat moment handmatig gekozen tijdelijke tijdverschuiving.
De hele regel moet binnen de gekozen audio vallen en tussen 0,75 seconde
en 5 minuten duren. De regel wordt toegevoegd aan de bestaande
**tijdelijke oefenfragmenten** (maximaal vier), met maximaal acht
unieke audiomarkeringen. Een exact dubbel fragment wordt niet opgeslagen;
markeringen die op minder dan 0,5 s van een andere grens liggen,
of onvoldoende beschikbare plekken, worden geweigerd met uitleg in
de speler. Bij een weigering blijft de bestaande route intact.

Een geslaagde toevoeging opent de bestaande markeringenlijst en toont
bij passende actuele ondertiteltijden tevens een kort broncuerelaas.
Daar kun je in de eigen volgorde, met eigen beoordeling
(*Nog oefenen / Gaat goed / Makkelijk*) en een eindige begeleide
A–B-oefenroute doorgaan. Zelf gekozen audiomarkeringen en vrije
A–B-punten blijven als onafhankelijke bedieningsopties beschikbaar.
Veranderen van audio, sluiten of een spelerfout wist de tijdelijke
fragmenten; een latere tijdverschuiving verandert opgeslagen audiotijden
niet heimelijk. Wie echt passende ondertiteltijden wil, moet ze zelf
controleren en opnieuw opslaan.

Geen spraakdetectie, automatische synchronisatie, woordscore,
transcriptmodel, server, account, nieuwe permissie of automatische
bestandsopslag. De gekozen SAF-bestandsprovider kan zelf cloudtoegang
gebruiken; kies lokaal opgeslagen media voor daadwerkelijk offline gebruik.


## Meerdere ondertitelregels als één begeleid A–B-fragment

Open in de speler **▾ Oefen een A–B-passage van meerdere regels**.
Gebruik de bestaande **Kies A-start** en **Kies B-einde** bij de eerste en
laatste gewenste ondertitel. Wanneer alle betrokken regels volledig binnen
je gekozen audio vallen en het gezamenlijke fragment tussen 0,75 seconde
en vijf minuten duurt, toont Tuddy de twee gekozen regelposities en de
bijbehorende bestaande (eventueel handmatig verschoven) tijdcodes.

Met **＋ Bewaar deze complete A–B-passage in mijn begeleide oefenroute**
wordt het gehele tijdvak vervolgens als één tijdelijk kaartje toegevoegd
aan de bestaande vier-kaartjes luisterroute. Een kaartje met één ondertitel
uit de vorige uitbreiding werkt hetzelfde, maar langere conversatiezinnen
hoeven nu niet meer via losse audiomarkeringen herbouwd te worden.
Kaartjes met een bronmatch tonen de eerste en laatste eigen
ondertiteltekst als herkenningslabel. Markeringen blijven maximaal acht
in dezelfde audiosessie; duplicaten en punten binnen 0,5 seconde van een
andere grens worden niet heimelijk samengevoegd. De app verandert
de oorspronkelijke SRT/VTT-tekst, audio of offset niet.

De gebruiker kiest zelf de volgorde en het aantal herhalingen;
bewaren begint nooit uit zichzelf af te spelen. Sluiten en audio wisselen
wissen alles zoals voorheen. Dit is geen bewijs van echte synchronisatie,
uitspraakscore of woordgrenzen: broncues moeten door de gebruiker zelf
kloppen. Geen opslag, microfoon, cloudverbinding of nieuwe permissie.


## A–B-passage kiezen zonder eerst door de audio te schuiven

Open **⌕ Zoek passage en spring naar ondertitel…** en zoek op een woord
of originele begintijd. Bij iedere gevonden regel staat nu ook
**Kies A-start** en, zodra een begin is gekozen, **Kies B-einde**.
Selecteer eerst A, dan een gelijke of latere B-regel. Je hoeft niet
eerst naar de ondertitel in de audioschuifbalk te springen: deze
handelingen veranderen de afspeelpositie, originele SRT/VTT en
handmatige tijdcorrectie niet.

Boven de zoekresultaten verschijnen de gekozen begin- en eindregels
met korte brontekst. Pas als **alle** tussengelegen ondertitelregels
volledig in de eigen audio passen én het gezamenlijke fragment
0,75 seconden tot maximaal vijf minuten duurt, verschijnt een exacte
begin-/eindpreview en de knop **Bewaar deze bekeken A–B-passage als
oefenfragment**. De bestaande routecapaciteit van vier kaartjes en
acht audiomarkeringen, en de grensafstand van 0,5 seconde, gelden
ook hier. Bij een geweigerde selectie blijft de zoeklijst open;
er wordt niet heimelijk ingekort of gequoot.

Na een succesvolle expliciete bewaaractie ga je terug naar de
bestaande eigen-audiospeler met de opgeslagen oefenkaart en
beschikbare begeleide route. Geen automatisch starten,
spraakherkenning, audio-opslag, cloudcontact of nieuwe permissie.
