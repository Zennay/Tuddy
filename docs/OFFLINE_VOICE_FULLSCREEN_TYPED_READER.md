# Grote tweeweg-leesweergave: bewust lokaal voorlezen

De grote weergave **Gesprek groot lezen** kan naast tappen voor
eigen woordkaarten nu ook elke tijdelijke gesprekszin afzonderlijk
uitspreken, uitsluitend na een gebruikershandeling.

Per gespreksregel verschijnt:
- **🔊 Origineel**: probeert de oorspronkelijke handmatig ingevoerde
  zin voor te lezen in de werkelijke gekozen brontaal;
- **🔊 Vertaling**: alleen bij daadwerkelijk afgeronde lokale
  vertaling; nooit een onvoltooide, mislukte of ontbrekende modeluitvoer;
- **Rustiger**: dezelfde afgeronde vertaling met langzaam tempo.

Een aparte **■ Stop alle lokale voorlezing**-knop stopt direct de
huidige stem. Bij sluiten of verlaten van de grote leesweergave wordt
de eigen Android TTS-client ook opgeruimd. Er is nooit automatische
spraak, voice-modeldownload, provider-fallback naar cloud of het delen
van de tekst.

Alle knoppen gebruiken de bestaande `LocalCaptionSpeaker`: die
weigert te lange of onveilige tekst en accepteert uitsluitend een
door Android als reeds geïnstalleerd, niet-netwerkafhankelijk gemelde
stem die overeenkomt met de opgegeven taal. Als zo'n stem ontbreekt,
toont de app een statusmelding in plaats van over internet uit te
wijken. De werkelijk uitgesproken inhoud kan onnauwkeurig zijn.

Deze functie is onafhankelijk van een Android offline **spraak-naar-
tekst**-model. Ze bewijst geen Kurmanji/Sorani/Pasjtoe offline
spraakherkenning of machinevertaling. De aparte licentie-, fysieke
toestel- en ondertekende-productiereleasegates blijven open.
