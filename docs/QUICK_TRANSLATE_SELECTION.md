# Geselecteerde tekst direct lokaal vertalen

Selecteer een passage in een Android-app en kies in het tekstselectiemenu
**Vertaal met Tuddy**. Gewone platte tekst opent een aparte, tijdelijke
vertaalweergave. De geselecteerde tekst wordt niet automatisch vertaald:
controleer eerst de bron- en doeltaal en tik daarna op **Vertaal lokaal**.

## Werking

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
