# Tweeweg-tekstgesprek: vaste taalrichtingen en lokale stem

Via **Tweeweg-tekstgesprek · offline** kan de gebruiker sinds PR #102
zelf zinnen typen, om-en-om de bron en doelrichting kiezen en na aparte
wifi-toestemming de vereiste ML Kit-tekstmodellen lokaal voorbereiden.

## Eigen favoriete tekstrichtingen

De nieuwe actie **☆ Bewaar tekstrichting** bewaart uitsluitend het
gekozen paar taal-ID's, zoals `nl → en`. Via **★ Favorieten** kunnen
tot zes aparte tekstrichtingen worden geopend of verwijderd.
Deze favorieten staan los van de bestaande spraakgerichte favorieten:
ook een ML Kit-teksttaal zonder Android-spraakpakket kan nu een favoriet
zijn. Opgeslagen codes worden bij het lezen opnieuw gecontroleerd tegen
de huidige 59 ML Kit-teksttalen. Een bewaarde taalrichting beweert
**niet** dat het bijbehorende offline model gedownload is.

Het activeren van een andere favoriete taalrichting wist een nog niet
verstuurde invoerzin om te voorkomen dat tekst voor taal A ongemerkt
als taal B wordt ingestuurd. Er wordt geen gesprekshistorie, audio,
vertaling, downloadstatus of microfooninstelling bewaard.

## Uitspraak op expliciete aanvraag

Bij een tijdelijke gespreksregel is nu **🔊 Origineel lokaal
voorlezen** beschikbaar. Als werkelijk een lokale vertaling klaar is,
kan die afzonderlijk met **🔊 Vertaling** of **Rustiger** worden
uitgesproken; **■ Stop lokale stem** stopt het geluid.

De bestaande `LocalCaptionSpeaker` weigert een netwerkstem,
probeert geen stemmen automatisch te downloaden en accepteert alleen
veilige korte tekst. Als Android geen geïnstalleerde, niet-netwerkstem
voor de taal meldt, krijgt de gebruiker een melding in plaats van
cloudfallback. Geluid stopt bij sluiten van de gespreksmodus en
bij een handmatige richtingwisseling.

Ook deze functie is uitsluitend een on-device gebruikershulpmiddel:
de echte uitspraak kan onnauwkeurig zijn, en automatische tekstvertaling
kan fouten bevatten. **Kurmanji, Sorani en Pasjtoe** krijgen hiermee
nog geen volledige offline speech-to-text of machinevertaling.
Fysieke toestelacceptatie, modellicenties/native-speakercontrole en
productierelease blijven aparte gates. De Android-bestandsopslag of
netwerkrechten worden voor dit onderdeel niet gewijzigd.
