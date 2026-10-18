# Afdrukbaar offline zinnenboek (HTML)

In **Mijn eigen offline zinnen** vind je de knop **▤ Maak afdrukbaar
tweetalig zinnenboek…**. De gebruiker kiest bewust tussen alleen de
huidige taal of alle drie de taalgroepen en krijgt vóór de Android-
bestandskiezer een aparte privacywaarschuwing met het aantal zinnen.

## Wat er wordt bewaard

- Alleen handmatig door de gebruiker opgeslagen Kurmanji-, Sorani- en
  Pasjtoe-zinnen, mét door de gebruiker zelf geschreven betekenis in NL/EN.
- Eén zelfstandig `.html`-document, met afdrukvriendelijke kaarten,
  leesbare grote letters en `dir=rtl` voor Sorani/Pasjtoe.
- Geen scripts, cookies, externe lettertypen, externe stylesheets,
  afbeeldingen, audio, modeldata, apparaat-ID of automatisch
  opgeslagen gespreksregels.
- Maximaal 80 zinnen; HTML-speciale tekens worden als tekst ge-escaped.
  Ongeldige controletekens, taalvelden, te grote tekst en te grote
  bestanden worden geweigerd; geen onbetrouwbare HTML-injectie.

Open het document in een browser die lokale HTML ondersteunt en gebruik
de afdrukfunctie van die browser. Android wordt niet automatisch naar
een printer, browser of deelbestemming gestuurd.

## Privacy en afbakening

De gebruiker moet zelf bevestigen dat het **eigen, ongecontroleerde**
vertalingen en betekenissen zijn, en kiest zelf de bestandslocatie via
Androids documentkiezer. Die provider kan het bestand buiten Tuddy
opslaan of synchroniseren. Als de onderliggende eigen zinnen zijn
gewijzigd tussen bevestigen en het terugkeren uit de bestandskiezer,
weigert Tuddy het oude snapshot te exporteren.

Een eenmaal extern opgeslagen HTML-bestand valt niet onder
**Wis gesprek** of **Wis mijn oefengegevens**. Het bestand is ook
**geen back-up voor herstel** in Tuddy; daarvoor bestaat apart een
privé JSON-back-upfunctie.

Deze toevoeging levert geen nieuwe offline spraakherkennings- of
vertaalmodellen en bewijst niet dat zinnen native-speaker-gevalideerd
zijn. Tuddy blijft Incubate.
