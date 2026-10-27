# Interne opslag en nieuwe offline modeldownloads

Tuddy gebruikt geïnstalleerde Android-spraakpakketten en lokale ML Kit-
tekstmodellen. Een opt-in download kan op een toestel met bijna volle
interne opslag mislukken, andere apps hinderen of een onduidelijke
modelstatus opleveren.

## Één controle voor de werkende Android-downloadroutes

De nieuwe `LocalModelStorageAdmission` controleert bij **iedere
nieuwe, bewust aangevraagde download** de beschikbare bytes op Androids
interne `/data`-partitie met `StatFs`. Dit geldt voor:

- een offline tekstmodel voor de huidige taalrichting;
- een tekstmodel dat vanuit een mislukte regel bewust wordt voorbereid;
- tekstmodellen uit de opgeslagen favoriete getypte taalparen (ook
  iedere volgende aanvraag in een reeks);
- een door de gebruiker via Android aangevraagd offline spraakpakket.

Het vrije-ruimtebuffer is **512 MiB**. Bij minder beschikbare ruimte,
of als Android de beschikbare opslag niet betrouwbaar kan rapporteren,
weigert Tuddy *uitsluitend een nieuwe downloadaanvraag* en geeft een
menselijke herstelmelding. Het probeert geen opslag vrij te maken,
wist geen andere modellen, downloadt niets elders en schakelt niet naar
een cloudspraakdienst.

In **Offline startcheck** is de momentane opslagstatus zichtbaar. Met
**Controleer interne opslag opnieuw** kun je Android opnieuw laten
meten; bij terugkomst uit instellingen ververst deze informatie. De
downloadknoppen zijn voorlopig uitgeschakeld als die controle niet
toelaatbaar is. Omdat de vrije ruimte daarna nog kan veranderen,
bevestigt de app dezelfde conditie opnieuw vlak vóór de werkelijke
aanvraag. Een toegestane aanvraag behoudt daarnaast alle bestaande
Wi-Fi-, expliciete bevestigings-, on-device- en modelrechtencontroles.

## Kostenvriendelijke netwerktoelating op Android

Naast de opslagcontrole is er nu één `LocalModelNetworkAdmission`
voor alle bewust gestarte downloads van offline ML Kit-tekstmodellen
en Android-spraakpakketten. Vlak vóór een nieuwe aanvraag leest Tuddy
uitsluitend de actieve Android-netwerkcapaciteiten:

- actieve **wifi** moet daadwerkelijk als wifi gerapporteerd worden;
- het netwerk moet **INTERNET** en **VALIDATED** hebben;
- het netwerk moet door Android als **NOT_METERED** (ongemeten)
  zijn gemarkeerd.

Onbekende status of een ontbrekende capaciteit betekent **niet
starten**. Hiermee vermijdt Tuddy het bewust starten van grote
modeldownloads op een door Android als beperkt of betaald gemeld
wifi-netwerk, waaronder veel telefoonhotspots. Een wifi-verbinding
is niet automatisch onbeperkt: `requireWifi()` in de bestaande
ML Kit-downloadvoorwaarden blijft onveranderd van kracht.

In **Offline startcheck** staan naast de vrije opslagruimte nu ook
de wifi-toelating, een knop om de toestand opnieuw te lezen en een
expliciete knop om de **Android-wifiinstellingen** te openen. Tuddy
schakelt de verbinding niet zelf om. Na terugkomst in de app wordt de
read-only status bijgewerkt; opnieuw downloaden gebeurt uitsluitend
na een nieuwe bewuste keuze. Een slechte status maakt de twee
modeldownloadknoppen onbeschikbaar. De aanvraagmethodes lezen de
status zelfstandig opnieuw, zodat downloads via tweeweg-tekst,
favorietenreeksen of het gerichte herstel van een mislukte zin niet
langs de schermcontrole heen kunnen starten.

**Grenzen:** Android bepaalt zelf of een verbinding als ongemeten
wordt geclassificeerd; een mobiele hotspot kan verkeerd zijn
geconfigureerd. De netwerkstatus kan veranderen *na* de aanvraag.
Tuddy kan eenmaal door ML Kit of Android geaccepteerde downloads
niet betrouwbaar intrekken. Dit is een conservatieve startcontrole,
geen belofte van nul dataverbruik, geen captive-portal-bypass en geen
onafhankelijke offline-acceptatie. Bestaande geïnstalleerde modellen,
read-only inventaris, herkenning en vertaling blijven bruikbaar als
wifi niet toegelaten wordt. Er worden geen SSID, IP-adressen,
verbruikscijfers, teksten of identiteiten opgeslagen.

## Wat het wel en niet garandeert

- Bestaande geïnstalleerde modellen, offline vertalingen, spraakherkenning
  en **read-only** modelinventaris blijven bruikbaar bij weinig ruimte.
- De buffer is **geen voorspelling van de uiteindelijke modelgrootte**,
  en StatFs reserveert geen ruimte. Androids modelleverancier kan de
  download alsnog weigeren, uitstellen of op andere opslaglocaties werken.
- Tuddy bewaart geen opslaghistorie, bestandsnamen, audio, transcript
  of ruimte-metingen buiten het tijdelijke actieve scherm.
- Een modeldownload wordt nooit het bewijs voor een volledig offline
  werkende spraak- én tekstvertaling. De fysieke Android 12+
  vliegtuigmodusproeven, exacte geïnstalleerde APK-checksum, rechten en
  native-speakercontrole voor Kurmanji/Sorani/Pasjtoe en ondertekende
  productie-release blijven aparte acceptatiegates.

Gebruik de Android-opslaginstellingen om zelf veilig ruimte vrij te
maken. Tuddy verwijdert geen gegevens automatisch.
