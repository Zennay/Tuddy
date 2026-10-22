# Lokale verbindingsstatus tijdens offline luisteren

In **Offline startcheck → Vliegtuigmodus voorbereiden** toont Tuddy een
momentopname van drie afzonderlijke Android-signalen:

- de systeeminstelling voor vliegtuigmodus;
- of de wifi-radio volgens Android aan staat;
- welke netwerktransporten Android momenteel meldt (wifi, mobiel,
  ethernet, VPN of bluetooth-netwerk).

**Ververs verbindingsstatus** vraagt dezelfde signalen opnieuw op.
Na een terugkeer uit Android-instellingen wordt het overzicht automatisch
vernieuwd. Tuddy schakelt geen radio aan of uit en leest geen SSID,
IP-adres, netwerknaam, gesprek of microfoondata.

De wifi-radio kan handmatig weer worden ingeschakeld **terwijl de
vliegtuigmodus nog aan staat**. De vliegtuigmodusindicator op zichzelf
is daarom geen bewijs voor een offline sessie. Ook een lege lijst met
netwerktransporten bewijst niet dat alle radios uit staan of dat andere
apps/Android-diensten geen verkeer verzenden. Onbekende of niet-leesbare
gegevens worden als onbekend getoond, nooit als veilig.

Het via de startcheck bewust gekopieerde of geëxporteerde
**apparaatrapport** neemt de wifi- en netwerktransportindicaties op
met de vaste regel `Connection snapshot independently proves isolation: no`.
Hiermee kan een gebruiker een eigen fysiek toestelsessie beter documenteren
zonder transcript of audio te exporteren. Het is **geen** automatisch
acceptatiebewijs; issue #1 vereist nog steeds menselijke handelingen,
radio-/netwerkcontrole en observatie op een geautoriseerd Android 12+-toestel.

Deze functionaliteit vereist alleen de Android normale permissies
`ACCESS_NETWORK_STATE` en `ACCESS_WIFI_STATE`. Tuddy vraagt geen
locatiemachtiging, leest geen wifi-netwerkidentiteit en bouwt geen
netwerkmonitor of automatische upload. Issue #2 (volledige offline
Kurmanji, Sorani en Pasjtoe) wordt hierdoor niet opgelost.

## Strikte vliegtuigmodus voor de microfoon (bewuste sessiekeuze)

Met **Strikte vliegtuigmodus voor luisteren inschakelen** kiest de gebruiker
alleen voor deze geopende app-sessie een extra fail-closed startregel.
Vóór het starten van de microfoon en vóór elke automatische herstart eist
Tuddy dan dat Android vliegtuigmodus meldt, de wifi-radio als uit rapporteert
en geen zichtbaar netwerktransport of onbekende transportstatus opgeeft.
Bij twijfel weigert de app de microfoon te starten, stopt een eerdere
herkenner en opent de offline startcheck met een concrete melding.

Het is uitdrukkelijk een **optionele** veiligheidsrem: offline
spraakherkenning werkt ook zonder die extra instelling. Er is geen
automatische aanpassing van vliegtuigmodus of wifi, en de keuze wordt
niet bewaard voor een volgende appstart. De schakelaar kan tijdens
luisteren niet worden gewijzigd. Zelfs met de schakelaar aan blijft
een menselijke toestelcontrole vereist: Android kan radios of andere
verkeerbronnen verbergen die niet via deze APIs zichtbaar zijn.

## Verbinding veranderd tijdens het luisteren

Wanneer de strikte sessiekeuze aan staat, controleert Tuddy tijdens
actief luisteren ongeveer elke 1,5 seconde dezelfde Android-signalen.
Als wifi terugkomt, een transport verschijnt of een indicator niet meer
leesbaar is, stopt Tuddy de herkenner en toont de offline startcheck.
Zonder de expliciete strikte sessiekeuze draait deze bewaking niet.
De app maakt hiervoor geen internetverzoek, scant geen netwerkidentiteit
en start geen achtergronddienst.

Het blijft een **best effort** Android-momentopname, geen firewall.
Tussen twee controles kan de systeemtoestand veranderen. Het afsluiten
van de app stopt ook de bewaking; de strikte keuze wordt niet opgeslagen.

## Status op het hoofdscherm

Het beginscherm vermeldt duidelijk of de strikte sessieregel aan of uit
staat, zodat een gebruiker na herstart niet stilzwijgend aanneemt dat de
vorige, vluchtige keuze nog geldt. Wijzigen gaat nog steeds alleen via
de Offline startcheck wanneer de microfoon uit staat.
