# Eigen offline zinnen-CSV direct openen met Tuddy

Tuddy kan nu een **vijfkoloms CSV met eigen Kurmanji-, Sorani- en
Pasjtoe-zinnen** bewust ontvangen via **Openen met Tuddy** in Android
Bestanden, of als **één** gedeeld CSV-bestand via Android Delen.
Tuddy biedt zich alleen aan voor `text/csv` en `application/csv`; de
bestaande ondertitel- en tekstdelen-routes blijven apart.

## Zo werkt het

1. Kies zelf in Android Bestanden een CSV en tik op **Openen met Tuddy**,
   of deel bewust één CSV-bestand naar Tuddy.
2. Tuddy opent automatisch het scherm **Mijn eigen offline zinnen**,
   maar **importeert niets automatisch**. Een strikt gevalideerde
   volledige CSV-preview verschijnt vóór alle wijzigingen.
3. Bekijk de bronzinnen, jouw zelfgeschreven NL/EN-betekenissen en de
   themakoppelingen. Selecteer eventueel alleen bepaalde regels,
   betekeniscorrecties of themawijzigingen.
4. Bevestig de gekozen atomaire import afzonderlijk. Annuleren,
   ongeldige UTF-8, onjuiste vijfkoloms-kopregel of te grote inhoud
   bewaren **geen** privégegevens.

De exacte kop is:
`language,phrase,meaningLanguage,ownMeaning,topic`.
De bestaande vierkoloms CSV, JSON of tekst-ondertitelbestanden zijn
**geen** automatische privéthema-import. Gebruik daarvoor de eigen
handmatige keuzes in Tuddy. Voor een vijfkoloms CSV gelden 50 KiB,
80 unieke zinnen, twaalf thema's en alle bestaande strenge
tekst-/taalcodegrenzen.

## Privacy en grenzen

- Alleen een door de gebruiker geopend `content://`-document van een
  Android provider wordt gelezen. `file://`, `http(s)://`, HTML,
  gemengde inhoud en meerdere bestanden worden geweigerd.
- De tijdelijke URI wordt niet als appgeschiedenis, accountgegeven of
  automatische back-up opgeslagen. Het bestand wordt begrensd tot
  50 KiB en buiten de UI-thread ingelezen vóór het bestaande strikte
  UTF-8- en CSV-parsepad. De bronprovider kan zelf externe synchronisatie
  gebruiken; Tuddy doet geen eigen upload.
- Het openen van een CSV activeert geen microfoon, ASR, automatische
  vertaling of download. De gebruiker moet iedere opslagactie expliciet
  bevestigen; het bestaande preview-, snapshot- en rollbackbeleid
  blijft van toepassing.
- De app noemt de eigen betekenissen nooit onafhankelijk gecontroleerde
  vertalingen. De volledige offline Kurmanji/Sorani/Pasjtoe-ASR+MT
  en fysieke Android-acceptatie blijven afzonderlijke open P1-gates.

Een provider die alleen `text/plain` aanbiedt kan zijn CSV nog
steeds via **Importeer zinnen mét thema's uit CSV…** laten kiezen.
