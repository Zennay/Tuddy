# Geselecteerde tekst direct lokaal vertalen

Selecteer een passage in een Android-app en kies in het tekstselectiemenu
**Vertaal met Tuddy**. Gewone platte tekst opent een aparte, tijdelijke
vertaalweergave. De geselecteerde tekst wordt niet automatisch vertaald:
controleer eerst de bron- en doeltaal (of vraag een optioneel lokaal brontaalvoorstel) en tik daarna op **Vertaal lokaal**.

## Werking

- **Stel brontaal lokaal voor** gebruikt de reeds ingebouwde on-device taalidentificatie op een korte selectie. Het voorstel wordt nooit automatisch toegepast zonder dat de gebruiker het ziet; controleer het zelf en kies desnoods handmatig.
- Tuddy controleert vóór vertaling of de exacte ML Kit-tekstmodellen al lokaal
  beschikbaar zijn. Engels gebruikt het ingebouwde ML Kit-model; voor andere
  talen moet het bijbehorende model al op het toestel staan.
- De selectieactie downloadt geen modellen, start geen microfoon en gebruikt
  geen online vertaalfallback. Ontbreekt een model, open dan Tuddy en ga naar
  **Offline startcheck → Tekst via wifi**. De geselecteerde tekst wordt bij het
  verlaten van de snelle vertaalweergave gewist; selecteer de passage daarna
  opnieuw.
- Na een geslaagde vertaling kun je de tekst kopiëren. Alleen als de bronapp
  aangeeft dat de selectie bewerkbaar is, verschijnt **Vervang geselecteerde
  tekst in de andere app**. Dat is een afzonderlijke bevestigde handeling; tot
  dan wordt geen resultaat teruggestuurd.
- Als Android de selectie als alleen-lezen markeert, blijft kopiëren mogelijk
  maar kan Tuddy de oorspronkelijke tekst niet vervangen.
- Bekende Tuddy-back-ups, correctie-CSV/JSON en complete ondertitelblokken
  blijven via de bestaande strikte import-/ondertitelpreview lopen; ze worden
  niet als een gewone zin vertaald.

## Privéweergave: geselecteerde tekst niet in schermafbeeldingen

De bestaande instelling **Screenshots en schermopname blokkeren** is nu
gedeeld tussen het gewone Tuddy-gesprek en het snelle Android-scherm
**Vertaal geselecteerde tekst**. In beide weergaven kun je de instelling
zelf wijzigen. De keuze wordt alleen als booleaanse appvoorkeur lokaal
bewaard: er wordt geen geselecteerde tekst, vertaling of screenshot
opgeslagen.

Als de instelling aan staat, past Tuddy `FLAG_SECURE` toe op **beide**
Android-vensters. Android probeert dan screenshots, schermopnames,
casting- en recente-appsvoorvertoningen van Tuddy-inhoud te verhinderen.
De geselecteerde-tekstweergave herstelt de instelling *voordat* het
privéselectieresultaat op het scherm verschijnt, ook als de MainActivity
de tekst rechtstreeks heeft doorgestuurd.

Een wijziging in de snelle vertaler geldt ook in de gewone app zodra
je daar terugkomt; de eerste hervatte weergave herstelt de huidige
venstervlag. Hierdoor blijft de privacyinstelling ook na Android-
activiteitswisselingen synchroon zonder het tijdelijke gesprek te
wissen of de microfoon automatisch te activeren.

Deze beveiliging is **optioneel** en standaard uit om handmatige
screenshots en schermdelen beschikbaar te houden wanneer je dat zelf
wilt. Je kunt in de snelle vertaler alsnog aan- of uitzetten,
ook na een geslaagde vertaling; een melding daarvoor verandert nooit
het vertaalresultaat. `FLAG_SECURE` is geen versleuteling of
uitzendbeveiliging: een externe camera, reeds elders gekopieerde
tekst of de ontvangende app vallen hier niet onder.

## Verwerking annuleren en opnieuw proberen

Een Android-dienst kan uitzonderlijk geen voltooiingscallback teruggeven.
Daarom heeft **Stel brontaal lokaal voor** een grens van **12 seconden**
en **Vertaal lokaal** een grens van **30 seconden**, inclusief de
controle van al aanwezige tekstmodellen en de daadwerkelijke lokale
ML Kit-vertaalaanvraag. Als het apparaat niet reageert, verdwijnt de
voortgangsindicator en verschijnt een begrijpelijke fout zonder
een verzonnen vertaling.

Tijdens beide acties staat nu **Annuleer lokale verwerking** onder de
voortgangsindicator. Annuleren maakt de bestaande detector- en
vertaalcallbackrevisies ongeldig, stopt de actieve lokale clients en
ruimt de Android-timers op. Het resultaat is dan **niet** teruggezet
in de oorspronkelijke app, op het klembord geplaatst of gedeeld.
De oorspronkelijke geselecteerde tekst blijft alleen in de open
tijdelijke weergave staan, zodat je bewust van taal kunt wisselen
of op **Vertaal lokaal** kunt tikken om opnieuw te proberen.

Een callback die ná annulering, timeout, taalwisseling of verlaten
van de Activity binnenkomt mag nooit alsnog een vertaling tonen.
De tijdslimiet stopt de **app-interactie**; hij bewijst niet dat
de onderliggende Android/ML Kit-provider elk intern werk meteen
heeft gestopt. Het scherm downloadt nooit automatisch modellen
en gebruikt geen online fallback.

## Mislukte lange selectie hervatten zonder eerdere delen opnieuw te vertalen

Bij een lange selectie werkt Tuddy lokaal in opeenvolgende
tekstgedeelten. Na een **mislukte of vastgelopen poging** verschijnt
**Herstel vanaf gedeelte X van Y** wanneer één of meer voorafgaande
gedeelten wél lokaal voltooid zijn. Dit is een bewuste vervolgactie,
geen automatische herstart of modeldownload.

Tuddy houdt die afgeronde delen tijdelijk **alleen in Activity-RAM**
en toont, kopieert, deelt of retourneert ze **niet afzonderlijk**. Bij
herstellen controleert Tuddy **opnieuw** de geïnstalleerde offline
ML Kit-modellen, vergelijkt een vingerafdruk van exact dezelfde
oorspronkelijke selectie en de gekozen bron- en doeltaal, en hervat
dan uitsluitend het eerste mislukte gedeelte en de resterende delen.
De volledige vertaling komt pas beschikbaar wanneer **alle** delen
geslaagd zijn. Het model wordt niet online aangevuld en Tuddy doet
geen verborgen tweede poging.

De tijdelijke gedeeltelijke inhoud wordt vernietigd wanneer je
**Annuleer**, **Vertaal lokaal** (volledig opnieuw), **Wissel talen**,
een ander taalpaar, afsluiten, een schermvergrendeling of een wissel
naar een andere app kiest. Een late callback van een vorige poging
kan geen deel toevoegen: iedere nieuwe poging heeft een eigen
Activity-generatie en de al bestaande ML Kit-werkcapaciteitsgrens.
De 30-secondenlimiet geldt ook voor een herstelpoging; er is geen
belofte dat Androids onderliggende native taak meteen stopt.

## Brontekst en vertaling lokaal laten voorlezen

Vanuit hetzelfde snelle Android-selectiescherm kun je **🔊 Lokaal voorlezen · kies tempo** openen. Kies **Origineel · normaal/langzaam** voordat of nadat je vertaalt; na een echt voltooide lokale vertaling kun je ook **Vertaling · normaal/langzaam** kiezen. **■ Stop voorlezen** stopt de Android-stem onmiddellijk. Vertalen, een nieuwe taalrichting kiezen, het scherm sluiten of naar de achtergrond gaan stopt de vorige stem eveneens.

Tuddy hergebruikt de bestaande `LocalCaptionSpeaker` en accepteert uitsluitend een reeds geïnstalleerde Android-stem die door Android als niet-netwerkafhankelijk wordt gemeld. **Korte** zinnen worden afzonderlijk voorgelezen; **lange** selecties (maximaal 8.000 UTF-16-tekens) worden vóór het starten volledig gevalideerd en in maximaal 100 korte spraakfragmenten verdeeld. Eén TTS-engine speelt de fragmenten op volgorde af met de bestaande audiofocus-, headset- en tijdslimietbescherming. Het scherm laat **deel X van Y** zien. **■ Stop voorlezen**, achtergrond gaan, het wisselen van taalrichting of het starten van een nieuwe actie beëindigt de resterende fragmenten en negeert verouderde voortgang.

De bron en voltooide vertaling worden **afzonderlijk** voorgelezen; tweetalig achter elkaar afspelen blijft bewust beperkt tot twee korte regels van elk maximaal 400 tekens. Dat tweetalige menu is voor langere passages niet beschikbaar. De uitvoer wordt nooit automatisch opgeslagen, gedeeld of online verzonden. Een ongeldige selectie, ontbrekende offline stem of Android-afspeelfout levert een expliciete melding op, geen spraakdownload, cloud-TTS, nieuwe machtiging of automatische microfoonactivatie. Een apparaatclaim blijft afhankelijk van echte vliegtuigmodus- en toestelacceptatie; Androids stemmetadata zijn geen onafhankelijke radiometing.

Ook het lokale brontaalvoorstel is nu tegen verouderde asynchrone callbacks beschermd: als de selectiepagina afsluit, de gebruiker tussentijds talen wisselt of de app naar de achtergrond gaat, kan een late herkenningsuitkomst de nieuwere selectie niet terugzetten.

## Vanuit Android Delen en naar andere apps terug

Ook via **Delen → Tuddy** kun je na de bestaande controle van een gewone
platte tekst kiezen voor **Vertaal apart · zonder gespreksgeschiedenis**.
Dit opent dezelfde lokale geselecteerde-tekstvertaler als Android
**Tekst verwerken**, maar in alleen-lezenmodus: de oorspronkelijke app
wordt niet heimelijk aangepast en geen gespreksregel wordt aangemaakt.

Na een geslaagde vertaling zijn er drie afzonderlijke mogelijkheden:
**Kopieer vertaling** gebruikt het met Androids *gevoelige inhoud*-vlag
gemarkeerde klembord (geen automatische inhoudsvoorvertoning op
ondersteunde Android-versies, maar andere apps kunnen inhoud nog lezen);
**Deel vertaling via Android…** toont eerst een privacybevestiging
en opent alleen met instemming Androids standaarddeelmenu;
**Vervang geselecteerde tekst** is uitsluitend beschikbaar bij
daadwerkelijk bewerkbare selecties die de bronapp als zodanig markeert.
Het delen naar een externe app kan daar opslag of netwerkgebruik
veroorzaken, onafhankelijk van Tuddy's eigen lokale vertaling.
Bij het verlaten van de snelle vertaler wordt het tijdelijke scherm
gewist. Bijlagen, ondertitels, back-ups en andere gestructureerde
gedeelde tekst blijven van de gewone vertaalactie gescheiden.

## Expliciete tweetalige overdracht van bron en vertaling

Na een **voltooide** lokale ML Kit-vertaling, of na het bewust overnemen
van je eigen handmatige correctie, verschijnt **Bekijk bron + vertaling
vóór kopiëren/delen…**. De bestaande knoppen **Kopieer vertaling** en
**Deel vertaling via Android…** bevatten nog steeds uitsluitend de
vertaalde tekst. Ook **Vervang geselecteerde tekst in de andere app**
geeft **alleen** de vertaling terug: de bronapp ontvangt geen extra
bronzin of privémetadata via die actie.

Het tweetalige voorbeeld bevat de bron en de voltooide vertaling, met
de geselecteerde taalnamen en taalcodes. Bij het resultaat staat
expliciet **Lokaal door ML Kit vertaald; niet onafhankelijk
geverifieerd** of **Door gebruiker aangepast; niet onafhankelijk
geverifieerd**. De tweede aanduiding claimt dus niet dat Tuddy de
handmatige correctie of taalkwaliteit heeft beoordeeld. Alle
tekst staat uitsluitend tijdelijk in de huidige Activity.

Het voorbeeld is precies de te kopiëren of te delen tekst (afgezien
van Androids eigen eventuele opmaak). Pas na bekijken en bewust kiezen
van **Kopieer beide** gebruikt Tuddy het bestaande gevoelige Android-
klembord met de optionele vervaltimer; andere apps kunnen meteen
**zowel bron als vertaling** lezen. **Deel beide via Android…** opent
het standaarddeelmenu alleen na die aparte keuze. De ontvangende app
kan beide teksten bewaren of online verwerken; Tuddy kan die kopie
niet terughalen. **Niets overdragen** sluit het voorbeeld zonder
iets extern te doen.

Beide teksten moeten binnen de bestaande grenzen vallen: maximaal
8.000 UTF-16-tekens per veld, geldige Unicode en geen onveilige
controletekens. De totale gemarkeerde tweetalige tekst is bovendien
begrensd op 17.000 tekens. Als de combinatie ongeldig of te groot is,
biedt Tuddy geen tweetalige overdracht aan; de bestaande
vertaling-only acties blijven onafhankelijk. Er wordt geen extra
ML Kit-model gedownload, geen tekst weggeschreven, geen microfoon
geopend en geen extra share- of clipboardactie gestart zonder een
expliciete keuze.

## Gedeelde capaciteit voor lokale tekstvertaling

De snelle Android-tekstselectie gebruikt dezelfde procesbrede grens
van maximaal **drie nog actieve ML Kit-tekstvertaaltaken** als het
gewone gesprek en de tijdelijke live-voorbeelden. Een vertaling kan
nog intern op Android draaien nadat je het venster sluit of annuleert.
Zo'n ingediende taak blijft meetellen tot een echte modelcallback.
Wanneer die grens vol is, toont de snelle vertaler een duidelijke
lokale foutmelding in plaats van nieuwe rekenbelasting of een
online fallback te starten. Niets wordt automatisch gedeeld of
naar de oorspronkelijke app teruggestuurd.

## Zelf een geïnstalleerde voorleesstem kiezen

Met **Stem bron kiezen** en **Stem vertaling kiezen** kun je op dit
snelle vertaalscherm de momenteel op het toestel geïnstalleerde,
door Android als niet-netwerkafhankelijk gemelde stemmen voor beide
talen bekijken. Kies een stem, luister naar een **vast voorbeeld**
(zonder privézin) of herstel de automatische veilige stemkeuze.
De instelling geldt ook voor de voorleesfuncties van het gewone
Tuddy-gesprek. Er is geen nieuwe machtiging, stemdownload, online-TTS
of modelondersteuning nodig. Zie
[ANDROID_INSTALLED_VOICE_PICKER.md](ANDROID_INSTALLED_VOICE_PICKER.md).

## Origineel en vertaling na elkaar beluisteren

Na een **voltooide** lokale vertaling biedt het bestaande
voorleesmenu nu ook **Origineel → vertaling · normaal** en
**Origineel → vertaling · langzaam** aan. Tuddy controleert
vooraf beide geïnstalleerde, niet-netwerkafhankelijke Android-stemmen,
gebruikt eenmalig audiofocus en spreekt pas de eerste zin als
beide stemmen beschikbaar zijn. Na de tweede zin, een hoofdtelefoon-
of focusonderbreking, Stop of achtergrond wordt TTS beëindigd.
Er wordt niets automatisch gedeeld of teruggeschreven naar de bronapp.

## Langere selecties, één complete lokale vertaling

De Android-optie **Vertaal geselecteerde tekst** verwerkt nu langere
platte tekst als een reeks *kleine, opeenvolgende* lokale ML Kit-opdrachten.
De selectie moet uit maximaal **8.000 UTF-16-tekens** bestaan; elk
gedeelte is ten hoogste **800 tekens**, er zijn maximaal **24 delen**,
en Tuddy breekt geen UTF-16-surrogaatpaar doormidden.

Tuddy toont **Gedeelte X van Y lokaal vertalen…** gedurende de
bewerking. De lokale translator en één plek in de gezamenlijke
ML Kit-werklimiet blijven onder controle: een volgend gedeelte begint
pas nadat Android de vorige opdracht heeft afgerond. De bestaande
tijdslimiet van **30 seconden voor de volledige actie** blijft gelden,
ook bij meerdere delen. Bij een fout, ontbrekend model, tijdslimiet,
afgebroken appselectie of **Annuleer lokale verwerking** worden
eventuele tijdelijke deelresultaten **niet** getoond, gekopieerd,
gedeeld of naar de aanroepende app teruggestuurd.

Pas als **alle** delen zijn geslaagd, combineert Tuddy ze in RAM tot
één resultaat van maximaal 8.000 tekens. Alineascheidingen blijven
zoveel mogelijk bewaard; de gebruiker moet zelf controleren of de
bedoeling klopt, want vertalen per tekstgedeelte kan context missen.
Kopiëren, Android-deelmenu en geselecteerde tekst vervangen vereisen
nog altijd een aparte bewuste actie. Deze route downloadt niets,
start geen microfoon of cloudvertaling en bewaart geen transcript.

Android/ML Kit kan doorgaan met een *al ingediende* lokale taak nadat
de gebruiker Annuleer tikt. De app maakt geen nieuwe vervolgtaken
meer aan en sluit de native client pas wanneer die taak klaar is;
de bestaande procesbrede werkbeperking blijft daarom gereserveerd
tot Android zijn afronding meldt.

## Menselijke correctie vóór uitvoer naar een andere app

Na succesvolle lokale ML Kit-vertaling zie je **Controleer of pas
vertaling handmatig aan**. Daarmee kun je het volledige resultaat
eerst lezen en in het tijdelijke invoerveld verbeteren, ook wanneer
de oorspronkelijke selectie alleen-lezen is.

- Tijdens het bewerken blijven voorlezen, klembord, Android-delen en
  vervangen in de oorspronkelijke app uitgeschakeld. De oorspronkelijke
  ML Kit-uitkomst blijft apart **alleen in het werkgeheugen** beschikbaar.
- **Neem correctie over** accepteert uitsluitend niet-lege tekst van
  maximaal **8.000 UTF-16-eenheden**, met geldige Unicode en zonder
  verborgen besturingstekens. Er wordt nooit stilzwijgend een lange
  correctie afgekapt. De gecorrigeerde versie wordt de tekst voor
  de bestaande uitvoeracties, pas als de gebruiker die daarna
  afzonderlijk aanklikt.
- **Annuleer** laat de al beschikbare vertaling onveranderd.
  **Herstel oorspronkelijke ML Kit-vertaling** maakt een handmatige
  wijziging ongedaan zonder opnieuw te vertalen of modellen te downloaden.
- Een handmatige correctie is **geen linguïstische goedkeuring**
  door Tuddy. Bij een andere bron/doeltaal of nieuwe vertaalpoging,
  schermverlaten, Android-focusverlies en Activity-vernietiging
  verdwijnen concept, correctie en herstelkopie uit de schermstatus.
  Geen voorkeur, conversatiearchief, analytische gebeurtenis of
  extern netwerkverzoek wordt hiervoor aangemaakt.
- Een alleen-lezen Android-bronapp kan nog steeds niet via
  `ACTION_PROCESS_TEXT` worden overschreven; kopiëren of delen blijven
  afzonderlijke, bewuste keuzes met de gebruikelijke Android-privacyrisico's.

## Privacy en grenzen

De functie verwerkt uitsluitend geselecteerde `text/plain`-tekst, begrensd
door de bestaande invoerlimiet. Er worden geen URL's geopend, bestanden
gelezen, gesprekken opgeslagen of analytics verstuurd. De tekst en vertaling
blijven alleen tijdelijk in het actieve scherm; bij achtergrond gaan of sluiten
worden ze gewist. De Android-klembordfunctie kan de vertaling buiten Tuddy
beschikbaar maken nadat je expliciet op **Kopieer vertaling** tikt.

Een geïnstalleerd tekstmodel bewijst geen spraakondersteuning, nauwkeurigheid
of fysieke vliegtuigmodusacceptatie. Kurmanji, Sorani en Pasjtoe blijven buiten
de ML Kit-vertaalcatalogus totdat hun afzonderlijke licentie-, runtime-,
native-speaker- en toestelgates zijn opgelost.
