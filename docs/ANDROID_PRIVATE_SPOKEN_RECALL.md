# Android: privé antwoord inspreken bij tweetalig overhoren

## Eén bewuste lokale opname

In **Gesprekstools → Tweetalig terugluisteren → Zelf overhoren** kun je,
naast typen of het antwoord alleen bedenken, op **Spreek mijn antwoord in**
tikken. De app vraagt zo nodig de bestaande Android-microfoontoestemming;
de gebruiker tikt na de toestemmingsdialoog bewust opnieuw. Er wordt
nooit automatisch opgenomen na terugkeer van Android-instellingen.

### Pakketcontrole vóór de oefenmicrofoon

Op **Android 13+** controleert Tuddy bij een expliciete tik eerst met
Androids `checkRecognitionSupport` of voor de **antwoordtaal** een
offline spraakpakket is geïnstalleerd. Dit is een **read-only** vraag:
geen microfoon, herkenningsresultaat, modeldownload of cloudspraak.
De vraag heeft een grens van **12 seconden**. Tijdens de controle kan
de gebruiker via **Stop controle/opname** annuleren, waarna zowel de
controle als latere callbacks hun geldigheid verliezen.

De oefening gebruikt taalcodes als `nl` en `en`, terwijl Android
geïnstalleerde regionale varianten als `nl-NL` en `en-US` kan melden.
Alleen **voor deze vrijwillige eenmalige oefening** mag een werkelijk
geïnstalleerde regionale variant van een kale taalcode tellen als
*voorzichtig beschikbare lokale spraak*. Dit is geen bewijs dat de
juiste dialectvariant voor de gevraagde zin beschikbaar is. De
bestaande controle van exacte taal-/regiotags voor live spraak blijft
ongewijzigd.

**De opname gebruikt nu de exact bevestigde Android-spraakvariant.**
Als de oefening bijvoorbeeld om `nl` vraagt maar Android alleen
`nl-NL` als daadwerkelijk geïnstalleerd meldt, verstuurt Tuddy
`nl-NL` als microfoontaalcode, niet stilzwijgend de onbevestigde
kale `nl`-code. Er wordt geen regio verzonnen uit een ML Kit-tekstcode.

Meldt Android **meerdere geïnstalleerde regionale spraakpakketten**
voor die antwoordtaal, dan stopt Tuddy de pakketcontrole **zonder de
microfoon te openen**. Het oefenpaneel toont uitsluitend de door
Android als geïnstalleerd gemelde codes. Kies bewust de gewenste
regio en tik **opnieuw** op *Spreek mijn antwoord in*. Tuddy
controleert bij die tweede tik opnieuw dat juist dat exacte pakket
werkelijk is geïnstalleerd; een inmiddels ontbrekende variant start
geen opname. De keuze bestaat alleen in deze tijdelijke oefenrichting,
niet als account- of toestelvoorkeur.

Bij één geïnstalleerde variant kan Tuddy die exact bevestigde regio
direct gebruiken. Bij onbekende pakketstatus blijft uitsluitend de
on-device-only poging over, uitdrukkelijk **zonder** bewering dat de
spraaktaal geïnstalleerd is. Een regio kiezen is geen native-speaker-
review of garantie op nauwkeurigheid. Andere live transcript- en
ML Kit-teksttaalinstellingen worden hiermee niet veranderd.

Meldt Android een pakket als ontbrekend, in behandeling, alleen online
of niet ondersteund, dan opent Tuddy **geen microfoon**. Het paneel
verwijst naar de bewuste pakketvoorbereiding via **Offline startcheck**
of het getypte antwoord. Bij een onleesbare status blijft een strikt
`createOnDeviceSpeechRecognizer`-verzoek mogelijk, met expliciete
waarschuwing dat de herkenning alsnog kan mislukken. Er is nooit een
fallback naar een netwerkherkenner.

**Android 12** heeft geen per-taal-pakketinventaris; Tuddy houdt
daarom de bestaande on-device-only opname met de expliciete beperking
dat lokale ondersteuning voor de gekozen taal niet vooraf kan worden
bevestigd.

Na een geslaagde controle blijven de bestaande toestel-, microfoon-,
headset-, stroom-, schermvergrendeling- en optionele vliegtuigmodus-
beschermingen gelden, en worden ze **opnieuw gecontroleerd** vóór
Androids microfoon werkelijk start. Ook een terugkeer naar de
achtergrond maakt een lopende controle ongeldig.

Tuddy start uitsluitend `SpeechRecognizer.createOnDeviceSpeechRecognizer`
met de gekozen **antwoordtaal** en `EXTRA_PREFER_OFFLINE`.
Er is geen fallback naar `createSpeechRecognizer`, geen server-ASR,
modeldownload of automatisch taalherkennen. Als het toestel voor deze
taal geen bruikbare lokale herkenning biedt, kan de poging alsnog
mislukken; de schrijf- en denkoefening blijven beschikbaar. Dit levert
**geen** nieuwe ondersteuning voor Kurmanji, Sorani of Pasjtoe.

De tijdelijke microfoonsessie wordt uiterlijk na **20 seconden** gestopt.
Alleen het definitieve, lengte- en Unicode-gecontroleerde resultaat
(maximaal 400 UTF-16-code-eenheden) verschijnt als **mijn gesproken
poging** in hetzelfde tijdelijke overhoorpaneel. Geen partiële tekst,
geen auto-score en geen automatische vergelijking met de bestaande
referentie. Die onthult de gebruiker nog altijd zelf.

## Kies zelf uit lokale Android-herkenningsalternatieven

Soms retourneert de on-device Android-spraakherkenner meerdere
plausibele transcripties voor dezelfde opgenomen zin. Vroeger toonde
Tuddy alleen het eerste resultaat; een verkeerd herkend woord was
daardoor lastig te herstellen zonder nogmaals in te spreken.

Na één bewuste opname inspecteert Tuddy nu **hoogstens acht** door
Android teruggegeven alternatieven, valideert ze met dezelfde strenge
Unicode-/tekstgrenzen en laat **maximaal vier unieke** transcripties
zien. Overlange, ongeldige en identieke alternatieven vallen weg.
Er wordt **niets afgekapt** of automatisch naar een andere taal
omgezet. De eerste bruikbare transcriptie is aanvankelijk geselecteerd,
maar je kunt **vóór Onthul referentie** bewust een andere kiezen die
beter overeenkomt met wat je werkelijk hebt uitgesproken.

Er is geen automatische spelling-, betekenis- of uitspraakbeoordeling,
geen geschatte confidence-score en geen tweede microfoonopname. De
oorspronkelijke vraag en referentie blijven verborgen totdat je zelf
op **Onthul referentie** tikt. Na onthullen blijft de gekozen transcriptie
staan en wordt zij gebruikt voor de bestaande optionele woord- en
audiovergelijking. Een andere herkenningsoptie kiezen **na** onthullen
kan niet, zodat een poging niet achteraf stilzwijgend herschreven wordt.

Alle opties zijn **tijdelijk in RAM** en worden bij wissen, nieuwe
opname, volgende/vorige oefenzin, oefenherstart, richtingwissel,
achtergrond/stop, of verwijderen van het oefenscherm samen gewist.
Er worden geen ruwe confidence-waarden, opnamebestanden,
modeldownloads, toetsenbordinvoer, analytics of externe uploads
aangemaakt. Android kan ook maar één of helemaal geen bruikbare
transcriptie teruggeven; in dat geval verschijnen geen keuzeknoppen.

Deze interface bewijst niet dat Android werkelijk offline was, dat
de transcriptie juist is of dat de taal/dialect door een mens is
gevalideerd. De fysieke Android 12+-vliegtuigmodusproef blijft
afzonderlijk verplicht.

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
