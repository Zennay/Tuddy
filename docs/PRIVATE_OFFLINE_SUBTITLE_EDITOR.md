# Offline SRT/WebVTT-ondertitelconcept bewerken

Open **Gesprekstools → Open SRT/WebVTT…** en kies bewust een
ondertiteltekstbestand met Androids bestandskiezer. Deze functie is
voor handmatige montagecorrecties, niet voor automatische transcriptie.

## Bewerk bestaande regels

- Zoek één of meer woorden en tik op een regel om die groot te lezen.
- **✎ Corrigeer deze regel handmatig** vervangt alleen de getoonde
  tekst in een tijdelijk documentconcept. De tijdcode blijft staan.
  Er wordt geen vertaling gemaakt of inhoudelijk gecontroleerd.
- **−0,5 s / +0,5 s** verplaatst alle tijdcodes in het concept tegelijk.
  De app weigert negatieve of buiten het bestandsbereik vallende tijden.
  Dit is handmatige tijdschuiving, géén verificatie tegen het audiogeluid.
- **Herstel** gooit alle tijdelijke bewerkingen weg en toont het
  oorspronkelijk ingelezen bestand terug.

## Alleen een nieuwe kopie bewaren

Kies **Bewaar kopie SRT…** of **Bewaar kopie VTT…** en bevestig de
privacywaarschuwing. Tuddy stelt een nieuwe bestandsnaam voor;
jij bepaalt de Android-bestandsprovider. Het **geopende origineel
wordt nooit overschreven door Tuddy**. De vorm is een nieuw, strikt
begrensd UTF-8 SRT- of WEBVTT-tekstbestand met maximaal 500 cues.
Je mag dus ook een VTT als SRT-kopie bewaren of omgekeerd.

Bestanden die buiten het formaat vallen of te groot zijn blijven
geweigerd. Automatische achtergrondopslag, cloudmodel, herkennen,
tekst vertalen, microphone recording, audiosync of delen is er niet.
De geselecteerde bestandsprovider kan wel zelfstandig synchroniseren.
Sluiten, teruggaan en de Activity in de achtergrond verwijderen
de niet-wegschreven tijdelijke wijzigingen. Een gekozen nieuwe
kopie buiten Tuddy kun je achteraf niet vanuit Tuddy wissen.

## Verder bewerken na een eigen kopie

Android kan de tijdelijke editor sluiten terwijl je een nieuwe SRT/VTT-locatie
kiest. Daarom houdt Tuddy precies voor die bewuste bestandskeuze de actuele
tekst-/tijdcoderegels, het uitvoerformaat en de begrensde exportbytes tijdelijk
in geheugen. Na Bewaren of Annuleren verschijnt hetzelfde concept opnieuw,
met de resultaatmelding. De eerder opgebouwde Undo/Redo-geschiedenis wordt
niet opnieuw aangemaakt; de tekstuele wijzigingen blijven wel bewerkbaar.
Het originele bestand wordt nooit door deze actie overschreven, er is geen
autosave, en bij procesafsluiting verdwijnen niet-opgeslagen gegevens.
