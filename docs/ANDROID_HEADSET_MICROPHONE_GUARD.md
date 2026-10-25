# Android: headsetmicrofoon vereist bij offline spraak

## Wat verandert er?

Tuddy's live en eenmalige offline microfoonflows kunnen nu, **alleen na
expliciet inschakelen**, vereisen dat Android een aangesloten
**headsetmicrofoon-invoerapparaat** rapporteert. Dit werkt naast de al
bestaande microfoontoestemming, muting/oproepbewaking, strikte
vliegtuigmodus, herkenningsdeadline en energiebewaking.

Onder de geavanceerde Android-instellingen staat
**Microfoon · headsetinvoer verplicht stellen**. De keuze wordt
uitsluitend als één booleaanse instelling op dit toestel opgeslagen,
staat standaard uit en kan alleen gewijzigd worden wanneer er niet
wordt geluisterd.

## Volledige luisterstroom

1. Bij elke eerste start of automatische herkenner-herstart voert Tuddy
   de bestaande toestemming/offline-modelcontroles uit én, als de optie
   aan staat, een controle van Androids **input**-apparaatinventaris.
   Bedrade headsetmicrofoons, USB-headsets en Bluetooth SCO/BLE-headsets
   tellen mee. Een hoofdtelefoon zonder microfoon telt niet mee.
2. Zonder zo'n Android-gerapporteerde ingang, of wanneer de lijst niet
   veilig kan worden gelezen, weigert de app de luisterstart. Het
   bestaande gesprek wordt niet gewist. Er wordt geen headset of
   microfoon automatisch gekoppeld.
3. Na het maken van de on-device SpeechRecognizer start een
   sessiegebonden Android `AudioDeviceCallback`. Een extra controle
   sluit een race tijdens registratie. Iedere twee seconden volgt ook
   een read-only check, zodat niet uitsluitend op vendor-events wordt
   vertrouwd.
4. Verdwijnt de headsetmicrofoon uit de Android-inputinventaris of
   kan de status niet meer worden gelezen, dan stopt Tuddy de lopende
   recognizer, maakt oude callbacks en geplande herstarts ongeldig,
   toont het probleem en vereist een **nieuwe bewuste Luister-actie**.
5. Bij Stop, achtergrond, fout, volgende herkenningsronde of Activity-
   afsluiting worden alle tijdelijke watchers verwijderd. Alleen het
   **aantal onderbrekingen** staat in de bestaande RAM-diagnostiek,
   zonder toesteladressen, routes, gesprekken of tijdlijn.
6. Wil je zonder headset microfoongebruik toelaten, schakel de optie
   uit terwijl het luisteren gestopt is. De bestaande on-device
   spraak-/tekstmodel-gates blijven ongewijzigd.

## Grenzen van Android en fysieke acceptatie

**Dit is geen attestatie van de daadwerkelijk geselecteerde
microfoon.** Android kan wel een inputapparaat als beschikbaar
rapporteren, maar een toestel- of vendor-spraakherkenner kan intern
een andere route gebruiken. Sommige Bluetooth-headsets verschijnen
pas in de inputinventaris na een Android-profielwissel; dan weigert
de optionele strikte modus terecht de start. Systeemevents en
periodieke controles kunnen bovendien vertraging hebben. Tuddy
wijzigt geen audiobron van de herkenner en claimt geen hardware-isolatie.

De fysieke Android 12+-vliegtuigmodusacceptatie, daadwerkelijke
NL→EN/EN→NL herkennings- en vertaalkwaliteit, onafhankelijke review,
rechten voor volledig offline Kurmanji/Sorani/Pasjtoe en de
ondertekende release blijven aparte gates. Er wordt niets uitgerold,
gedownload of online omgezet door deze instelling.
