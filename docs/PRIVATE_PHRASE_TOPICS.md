# Privéthema's voor mijn eigen offline gesprekszinnen

Je kunt je eigen Kurmanji-, Sorani- en Pasjtoe-zinnen nu indelen in
maximaal **twaalf** thema's, bijvoorbeeld *arts*, *werk*, *reizen* of
*afspraken*. Thema's worden **alleen door de gebruiker** gekozen, niet
door een AI, vertaalmodel of taalherkenner.

## Zelf indelen

1. Open **Mijn eigen offline zinnen**.
2. Kies **Mijn zinnenthema's beheren** en voeg een korte naam toe.
3. Scroll naar een eigen zin, tik **Kies thema voor deze zin** en selecteer
   het thema of **Zonder thema**.
4. Filter in hetzelfde scherm op **Alle thema's**, **Zonder thema** of
   een specifiek thema. Zoeken en taalweergave blijven beschikbaar.
5. Tik **Oefen dit thema** om uitsluitend de eigen zinnen van de
   gekozen groep te oefenen. De bestaande oefenkaarten blijven
   tijdelijk en slaan geen leerprofiel op.

Je kunt thema's hernoemen of verwijderen. Verwijderen wist alleen de
indeling, **nooit de oorspronkelijke zinnen, eigen betekenissen,
favorieten of herhaalplanning**.

## Grenzen, opslag en back-up

- Namen bevatten 1 tot 30 tekens, hoogstens 12 thema's, ieder bewaard
  zinnetje maximaal één thema. Onveilige controltekens worden geweigerd.
- Toewijzingen bevatten alleen een SHA-256-vingerafdruk van de
  bestaande privézin en het door de gebruiker gekozen thema.
  Geen tweede kopie van zin/betekenis, geen microfoon, cloud of netwerk.
- Bij wijzigen of verwijderen van de **zin-identiteit** wordt een
  verouderde koppeling in dezelfde lokale opslagtransactie verwijderd.
  Een wijziging van alleen de eigen betekenis behoudt de indeling.
- Het bewust **vervangen** van de volledige zinnenlijst uit een JSON/TSV
  back-up verwijdert de oude indeling. **Samenvoegen** behoudt de
  bestaande indeling. Deze back-ups bevatten zelf géén themagegevens.
- **Wis mijn volledige eigen zinnenlijst** en **Wis mijn
  oefengegevens** wissen ook de thema's en toewijzingen.
- Namen en hashkoppelingen blijven in Android app-private opslag met
  `allowBackup=false`; geen account of synchronisatie.
- De eigen zinnen en betekenissen zijn **niet onafhankelijk
  gecontroleerd** en deze functie biedt geen volledige
  Kurmanji/Sorani/Pasjtoe-spraakherkenning of vertaalmodellen.

Tuddy blijft Incubate / Verifying tot de afzonderlijke
hardware-, model-, licentie-, native-speaker- en signinggates echt
afgerond zijn. Geen productie-admission of toestelacceptatie geclaimd.
