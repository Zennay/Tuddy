# Strikte vliegtuigmodus: bewaking tijdens de microfoonsessie

## Wat Tuddy nu doet

De optionele schakelaar **Strikte vliegtuigmodus voor luisteren** bestond
al in **Offline startcheck**. Eerder weigerde de Android-microfoon een
nieuwe herkenningsronde als vliegtuigmodus niet door Android bevestigd
was, wifi aan stond, of een netwerktransport zichtbaar of onleesbaar was.

Deze veilige startcontrole blijft bestaan, maar na een succesvolle
start bewaakt Tuddy nu **de hele actieve herkenningssessie**. Dat is
nodig omdat wifi, vliegtuigmodus of een verbinding ook **tijdens** een
uitspraak kunnen veranderen.

1. Vóór het starten controleert Tuddy de huidige vliegtuigmodus,
   Wi-Fi-status en Androids zichtbare netwerktransporten. Onzeker
   betekent **niet starten**.
2. Na het aanmaken van de on-device herkenner registreert de app
   tijdelijke listeners voor Android-vliegtuigmodus, Wi-Fi- en
   netwerkgebeurtenissen én een **periodieke hercontrole iedere
   3 seconden**. Deze onafhankelijke hercontrole detecteert ook
   veranderingen waarvoor Android geen callback afleverde.
   Kunnen de listeners of de periodieke controle niet veilig worden
   geregistreerd, dan stopt de app de herkenner **zonder te luisteren**.
   Na registratie controleert de app dezelfde status opnieuw om een
   verandering tijdens registratie niet te missen.
3. Bij een door Android gemelde gebeurtenis of periodieke controle
   leest de app de *actuele* toestand opnieuw. De inhoud van een
   broadcast wordt nooit vertrouwd.
   Als vliegtuigmodus uit staat, wifi aan staat, Android geen geldige
   netwerkstatus kan geven of enig netwerktransport zichtbaar is,
   dan sluit Tuddy direct de herkenner. Ook de volgende ingeplande
   herkenningsronde wordt geannuleerd, zodat geen automatische
   herstart op een gewijzigde verbinding volgt.
4. De gebruiker ziet of een veranderde/ongekende radio- of
   netwerkstatus dan wel **uitgevallen periodieke bewaking** de
   sessie onderbrak, en krijgt weer de **Offline startcheck**. De controle doet geen modeldownloads,
   start niet vanzelf opnieuw en zet geen telefooninstellingen om.
5. **Stop**, de achtergrond-lifecycle en een gewone herkenningsfout
   sluiten alle tijdelijke Android-listeners en annuleren de periodieke
   controle. Iedere nieuwe herkenningsronde begint met een eigen
   monitoringversie: callbacks van vorige rondes mogen een nieuwe
   sessie niet veranderen. Elke nieuwe ronde controleert de toestand opnieuw. De app bewaart geen SSID, IP,
   audio, herkenningszin, verbindingslog of tijdlijn.
6. In de bestaande sessiediagnostiek wordt uitsluitend het aantal
   door de bewaking afgebroken strikte sessies bijgehouden, tijdelijk
   in RAM. Na **Wis sessietellers** is die telling weg.

## Verwachte praktijk op een echt toestel

In **Offline startcheck** kan de gebruiker strikte vliegtuigmodus
inschakelen en de Android-vliegtuigmodus- en wifi-instellingen openen.
Die twee systeeminstellingen worden **niet** door Tuddy aangepast.
Dezelfde gebruiker start daarna bewust een NL→EN- of EN→NL-
spraakherkenningssessie. Als wifi tijdens een actieve sessie wordt
ingeschakeld of een netwerkverbinding beschikbaar wordt, moet de
microfoon stoppen en een nieuwe startcheck vragen. De bestaande
acceptatierapportage blijft handmatig en vereist echte fysieke
toestelwaarnemingen.

De bewaking is **geen onafhankelijke garantie van volledige
radiostilte**: Android kan netwerk-/radio-events vertraagd of
onvolledig melden, en de aanvullende controle kan maximaal ongeveer
3 seconden (plus Android-planningvertraging) achterlopen. Niet alle
radiofrequenties worden geobserveerd en dit bewijst geen nul-netwerkverkeer.
Een fysieke Android 12+-proef, bevestiging van de daadwerkelijk
geïnstalleerde APK, correcte herkennings-/vertaalresultaten in beide
richtingen, review door een tweede persoon en aparte modelrechten-
en release-goedkeuring blijven vereist.

Dit is uitsluitend device-/app-integratiewerk, geen toestemming voor
een uitrol en geen wijziging in de offline modelondersteuning.
