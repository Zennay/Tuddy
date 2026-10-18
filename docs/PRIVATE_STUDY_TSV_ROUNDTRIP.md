# Eigen studielijsten als TSV exporteren

Tuddy kan je **zelfgeschreven** woordnotities en Kurmanji-, Sorani- en
Pasjtoe-zinnen als afzonderlijke, bewerkbare UTF-8 TSV-bestanden opslaan.
De gegevens komen uitsluitend uit je privé-opgeslagen eigen lijsten, nooit
uit gesprekken, microfoongeluid, modeluitvoer of een extern woordenboek.

## Zelf opslaan

Open **Mijn eigen woorden** of **Mijn eigen zinnen** en tik op
**Exporteer mijn eigen woorden (TSV)…** of **Exporteer mijn eigen zinnen
(TSV)…**. Kies vervolgens in Android bewust de opslaglocatie. Er wordt
zonder deze keuze niets bewaard, gedeeld of verstuurd. De Android-bestandsprovider
kan een cloudmap aanbieden; Tuddy kiest die niet zelf.

De export van zinnen heeft precies deze kopregel:

```text
language	phrase	meaningLanguage	ownMeaning
```

De export van woorden heeft precies deze kopregel:

```text
language	word	meaning
```

Elke record staat op een aparte regel met tabgescheiden velden. Bestaande
validatie weigert tabs, nieuwe regels, besturingscodes, dubbele identiteit,
onjuiste taalcodes en te lange waarden binnen de invoervelden. Uitvoer wordt
opnieuw door de bestaande TSV-parser gelezen voordat Android mag opslaan.
Dezelfde limieten gelden: maximaal 80 zinnen of 120 woorden en per bestand
maximaal 50 kB. Lege of ongeldige lijsten worden niet geëxporteerd.

## Opnieuw importeren

Bewerk de eigen TSV-lijst eventueel met een spreadsheet- of teksteditor.
Bewaar als **UTF-8** en laat de kopregel en tabkolommen intact. Gebruik
**Importeer eigen woordenlijst (TSV)…** of **Importeer eigen zinnenlijst
(TSV)…** om het bestand lokaal voor te bekijken. Na het inlezen kies je
expliciet **Samenvoegen** (lokale identiteiten winnen) of **Vervangen**
(hele eigen lijst wordt vervangen); bij ongeldige inhoud blijft de bestaande
privélijst behouden.

Tijdens een open Android-opslagvenster kan de lijst wijzigen. Tuddy
vergelijkt daarom vlak voor het schrijven de actuele lijst met de eerder
gekozen export; een verouderd momentbeeld wordt geweigerd, ook voor de
bestaande JSON-zinnenback-up.

Deze teksten zijn **door de gebruiker geschreven en niet gecontroleerd door
native speakers**. TSV-import en -export leveren geen vrije offline
spraakherkenning of automatische vertaling. Fysieke Android-vliegtuigmodus-,
modellicentie- en productie-releasegates blijven open.
