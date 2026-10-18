# Twee SRT/VTT-documenten offline combineren

Open via Tuddy's bestaande **Open ondertitelbestand**-route een eigen
SRT- of WebVTT-bestand. Kies in de ondertiteleditor **＋ Tweede
SRT/VTT-document toevoegen…** en selecteer via Android een tweede
bestand. Een document mag maximaal 256 KiB zijn en maximaal 500
ondertitelregels hebben. Beide moeten daadwerkelijk geldige UTF-8
SRT/WebVTT-ondertitels zijn; onbekende formats en kapotte tijdcodes
worden geweigerd, ongeacht hun bestandsnaam.

## Kies bewust de tijdlijn
- **Aansluiten na laatste regel (+1 seconde):** de eerste regel van het
  tweede document begint één seconde na het laatst eindigende moment
  van het eerste concept. De onderlinge tijden van het tweede document
  blijven relatief behouden.
- **Originele tijdcodes naast elkaar:** voeg beide tijdlijnen bij elkaar
  op oplopende begintijd. Bij gelijke begintijd staat de oorspronkelijke
  regel eerst. Gelijktijdige of overlappende regels blijven bestaan en
  worden als overlappingen gemeld; Tuddy probeert ze niet samen te voegen
  of automatisch op audio uit te lijnen.

Je ziet vooraf het totaal aantal regels en de aangrenzende
tijdsoverlappingen. Alleen na **Voeg alleen toe aan tijdelijk concept**
verandert de bewerkbare lijst in het werkgeheugen. **Terug / Opnieuw**
blijven werken; **Herstel origineel** verwijdert de toevoeging uit het
tijdelijke concept. Via **Bewaar kopie SRT/VTT…** kies je apart een
nieuwe bestandslocatie. Het oorspronkelijk geopende document en het
tweede geselecteerde document worden niet overschreven.

Veiligheidsgrenzen: samen maximaal 500 cues, maximaal 256 KiB per
uitvoer, geldige begrensde tijdstempels, geen verborgen cloudfallback,
geen microfoon, geen automatische taal-/sprekeridentificatie, geen
taalmodel-download, geen toestemming om documenten naar buiten te
versturen. Android-bestandsproviders kunnen op jouw verzoek wel
synchroniseren. Tijdcodes en tekst uit eigen documenten worden niet
geverifieerd en zijn geen precieze media-uitlijning.

Dit is functionaliteit van de debug-Android-app en verandert
**Incubate / Verifying** niet: fysieke vliegtuigmodus-acceptatie,
vrij gelicentieerde Kurdish/Pashto-ASR+MT/native-speakercontrole,
productieondertekening en zCloud-admission moeten nog apart
aangetoond worden.

## Een tweede bestand veilig kiezen op Android

Android kan Tuddy op de achtergrond zetten zodra de bestandskiezer opent.
Daarom wordt de tweede keuze voortaan door de hoofdactiviteit afgehandeld:
de editor zelf wordt volgens de bestaande privacyregel gesloten. Het
**precies op het moment van kiezen aanwezige tijdelijke ondertitelconcept**
blijft uitsluitend tijdens deze bewuste pickeractie in RAM. Na terugkomst
komt het concept terug met de afzonderlijk geselecteerde tweede bron,
waarna Tuddy de bestaande combineerpreview en expliciete bevestiging
toont. Annuleren heropent alleen het tijdelijke concept. Een ongeldig
bestand toont een verklaring en laat het eerste concept ongemoeid.

Een nieuwe bewerkersessie na de externe keuze heeft geen vorige
Terug-/Opnieuw-geschiedenis; nog niet bewaarde tekst-/tijdcodebewerkingen
zitten wel in het herstelde concept. Procesafsluiting bewaart niets.
Bewaren als bestand vereist altijd de al bestaande, afzonderlijke
Android-keuze. Er worden geen nieuwe Android-permissies aangevraagd.
