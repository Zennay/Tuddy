# Tijdelijke gespreksmarkeringen

In het live gesprekscherm kan de gebruiker per afgeronde uitspraak op
**☆ Markeer** tikken. De kaart toont dan **★ Gemarkeerd**.
Boven de tijdlijn opent **★ N gemarkeerd** een privéoverzicht van maximaal
80 geselecteerde uitspraken met taal, tijdstip en (indien afgerond)
de echte lokale vertaling.

Uit het overzicht kun je met **Ga naar uitspraak** terug naar precies
die uitspraak in de tijdlijn; automatisch meescrollen wordt dan
gepauzeerd zodat je rustig kunt teruglezen. **Wis markeringen**
verwijdert alleen de markeringen, niet de uitspraken.

Met **Bewaar selectie…** maak je optioneel één gewone privé-TXT-export
van uitsluitend de gemarkeerde regels. Dit gebruikt dezelfde bestaande
Android-bestandskiezer als de volledige gespreksexport, en is alleen
beschikbaar wanneer de microfoon niet luistert. De gebruiker kiest zelf
de bestemming; een externe bestandsprovider kan die tekst synchroniseren.
Lopende vertalingen worden nooit als voltooid weergegeven.

## Privacy en begrenzing

Markeringen zijn enkel tijdelijke ID's in het geheugen van het
huidige gesprek. Geen netwerkverkeer, geen app-private opslag, geen
nieuwe permissies, geen modelprompt, geen wachtwoord of verborgen
automatische export. **Wis gesprek** verwijdert zowel de zinnen als
alle markeringen. Als het geheugenverloop de oudste van maximaal
500 transcriptregels weggooit, verdwijnt ook die markering.
De sessie eindigt bij app-afsluiting; markeringen verschijnen niet
in woordenback-ups, eigen-zinnenback-ups of een volgende sessie.

Geselecteerde tekst is nog altijd een automatische, mogelijk foutieve
transcriptie/vertaling. Een gemarkeerde uitspraak vormt geen bewijs
van taalnauwkeurigheid of geschiktheid voor medische/juridische/asielzaken.
De broncode behoeft nog de bestaande Android-build, gewone releasegate
en echte toestelacceptatie.
