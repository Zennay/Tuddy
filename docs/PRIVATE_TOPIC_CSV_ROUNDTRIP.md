# Bewerkbare CSV met eigen zinnen én thema's

De bestaande CSV met vier kolommen blijft beschikbaar voor eenvoudige
import, export en plakken van eigen zinnen. Daarnaast verschijnen in
**Mijn eigen offline zinnen** twee aparte knoppen:
**Exporteer zinnen mét thema's als CSV…** en **Importeer zinnen mét thema's
uit CSV…**. Deze vijfkolomsvariant is bedoeld voor zelfgeschreven
Kurmanji-, Sorani- en Pasjtoe-zinnen mét hun persoonlijke thema-indeling.

## Eigen formaat

Gebruik een UTF-8-bestand met de exacte kolomkop hieronder:

    language,phrase,meaningLanguage,ownMeaning,topic
    kmr,"Silav, tu çawa yî?",nl,"Hallo, hoe gaat het?",Kennismaken
    ckb,"سڵاو",nl,Hallo,Kennismaken
    ps,"سلام",en,Hello,

Alle vijf kolommen zijn verplicht; de laatste mag leeg blijven voor
**zonder thema**. Taalcode voor de bron is kmr, ckb of ps; de betekenis
heeft code nl of en. Quote velden met komma's; schrijf een
aanhalingsteken binnen een gequote veld dubbel. Regeleinden binnen
cellen worden geweigerd. De grenzen: maximaal 80 unieke zinnen, 12
verschillende thema's, 120 tekens bronzin, 180 tekens betekenis,
30 tekens per themanaam, 50 KiB bestandsgrootte.

Ongeldige UTF-8, dubbele bronidentiteiten, niet-toegelaten
opmaaktekens, extra kolommen en beschadigde quotes worden volledig
geweigerd. Het bestand biedt geen automatische vertaling of ASR.

## Exporteren

De export toont **alle zelfgeschreven zinnen met hun thema** in
een volledige controlepreview. Pas na afzonderlijke bevestiging
kiest de gebruiker een Android Documenten-locatie voor
Tuddy-eigen-zinnen-met-themas.csv. Bij wijzigingen van zinnen
of thema's terwijl de bestandskiezer open is, wordt het
exportbestand geweigerd: begin dan opnieuw met de preview.

De CSV bevat alleen eigen zinnen en hun gekoppelde thema per zin,
geen gespreksgeschiedenis, favorieten, audio of herhaaldatums.
Lege, ongebruikte thema's worden niet meegegeven. Gebruik de
bestaande JSON-archiefoptie om ook ongebruikte themanamen te bewaren.

## Importeren

Selecteer bewust het CSV-bestand via Android Documenten.
De volledige inhoud wordt eerst lokaal gecontroleerd en
**iedere bronzin, betekenis en thema** getoond. Daarna volgt
een afzonderlijke keuze tussen:

- **Voeg uitsluitend nieuwe zinnen toe:** behoudt bestaande zinnen,
  betekenissen, themakoppelingen, favorieten en geplande herhalingen.
  Alleen nieuwe zinnen en hun thema's worden atomair toegevoegd.
  Bij overschrijding van 80 zinnen of 12 thema's verandert niets.
- **Vervang alle eigen zinnen en thema's:** vervangt de volledige
  persoonlijke lijst en thema-indeling door de getoonde import.
  Daarbij verdwijnen bestaande persoonlijke favorieten en
  herhaaldatums. Dit is een bewust destructieve optie.

Bij gewijzigde lokale gegevens tussen de volledige preview en
de bevestiging weigert Tuddy de import. Deze CSV-functie gebruikt
de bestaande veilige, atomaire archief-importfuncties. Geen
ongevraagde netwerktoegang, klembordtoegang, microfoon of opslagactie.
Android-bestandsproviders kunnen door de gebruiker gekozen worden
en zelf synchroniseren.

**Afzonderlijk open:** fysieke Android 12+ acceptatie (#1) en
vrijgelicentieerde/native-speaker-beoordeelde volledige offline
Kurmanji/Sorani/Pasjtoe ASR+MT (#2). Deze handgeschreven zinnen
voldoen niet aan die P1-eisen.

## Alleen gekozen zinnen delen: taal- en themafilter

Je hoeft je hele persoonlijke boek niet uit te wisselen. Kies
**Exporteer gekozen zinnen mét thema's als CSV…** en selecteer eerst
bewust één van de drie brontalen of alle talen, en één eigen thema,
alle thema's of uitsluitend de groep **Zonder thema**.
Daarna toont Tuddy de **volledige daadwerkelijke subset**
inclusief zelfgeschreven betekenis en optionele themanaam.

Er worden nooit stilzwijgend andere privékaarten toegevoegd.
Een lege selectie kan niet worden opgeslagen. Bij Androids
bestandskeuze controleert Tuddy niet alleen de bestaande zinnen
en hun thema's opnieuw, maar ook dezelfde taal- en themaselectie.
Bij tussentijdse veranderingen wordt de export geweigerd, zodat
de gebruiker opnieuw kan controleren. De gekozen CSV blijft
geschikt voor dezelfde vijfkoloms-import op een ander toestel.

Ook deze export is een **bewuste externe kopie** en kan door
een door jou gekozen Android-documentprovider online worden
gesynchroniseerd. De interne persoonlijke lijst verandert niet.
