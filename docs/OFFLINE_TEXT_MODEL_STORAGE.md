# Lokaal tekstmodelbeheer in Tuddy

Via **Taal- en modelinstellingen → Beheer offline tekstmodelopslag** kan een gebruiker zien welke ML Kit **tekstvertaalmodellen** daadwerkelijk op het toestel zijn gedownload. Tik op **Ververs modellen** om de lokale inventaris opnieuw op te vragen. Een ondersteunde taal in de keuzelijst is niet automatisch gedownload.

## Expliciet model verwijderen

- Per aanwezig model verschijnt een knop **Verwijder**. De gekozen bron- en doeltaal zijn beschermd; tijdens luisteren, modelvoorbereiding of een andere opslagactie staat verwijderen uit.
- Een tweede dialoog toont exact welke taal wordt verwijderd. Alleen na **Verwijder model** vraagt de app ML Kit om het betreffende gedownloade model te verwijderen; de aanwezigheid wordt vlak ervoor opnieuw gecontroleerd.
- Er wordt niets automatisch opgeschoond. Stoppen of verlaten van de app trekt UI-callbacks in, maar een al gestarte ML Kit-verwijderactie kan niet worden teruggedraaid.
- Na succesvolle verwijdering is een eventuele latere wifi-download nodig voordat tekstvertaling met die taal weer werkt. Bewaarde woorden en je huidige RAM-only transcriptregels blijven intact.
- De beheerder gebruikt geen cloudspraak, vraagt geen microfoonrechten en verwijdert **geen Android-spraakpakketten**. Hij doet geen claims over precieze opslagbesparing of volledige offline-taaldekking.

## Afbakening

Dit beheert uitsluitend de **ML Kit-tekstmodellen** die Tuddy via de lokale Android API mag zien. De installatie van echte offline spraakherkenning is een afzonderlijke Android-voorziening. Een model op het toestel maakt Kurmanji/Sorani/Pasjtoe vrije spraak- of tekstvertaling niet ineens mogelijk. App-release, fysieke vliegtuigmodusproef en toestelrechten blijven apart vereist. Dit document beschrijft geïmplementeerde Android-broncode, geen fysiek gevalideerde toesteluitvoering.
