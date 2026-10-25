# Eén tik: tijdelijk gesprek afdekken

Tuddy verwerkt live spraak en lokaal vertaalde ondertitels in een tijdelijk
gesprek. Wanneer de telefoon bijvoorbeeld even in een andere hand ligt,
kunnen de oorspronkelijke zinnen of vertalingen op het scherm zichtbaar
zijn zonder dat iemand bewust het gesprek heeft gedeeld.

## Functie

- Gebruik in het gewone gesprek **🔒 Gesprek afdekken · stop microfoon en
  audio**, in de fullscreen leesmodus **🔒 Gesprek nu afdekken**, of in
  de tegenover-elkaar-weergave **🔒 Afdekken**.
- Tuddy beëindigt **vóór** de neutrale weergave de actieve on-device
  microfoonherkenning, geplande automatische spraakherstarts en lokale
  TTS-uitvoer. Vanuit de tegenover-elkaar-weergave wordt ook diens
  afzonderlijke offline TTS-adapter gestopt.
- De fullscreen dialoog toont geen ondertitels, talen, transcript-
  metadata, bestandsnamen, geschatte spreker of gebruikersidentiteit.
  De bestaande regels, handmatige correcties en concepten blijven alleen
  tijdelijk in RAM beschikbaar, totdat de gebruiker ze zelf wist of
  Android de Activity beëindigt.
- Een reguliere Android **Terug**-actie of tik buiten het scherm kan de
  privéweergave niet sluiten. Bewust herstellen gaat in twee stappen:
  **Gesprek weer tonen…**, vervolgens **Ja, toon het gesprek**.
  **Afgedekt houden** annuleert uitsluitend de onthulling.
- De microfoon blijft uit na heropenen. Een nieuwe luistersessie vraagt
  een expliciete tik op **Luister** en doorloopt dezelfde bestaande
  microfoonpermissie-, offline-model- en veiligheidscontroles.
- Het afdekvenster zelf start geen internetverzoek, speech service,
  transcriptopslag, audio of modeldownload en bewaart geen voorkeur
  of gesprekshistorie. Dit is geen apart archief.

## Grenzen

Dit is **schermbediening, niet versleuteling of authenticatie**.
Iedereen met toegang tot de reeds ontgrendelde telefoon kan de
tweestapsactie zelf uitvoeren. Androids notificaties, systeemschermen,
het klembord en inhoud die al naar een andere app is gedeeld, worden
niet afgedekt of op afstand ingetrokken. Het wegvallen van RAM na
procesbeëindiging is mogelijk; afdekken is geen automatische back-up.
Voor toestelbeveiliging hoort de eigenaar Android zelf te vergrendelen.

De instelling voor optionele Android-screenshotbeveiliging blijft
ongewijzigd. Een source merge of groene Android-build bewijst niet
dat een fysieke Android 12+ vliegtuigmodustest of release-admission
is voltooid. Geen afwijking van de aparte toestemming-, modelrechten-
of productiegates.
