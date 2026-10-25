# Geselecteerde tekst direct lokaal vertalen

Selecteer een passage in een Android-app en kies in het tekstselectiemenu
**Vertaal met Tuddy**. Gewone platte tekst opent een aparte, tijdelijke
vertaalweergave. De geselecteerde tekst wordt niet automatisch vertaald:
controleer eerst de bron- en doeltaal (of vraag een optioneel lokaal brontaalvoorstel) en tik daarna op **Vertaal lokaal**.

## Werking

- **Stel brontaal lokaal voor** gebruikt de reeds ingebouwde on-device taalidentificatie op een korte selectie. Het voorstel wordt nooit automatisch toegepast zonder dat de gebruiker het ziet; controleer het zelf en kies desnoods handmatig.
- Tuddy controleert vóór vertaling of de exacte ML Kit-tekstmodellen al lokaal
  beschikbaar zijn. Engels gebruikt het ingebouwde ML Kit-model; voor andere
  talen moet het bijbehorende model al op het toestel staan.
- De selectieactie downloadt geen modellen, start geen microfoon en gebruikt
  geen online vertaalfallback. Ontbreekt een model, open dan Tuddy en ga naar
  **Offline startcheck → Tekst via wifi**. De geselecteerde tekst wordt bij het
  verlaten van de snelle vertaalweergave gewist; selecteer de passage daarna
  opnieuw.
- Na een geslaagde vertaling kun je de tekst kopiëren. Alleen als de bronapp
  aangeeft dat de selectie bewerkbaar is, verschijnt **Vervang geselecteerde
  tekst in de andere app**. Dat is een afzonderlijke bevestigde handeling; tot
  dan wordt geen resultaat teruggestuurd.
- Als Android de selectie als alleen-lezen markeert, blijft kopiëren mogelijk
  maar kan Tuddy de oorspronkelijke tekst niet vervangen.
- Bekende Tuddy-back-ups, correctie-CSV/JSON en complete ondertitelblokken
  blijven via de bestaande strikte import-/ondertitelpreview lopen; ze worden
  niet als een gewone zin vertaald.

## Brontekst en vertaling lokaal laten voorlezen

Vanuit hetzelfde snelle Android-selectiescherm kun je **🔊 Lokaal voorlezen · kies tempo** openen. Kies **Origineel · normaal/langzaam** voordat of nadat je vertaalt; na een echt voltooide lokale vertaling kun je ook **Vertaling · normaal/langzaam** kiezen. **■ Stop voorlezen** stopt de Android-stem onmiddellijk. Vertalen, een nieuwe taalrichting kiezen, het scherm sluiten of naar de achtergrond gaan stopt de vorige stem eveneens.

Tuddy hergebruikt de bestaande `LocalCaptionSpeaker` en accepteert uitsluitend een reeds geïnstalleerde Android-stem die door Android als niet-netwerkafhankelijk wordt gemeld. Een enkele voorleeszin is maximaal 400 tekens en moet voldoen aan de bestaande tekst-/stembescherming. Ontbreekt die stem of is de selectie te lang, dan verschijnt alleen een begrijpelijke melding: geen spraakdownload, cloud-TTS, nieuwe machtiging of automatische microfoonactivatie. De getoonde gesproken inhoud wordt niet opgeslagen. Een apparaatclaim blijft afhankelijk van echte vliegtuigmodus- en toestelacceptatie; Androids stemmetadata zijn geen onafhankelijke radiometing.

Ook het lokale brontaalvoorstel is nu tegen verouderde asynchrone callbacks beschermd: als de selectiepagina afsluit, de gebruiker tussentijds talen wisselt of de app naar de achtergrond gaat, kan een late herkenningsuitkomst de nieuwere selectie niet terugzetten.

## Privacy en grenzen

De functie verwerkt uitsluitend geselecteerde `text/plain`-tekst, begrensd
door de bestaande invoerlimiet. Er worden geen URL's geopend, bestanden
gelezen, gesprekken opgeslagen of analytics verstuurd. De tekst en vertaling
blijven alleen tijdelijk in het actieve scherm; bij achtergrond gaan of sluiten
worden ze gewist. De Android-klembordfunctie kan de vertaling buiten Tuddy
beschikbaar maken nadat je expliciet op **Kopieer vertaling** tikt.

Een geïnstalleerd tekstmodel bewijst geen spraakondersteuning, nauwkeurigheid
of fysieke vliegtuigmodusacceptatie. Kurmanji, Sorani en Pasjtoe blijven buiten
de ML Kit-vertaalcatalogus totdat hun afzonderlijke licentie-, runtime-,
native-speaker- en toestelgates zijn opgelost.
