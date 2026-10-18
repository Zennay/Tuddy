# Zelf ingevoerde oefenzinnen aanpassen

Open **Mijn eigen Kurmanji/Sorani/Pasjtoe zinnen**, zoek de zin en
kies **✎ Bewerk deze zin**. Tuddy vult de bestaande taal, tekst, taal
van de eigen betekenis en de betekenis opnieuw in in het formulier.
Pas één of meerdere velden aan en kies **Bewaar wijziging aan eigen zin**.
Met **Annuleer bewerken** blijft de oorspronkelijke notitie ongewijzigd.

Bewerken is een **atomaire lokale vervanging van één bestaand item**:
- De bronnotitie moet nog exact overeenkomen met de gekozen notitie.
  Bij wijziging vanuit een ander scherm wordt niets blind overschreven.
- Duplicaten van een taal/zin-combinatie zijn verboden, ook na een
  taalwissel of verandering van hoofdletters; alle 80 regels behouden
  dezelfde bestaande tekstlimieten en formuliercontrole.
- De rest van de lijst blijft ongewijzigd. Zelfs als een wijziging
  wordt geweigerd, is de oude zin niet eerst verwijderd.
- Bij het verwijderen, wissen of herstellen van de bewerkte notitie
  wordt de open bewerkmodus veilig gesloten.

Deze zinnen en betekenissen worden uitsluitend door de gebruiker
geschreven en **niet** door Tuddy vertaald of gecontroleerd. Ze blijven
in de app-private opslag omdat de gebruiker ze expliciet bewaart.
Een optionele back-up naar een zelf gekozen bestandsprovider kan buiten
Tuddy synchroniseren. Er is geen koppeling met live-opnames of transcripties.

Beschikbaarheid in GitHub-broncode bewijst nog geen succesvolle fysieke
Android-toestelinstallatie of vrijgegeven APK.

## Eigen zin als grote gesprekskaart tonen

Bij een opgeslagen eigen zin is **▣ Toon groot · gesprekskaart** beschikbaar.
Daarmee toon je de zelfgeschreven zin én eigen betekenis op het scherm,
kun je binnen dezelfde taalset bladeren en de leesgrootte/volgorde aanpassen.
Dit is geen machinevertaling en gebruikt geen microfoon, cloud of TTS.
[Bediening en grenzen](OFFLINE_BILINGUAL_PHRASE_PRESENTER.md).

## Complete eigen zinnenlijst vanuit TSV importeren

Via **Mijn eigen offline zinnen → Importeer eigen zinnenlijst (TSV)…**
kun je een zelf voorbereid, lokaal UTF-8-tekstbestand kiezen. De eerste
regel moet exact deze vier kolommen bevatten, gescheiden door echte tabs:

```text
language	phrase	meaningLanguage	ownMeaning
kmr	Slav	nl	Mijn eigen Nederlandse uitleg
ckb	سڵاو	en	My own example meaning
ps	سلام	nl	Mijn eigen Pasjtoe-omschrijving
```

De voorbeeldinhoud is **door een gebruiker ingevoerd en niet onafhankelijk
geverifieerd**. Alleen `kmr` (Kurmanji), `ckb` (Sorani) en `ps` (Pasjtoe)
zijn toegelaten voor de zintaal; de taal van de betekenis is `nl` of `en`.
Een zin mag maximaal 120 tekens en de eigen betekenis maximaal 180 tekens
bevatten. Het bestand heeft maximaal 80 dataregels en is maximaal 50 KB.
Onleesbare UTF-8, extra tabkolommen, dubbele taal+zin-identiteiten,
ongeldige codes en onveilige tekens worden als geheel geweigerd.

Na het inlezen verschijnt een aparte keuze tussen **alleen nieuwe zinnen
toevoegen** en **de huidige eigen lijst vervangen**. Samenvoegen laat
bestaande lokaal geschreven betekenissen en wijzigingen ongemoeid.
Het importeren van een tekstbestand doet zelf geen spraakherkenning of
vertaling, stuurt niets naar een server en wijzigt zonder de zichtbare
bevestiging geen lokale gegevens. Het bestaande apparaatprivé favorieten-
beheer ruimt bij echt vervangen ongeldige snelkoppelingen op.
