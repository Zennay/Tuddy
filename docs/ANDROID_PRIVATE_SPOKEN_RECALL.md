# Android: privé antwoord inspreken bij tweetalig overhoren

## Eén bewuste lokale opname

In **Gesprekstools → Tweetalig terugluisteren → Zelf overhoren** kun je,
naast typen of het antwoord alleen bedenken, op **Spreek mijn antwoord in**
tikken. De app vraagt zo nodig de bestaande Android-microfoontoestemming;
de gebruiker tikt na de toestemmingsdialoog bewust opnieuw. Er wordt
nooit automatisch opgenomen na terugkeer van Android-instellingen.

Tuddy start uitsluitend `SpeechRecognizer.createOnDeviceSpeechRecognizer`
met de exact gekozen **antwoordtaal** en `EXTRA_PREFER_OFFLINE`.
Er is geen fallback naar `createSpeechRecognizer`, geen server-ASR,
modeldownload of automatisch taalherkennen. Als het toestel voor deze
taal geen bruikbare lokale herkenning biedt, kan de poging mislukken;
de schrijf- en denkoefening blijven gewoon mogelijk. Dit levert
**geen** nieuwe ondersteuning voor Kurmanji, Sorani of Pasjtoe.

De tijdelijke microfoonsessie wordt uiterlijk na **20 seconden** gestopt.
Alleen het definitieve, lengte- en Unicode-gecontroleerde resultaat
(maximaal 400 UTF-16-code-eenheden) verschijnt als **mijn gesproken
poging** in hetzelfde tijdelijke overhoorpaneel. Geen partiële tekst,
geen auto-score en geen automatische vergelijking met de bestaande
referentie. Die onthult de gebruiker nog altijd zelf.

## Android-lifecycle en privacy

- De gebruiker kan stoppen en de gesproken poging afzonderlijk wissen.
- Een nieuwe vraag, nieuwe taalrichting, oefenherstart, verlaten van de
  voorgrond, schermvergrendeling of verwijderen van het paneel wist
  de gesproken poging en beëindigt de recognizer.
- **Live ondertiteling en oefeninspreken sluiten elkaar uit.** De
  bestaande microfoon-Start/Stop ruimt de oefenmicrofoon op; het starten
  van tweetalig voorlezen, aanpassen van oefeninstellingen en lokale
  audio van een vraag of referentie doen hetzelfde.
- De resultaten worden nooit in Androids EditText/toetsenbord geplaatst,
  in een bestand of woordenlijst geschreven of door ML Kit vertaald.
  Een apart handmatig getypt antwoord blijft beschikbaar.
- Schermvergrendeling wordt extra door een tijdelijke ontvanger voor
  Androids systeem-`ACTION_SCREEN_OFF` bewaakt. Asynchrone resultaten
  na stop worden afgewezen met een sessiegeneratie en een
  per-turn terminale callbackpoort.
- Android en de op het toestel gekozen on-device herkenningsprovider
  blijven verantwoordelijk voor hun eigen gedrag. `isOnDeviceRecognitionAvailable`
  is geen onafhankelijk bewijs van netwerkisolatie, taalpakketten of
  inhoudelijke juistheid. Gebruik op een echt toestel vereist een
  afzonderlijke fysieke acceptatie.

Er zijn geen nieuwe permissies, netwerkopslagsystemen, cloudmodellen,
achtergrondservices of QA-regressiesuites toegevoegd. De bestaande
Android-build kan deze integratie compileren, maar de handmatige
Android 12+-vliegtuigmodusproef, taalmodelrechten/native-speakerreview
en getekende release blijven onafhankelijke open eisen.
