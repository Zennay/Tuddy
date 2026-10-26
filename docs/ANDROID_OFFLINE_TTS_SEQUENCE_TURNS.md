# Offline voorleesreeks: per regel betrouwbare Android-voortgang

## Probleem en oplossing

Androids geïnstalleerde, niet-netwerkafhankelijke TextToSpeech-engine kan
`onStart`, `onRangeStart`, `onDone` en `onError` vertraagd of meermaals
terugsturen. Eén lange reeks is niet één uitspraak: de engine moet elke zin
apart voltooien voordat Tuddy de volgende start. Een late spraakfout voor
de *vorige* zin mocht de *volgende* zin niet afbreken.

Tuddy bewaakt nu voor elke door de gebruiker gestarte offline voorleesreeks:

- De **hele** Android-utterance-ID, inclusief generatie en regelnummer; een
  los volgnummer of willekeurige oude fout is geen bewijs van de actuele zin.
- **Precies één terminale voltooiing per regel**. Een tweede `onDone`
  kan niet opnieuw tellen, doorschakelen of extra audio starten.
- **Alleen fouten van de huidige zin** mogen de reeks stoppen. Een oude
  `onError` kan geen later segment, aangevangen door een geldig `onDone`,
  annuleren. Bij een echte fout stopt de reeks wel fail-closed.
- **Monotone woordposities** voor de bestaande visuele meeleesmarkering:
  lege, ongeldige, buiten-de-regel of naar achter springende posities
  veranderen de markering niet. Een eerste geldige `onRangeStart` mag
  ook de startmarkering aanzetten als Android `onStart` niet stuurt.
- Een regel wordt pas als **gestart** gemeld wanneer Android daarvoor
  het start- of woordbereik-event meldt, niet reeds op het moment dat
  Tuddy `TextToSpeech.speak` aanvraagt.
- Na iedere afgeronde regel wordt de bestaande watchdog vernieuwd,
  zonder audiogegevens op te slaan. Bij een ontbrekende terminale
  callback stopt de engine alsnog binnen de bestaande begrensde
  deadline en wordt de audiofocus vrijgegeven.

Dit werkt centraal voor de bestaande gebruiker-gestarte voorleesreeksen
in de offline studie- en ondertitelweergaven. Dezelfde adapter houdt de
bestaande lokale-stemcontrole, hoofdtelefoon-/audiofocusbescherming en
Android-Stop/achtergrondopruiming aan. De wijzigingen voegen geen
machtigingen, cloudspraak, modeldownloads of permanente transcriptopslag toe.

## Grenzen van de oplevering

Een succesvolle Android APK-build en CI-groene broncode leveren geen
waarneming op van een fysiek toestel. Werkelijke audioweergave,
woordmarkering, headsetwissel en vliegtuigmodus blijven afhankelijk van
een geautoriseerde Android 12+-toestelproef en onafhankelijke review.
Geen native-speaker- of modelrechten-acceptatie is hierdoor voltooid.
