# Vrijwillige herhaalplanning voor mijn eigen woordbetekenissen

De tijdelijke oefenkaarten voor eigen woorden blijven bestaan. Nieuw
is een **afzonderlijke, uitgeschakelde-tot-toestemming** herhaalplanning.

## Op het toestel

1. Voeg zelf een woord en jouw **eigen** betekenis toe via
   **Mijn eigen offline woorden**. Deze notitie is niet door Tuddy
   of een native speaker geverifieerd.
2. Tik **Start privé herhaalplanning eigen woorden…** en bevestig expliciet.
3. Open **Herhaal mijn eigen woorden vandaag**. Draai desgewenst de
   vraagrichting om, inclusief rechts-naar-links voor Sorani/Pasjtoe.
4. Bekijk je eigen antwoord pas na **Toon mijn eigen antwoord**.
5. **Nog oefenen** plant de volgende dag; **Ik wist het** schuift
   het interval naar 2, 4, 7, 14 en uiteindelijk 30 dagen.
6. **Sla over zonder beoordeling** schuift alleen de huidige kaart
   uit beeld. De herhaaldatum blijft onveranderd. Na de laatste kaart
   zie je het aantal beoordeelde en nog overgeslagen woorden. Kies
   **Herhaal overgeslagen woorden** om alleen die woorden opnieuw te oefenen
   zonder eerdere beoordelingen nogmaals aan te bieden.
7. **Wis herhaalplanning eigen woorden** wist deze extra voortgang
   zonder jouw woorden en betekenissen te verwijderen.

Er zijn geen pushmeldingen of achtergrondtimers. De lokale
telefoondatum is leidend; een klokwijziging kan de planning verschuiven.

## Privacy en dataverlies

- Alleen na toestemming slaat Tuddy per bewust opgeslagen eigen woord
  een **SHA-256-vingerafdruk**, een lokaal oefenniveau en een dagnummer
  op. Er ontstaat geen tweede woorden- of betekenissenlijst.
- De app gebruikt geen account, cloud, vertaalmodel, spraakherkenning,
  microfoon of persoonlijke scoreservice. Android `allowBackup=false`
  blijft van kracht.
- Een **gewijzigde eigen betekenis** begint weer als nieuwe oefenkaart.
  Verwijderen van een woord ruimt diens herhaaldatum in dezelfde
  opslagtransactie op.
- Bewust **Vervang volledige eigen lijst** via eigen JSON/TSV-back-up
  wist de oude herhaalplanning; **Samenvoegen** behoudt de planning
  van onveranderde bestaande notities.
- **Wis mijn oefengegevens** wist ook de opt-in status en alle
  herhaaldatums. Dit geldt niet voor zelf gekozen externe exportbestanden.
- Normale eigen-woorden-back-ups bevatten **geen** oefenniveaus of datums.
- Niet-onafhankelijk-beoordeelde betekenissen worden nooit als
  gecontroleerde vertalingen of taalvaardigheidsdiagnoses weergegeven.

De bestaande offline ASR/MT-, native-speaker-, fysiek toestel- en
productierelease-gates zijn hierdoor niet vervuld.
