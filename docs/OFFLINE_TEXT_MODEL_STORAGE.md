# Lokaal tekstmodelbeheer in Tuddy

Via **Taal- en modelinstellingen → Beheer offline tekstmodelopslag** kan een gebruiker zien welke ML Kit **tekstvertaalmodellen** daadwerkelijk op het toestel zijn gedownload. Tik op **Ververs modellen** om de lokale inventaris opnieuw op te vragen. Een ondersteunde taal in de keuzelijst is niet automatisch gedownload.

## Expliciet model verwijderen

- Per aanwezig model verschijnt een knop **Verwijder**. De gekozen bron- en doeltaal zijn beschermd; tijdens luisteren, modelvoorbereiding of een andere opslagactie staat verwijderen uit.
- Een tweede dialoog toont exact welke taal wordt verwijderd. Alleen na **Verwijder model** vraagt de app ML Kit om het betreffende gedownloade model te verwijderen; de aanwezigheid wordt vlak ervoor opnieuw gecontroleerd.
- Er wordt niets automatisch opgeschoond. Stoppen of verlaten van de app trekt UI-callbacks in, maar een al gestarte ML Kit-verwijderactie kan niet worden teruggedraaid.
- Na succesvolle verwijdering is een eventuele latere wifi-download nodig voordat tekstvertaling met die taal weer werkt. Bewaarde woorden en je huidige RAM-only transcriptregels blijven intact.
- De beheerder gebruikt geen cloudspraak, vraagt geen microfoonrechten en verwijdert **geen Android-spraakpakketten**. Hij doet geen claims over precieze opslagbesparing of volledige offline-taaldekking.

## Meerdere modellen in één bevestigde opruimactie

- Vink in de aanwezige modelinventaris meerdere ongebruikte tekstmodellen aan of kies **Selecteer ongebruikte**. Het actieve bron-/doeltaalpaar blijft uitgesloten.
- **Verwijder selectie** toont vóór de actie de volledige lijst met taalnaam en taalcode. Niets wordt verwijderd voordat de gebruiker die lijst bevestigt.
- Na bevestiging verwijdert Tuddy modellen één voor één. Vóór iedere verwijdering wordt de actuele ML Kit-inventaris opnieuw gelezen en wordt het huidige taalpaar opnieuw gecontroleerd.
- Bij de eerste fout stopt de reeks. Tuddy toont hoeveel modellen al verwijderd zijn en leest daarna de inventaris opnieuw in. Eerder geslaagde verwijderingen worden niet als teruggedraaid voorgesteld.
- Een lopende batch kan niet transactioneel worden teruggedraaid. Bij verlaten van de app worden vervolgcallbacks ongeldig gemaakt; de inventaris moet daarna opnieuw worden gelezen om de actuele toestand te tonen.
- Alleen ML Kit-tekstmodellen worden beheerd. Android-spraakpakketten, gesprekken, opgeslagen woorden en de taalkeuze zelf worden niet verwijderd. Er worden geen exacte bytes of opslagbesparingen geclaimd.

## Afbakening

Dit beheert uitsluitend de **ML Kit-tekstmodellen** die Tuddy via de lokale Android API mag zien. De installatie van echte offline spraakherkenning is een afzonderlijke Android-voorziening. Een model op het toestel maakt Kurmanji/Sorani/Pasjtoe vrije spraak- of tekstvertaling niet ineens mogelijk. App-release, fysieke vliegtuigmodusproef en toestelrechten blijven apart vereist. Dit document beschrijft geïmplementeerde Android-broncode, geen fysiek gevalideerde toesteluitvoering.
