# Tekst delen vanuit andere apps naar de offline tweewegmodus

Android kan een door de gebruiker geselecteerde tekst via **Delen**
of **Tekst verwerken** naar Tuddy sturen. Sinds deze uitbreiding toont
het bestaande reviewvenster de extra keuze
**⇄ Gebruik in tweeweg-tekstgesprek (59 teksttalen)**.

Deze stap begint geen vertaling. De gekozen tekst wordt alleen als
**niet-verstuurd bewerkbaar concept** in de tweeweg-editor geopend.
De gebruiker moet daar zelf de echte bron- en gewenste doeltaal
kiezen, de inhoud controleren en pas daarna op
**Voeg deze beurt toe en vertaal lokaal** tikken.

Anders dan het spraak-taalmenu gebruikt de tweeweg-editor de bestaande
59 door ML Kit ondersteunde **tekst**talen, onafhankelijk van lokale
Android-spraakpakketten. Voor een ontbrekend tekstmodel is eerst een
afzonderlijke expliciete wifi-voorbereiding nodig, zonder automatische
cloudvertaalfallback. Het nieuwe pad opent geen microfoon.

Ook bij keuze van een bewaard favoriete tekstrichting blijft een
nog niet verstuurde **uit een andere app gedeelde** zin staan, met een
waarschuwing om de bron opnieuw te controleren. Zelf getypte gewone
conceptzinnen blijven de bestaande bescherming behouden: een bewuste
favorietenwissel wist die niet-overeenkomende concepttekst.
Na daadwerkelijk toevoegen, annuleren, sluiten, app-achtergrond of
vernietiging van de Activity wordt het doorgestuurde concept uit
het tijdelijk Activity-geheugen gewist. Er zijn geen nieuwe opgeslagen
voorkeuren, clipboard-uitlezingen, bestandsacties of automatische shares.

Tuddy behandelt privé of gevoelige tekst niet als gevalideerde
taalbron. Volledige Kurmanji/Sorani/Pasjtoe offline ASR en MT,
fysieke toestelacceptatie, licentierechten en production signing
blijven nog open.
