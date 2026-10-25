# Android-deelmenu: deel platte tekst naar Tuddy

Tuddy kan vanuit het standaard Android-deelmenu als bestemming verschijnen voor **één platte tekst** (ACTION_SEND, `text/plain`). Dat kan bijvoorbeeld een door jou geselecteerde chatzin of passage zijn.

Bij een gewone zin opent na **Delen → Tuddy** het venster **Gedeelde tekst · eerst controleren**. De tekst is slechts een bewerkbaar concept in het tijdelijke geheugen. Controleer de inhoud, geselecteerde talen en eventuele persoonlijke gegevens. Pas na **Vertaal lokaal** wordt de tekst als nieuwe regel aan het tijdelijke Tuddy-gesprek toegevoegd en via de bestaande ML Kit-tekstvertaler verwerkt. **Annuleren** en naar de achtergrond gaan wissen het open concept.

## Geselecteerde tekst direct vertalen

Selecteer tekst in een Android-app en kies **Vertaal met Tuddy** in het
tekstselectiemenu. Gewone platte tekst opent een aparte, tijdelijke
vertaalweergave. De selectie wordt **niet automatisch vertaald**: controleer
eerst de bron- en doeltaal (of vraag een optioneel lokaal brontaalvoorstel) en tik daarna op **Vertaal lokaal**.

**Stel brontaal lokaal voor** gebruikt de ingebouwde on-device taalidentificatie op een korte selectie. Het voorstel wordt zichtbaar getoond en nooit automatisch als vertaalrichting toegepast.

Tuddy controleert vóór vertaling of de exacte ML Kit-tekstmodellen al lokaal
beschikbaar zijn. Engels gebruikt het ingebouwde model; voor andere talen moet
het bijbehorende model al op het toestel staan. De selectieactie downloadt geen
modellen, start geen microfoon en gebruikt geen online vertaalfallback. Als een
model ontbreekt, open dan Tuddy → **Offline startcheck → Tekst via wifi** en
selecteer de tekst daarna opnieuw.

Na een geslaagde vertaling kun je de tekst kopiëren. Alleen wanneer de bronapp
de selectie als bewerkbaar markeert, verschijnt **Vervang geselecteerde tekst
in de andere app**. Pas na die afzonderlijke tik geeft Tuddy het resultaat via
`EXTRA_PROCESS_TEXT` terug. Voor een alleen-lezen selectie blijft kopiëren
mogelijk, maar wordt de oorspronkelijke tekst niet vervangen.

Bekende Tuddy-back-ups, correctie-CSV/JSON en complete ondertitelblokken blijven
via de bestaande strikte import-/ondertitelpreview lopen; ze worden niet als
gewone zinnen vertaald. Ongeldige of te lange tekst wordt geweigerd. De selectie
en vertaling worden niet opgeslagen en verdwijnen wanneer de snelle vertaal-
weergave naar de achtergrond gaat of wordt gesloten.
 
## Snelle vertaling zonder gespreksgeschiedenis

Bij een gewone `text/plain`-deelactie blijft **Gedeelde tekst · eerst controleren**
de veilige standaardroute. Na controle kun je nu ook bewust kiezen voor
**Vertaal apart · zonder gespreksgeschiedenis**. Dit opent de bestaande
tijdelijke lokale Android-vertaler met alleen de begrensde tekst als
bewerkingsvrije selectie; je live gespreksregels, transcriptcorrecties en
gesprekstalen worden niet aangepast. Een andere taalrichting kun je in dat
scherm zelf kiezen. Zonder die expliciete tik wordt er geen model geopend
of zin vertaald.

Na een lokale vertaling kun je **Kopieer vertaling** kiezen. Het Android-klembord
wordt voor ondersteunde systeemversies als gevoelige inhoud gemarkeerd, zodat
Android de automatische inhoudsvoorvertoning kan onderdrukken. Andere apps
kunnen gekopieerde inhoud nog steeds lezen. **Deel vertaling via Android…**
vraagt eerst afzonderlijk akkoord en opent pas daarna Androids
deelmenu; de ontvangende app kan de tekst opslaan of online verwerken.
Bij het verlaten van het tijdelijke scherm verdwijnen de tekst en het
vertaalresultaat uit de actieve Tuddy-weergave. Bestandsbijlagen,
ondertitels en bekende privéback-ups blijven via hun bestaande strikte
preview lopen, niet via deze snelle vertaling.

## Gedeelde getimede ondertitels (SRT/VTT/LRC/CSV/TSV)

Een volledig, geldig `text/plain`-ondertitelblok dat expliciet via
Androids deelmenu of **Tekst verwerken** naar Tuddy wordt gestuurd,
opent **eerst de bestaande privé-ondertitelpreview**. Dit geldt voor
SRT, WebVTT, LRC en nu ook voor de door Tuddy te exporteren
getimede **CSV/TSV-tabellen met exact drie of vier kolommen**:
begintijd, eindtijd, eigen tekst en eventueel eigen tweede tekstregel.
Een tweede kolom is **geen geverifieerde vertaling**. CSV-aanhalingstekens
worden met de bestaande strikte CSV-parser verwerkt; TSV-velden zijn
letterlijk tabgescheiden. Elk ongeldig tijdveld, ontbrekende kolom,
dubbele betekenis of verkeerd geordende rij weigert het hele blad.

De gekozen CSV/TSV-modus blijft zichtbaar in de bestaande
bevestigingsdialoog. De ontvangen oorspronkelijke tekst is slechts
een **tijdelijk, bewerkbaar concept in RAM**. Er wordt niet naar het
live gesprek doorgeschoven, geen spreker herkend, geen model gestart
en geen opslag uitgevoerd totdat de gebruiker afzonderlijk bevestigt.
Bij SRT/VTT en CSV/TSV komen begintijden en eindtijden van de
gekozen tekst; bij LRC worden eindtijden nog altijd afgeleid.
Dit pad gebruikt **geen bijlagen of URI's**: kies voor echte bestanden
de aparte Android-knop **Open ondertitelbestand…**.

## YouTube/SBV-tekst delen

Ook volledige, geldige **YouTube/SubViewer SBV** met
`H:MM:SS.mmm,H:MM:SS.mmm` en door lege regels gescheiden captions kan
expliciet als `text/plain` gedeeld worden. De bestaande strikt begrensde
SBV-lezer controleert alle tijden en teksten voordat de preview opent.
Een SBV is alleen een gebruiker aangeleverde ondertitel, geen YouTube-link,
audioherkenning of bewijs van correcte synchronisatie. De gebruiker
bekijkt de oorspronkelijke regels in de bestaande tijdelijke
ondertiteldialoog en bevestigt afzonderlijk. Afwijzen wist de
ontvangen tijdelijke inhoud zonder nieuw bestand of vertaling te maken.
Na bewuste bewerking is een **nieuwe SBV-kopie met begin- én eindtijden**
ook beschikbaar via de private ondertitel-editor en Androids
opslagkiezer; zie [PRIVATE_OFFLINE_SUBTITLE_READER.md](PRIVATE_OFFLINE_SUBTITLE_READER.md).

## Gedeelde JSON3-ondertitels uit YouTube

Via **Delen → Tuddy** of **Tekst verwerken** kan een gebruiker
ook een compleet `text/plain` JSON3-ondertitelblok delen. De app
herkent alleen een geldige `events`-structuur met expliciete
`tStartMs`, `dDurationMs` en `segs[].utf8`, met dezelfde lokale
cue-validatie als het bewust gekozen bestand. Android-deelacties en
handmatig **plakken** zijn voor JSON3 extra begrensd op **64 KiB**
om langdurig ontleden in de interface te voorkomen. Voor grotere
JSON3-bronnen gebruikt de gebruiker **Open ondertitelbestand…**,
dat maximaal 256 KiB asynchroon via de gekozen Android-provider leest.
De gekozen tekst blijft een tijdelijke preview zonder vertaling of
automatische opslag. Een ongeldig gestructureerd JSON3-bericht wordt
niet stilzwijgend als een gewone vertaalzin behandeld.

## Een volledig ondertitelbestand delen vanaf Android Bestanden

Wanneer een Android-bestandsapp **Delen → Tuddy** gebruikt voor **één
tekstondertitelbestand** (bijvoorbeeld een `.srt`, `.vtt`, `.lrc`,
`.sbv` of `.json3`), bevat het deelverzoek soms een
`content://`-URI via `ACTION_SEND`/`EXTRA_STREAM` in plaats van
platte `EXTRA_TEXT`-inhoud. Tuddy ondersteunt nu beide bewuste
routes, maar **verwisselt een bestandsbijlage nooit met een chatzin**.

De bijlageroute accepteert uitsluitend één document van een expliciet
toegelaten MIME-type (`text/plain`, `text/vtt`, `text/srt`,
`application/x-subrip`, `application/json`). Aangeleverde
gemengde tekst/HTML of meerdere bijlagen worden geweigerd. Bij andere
MIME-typen, zoals generiek `application/octet-stream`, gebruik je
**Open ondertitelbestand…**. Geen `file://`, links, wildcard
`*/*`, opslagmachtigingen, cloud-downloads of blijvende
providerrechten. Een externe Android-provider kan zelf synchroniseren.

Tuddy leest alleen de gekozen `content://`-bijlage via de bestaande
256-KiB begrensde UTF-8 documentlezer, buiten de UI-thread. Na strikte
validatie toont de app **alle ondertitelregels en hun tijdcodes**
in een aparte scrollbare preview. Pas met **Open tijdelijk in editor**
worden de regels een bewerkbaar lokaal concept. Annuleren of naar de
achtergrond gaan maakt de preview leeg, zonder automatische
tekstvertaling, microfoonactivatie of permanente opslag. De oorspronkelijke
bijlage blijft onaangeroerd.

## Grenzen en privacy

- **Geen automatische vertaling, opslag, export, microfoonactivatie of serverupload** bij binnenkomst van een deelverzoek.
- `EXTRA_TEXT` blijft uitsluitend platte geselecteerde tekst. Eén expliciet gedeelde ondertitelbijlage via `EXTRA_STREAM` wordt apart via een strikt begrensde `content://`-lezer aangeboden, nooit als gesprekstekst. Geen `ACTION_SEND_MULTIPLE`, HTML, foto's, generieke bijlagen of automatisch ophalen van webpagina's.
- Gewone gesprekstekst blijft begrensd op 1500 UTF-16-code-eenheden. Een **volledig valide gedeeld ondertiteldocument** volgt in plaats daarvan de bestaande strikte 256-KiB-/500-cue-parserlimieten. Niet-passende of onveilige inhoud wordt niet automatisch overgenomen als ondertitel.
- Een ontvangen geldig deelverzoek stopt eventueel actieve on-device microfoonluistermodus voordat het invoervenster opent. Zonder bevestiging wordt geen model aangevraagd.
- Het delen vanuit een andere app betekent niet dat Tuddy die andere app beheert; de verzendende app kan een eigen kopie bewaren. Gedeelde tekst kan persoonsgegevens bevatten.
- Tijdelijke tekst en de ontvangststatus worden **niet** opgeslagen in `savedInstanceState`, bestanden of voorkeuren. Een Android-rotatie/herstart importeert het oorspronkelijke deelverzoek niet opnieuw.
- Tekstvertaling vereist daadwerkelijk beschikbare lokale ML Kit-modellen. Android-spraakpakketten, onbewezen vrije Kurmanji/Sorani/Pasjtoe-vertaling en toestel-/releasevalidatie zijn hiervan onafhankelijk.

Deze pagina beschrijft de Android-implementatie; fysieke Android-toestelacceptatie of een getekende release-APK is nog niet vastgesteld.
