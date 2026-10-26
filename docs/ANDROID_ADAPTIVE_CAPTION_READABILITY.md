# Grote, leesbare tweetalige Android-gesprekken

## Eén leesinstelling voor live en archief

Open Tuddy's **Leesmodus** of de fullscreen archieflezer en tik op
**A A · Leesvergroting**. Met **A−** en **A+** vergroot of verklein je de
gesprekszinnen in vier vaste stappen: 100%, 125%, 150% of 175% boven op
de bestaande app-ondertitelgrootte.

Androids eigen systeemlettergrootte blijft daar **onafhankelijk**
bovenop werken. Tuddy vermenigvuldigt de systeemfactor niet dubbel
wanneer het de feitelijke grootte van de tekst kiest. Origineel,
afgeronde vertaling, live tussentekst en één-zin-TalkBack-leesmodus
gebruiken dezelfde vergroting. Bij grotere letters neemt Tuddy meer
regelafstand om elkaar overlappende regels te vermijden.

De gekozen vergrotingsstap wordt met Androids private appvoorkeuren
bewaard. Er wordt **geen** transcript, audiogegevens, vertaling,
geselecteerde regel, zoekterm, scrollpositie, afspeelstatus of
microfooninstelling opgeslagen.

## Automatische breedtecontrole

Tuddy gebruikt Androids actuele schermbreedte in dp en de effectieve
tekstgrootte om te beslissen of twee tweetalige kolommen zinvol zijn.

- Op een smal scherm (onder 600 dp) altijd **onder elkaar**.
- Bij matige vergroting alleen kolommen met voldoende breedte.
- Bij zeer grote tekst pas twee kolommen op een breed tablet (minstens
  900 dp). Anders staan beide talen onder elkaar.

Een bestaande bewuste keuze voor kolommen blijft intact waar de
werkelijke leesruimte dat toelaat. Wordt het scherm smaller of vergroot
je de letters, dan kiest de leesmodus automatisch de veilige
enkelkolomsweergave; geen gespreksregel wordt gewijzigd of gewist.

## Acceptatie en grenzen

De presentatie kan bij Android-rotatie, fontgrootte-aanpassing of
ingebouwde schermvergroting zonder nieuwe netwerk-/modeloproep wijzigen.
De schermlezer krijgt in één-zin-modus nog steeds een stabiele originele
tekst en de bijbehorende vertaling; er wordt **geen** spraak automatisch
afgespeeld of verstuurd. Dit is een app-/UI-verbetering, geen bewijs
voor fysieke Android 12+ vliegtuigmodustests of taalvalidatie.

De bestaande release-gates blijven apart: geïnstalleerde debug-APK
identiteit en radio-isolatie op een echt toestel, NL→EN/EN→NL
ASR+MT-observaties, rechten- en moedertaalsprekerbeoordeling voor
Kurmanji/Sorani/Pasjtoe en een apart ondertekende productie-release.
