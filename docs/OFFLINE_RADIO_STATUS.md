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
