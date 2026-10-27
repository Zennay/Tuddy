# Tuddy · privé overhoorronde afschermen

De tweetalige overhoorronde gebruikt uitsluitend reeds lokaal voltooide
vertalingen uit het actuele, tijdelijke Android-gesprek. Getypte en
gesproken oefenpogingen blijven in geheugen en worden nooit als cijfer,
woordenschat of vertaling opgeslagen.

## Eén zichtbare privacyhandeling

Tijdens de ronde kan een leerling op **Verberg privé oefening** tikken.
De app stopt eerst de lokale Android-TTS en de losse on-device
antwoordmicrofoon (inclusief een nog lopende spraakpakketcontrole).
De huidige vraag, onthulde referentie, vergelijking, getypte antwoord,
gesproken hypothesen, toetsenbord en audiomeldingen verdwijnen uit de
oefeninterface. Het onderliggende native Android-tekstveld wist bij het
verwijderen ook zijn eigen buffer en verbergt het toetsenbord.

Diezelfde afscherming gebeurt wanneer Tuddy naar de achtergrond gaat,
het toestel een andere Activity toont of de oefenbediening tijdelijk
niet beschikbaar is. Een terugkerende Activity onthult geen referentie
of eerdere poging. Een laat antwoord op het Android-microfoon-
toestemmingsvenster start evenmin automatisch een opname en brengt
de oude oefening niet opnieuw in beeld.

## Hervatten of definitief wissen

**Hervat privé oefening bewust** is pas beschikbaar wanneer de
oefenbediening in de voorgrond bruikbaar is. De leerling kiest bewust
om terug te keren naar dezelfde oefenzin en dezelfde al **zelf**
gemarkeerde voortgang. Het eerder onthulde referentieantwoord,
getypte tekst en spraakpogingen komen **niet** terug. Je kunt de
referentie pas met een nieuwe tik opnieuw onthullen.

**Wis deze tijdelijke oefenronde** verwijdert ook de overgebleven
zelfbeoordelingen, het huidige indexnummer en eventuele
gemiste-zinnenronde, zonder de oefenzin kort in beeld te zetten.
Het gewone transcript en de lokale modellen worden hierdoor
niet aangepast. Verlaat je het paneel helemaal, dan blijft er
geen opgeslagen oefenronde voor een volgende sessie.

## Grenzen

Dit is een productmatige privacyafscherming, geen belofte dat een
externe Android-toetsenbordapp zich aan alle privacyhints houdt of dat
een al zichtbaar scherm nooit door de systeemsoftware is vastgelegd.
De bestaande toestelgebonden microfoon- en netwerkisolatie,
fysieke Android 12+-acceptatie, rechtencontrole voor alle
Kurmanji/Sorani/Pasjtoe-modellen en de getekende release blijven
aparte gates. Er is geen modeldownload, cloudfallback, QA-suite
of productie-uitrol nodig voor deze functie.
