# Automatisch privégesprek afdekken na inactiviteit

## Wat het doet

Tuddy bewaart het actieve gesprek uitsluitend in de tijdelijke RAM-sessie.
Met **Privacy → Gesprek automatisch afdekken bij inactiviteit** kun je
expliciet kiezen voor **Uit** (standaard), **5**, **15** of **30 minuten**.

Wanneer Tuddy op de voorgrond staat en Android gedurende die periode
geen nieuwe aanraking of toetsbediening aan de Activity meldt, gebruikt
de app exact dezelfde afsluitroute als **Gesprek afdekken**:

- een actieve Android-on-device microfoon, eventuele automatische
  herkenningsherstarts en microfoontimers stoppen;
- lokale TextToSpeech-uitvoer en de tijdelijke audiofocus stoppen;
- de live/fullscreen leesweergave verdwijnt achter een neutrale
  privacyafdekking;
- de bestaande regels en correcties blijven alleen in de actieve
  RAM-sessie; er wordt niets automatisch gewist, gekopieerd of gedeeld.

Om de inhoud weer te tonen is een **bewuste tik op Toon gesprek** nodig.
De microfoon start daarna nooit vanzelf opnieuw.

## Tijd en Android-lifecycle

- Elke echte aanraking of toetsbediening die Android aan
  `MainActivity.onUserInteraction` rapporteert, reset de termijn.
- Bij `onPause`, bijvoorbeeld het openen van Android-instellingen
  of een ander scherm, wordt de actieve timer verwijderd. Terugkeer op
  de voorgrond begint een **nieuwe volledige** termijn, geen oude
  wachtende callback.
- De afdekking annuleert de teller totdat de gebruiker zelf weer
  het gesprek onthult. Zonder ingerichte callback wordt geen timer
  ingepland.
- Na Activity-vervanging wordt uitsluitend de termijnvoorkeur herlezen.
  Geen sessiegeschiedenis of deadline wordt opgeslagen.
- Uitschakelen van de keuze verwijdert de timer meteen. Ook een
  mislukte keuze wordt niet als opgeslagen getoond.

De keuze is alleen een extra **best-effort** bescherming tegen een
onbeheerd zichtbaar toestel. Android-interacties zijn geen
onafhankelijke aanwezigheidssensor. De OS kan callbacks/timers
vertraging geven; een app buiten de voorgrond wordt niet door
deze functie gecontroleerd. Een schermafdrukbeperking via
Android `FLAG_SECURE` is een **aparte expliciete instelling**,
en biometrische vergrendeling wordt hier niet geclaimd.

## Release en acceptatie

De hostbuild en de code-integratie bewijzen geen werkelijke
Android 12+ toestelacceptatie, radiostilte, perfecte ondertitelings-
of vertaalnauwkeurigheid. De fysieke NL→EN/EN→NL-proeven,
rechtencontrole voor Koerdisch/Pasjtoe ASR+MT, onafhankelijke
review en ondertekende productierelease blijven afzonderlijk open.
Geen modellen worden gedownload, geen nieuwe rechten gevraagd
en er is geen netwerkfallback toegevoegd.
