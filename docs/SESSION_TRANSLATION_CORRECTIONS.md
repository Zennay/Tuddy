# Eigen vertaalcorrecties opnieuw gebruiken binnen hetzelfde gesprek

Tuddy kan een bewust handmatig gecorrigeerde vertaling als *voorstel*
onthouden in het RAM van de huidige app-sessie. Dat maakt terugkerende
uitdrukkingen makkelijker te corrigeren zonder het on-device taalmodel
of je privéwoordenboek te veranderen.

## Zo werkt het

1. Laat een zin lokaal vertalen, of typ zelf een ondersteunde tekstzin.
2. Open **Corrigeer vertaling** voor een afgeronde regel en pas de
   tekst aan. De bestaande revisiecontrole voorkomt toepassing op een
   inmiddels gewijzigde regel.
3. Zet optioneel **Onthoud deze exacte correctie alleen voor dit gesprek**
   aan. De keuze staat standaard uit. Tik pas daarna op **Bewaar correctie**.
4. Als dezelfde **exacte oorspronkelijke tekst** later terugkomt met
   dezelfde bron- en doeltaal, verschijnt in het correctievenster een
   herkenbaar eigen voorstel. Tik op **Vul eigen voorstel in**, bekijk de
   volledige tekst en bevestig vervolgens zelf met **Bewaar correctie**.

Niets wordt automatisch als vertaling toegepast: het lokale taalmodel
blijft de gewone route. Een eigen voorstel is **geen geverifieerde**
machinevertaling, geen bewijs voor ondersteunde Koerdische/Pasjtoe taalparen
en geen basis om het spraakpakket- of offline toestelonderzoek over te slaan.

## Zelf controleren en wissen

Open **Gesprekstools → Tijdelijke vertaalcorrecties** om de exacte
oorspronkelijke tekst, taalrichting en menselijke vertaling terug te zien.
Je kunt afzonderlijke zinnen vergeten of na een aparte bevestiging alles
verwijderen. Maximaal 40 paren blijven in het geheugen; het oudste paar
verdwijnt wanneer dit maximum wordt bereikt. Per exacte zin en taalrichting
bestaat maar één actuele correctie. Alle tekst is begrensd, en opgeslagen
invoer wordt niet naar externe aanbieders gestuurd.

**Wis gesprek** wist ook deze voorstellen. Bij beëindigen van de
app-sessie verdwijnen ze eveneens. Geen SharedPreferences, database,
bestand, backup, modeldownload, netwerkverzoek of achtergrondproces
bevat deze correcties. Een bewuste export van een gespreksregel via de
bestaande aparte Android-dialoog blijft een zelfstandige keuze.

Geen automatische uitspraakherkenning voor Kurmanji, Sorani of Pasjtoe
is door dit onderdeel toegevoegd. De fysieke Android-acceptatie en het
onderzoek naar de rechten en kwaliteit van volledige on-device modellen
blijven open.
