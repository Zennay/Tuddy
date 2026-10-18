# Offline startcheck: spraak en tekst onafhankelijk controleren

Vanaf Tuddy's hoofdscherm opent **Offline startcheck** een compacte begeleide
setup voor het gekozen bron- en doeltaalpaar. De gebruiker hoeft hiervoor
de uitgebreide instellingen niet eerst open te zoeken.

## Twee afzonderlijke apparaatcontroles

1. **Controleer spraak** vraagt alleen Androids bestaande
   `createOnDeviceSpeechRecognizer`-gebaseerde pakketcontrole. De app
   accepteert uitsluitend een daadwerkelijk als geïnstalleerd gemelde
   lokale spreektaal (met volledige taal-/regiocode). Op Android 12 kan
   de spraakpakketstatus niet per taal vastgesteld worden: Tuddy claimt
   dan **niet** dat de taal klaarstaat. Heeft het toestel alleen
   cloudspraak of geen offline pakket, dan is dat zichtbaar.
2. **Controleer tekst** vraagt aan ML Kit welke lokale taalmodellen
   daadwerkelijk geïnstalleerd zijn. Bij ontbrekende modellen blijft
   de status zichtbaar ongereed. Gelijke brontaal en doeltaal hebben
   geen afzonderlijke tekstvertaling nodig, maar vereisen nog steeds
   echte lokale spraakherkenning voor gesproken ondertitels.

**Controleer beide opnieuw** start beide read-only checks. Een ‘beide
aanwezig’-melding is uitsluitend mogelijk wanneer *beide* actuele,
bij het gekozen taalpaar passende controleresultaten gereed zijn.

## Gerichte voorbereiding

Via afzonderlijke, expliciete knoppen **Spraak via wifi** en
**Tekst via wifi** kan de gebruiker ontbrekende pakketten voorbereiden.
Een spraakdownloadknop verschijnt alleen als Android daarvoor een
downloadmogelijkheid meldt. De bestaande toestel-/wifi- en
toestemmingsbeveiliging blijft van toepassing. Er is geen automatische
modeldownload zodra het venster wordt geopend en nooit een cloudfallback
voor spraakherkenning.

**Wijzig talen of bekijk details** opent de bestaande instellingen;
**Of typ een zin zonder microfoon** opent de lokale ML Kit-tekstmodus.
De huidige gesprekshistorie wordt niet gewist. Achtergrond verlaten
sluit het venster en de bestaande microfoon-lifecycle blijft actief.

Een groen pakketoverzicht bewijst **niet** dat de vrije spraakherkenning of
vertaling in vliegtuigmodus op een fysiek toestel is gevalideerd. Vrije
Kurmanji-/Sorani-/Pasjtoe-ASR en MT en getekende APK-release blijven
onafhankelijk onbewezen. Geen QA-suites toegevoegd en geen
toestelacceptatie geclaimd.
