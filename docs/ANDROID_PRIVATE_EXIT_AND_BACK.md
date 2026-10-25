# Android Terug en privé afsluiten

## Waarom deze bescherming bestaat

Tuddy houdt actuele gespreksregels, lokale vertalingen, correcties en
geïmporteerde voorbeelden bewust uitsluitend in het geheugen. Een tik op
**Terug** mocht daarom niet ongemerkt een actieve sessie afsluiten.

## Werkende Android-flow

- De systeemactie **Terug** en de zichtbare knop **Afsluiten** gebruiken
  nu dezelfde veilige keuze. Een lege app zonder tijdelijk werk kan
  direct worden afgesloten.
- Bij een actieve microfoon, nog niet geëxporteerde gespreksregels,
  concepttekst, een nog te beoordelen één-zinsresultaat of tijdelijk
  geïmporteerde/te exporteren inhoud verschijnt eerst **Tuddy afsluiten?**.
- Wanneer de gebruiker wil afsluiten, stopt de actieve on-device
  microfoon **onmiddellijk**; eventueel lokaal Android-voorlezen stopt
  eveneens. Door **Blijf in Tuddy** te kiezen blijven de tijdelijke
  gespreksregels in het geheugen. Luisteren start nooit automatisch
  opnieuw na een afgebroken afsluiting.
- **Bekijk exportopties** opent alleen de bestaande, ingeklapte
  gespreksgereedschappen. Er wordt geen bestand aangemaakt, niets
  gekopieerd en geen ander app-/netwerkproces gestart zonder een
  afzonderlijke bewuste exportactie. Bestaande Android-bestandspickers
  en hun toestemming blijven onafhankelijk.
- **Sluit zonder bewaren** sluit de Activity en ruimt de on-device
  recognizer, timers, TTS-activiteit en vluchtige sessiegegevens op.
  Ook een onverwachte processtop kan tijdelijke inhoud nog steeds
  verwijderen; deze bevestiging is geen crashherstel of automatische
  opslag.
- Open Android-/Compose-dialogen behandelen hun eigen Terug-actie;
  dit is uitsluitend de afsluitbeveiliging van het hoofdscherm,
  niet een vervanging voor bestaande formulier- of modalnavigatie.

## Privacy en gates

Tuddy schrijft het transcript niet weg in Preferences, SavedState,
App Backup of een externe dienst. Alleen reeds bewust opgeslagen
privéwoorden/-zinnen behouden hun bestaande eigen bewaarbeleid.
De beveiliging verleent geen achtergrondmicrofoontoegang en schakelt
nooit cloudherkenning in.

Een geslaagde GitHub Android-build is geen fysieke vliegtuigmodus-
acceptatie. De menselijke Android 12+-test, exacte geïnstalleerde
APK-identiteit, beide NL↔EN-spraak-/vertaalrichtingen, rechtengecontroleerde
Koerdische/Pasjtoe-modellen en de ondertekende release blijven aparte
go/no-go-besluiten.
