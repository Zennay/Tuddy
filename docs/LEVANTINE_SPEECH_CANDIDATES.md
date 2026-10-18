# Syrisch en Palestijns Arabisch: aparte Android-spraakkandidaten

Tuddy toont nu drie aparte keuzes voor gesproken Arabisch:
**algemeen (ar-SA)**, **Syrië (ar-SY)** en **Palestina (ar-PS)**.
De laatste twee zijn uitsluitend historische **locale hints**
uit de echte LiveLingo-v0.4-bron
(`model/Languages.kt`, `voiceHints`), niet nieuw geleverde
spraakherkenningsmodellen.

Wanneer je een regiocode kiest, stopt de microfoon volgens de
bestaande lifecycle-regels en wordt de actuele on-device pakketstatus
ingetrokken. **Controleer spraak** vraagt Android naar een exact
geïnstalleerd `ar-SY`- of `ar-PS`-pakket, zonder een generiek
`ar` of `ar-SA` stilzwijgend als bewijs te accepteren.
Android 12 kan zo'n pakket niet betrouwbaar per regio inventariseren;
de status blijft dan onbekend. Ontbreekt een lokaal pakket, dan
blijft een cloudherkenner verboden.

Het tekstmodel blijft **ML Kit Arabisch (`ar`)**, één van de
bestaande 59 tekstdoelen. Dit model bewijst **geen dialectgetrouwe
Levantijnse vertaling** of goede herkenning van Syrisch of Palestijns
Arabisch. De mogelijkheid is een duidelijk aangeduide kandidatenlijst,
geen verklaring dat die taalvarianten op iedere telefoon werken.
De regionale voorkeur kan als taalpaarfavoriet worden opgeslagen
zonder enige uitspraak, transcript of modelbewijs te bewaren.

Automatische richtingomkering naar generiek Arabisch kan een andere
Arabische regio kiezen: controleer de bronregionale keuze voor elk
nieuw gesprek expliciet.

Er worden geen extra machtigingen of cloud-API's toegevoegd,
en geen ongetoetste dialectmodellen gedownload. Fysieke toestelproeven
en onafhankelijke moedertaalsprekercontrole blijven noodzakelijk,
vooral voor juridische, medische of asielzaken. Geen getekende
release geclaimd.
