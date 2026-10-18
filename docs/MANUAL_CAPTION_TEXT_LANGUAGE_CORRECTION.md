# Taalrichting van één gesprekszin corrigeren

Wanneer lokale herkenning of taaldetectie de bron verkeerd heeft
ingeschat, kun je voortaan één afgeronde of onvoltooide
gespreksregel corrigeren zonder de microfoon of andere regels te
veranderen.

1. Stop eerst de microfoon.
2. Tik bij de gewenste zin op **Corrigeer taalrichting van deze zin**.
3. Kies de daadwerkelijke oorspronkelijke **teksttaal** en
   de gewenste lokale **vertalingstaal** in de doorzoekbare ML Kit-catalogus.
4. Bevestig de wijziging. Alleen deze zin krijgt de nieuwe
   taalrichting, een nieuwe vertaalrevisie en een lokale hervertaling.

De oorspronkelijke gesproken of getypte tekst, timestamp en
eventuele afzonderlijke eigen uitleg blijven bestaan. De huidige
spraakinstelling, andere gesprekszinnen en spraakpakketkeuze worden
niet gewijzigd. De handmatig geselecteerde brontaal wordt bij deze
hervertaling niet opnieuw overschreven door automatische
kortetekstdetectie. Verlate eerdere vertaalresultaten zijn via
de bestaande revisiebeveiliging ongeldig.

De app downloadt **geen** model bij correctie. Ontbrekende
tekstmodellen worden zichtbaar als onvoltooide vertalingen gemarkeerd;
modellen kunnen alleen via een afzonderlijke optionele wifi-actie
worden voorbereid.

Dit is een **teksttaalcorrectie**, geen echte herkenning van
vrije Kurmanji-, Sorani- of Pasjtoe-spraak. ML Kit ondersteunt
die drie niet als vrije teksttaalcodes. Tekst en aangepaste taalcodes
blijven uitsluitend in het bestaande tijdelijke, begrensde gesprek
totdat de gebruiker zelf optioneel export kiest. Ook na correctie
is de betekenis niet onafhankelijk geverifieerd.
