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
