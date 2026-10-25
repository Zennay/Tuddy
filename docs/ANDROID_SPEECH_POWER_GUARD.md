# Toestelbeveiliging voor langdurig on-device luisteren

## Bewust inschakelen

De bestaande Tuddy-microfoonsessie is alleen op de voorgrond toegestaan en
heeft al een optionele tijdslimiet, Android-microfoon-/oproepbewaking en
eventueel een strikte vliegtuigmodus. **Toestelbeveiliging · lokaal
luisteren en accu** is daarbovenop een afzonderlijke optionele bescherming.
Deze staat standaard **uit**, zodat gebruikers op toestellen met afwijkende
Android-sensorondersteuning geen nieuwe onverwachte startbeperking krijgen.

De gebruiker kan de instelling alleen wijzigen wanneer de microfoon is
gestopt. De keuze wordt als één boolean uitsluitend in lokale
appvoorkeuren opgeslagen, niet in cloud- of analysetelemetrie.

## Wat wordt bewaakt?

Tijdens een bewust gestarte on-device luisterronde inspecteert Tuddy:

- De **Android-thermische status**: bij `THERMAL_STATUS_SEVERE` of hoger
  weigert Tuddy een nieuwe ronde of stopt de actieve microfoon.
- De **Android-accu-indicatie**: wanneer Android maximaal **5%** meldt
  en het toestel niet als aangesloten/opladend beschrijft, weigert Tuddy
  een nieuwe ronde of stopt de huidige.
- Android-batterij- en laadstatusgebeurtenissen en een
  `PowerManager`-thermal listener tijdens de microfoonsessie.
- Een extra controle elke **12 seconden** zolang de optie actief is,
  voor het geval Android een callback niet meteen aflevert.

De app leest de *actuele* systeemtoestand opnieuw. Hij vertrouwt
broadcast-inhoud niet als bewijs. De controle geldt opnieuw wanneer
Android de herkenner voor een volgende zin automatisch moet starten.
Bij een ernstige melding worden de herkenner en geplande
herstarts afgesloten, met de bestaande generation-fence die oude
callbacks ongeldig maakt. De oorspronkelijke tijdelijke gespreksregels
blijven zichtbaar. **Opnieuw luisteren vraagt altijd een eigen tik.**

Kan Tuddy bij inschakelde bescherming de tijdelijke Android-listeners
niet veilig registreren, dan wordt de nieuwe luistersessie geweigerd.
Bij Stop, achtergrond, wegvallen van focus, foutherstel of gewone
spraakherkennerwissel verdwijnen alle listeners en de timer.

## Privacy, feedback en grenzen

De toestelbewaking verwerkt **geen audio, transcript, SSID, locatie,
temperatuurgeschiedenis, oplaadgeschiedenis of blijvend sessielog**.
Het bestaande privacyrapport kan wel melden of de optie aanstaat,
of een tijdelijke observer actief is en hoeveel keer de app in deze
RAM-sessie bewust is gestopt. Geen tijdstempels of ruwe
batterijwaarden worden in het rapport geplaatst.

Android levert niet overal volledige of tijdige thermische en
batterijsignalen. **Onbekende sensorwaarden bewijzen geen veilige
toestand** en een uitblijvend signaal is geen garantie tegen hitte of
accuschade. De app gebruikt de drempels alleen wanneer Android ze
daadwerkelijk rapporteert; dit vervangt geen Android-eigen thermische
bescherming. Als je toestel heet aanvoelt, stop zelf met luisteren.

Deze broncode en een succesvolle GitHub Android-build zijn geen
fysieke Android 12+-proef, geen gegarandeerde radiostilte, geen
native-speaker-acceptatie van spraak/vertaling en geen getekende
release-APK. De bestaande rechten-, modellen-, fysieke acceptatie-,
UX- en productiegates blijven open.
