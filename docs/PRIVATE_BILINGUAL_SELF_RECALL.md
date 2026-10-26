# Zelfgestuurde tweetalige overhoorronde — offline en privé

## In het bestaande gespreksgereedschap

Open **Tweetalig terugluisteren** en kies de taalrichting, het aantal
afgeronde zinnen (maximaal acht) en desgewenst een startzin. Onder de
bestaande luisteropties staat nu **Zelf overhoren**. De overhoorronde
gebruikt **precies dezelfde, reeds afgeronde en lokaal vertaalde zinnen**
als de bestaande replay, zonder een nieuw tekstmodel, spraakmodel,
download, synchronisatie of externe dienst.

Tik **Begin stille overhoorronde**. Tuddy laat één oorspronkelijke zin
zien. De bestaande vertaling voor die zin blijft in dit paneel verborgen
tot je bewust op **Onthul vertaling** tikt. Dat voorkomt dat de app je
ongevraagd het antwoord voorzegt; buiten dit paneel blijft het bestaande
gesprek wel toegankelijk. De lokale vertaling kan inhoudelijke fouten
bevatten en is geen onafhankelijke beoordeling.

### Antwoord zelf typen en twee richtingen oefenen

Boven **Begin stille overhoorronde** kun je nu expliciet kiezen:
**oorspronkelijke taal → vertaling** of **vertaalde taal → oorspronkelijke
taal**. De tweede richting gebruikt uitsluitend hetzelfde bestaande
tweetalige zinspaar, maar verwisselt vraag en referentie. Er ontstaat
geen nieuwe ML Kit-vertaling of claim over de kwaliteit van een
terugvertaling. Wissel je van richting, dan stopt eventuele audio en
wordt een lopende overhoorronde volledig opnieuw gestart.

Tijdens de vraag kan je je eigen antwoord **in een tijdelijk
tekstveld typen** (maximaal 400 afdrukbare tekens). Ook
denken zonder typen blijft mogelijk. **Wis mijn getypte antwoord**
gooit je concept direct weg. Na **Onthul referentie** toont Tuddy je
eigen poging naast de al bestaande lokale tekst: de app corrigeert
niet automatisch, maakt geen correctievoorstel en slaat geen oordeel
van een model op. De vraag en referentie kunnen desgewenst afzonderlijk
worden voorgelezen via al lokaal geïnstalleerde Android-stemmen, op
een expliciete tik, ook bij omgekeerde oefenrichting.

**Privacy:** ingevoerde antwoorden staan alleen in tijdelijke
Compose-toestand; er is geen Android SavedState, back-up, netwerk,
cloudmodel, woordenboekopslag of synchronisatie. Bij de volgende
vraag, een nieuwe ronde, richting wisselen, sluiten of Android
naar de achtergrond wordt gebracht, wordt het antwoord gewist.
Tuddy zet autocorrectie uit voor dit tekstveld, maar het
**door de gebruiker ingestelde Android-toetsenbord** kan eigen
woordenleer-, netwerk- en privacysinstellingen hebben; Tuddy kan
die externe toetsenbordapp niet controleren. Gebruik geen gevoelige
persoonsgegevens in je oefenantwoord.

Daarna kun je de zin zelf markeren als **Zelf goed** of **Nog oefenen**.
De app houdt alleen tijdens deze actieve UI-sessie de indexen van de
zinnen bij die je zelf nog wilde oefenen. Na de ronde kun je uitsluitend
die gemiste zinnen herhalen, desnoods meerdere rondes. Als je iedere
zin als goed beoordeelt, laat de app dat als **zelfbeoordeelde
oefenvoortgang** zien; dit wordt nooit gepresenteerd als bewezen
vertaalkwaliteit, taalvaardigheid of een geslaagde offline toestelproef.

### Audio is nooit automatisch

De knoppen **Luister vraag** en, pas na onthulling, **Luister
referentie** zijn afzonderlijke acties. Alleen als je erop tikt gebruikt
Tuddy zijn bestaande gecontroleerde offline Android-voorleesstemmen
met audiofocus en uitvoerapparaatbewaking. Verdergaan, opnieuw
beginnen, een andere taalrichting/zin kiezen, het scherm verlaten
en een gewijzigde gesprekszin stoppen eventueel nog lopend audio.

### Gegevensgrenzen en vrijgave

De app bewaart geen antwoorden, score, gemiste zin, opname, voortgang,
identiteit of tijdstip op schijf. Alle oefenstatus is tijdelijk en
wordt gewist als de gekozen oefenzinnen veranderen of het scherm
verdwijnt. De app verandert de originele/vertalde gespreksregels niet
en maakt geen automatisch opgeslagen eigen woorden aan.

De GitHub Android-build op de exacte hoofdbranch bewijst alleen dat
de broncode een APK kan bouwen. Issue #1 blijft open voor echte
Android 12+-vliegtuigmodusobservaties, de correcte fysieke APK,
beide NL→EN/EN→NL-richtingen en onafhankelijke beoordeling.
Volledige Koerdisch/Pasjtoe-modelrechten, native-speakerkwaliteit,
UX-metingen en een ondertekende productierelease blijven
afzonderlijke gates.
