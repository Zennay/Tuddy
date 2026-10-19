# Eigen audio met een SRT- of WebVTT-tijdlijn bekijken

Open in Tuddy een eigen **SRT/WebVTT** via de bestaande Android-bestandskiezer. Tik in de tijdelijke ondertiteleditor op **▶ Eigen audio met tijdcodes afspelen…** en kies via een tweede, afzonderlijke Android-bestandskiezer een audio-opname. Tuddy opent die opname met Android MediaPlayer, **zonder automatisch af te spelen**. Tik op **Afspelen**, **Pauze**, **Vorige** of **Volgende** om door de ondertitelregels te luisteren. De schuifbalk laat je direct naar een positie springen; tijdens het schuiven zie je de cue die bij die tijd hoort.

De grote ondertitelkaart volgt alleen de **begin- en eindtijd die in jouw SRT/VTT staat**. Bij overlappende regels geldt de laatst begonnen nog actieve cue; bij een gat verschijnt expliciet dat er geen regel actief is. Het is een luister- en montagehulp, geen spraakherkenning, karaoke-woordtijdlijn, audiodetectie of bewijs van juiste synchronisatie. Je kunt tijdcodes in de bestaande editor aanpassen en een **afzonderlijke kopie** opslaan; de audio-preview zelf verandert niets aan het bronbestand.

## Privacy en grenzen

- De gebruiker kiest het ondertitelbestand en het audiobestand **ieder afzonderlijk**. Niets speelt af zonder expliciete actie.
- De audio wordt **niet** gekopieerd naar het Tuddy-profiel, niet opgeslagen in een export en niet naar een Tuddy-server verzonden. Androids gekozen documentenprovider kan zélf een cloudbestand downloaden of synchroniseren: kies een offline lokaal bestand voor echte vliegtuigmoduservaring.
- De speler pauzeert zodra de app de voorgrond verlaat en wordt vrijgegeven als je het previewscherm sluit. Opnieuw kiezen van audio vervangt de vorige mediabron.
- Alleen Android-content-URI's via de bestandskiezer zijn toegestaan. Onbekende of langer dan zes uur durende opnames worden geweigerd; niet-ondersteunde audioformaten krijgen een foutmelding.
- Het is geen vervanging voor de ontbrekende fysieke Android-vliegtuigmodusacceptatie, native-speaker beoordeelde vrij gelicentieerde Kurmanji/Sorani/Pasjtoe ASR+MT, signed release of zCloud admission.

Geen testsuites, fictieve live audio-woorden of automatische netwerkfallback zijn aan dit productonderdeel toegevoegd.

## Langzaam luisteren en één cue herhalen

Na het laden van audio kun je kiezen uit **Rustig 0,75×**, **1×** en
**Vlot 1,25×**. De tempo-optie geldt uitsluitend voor lokaal beluisteren en
wijzigt nooit het audiobestand of de SRT/VTT. Op een Android-speler die de
andere snelheid weigert meldt Tuddy dat; normaal afspelen blijft mogelijk.

Met **↻ Herhaal deze ondertitelregel** herhaal je tijdens het afspelen
alleen het in jouw document ingestelde tijdvak van de huidige cue.
De lengte moet minstens 0,75 seconden zijn en volledig binnen de
gekozen audio vallen. Tik nogmaals om herhaling uit te schakelen; naar
een tijdstip buiten de gekozen cue springen stopt herhaling eveneens.
Sluiten, audio wisselen en een nieuw gesprek starten houden geen
herhaalsessie of instellingen vast. Audio-pauze bij achtergrond blijft
van kracht, ook tijdens herhaling. Tijdvakken kunnen fout of overlappend
zijn: er wordt niets automatisch op spraakwoorden uitgelijnd.

## Android-bestandskiezer en veilig terugkeren naar de editor

Tuddy gebruikt voor de audio een **bestandskiezer die aan de hoofdactiviteit hangt**. Als Android de app tijdens de externe bestandskeuze op de achtergrond zet, verdwijnen de open editor en eventuele live microfoon zoals de privacyregels voorschrijven. Alleen het exact op dat moment door jou gekozen tijdelijke SRT/VTT-concept wordt voor die expliciete pickeractie kort in RAM behouden; na een geldige audioselectie opent een zelfstandige afspeelmodus. **Annuleren** keert terug naar dat concept. **Sluiten** in de audiomodus opent datzelfde tijdelijke tekst-/tijdcodeconcept weer; eerdere editor-undo-stappen worden niet meeverhuisd. Niets hiervan overleeft procesafsluiting of wordt ongevraagd naar schijf geschreven. Audio wordt nooit automatisch afgespeeld na terugkeer.

## Zoek een passage en spring rechtstreeks naar audio

Bij geladen audio kun je **⌕ Zoek passage en spring naar ondertitel…**
openen. De speler wordt eerst gepauzeerd. Zoek lokaal op een woord of
begintijd in de maximaal 500 cues van het bewust geselecteerde SRT/VTT
document. Je ziet de overeenkomende tekst, regelnummers en tijdcodes.
Tik een regel om de audiopositie naar die **zelf aangeleverde begintijd**
te verplaatsen; een eerdere fragmentherhaling wordt uitgeschakeld.
Regels die pas ná het einde van het gekozen audiobestand beginnen zijn
zichtbaar maar niet selecteerbaar. Lege resultaten geven een duidelijke
melding; **Terug naar audio** verandert het gekozen bronbestand niet.

De zoekterm bestaat alleen in de tijdelijke afspeelmodus. Er wordt geen
audio doorzocht, geen spraak herkend en geen tijdcode tegen geluid
geverifieerd. De functie verstuurt geen tekst naar een server.


## Tik op een ondertitelwoord terwijl je naar eigen audio luistert

In **▶ Eigen audio met tijdcodes afspelen…** kun je nu direct op een
woord in de grote actieve ondertitel tikken. Kies eerst zelf de
**taal van de tekst** via de nieuwe taalkiezer: uit een geïmporteerde
SRT/VTT kan Tuddy immers geen betrouwbare gesproken taal afleiden.
De woordkaart gebruikt dezelfde beperkt beschikbare lokale woordenlijst
als de bestaande ondertiteleditor. Wanneer er geen gecontroleerde
lokale betekenis is, zegt Tuddy dat eerlijk en verzint geen vertaling.

Je kunt vervolgens bewust een **eigen betekenis** typen (maximaal 180
tekens) en alleen met **Bewaar mijn eigen betekenis** toevoegen aan
de reeds bestaande app-privénotities **Mijn eigen woorden**. Deze zelf
geschreven betekenis is nadrukkelijk niet geverifieerd. Een bekende
eigen notitie wordt in dezelfde kaart teruggelezen; je kunt hem
later in Mijn eigen woorden beheren, vervangen, exporteren of wissen.
Het openen van een woordkaart, pauzeren of veranderen van audio slaat
niets op. Alleen een afzonderlijke opslagactie op de woordkaart
schrijft naar de bestaande lokale woordenlijst; de gebruiker kan
die duurzame notities via **Wis mijn oefengegevens** verwijderen.

Dit maakt geen audio-opname, kopie van een SRT/VTT, transcriptgeschiedenis,
AI-spraakherkenning, woorduitlijning, cloudverzoek of nieuwe permissie.
De tijdelijke **teksttaalkeuze** verdwijnt bij sluiten/audio wisselen;
opgeslagen eigen notities blijven bestaan totdat je ze bewust wijzigt
of wist. Androids zelfgekozen bestandsprovider kan afhankelijk van de
locatie zelfstandig audio downloaden of synchroniseren.


## Zelfgekozen ondertitelzin bewaren in Mijn eigen zinnen

Tijdens het beluisteren van je eigen SRT/VTT kun je na het
expliciet selecteren van **Kurmanji (kmr)**, **Sorani (ckb)** of
**Pasjtoe (ps)** kiezen voor **Bewaar deze zin met mijn eigen betekenis…**.
De volledige actieve cue wordt als **bewerkbaar concept** geopend.
Je moet de frase zelf controleren, indien nodig inkorten tot maximaal
120 tekens, en **zelf** een betekenis van maximaal 180 tekens
in het Nederlands of Engels typen. Dit is geen officiële of door
een model gecontroleerde vertaling.

Tuddy slaat pas op na het aanvinken van de aparte bewaartoestemming
en een tweede bewuste klik. Een bestaande zin met hetzelfde
taallabel en dezelfde tekst wordt **niet** overschreven: wijzig
hem uitsluitend bewust vanuit het bestaande paneel
**Mijn eigen zinnen**. Bij onbekende taal, ongeldige tekens,
een volle privézinnenlijst of weigering van app-opslag
wordt geen bestaand item gewijzigd.

Je eigen zin wordt in de bestaande app-private woorden-/zinnenomgeving
bewaard. Dit is een **bewuste uitzondering** op de verder volledig
tijdelijke audiopreview: de zelfstandige zin en zelf geschreven betekenis
blijven na het sluiten van het audio-SRT-scherm beschikbaar
voor persoonlijk oefenen en kunnen via bestaande functies gewijzigd,
geëxporteerd of gewist worden. Het bron-SRT/VTT en de audio zelf
worden nooit veranderd of opgeslagen, er is geen ASR, automatische
tekstvertaling, netwerkverzoek of nieuwe Android-permissie.


Een zelf gekozen zin kan bovendien vanuit **⌕ Zoek passage** worden
bewaard: zoek zelf de gewenste ondertitelregel en kies onder dat
zoekresultaat **Regel … als eigen zin met betekenis bewaren**. Dit opent
precies hetzelfde bewerkbare privéconcept en dezelfde dubbele
opslagtoestemming; er wordt niets automatisch van de zoeklijst
of audio naar de persoonlijke zinnenlijst gekopieerd. Je hoeft
de afspeelpositie niet te verplaatsen om een gevonden zin te bewaren.
