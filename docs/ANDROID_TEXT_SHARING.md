# Android-deelmenu: deel platte tekst naar Tuddy

Tuddy kan vanuit het standaard Android-deelmenu als bestemming verschijnen voor **één platte tekst** (ACTION_SEND, `text/plain`). Dat kan bijvoorbeeld een door jou geselecteerde chatzin of passage zijn.

Na **Delen → Tuddy** opent het normale venster **Gedeelde tekst · eerst controleren**. De tekst is slechts een bewerkbaar concept in het tijdelijke geheugen. Controleer de inhoud, geselecteerde talen en eventuele persoonlijke gegevens. Pas na **Vertaal lokaal** wordt de tekst als nieuwe regel aan het tijdelijke Tuddy-gesprek toegevoegd en via de bestaande ML Kit-tekstvertaler verwerkt. **Annuleren** en naar de achtergrond gaan wissen het open concept.

## Grenzen en privacy

- **Geen automatische vertaling, opslag, export, microfoonactivatie of serverupload** bij binnenkomst van een deelverzoek.
- Alleen `ACTION_SEND` met `text/plain` en Androids `EXTRA_TEXT` wordt aanvaard. Geen `ACTION_SEND_MULTIPLE`, HTML, foto's, bijlagen, content-URI-lezen of automatisch ophalen van webpagina's.
- Tekst moet voldoen aan dezelfde strikte invoergrens (maximaal 1500 UTF-16-code-eenheden, geen ongeoorloofde stuurtekens). Ongeldige of te lange inhoud wordt geweigerd en niet toegevoegd.
- Een ontvangen geldig deelverzoek stopt eventueel actieve on-device microfoonluistermodus voordat het invoervenster opent. Zonder bevestiging wordt geen model aangevraagd.
- Het delen vanuit een andere app betekent niet dat Tuddy die andere app beheert; de verzendende app kan een eigen kopie bewaren. Gedeelde tekst kan persoonsgegevens bevatten.
- Tijdelijke tekst en de ontvangststatus worden **niet** opgeslagen in `savedInstanceState`, bestanden of voorkeuren. Een Android-rotatie/herstart importeert het oorspronkelijke deelverzoek niet opnieuw.
- Tekstvertaling vereist daadwerkelijk beschikbare lokale ML Kit-modellen. Android-spraakpakketten, onbewezen vrije Kurmanji/Sorani/Pasjtoe-vertaling en toestel-/releasevalidatie zijn hiervan onafhankelijk.

Deze pagina beschrijft de Android-implementatie; fysieke Android-toestelacceptatie of een getekende release-APK is nog niet vastgesteld.
