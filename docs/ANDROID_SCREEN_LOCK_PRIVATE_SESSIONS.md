# Android-schermvergrendeling: microfoon, vertaling en TTS direct stoppen

## Nieuwe toestelgrens

Android kan het scherm laten slapen of de vergrendeling tonen **voordat**
een zichtbare Activity de normale lifecycle-melding `ON_PAUSE` krijgt.
Tuddy stopt opname en lokale audio al bij `ON_PAUSE`, maar gebruikt nu
ook een tijdelijk geregistreerde listener voor Androids door het systeem
beschermde `ACTION_SCREEN_OFF`-melding. Deze melding volgt de
*interactieve toestand* van het apparaat, niet per se uitsluitend het
fysieke display. De listener werkt alleen zolang het betrokken
Tuddy-scherm op de voorgrond en **RESUMED** is. Hij vraagt geen extra
Android-permissie, wake lock, accessibility service of achtergrondservice.

De bewaker controleert vóór activatie en **na registratie** of Android
het toestel als interactief meldt en er geen vergrendelingsscherm
zichtbaar is. Kan deze toestand niet betrouwbaar worden vastgesteld
of lukt het registeren niet, dan weigert de hoofdapp een nieuwe
microfoonsessie. De snelle tekstvertaler wist in dat geval zijn tijdelijke
selectie. Er start nooit automatisch iets op het ontgrendelscherm.

## Gedrag van het gewone gesprek

- Bij een echte Android-scherm-uitmelding stopt de microfoon direct.
  Androids recognizer wordt afgesloten, verlopen callbacks worden
  ongeldig gemaakt en ingeplande automatische herstarts vervallen.
- Een onbevestigd één-zinsvoorstel wordt gewist. Al gecontroleerde
  gespreksregels blijven **alleen in het tijdelijke werkgeheugen**,
  zoals bij de bestaande Stop-functie.
- Hetzelfde Stop sluit lokale spraakuitvoer (TTS), de aangesloten
  apparaatbewakers en microfoontimers. De melding legt uit dat
  **opnieuw bewust op Luister** nodig is na ontgrendelen.
- Een late runtime-permissietoestemming of herkenningsherstart mag de
  microfoon niet starten wanneer de bewaker niet geactiveerd is of
  Android het toestel als niet-interactief/vergrendeld meldt.

## Gedrag van Android geselecteerde tekst

Wanneer **Vertaal met Tuddy** als eigen tekstselectiescherm actief is,
wis en annuleer bij dezelfde system screen-off-melding:

- geselecteerde bronzin, vertaalresultaat en mogelijke reviewcorrectie;
- lopende taalherkenning en vertaling (late callbacks worden geweigerd);
- eventuele lokale TTS-uitvoer en de bijbehorende audiofocus.

Er wordt niets automatisch gekopieerd, gedeeld of teruggestuurd naar
de oorspronkelijke app. Na ontgrendeling moet de gebruiker **zelf
opnieuw tekst selecteren**. Buiten het scherm blijft geen listener
draaien. De bestaande `ON_PAUSE`-redactie blijft als tweede grens
actief voor split-screen, appwissel en andere Android-focuswisselingen.

## Losse fysieke acceptatie (niet uitgevoerd door een codebuild)

Een geautoriseerde Android 12+-tester kan de **exacte main-debug-APK**
installeren, een NL→EN- of EN→NL-luistersessie starten en de fysieke
powerknop gebruiken om het scherm te vergrendelen. Na ontgrendeling:
controleer of de opname is gestopt, de app niet automatisch weer luistert,
een onbevestigd spraakvoorstel is gewist, en eerder voltooide regels niet
opnieuw of extern worden gedeeld. Herhaal tijdens privévoorlezen en
`Vertaal met Tuddy` om te observeren of audio stopt en de tijdelijk
geselecteerde tekst wordt geredigeerd.

**Belangrijk:** Android-systeemmeldingen en `PowerManager.isInteractive`
zijn software-observaties. Ze zijn geen onafhankelijk bewijs van
fysieke netwerkisolatie, gegarandeerde radio-uitstand, onmiddellijke
microfoonsensorstilte of perfecte Android OEM-eventtiming. Dit werk
vervangt de aparte Android-vliegtuigmodusacceptatie, native-speaker-
en licentiegates voor aanvullende talen, en releaseondertekening niet.
