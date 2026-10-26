# Privé luistervergelijking bij tweetalige overhoorrondes

## Wat er gebeurt

In **Zelf overhoren** blijft de bestaande lokale vertaaldialoog het
uitgangspunt. Je ziet eerst alleen de vraag en kiest zelf of je typt,
niets invult of bewust een kort antwoord inspreekt via Androids
on-device herkenning. **De referentie blijft verborgen** totdat je op
**Onthul referentie** tikt. Er is geen automatische beoordeling.

Wanneer de referentie eenmaal zichtbaar is en je een getypte of lokaal
herkende poging hebt, verschijnt ook **Luister naar mijn poging en de
referentie**. Kies eventueel welke poging je wilt beluisteren en
**Normaal** of **Langzaam**, en tik dan bewust op
**Speel poging → referentie lokaal af**. Tuddy speelt maximaal twee
korte regels, achter elkaar, met dezelfde als lokaal geïnstalleerd
aangemelde Android-TTS-stem in de antwoordtaal.

**Stop vergelijking** onderbreekt de hele reeks. Een gewijzigde
getypte poging, nieuw oefenkaartje, gewijzigde oefenrichting, Stop,
scherm verlaten of de Android-achtergrond beëindigt de oude audio
eveneens. Reeksen worden nooit automatisch herhaald of opgestart
bij het onthullen van de referentie. De geselecteerde poging,
tempo-keuze en meldingen zijn alleen RAM-/UI-status; er wordt geen
audio, antwoord of vergelijking op schijf gezet.

## Grenzen en veiligheid

Een eigen poging en de onthulde referentie moeten afzonderlijk binnen
dezelfde veilige Unicode- en lengtebeperking (maximaal 400
UTF-16-code-eenheden per uiting) vallen. Te lange, lege of ongeldige
tekst wordt geweigerd in plaats van afgekapt of online verwerkt.
De bestaande `LocalCaptionSpeaker` kiest enkel al geïnstalleerde,
als niet-netwerkafhankelijk gemelde Android-stemmen. Het bestaande
audiofocus-, headset- en afspeeltijdslimietbeleid blijft gelden.
De voorleesactie start geen SpeechRecognizer, tekstvertaling,
modeldownload of netwerkfallback.

De weergave **beoordeelt geen betekenis, grammatica, uitspraak of
vertaalkwaliteit**. Androids TTS-stemmetadata zijn ook geen
onafhankelijk bewijs van volledige vliegtuigmodusisolatie.
De openstaande fysieke Android 12+-acceptatie, toegestane modellen
voor Kurmanji/Sorani/Pasjtoe, native-speaker-review en getekende
productierelease blijven afzonderlijke gates.
