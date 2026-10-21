# Android-deelmenu: deel platte tekst naar Tuddy

Tuddy kan vanuit het standaard Android-deelmenu als bestemming verschijnen voor **één platte tekst** (ACTION_SEND, `text/plain`). Dat kan bijvoorbeeld een door jou geselecteerde chatzin of passage zijn.

Bij een gewone zin opent na **Delen → Tuddy** het venster **Gedeelde tekst · eerst controleren**. De tekst is slechts een bewerkbaar concept in het tijdelijke geheugen. Controleer de inhoud, geselecteerde talen en eventuele persoonlijke gegevens. Pas na **Vertaal lokaal** wordt de tekst als nieuwe regel aan het tijdelijke Tuddy-gesprek toegevoegd en via de bestaande ML Kit-tekstvertaler verwerkt. **Annuleren** en naar de achtergrond gaan wissen het open concept.

## Geselecteerde tekst vertalen

Selecteer tekst in een Android-app en kies waar die app het aanbiedt
**Tekst verwerken / Tuddy**. Android geeft slechts die selectie mee via
`ACTION_PROCESS_TEXT`, `text/plain` en `EXTRA_PROCESS_TEXT`. Tuddy
opent exact hetzelfde tijdelijke, bewerkbare bevestigingsvenster als
via Delen. De selectie wordt **niet** direct vertaald en de originele
tekst in de andere app wordt **niet** vervangen. Tuddy geeft geen
gewijzigde tekst als resultaat terug; eerdere apps houden hun eigen data.

Deze systeemactie is niet in elk tekstveld of elke app beschikbaar.
Ontbrekende selectie, HTML, bestanden, te lange tekst en onverwachte
stuurtekens worden geweigerd. Ontvangen tekst wordt na eenmalig
verwerken ook uit de relevante Android-Intent-extra verwijderd.

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

## Grenzen en privacy

- **Geen automatische vertaling, opslag, export, microfoonactivatie of serverupload** bij binnenkomst van een deelverzoek.
- Alleen `ACTION_SEND` met `text/plain` en Androids `EXTRA_TEXT` wordt aanvaard. Geen `ACTION_SEND_MULTIPLE`, HTML, foto's, bijlagen, content-URI-lezen of automatisch ophalen van webpagina's.
- Gewone gesprekstekst blijft begrensd op 1500 UTF-16-code-eenheden. Een **volledig valide gedeeld ondertiteldocument** volgt in plaats daarvan de bestaande strikte 256-KiB-/500-cue-parserlimieten. Niet-passende of onveilige inhoud wordt niet automatisch overgenomen als ondertitel.
- Een ontvangen geldig deelverzoek stopt eventueel actieve on-device microfoonluistermodus voordat het invoervenster opent. Zonder bevestiging wordt geen model aangevraagd.
- Het delen vanuit een andere app betekent niet dat Tuddy die andere app beheert; de verzendende app kan een eigen kopie bewaren. Gedeelde tekst kan persoonsgegevens bevatten.
- Tijdelijke tekst en de ontvangststatus worden **niet** opgeslagen in `savedInstanceState`, bestanden of voorkeuren. Een Android-rotatie/herstart importeert het oorspronkelijke deelverzoek niet opnieuw.
- Tekstvertaling vereist daadwerkelijk beschikbare lokale ML Kit-modellen. Android-spraakpakketten, onbewezen vrije Kurmanji/Sorani/Pasjtoe-vertaling en toestel-/releasevalidatie zijn hiervan onafhankelijk.

Deze pagina beschrijft de Android-implementatie; fysieke Android-toestelacceptatie of een getekende release-APK is nog niet vastgesteld.
