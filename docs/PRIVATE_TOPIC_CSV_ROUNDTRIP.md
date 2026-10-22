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

## Plak een thema-CSV bewust in Tuddy

Naast Android Documenten kun je **Plak mijn eigen zinnen mét thema's
als CSV…** kiezen. Plak zelf maximaal 12.000 tekens inclusief de
verplichte vijfkoloms-kopregel. Tuddy leest niet automatisch het
klembord, haalt niets van het netwerk en gebruikt geen spraakmodel.
Dezelfde strikte CSV-regels en limieten gelden als bij bestandsimport.

Ongeldige of dubbele regels worden vóór iedere wijziging geweigerd;
de geplakte tekst blijft bewerkbaar totdat je de fout corrigeert of
annuleert. Na een geldige invoer verschijnt **de volledige preview**
van alle bronzinnen, eigen betekenissen en themanamen. Pas daarna
kun je bewust veilig samenvoegen of destructief vervangen kiezen.
Annuleren of het sluiten van de plakdialoog bewaart niets.

## Selectief overnemen uit een grote eigen thema-CSV

Ook bij de **import** kun je na het lezen van een geldige vijfkoloms-CSV
bewust één brontaal en/of één thema kiezen. De standaard is **alles**.
De preview toont telkens precies de zinnen en zelfgeschreven betekenissen
die daadwerkelijk bij bevestiging worden verwerkt.

Een **deelimport is alleen veilig samenvoegen**: destructief vervangen
is uitgeschakeld zodra je een filter instelt. Zo kan het importeren van
één thema nooit per ongeluk alle andere eigen zinnen, favorieten of
herhaaldatums wissen. Kies je expliciet weer alle talen én alle thema's,
dan blijft de eerder bestaande volledig-destructieve vervangoptie
beschikbaar met haar eigen waarschuwing.

Een lege selectie heeft geen importknop. Een gewijzigde huidige
woordenlijst/thema-indeling na het bekijken van de CSV zorgt nog
steeds voor een volledige weigering in plaats van een gedeeltelijke
opslag. Bestandsselectie en handmatig plakken gebruiken dezelfde
selectieregels, atomaire toevoeging en volledige voorafgaande preview.

## Welke bestaande zinnen worden bij samenvoegen overgeslagen?

De selectieve importpreview telt nu expliciet het aantal nieuwe zinnen
en zinnen die al in de huidige persoonlijke lijst bestaan.
Bij elke regel staat **nieuw** of **bestaat lokaal**. Bij veilig
samenvoegen blijft voor een bestaande bronzin zowel je eigen
betekenis als je huidige thema altijd leidend, ongeacht wat
de externe CSV laat zien. Voor nieuwe zinnen toont Tuddy welk
eigen thema daadwerkelijk zal worden toegevoegd.

Dit helpt de gebruiker vooraf verschil te zien tussen een nieuwe
taalgroep importeren en een bestaande lokale betekenis onbedoeld
proberen bijwerken. De preview is geen automatische vertaling
en voert zelf geen opslagactie uit.

## Bewust delen via Android, zonder eerst een bestand te bewaren

Na het kiezen van een eigen taal/thema-subset en de volledige
controlepreview kan de gebruiker **Bekijk volledige CSV en deel
bewust…** kiezen. Een tweede, aparte privacypreview toont de
**exacte vijfkoloms UTF-8 CSV als ruwe tekst**, inclusief ieder
zelfgeschreven zinnenpaar en de eventuele themanamen.
Pas na **Ik begrijp dit, open Android Delen…** wordt het
Android-deelvenster geopend met die exacte tekst.

Vóór het deelvenster wordt de actuele lokale selectie opnieuw
vergeleken met de hele preview: een gewijzigde zin of
thema-indeling weigert het delen. Er wordt geen ander
woordboek, transcript of audio toegevoegd en Tuddy kiest
zelf geen ontvanger. Een door de gebruiker gekozen externe
app kan de data wel uploaden, synchroniseren of doorsturen;
Tuddy kan die externe kopie niet verwijderen. De functie
is standaard uit totdat de gebruiker bewust beide stappen
doorloopt en is geen automatische cloudbackup.


## Zelf afzonderlijke CSV-regels kiezen bij import

Bij **Importeer zinnen mét thema's uit CSV…** en **Plak mijn eigen zinnen
mét thema's als CSV…** kun je na de taal- en themafilter ook **per regel**
bewust kiezen tussen **☑ Meenemen** en **☐ Overgeslagen**. De bronzin,
je eigen betekenis en het thema blijven zichtbaar, ook wanneer je de regel
uitsluit. Tik opnieuw om een overgeslagen regel weer mee te nemen.

Met **Neem alle zichtbare zinnen mee** of **Sla alle zichtbare zinnen over**
kun je in één keer de op dat moment gefilterde regels kiezen. De keuze
voor andere, verborgen filtergroepen blijft behouden. **Herstel volledige
CSV-selectie** wist de filters én alle overgeslagen regels.

Het scherm toont uitsluitend voor werkelijk geselecteerde regels de
aantallen **nieuw** en **bestaat lokaal**. Bij nul gekozen regels is
bevestigen onmogelijk. Zodra je zelfs één regel uitsluit, is **Vervang hele
privé-lijst** uitgeschakeld: een deelimport kan uitsluitend veilig
samenvoegen en verandert nooit een bestaande betekenis, thema of
herhaalplanning. Pas nadat de gehele CSV weer is gekozen, is de apart
gewaarschuwde volledige vervangoptie beschikbaar.

De rijselectie wordt uitsluitend tijdens de huidige preview in geheugen
bewaard. Een nieuwe bestandsimport of nieuwe handmatige plakimport begint
met alle geldige regels geselecteerd. Vóór opslag blijft de bestaande
strenge controle tegen inmiddels gewijzigde lokale zinnen en thema's
actief; bij conflict wordt niets gedeeltelijk opgeslagen. Er is geen
automatische synchronisatie, vertaling, of verwerking van conversatieaudio.
