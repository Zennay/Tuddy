# Vrijwillige offline herhaalplanning voor bewaarde basiswoorden

Tuddy biedt naast de bestaande **Oefen opgeslagen woorden** (een tijdelijke
oefenronde zonder profiel) een apart opt-in schema dat woorden op een later
moment opnieuw toont.

## Zo werkt het

1. Bewaar een woord uit de kleine, reeds lokaal meegeleverde woordenlijst.
2. Open **Mijn opgeslagen woorden** en kies **Start privé herhaalplanning…**.
3. Lees de toestemmingsuitleg en bevestig **Start alleen lokaal**.
4. Open **Herhaal vandaag**. De betekenis is standaard verborgen.
5. Tik **Toon betekenis** en beoordeel zelf: **Nog oefenen** (morgen weer)
   of **Ik wist het** (na 2, 4, 7, 14 of 30 dagen opnieuw).
6. Gebruik **Sla over zonder beoordeling** als je vandaag geen beoordeling
   wilt geven. De bestaande herhaaldatum blijft staan. Aan het einde zie je
   hoeveel je daadwerkelijk beoordeeld of nog overgeslagen hebt en kun je
   met **Herhaal overgeslagen woorden** uitsluitend die kaarten nogmaals
   bekijken binnen dezelfde sessie. Sluiten schrijft voor overgeslagen kaarten
   geen nieuwe voortgang.
7. **Wis mijn herhaalplanning** verwijdert het schema zonder de woorden te wissen.
   **Wis alle woorden** en **Wis mijn oefengegevens** verwijderen beide.

Een nieuw bewaard woord staat de eerste keer direct klaar voor herhaling.
Per keer verschijnen alleen de woorden die vandaag of eerder gepland staan.
Het scherm doet geen automatische uitspraak, meting of interpretatie
van hoeveel iemand kan. Er zijn geen meldingen: open de app zelf wanneer
je wilt oefenen. De planning volgt de lokale datum van het toestel;
een gewijzigde telefoonklok kan dus de volgorde beïnvloeden.

## Privacy en beperkingen

- Standaard **uit**. Pas na aparte toestemming bewaart Tuddy per opgeslagen
  woordidentiteit een klein geheel getal voor het niveau en een lokale
  kalenderdag voor de volgende herhaling.
- App-private Android-opslag, `allowBackup=false`, geen login, geen server,
  geen microfoon, geen opnamen en geen sessietranscript.
- De gewone woorden-JSON-back-up bevat **geen herhaaldatums of niveaus**.
  Back-upherstel vervangt woorden en wist oude voortgang; ingeschakelde
  herhaalplanning behandelt de herstelde woorden als nog te herhalen.
- Bij verwijderen van een woord wordt ook de bijbehorende voortgang gewist.
  Globaal wissen verwijdert de woorden en de opt-in-status in één
  lokale opslagbewerking.
- Alleen de ingebouwde NL/EN/DE/FR/ES-basiswoorden zijn geschikt.
  Dit is **geen** Pasjtoe-/Koerdische spraakherkenning of volledig vertaalsysteem,
  en zelfbeoordelingen vormen geen leerdiagnose.
- Deze functie vervangt niet de nog ontbrekende fysieke Android 12+
  vliegtuigmodusacceptatie, vrij gelicentieerde volledige ASR/MT,
  onafhankelijke taalreview of releaseondertekening.
