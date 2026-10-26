# Vrijwillige woordvergelijking bij tweetalig overhoren

## Nieuwe app-core-functionaliteit

Tuddy laat je al kiezen uit een reeds vertaald taalpaar, de oefenzin
omgekeerd oefenen, een eigen antwoord **typen** of eenmaal **lokaal
inspreken**, de referentie **zelf onthullen** en vervolgens onafhankelijk
**Zelf goed** of **Nog oefenen** aangeven.

Na die bewuste onthulling kan de leerling nu apart kiezen voor
**Vergelijk getypte woorden** of **Vergelijk gesproken woorden**. De app
toont uitsluitend een eenvoudige tekstvergelijking van het geselecteerde
antwoord met de al bestaande referentiezin:

- woorden die in **dezelfde volgorde** letterlijk overeenkomen;
- woorden die alleen in de referentiezin terugkomen;
- woorden die alleen in de eigen poging terugkomen.

De vergelijking werkt met een begrensde *longest-common-subsequence*
op woorden, zodat herhaalde woorden niet als één woord geteld worden.
Hoofdletterverschillen worden genegeerd en Unicode wordt naar NFC
genormaliseerd; diakritische tekens en woordvolgorde worden niet
weggepoetst. Geen semantische AI, fonetische score, cloudverzoek,
grammaticacontrole of correctie van de door ML Kit geleverde referentie.
Een synoniem, dialectspelling of andere geldige woordvolgorde kan
volkomen terecht als verschil verschijnen. Bij lege, ongeldige, te lange
of overmatig complexe input blijft de vergelijking uit.

## Privacy en interactie

Vergelijken wordt **nooit automatisch** gestart bij typen, luisteren of
het tonen van de referentie; de leerling moet na het onthullen een
afzonderlijke knop indrukken. De gegenereerde vergelijking bestaat
uitsluitend als afgeleide van het op dat moment zichtbare tijdelijke
antwoord. Er is geen opslag, export, telemetry, klembordgebruik,
modeldownload of koppeling met het spraakmodel.

Wisselen van oefenrichting, een nieuwe of 'gemiste' ronde, zelf een
oordeel geven, antwoord wissen of aanpassen, een nieuwe gesproken
poging, het scherm verlaten en het toestel vergrendelen verbergen de
vergelijking. De bestaande handmatige keuze **Zelf goed** /
**Nog oefenen** blijft beslissend en de gespreksregels blijven
ongewijzigd. Getypte en gesproken pogingen worden niet samengevoegd:
de gebruiker kiest één bron om lokaal te vergelijken.

Deze broncodewijziging levert **geen** fysieke Android
vliegtuigmodusacceptatie, native-speakerbeoordeling of
productie-release op. Offline modelrechten voor Koerdisch en Pasjtoe
blijven onafhankelijke releasegates.
