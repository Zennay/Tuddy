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

## Verwerking annuleren en opnieuw proberen

Een Android-dienst kan uitzonderlijk geen voltooiingscallback teruggeven.
Daarom heeft **Stel brontaal lokaal voor** een grens van **12 seconden**
en **Vertaal lokaal** een grens van **30 seconden**, inclusief de
controle van al aanwezige tekstmodellen en de daadwerkelijke lokale
ML Kit-vertaalaanvraag. Als het apparaat niet reageert, verdwijnt de
voortgangsindicator en verschijnt een begrijpelijke fout zonder
een verzonnen vertaling.

Tijdens beide acties staat nu **Annuleer lokale verwerking** onder de
voortgangsindicator. Annuleren maakt de bestaande detector- en
vertaalcallbackrevisies ongeldig, stopt de actieve lokale clients en
ruimt de Android-timers op. Het resultaat is dan **niet** teruggezet
in de oorspronkelijke app, op het klembord geplaatst of gedeeld.
De oorspronkelijke geselecteerde tekst blijft alleen in de open
tijdelijke weergave staan, zodat je bewust van taal kunt wisselen
of op **Vertaal lokaal** kunt tikken om opnieuw te proberen.

Een callback die ná annulering, timeout, taalwisseling of verlaten
van de Activity binnenkomt mag nooit alsnog een vertaling tonen.
De tijdslimiet stopt de **app-interactie**; hij bewijst niet dat
de onderliggende Android/ML Kit-provider elk intern werk meteen
heeft gestopt. Het scherm downloadt nooit automatisch modellen
en gebruikt geen online fallback.

## Brontekst en vertaling lokaal laten voorlezen

Vanuit hetzelfde snelle Android-selectiescherm kun je **🔊 Lokaal voorlezen · kies tempo** openen. Kies **Origineel · normaal/langzaam** voordat of nadat je vertaalt; na een echt voltooide lokale vertaling kun je ook **Vertaling · normaal/langzaam** kiezen. **■ Stop voorlezen** stopt de Android-stem onmiddellijk. Vertalen, een nieuwe taalrichting kiezen, het scherm sluiten of naar de achtergrond gaan stopt de vorige stem eveneens.

Tuddy hergebruikt de bestaande `LocalCaptionSpeaker` en accepteert uitsluitend een reeds geïnstalleerde Android-stem die door Android als niet-netwerkafhankelijk wordt gemeld. Een enkele voorleeszin is maximaal 400 tekens en moet voldoen aan de bestaande tekst-/stembescherming. Ontbreekt die stem of is de selectie te lang, dan verschijnt alleen een begrijpelijke melding: geen spraakdownload, cloud-TTS, nieuwe machtiging of automatische microfoonactivatie. De getoonde gesproken inhoud wordt niet opgeslagen. Een apparaatclaim blijft afhankelijk van echte vliegtuigmodus- en toestelacceptatie; Androids stemmetadata zijn geen onafhankelijke radiometing.

Ook het lokale brontaalvoorstel is nu tegen verouderde asynchrone callbacks beschermd: als de selectiepagina afsluit, de gebruiker tussentijds talen wisselt of de app naar de achtergrond gaat, kan een late herkenningsuitkomst de nieuwere selectie niet terugzetten.

## Vanuit Android Delen en naar andere apps terug

Ook via **Delen → Tuddy** kun je na de bestaande controle van een gewone
platte tekst kiezen voor **Vertaal apart · zonder gespreksgeschiedenis**.
Dit opent dezelfde lokale geselecteerde-tekstvertaler als Android
**Tekst verwerken**, maar in alleen-lezenmodus: de oorspronkelijke app
wordt niet heimelijk aangepast en geen gespreksregel wordt aangemaakt.

Na een geslaagde vertaling zijn er drie afzonderlijke mogelijkheden:
**Kopieer vertaling** gebruikt het met Androids *gevoelige inhoud*-vlag
gemarkeerde klembord (geen automatische inhoudsvoorvertoning op
ondersteunde Android-versies, maar andere apps kunnen inhoud nog lezen);
**Deel vertaling via Android…** toont eerst een privacybevestiging
en opent alleen met instemming Androids standaarddeelmenu;
**Vervang geselecteerde tekst** is uitsluitend beschikbaar bij
daadwerkelijk bewerkbare selecties die de bronapp als zodanig markeert.
Het delen naar een externe app kan daar opslag of netwerkgebruik
veroorzaken, onafhankelijk van Tuddy's eigen lokale vertaling.
Bij het verlaten van de snelle vertaler wordt het tijdelijke scherm
gewist. Bijlagen, ondertitels, back-ups en andere gestructureerde
gedeelde tekst blijven van de gewone vertaalactie gescheiden.

## Zelf een geïnstalleerde voorleesstem kiezen

Met **Stem bron kiezen** en **Stem vertaling kiezen** kun je op dit
snelle vertaalscherm de momenteel op het toestel geïnstalleerde,
door Android als niet-netwerkafhankelijk gemelde stemmen voor beide
talen bekijken. Kies een stem, luister naar een **vast voorbeeld**
(zonder privézin) of herstel de automatische veilige stemkeuze.
De instelling geldt ook voor de voorleesfuncties van het gewone
Tuddy-gesprek. Er is geen nieuwe machtiging, stemdownload, online-TTS
of modelondersteuning nodig. Zie
[ANDROID_INSTALLED_VOICE_PICKER.md](ANDROID_INSTALLED_VOICE_PICKER.md).

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
