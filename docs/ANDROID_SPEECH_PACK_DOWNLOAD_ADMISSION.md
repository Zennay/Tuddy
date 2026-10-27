# Android-spraakpakketten: nooit een tweede onbevestigde aanvraag

## Probleem en productgedrag

Het offline spraakpakket voor een taal wordt uitsluitend **na een
expliciete gebruikersactie** aangevraagd bij de Android-systeemdienst.
Android mag die download laten doorlopen nadat Tuddy's startcheck wordt
gesloten of de tijdelijke SpeechRecognizer is vernietigd. Stoppen met
het volgen van die dienst is **geen bewijs** dat de modeldownload
daadwerkelijk is gestopt.

Daarom heeft Tuddy nu één tijdelijke, app-procesbrede
**toelatingsplaats voor Android-spraakpakketaanvragen**. Die wordt
door alle controllers gedeeld, zodat een andere dialoog of een nieuw
geopend scherm niet alsnog een tweede pakketdownload start.

1. De gebruiker kiest een ondersteunde spreektaal, controleert de lokale
   spraakpakketstatus en kiest bewust **Spraak via wifi**. De bestaande
   opslag-, gevalideerde **ongemeten wifi**- en API-toelating blijven
   verplicht.
2. Als er nog een onbevestigd Android-verzoek voor *welke* taal dan ook
   bekend is in dit app-proces, weigert Tuddy de nieuwe aanvraag en toont
   de taal die al wacht. De downloadknop in **Offline startcheck**
   wordt dan uitgeschakeld. Alleen een afzonderlijke **read-only
   pakketcontrole** blijft toegestaan.
3. Zodra een verzoek aan Android wordt aangeboden, houdt Tuddy de plaats
   bezet. Een verborgen scherm, een verloren callback, een oude
   SpeechRecognizer of een verlopen UI-status geeft deze plaats
   **niet** automatisch vrij.
4. Android 14+ kan afronding of een echte systeemfout melden. Een
   terminale melding geeft de toelatingsplaats vrij, ook als het
   scherm inmiddels gesloten is. De uitkomst **READY** wordt uitsluitend
   na een onafhankelijke controle van het exact geïnstalleerde
   Android-spraakpakket getoond.
5. Android 13 heeft geen betrouwbare downloadafronder voor deze route:
   een geaccepteerd verzoek blijft dus voor dit app-proces bezet
   totdat een later read-only onderzoek het **exacte** taalpakket als
   *geïnstalleerd* bevestigt of totdat het app-proces eindigt. Er wordt
   niet geraden op basis van een downloadpoging, datum, voortgang of
   het heropenen van een scherm.
6. Een Android 14+-melding **ingepland** is geen bewijs van fysieke
   voltooiing: ook dan wacht Tuddy op een betrouwbaar resultaat of een
   later exact geïnstalleerd pakket. Een mislukte preflight **vóór
   indiening** ruimt de plaats wel direct op.
7. Iedere indiening heeft een eigen interne aanvraagidentiteit. Late
   callbacks van een eerdere aanvraag kunnen een nieuwere aanvraag
   niet vrijgeven. Er wordt geen transcript, audio, IP-adres, SSID of
   modelbestand bewaard.

## Grenzen

De gedeelde plaats bestaat alleen in het lopende app-proces. Android
kan eerdere modeldownloads buiten het proces blijven beheren; na een
procesherstart moet de gebruiker opnieuw de **echte lokale
pakketstatus** opvragen. Dit is een fail-closed maatregel tegen
overlapping binnen Tuddy, **geen garantie** van Androids eigen
netwerk-/downloadplanning of werkelijke offline beschikbaarheid.

Dit werk is geen activatie van nog niet gelicentieerde Kurmanji,
Sorani of Pasjtoe-ASR/MT-modellen. Fysieke Android 12+-acceptatie,
echte radio-isolatie, NL↔EN-resultaten op het toestel, onafhankelijke
review en ondertekende productie-uitrol blijven afzonderlijke gates.
