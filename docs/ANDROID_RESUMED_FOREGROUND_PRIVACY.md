# Android-focusgrens voor lokale spraak en geselecteerde tekst

## Waarom alleen een zichtbare Activity niet voldoende is

Android kan Tuddy in split-screen of bij de overgang naar een andere app
**STARTED** laten terwijl die Activity niet meer de actieve **RESUMED**
Activity is. Zonder extra controle kan een lopende on-device
SpeechRecognizer op dat moment blijven luisteren, of kan een
toestemmingscallback een nieuwe herkenningsronde starten nadat iemand
zijn aandacht elders heeft.

## Gedeelde controle in de echte app

De bestaande `MicrophoneStartPolicy` eist nu expliciet
`Lifecycle.State.RESUMED` bij alle spraakstarts. Dit geldt ook voor
automatische herkenningsherstarts en terugkerende
`RECORD_AUDIO`-toestemmingscallbacks. De app zet geen Android-
toestemming zelfstandig aan en start **nooit** automatisch opnieuw
wanneer het scherm terugkomt.

In `MainActivity` stopt `ON_PAUSE` een actieve on-device microfoon,
annuleert de bestaande watchdogs en herstarttaken via `stop()`,
sluit lokaal voorlezen en gooit een onbevestigd **één-zinsvoorstel**
weg. De niet-bevestigde herkende tekst is nooit in de gespreksgeschiedenis
gepubliceerd. De gebruiker ziet dat Android de focus heeft verplaatst
en moet bewust opnieuw op **Luister** drukken na terugkeer.

Al bevestigde, uitsluitend tijdelijke gespreksregels worden **niet**
automatisch gewist door een focuswisseling. Daarmee blijft het gewone
gesprek behouden zonder dat er op de achtergrond een microfoon
actief is. De bestaande strengere `ON_STOP`-opruiming blijft actief
voor daadwerkelijk verlaten of achtergrond.

In het **Android 'Vertaal geselecteerde tekst'**-scherm worden privé-
tekst en vertaalresultaat al bij `ON_PAUSE` direct gewist, ook als
het scherm via split-screen nog zichtbaar is. Lopende ML Kit-taken
worden ongeldig, TTS stopt en het resultaat wordt niet automatisch
gedeeld of teruggestuurd naar de bronapp. Dit kan ook gebeuren bij
het openen van een Android-deelmenu: na terugkeer moet de gebruiker
privétekst bewust opnieuw selecteren.

## Geen toestemming voor productie of echte offline-acceptatie

Dit is broncode-integratie van Android Activity-lifecycle en on-device
spraak en geen gegarandeerde fysieke mic-/radio-isolatie. Besturing van
andere apps, systeemdiensten en Android-toegankelijkheidsfuncties ligt
buiten Tuddy. Test op een echt Android 12+-toestel ook met split-screen,
een binnenkomend telefoongesprek en een weggenomen appfocus of audio en
herkenning daadwerkelijk stoppen.

De aparte vereisten voor een exacte geïnstalleerde debug-APK SHA,
twee richtingen NL→EN/EN→NL, fysieke vliegtuigmodus, onafhankelijke
review en ondertekende productie-release blijven open. Er is geen
nieuwe test- of regressiesuite geschreven en er wordt geen spraak/
model/cloudfunctie geactiveerd.
