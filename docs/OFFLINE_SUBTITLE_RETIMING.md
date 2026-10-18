# Tijdlijn offline schalen en verschuiven

Open een eigen SRT/WebVTT-bestand in **Eigen ondertitelconcept**. Kies
**⏱ Tijdcodes van alle regels aanpassen…**.

De schaalkeuzes zijn 80%, 90%, 95%, 100%, 105%, 110% en 125%; daarnaast
kun je alle tijdcodes met 0,5, 2 of 5 seconden in beide richtingen
verschuiven. **Eerste regel** gebruikt de bestaande eerste cue als
schaalanker, **00:00:00** gebruikt nul. De verschuiving geldt daarna voor
de hele tijdlijn.

Bekijk de eerste start, laatste eindtijd en het aantal overlappende
buurregels vóór **Pas toe in tijdelijk concept**. Negatieve tijden,
tijden buiten 99:59:59.999, ongeldige tijdsvolgorde en documenten boven
de bestaande grenzen worden geweigerd. De inhoud van de regels blijft
ongewijzigd; hun duur schaalt mee. Dit is géén automatische synchronisatie
met audio of video en géén verificatie van vertaalde tekst.

De wijziging is één herstelbare actie in de bestaande undo/redo-
geschiedenis. Het geopende bronbestand wordt nooit overschreven.
Alleen na afzonderlijk **Bewaar kopie SRT/VTT…** wordt een nieuw bestand
aan een door jou gekozen Android-bestandsprovider aangeboden. Die provider
kan het bestand buiten de app synchroniseren. Er zijn geen extra
netwerkverbindingen, permissies, modellen, accounts of opslagprofielen.
