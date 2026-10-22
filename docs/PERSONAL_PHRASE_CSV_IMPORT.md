# Eigen zinnen uit CSV importeren (offline)

Gebruik **Mijn eigen offline zinnen → Importeer eigen zinnen uit CSV…**
om een zelfgemaakt of bewust ontvangen tekstbestand te selecteren via de
Android Documenten-kiezer. Tuddy leest maximaal 50.000 bytes lokaal.
Er vindt geen automatische vertaling, spraakherkenning, cloudverzoek of
modeldownload plaats.

## Bestandsopmaak

Sla het document op als **UTF-8**. De eerste regel moet exact luiden:

```csv
language,phrase,meaningLanguage,ownMeaning
kmr,"Silav, tu çawa yî?",nl,"Hallo, hoe gaat het?"
ckb,"سڵاو",nl,"Hallo"
ps,"سلام",en,"Hello"
```

- De taalcode van een eigen zin is `kmr`, `ckb` of `ps`; een betekenis
  moet als `nl` of `en` gelabeld zijn. Tuddy controleert de inhoud van
  die betekenissen niet.
- In totaal 1–80 gegevensregels, maximaal 120 tekens voor de eigen zin
  en 180 voor de eigen betekenis. Identieke bronzinnen in dezelfde taal
  worden geweigerd, ook als hoofdletters verschillen.
- Komma's in een veld vereisen dubbele aanhalingstekens rondom dat veld;
  een letterlijk aanhalingsteken binnen zo'n veld schrijf je als `""`.
  Elke regel bevat precies vier velden. Een regeleinde binnen één veld
  is niet toegestaan. Zowel Unix- als Windows-regeleinden worden gelezen.
- Ongeldige UTF-8, onbekende talen, gevaarlijke stuur-/opmaaktekens en
  beschadigde, dubbele of te grote lijsten worden in hun geheel geweigerd.

## Controle en bewaren

Na het kiezen toont de bestaande Tuddy-importpreview **alle** gevonden
zelfgeschreven paren, zonder alvast iets te bewaren. Kies afzonderlijk
**Voeg alleen nieuwe zinnen toe** of **Vervang volledige lijst**.
Samenvoegen laat bestaande eigen betekenissen, favoriete kaarten en
herhaaldatums ongemoeid; duplicaten van al bestaande zinnen worden
overgeslagen. Als de limiet van 80 kaarten overschreden wordt, verandert
niets. Volledige vervanging wist bewust de bestaande lijsten en hun
gekoppelde herhaalstatus. Bij een lokale wijziging na de preview wordt
vervanging geweigerd. Een geweigerd bestand wijzigt nooit je gegevens.

Een gekozen Android-bestandsprovider kan een bestand van buiten Tuddy
halen of zelf synchroniseren. Tuddy vraagt zelf geen netwerktoegang.
Het gaat om persoonlijke, niet onafhankelijk gecontroleerde studiekaarten,
**niet** om volledige offline Kurmanji/Sorani/Pasjtoe ASR/MT of
de open acceptatiegates in GitHub issues #1 en #2.

## Export van een zelfgekozen deel van de zinnenlijst

Open **Mijn eigen offline zinnen → Exporteer gekozen zinnen als CSV…**.
Kies of de huidige taal of alle drie talen meegaan en, wanneer een
specifiek thema geselecteerd is, of je alleen dat thema wilt exporteren.
De **volledige lijst met te exporteren privézinnen** verschijnt eerst
op het scherm. Alleen na jouw aparte bevestiging opent de Android
Documenten-kiezer met een `.csv`-bestandsnaam.

Vóór het daadwerkelijke wegschrijven leest Tuddy de huidige eigen
zinnen en thema-indeling opnieuw. Als de geselecteerde tekst tussentijds
wijzigt, weigert de app te exporteren en moet je opnieuw bevestigen.
Een leeg of beschadigd deel wordt niet geëxporteerd. Tuddy gebruikt
hetzelfde beperkte UTF-8 CSV-formaat als de import: speciale komma's
en dubbele aanhalingstekens blijven behouden.

De export bevat bewust **geen** gesprekshistorie, audio, favoriete
markeringen, thema-indelingen of herhaaldata. Een gekozen externe
Android-documentprovider kan zelf synchroniseren; dat is een bewuste
actie buiten Tuddy. Je kunt het bestand in je eigen spreadsheet-editor
bewerken zolang je de vier kolommen en regels behoudt, en later bewust
via **Importeer eigen zinnen uit CSV…** opnieuw kiezen. Duplicaten
worden bij veilig samenvoegen niet overschreven.
