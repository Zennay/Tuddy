# Offline ondertitel-oefenblad met apart antwoordenblad

Open in Tuddy bewust een SRT- of WebVTT-document via de bestaande
ondertiteleditor. Filter desgewenst de regels op een zoekterm. Kies
**Bewaar geselecteerde regels als oefenblad…**. Tuddy neemt maximaal
24 van de **huidige gefilterde regels**, toont eerst tijdcodes en een
kort startstuk van de zin met schrijfruimte, en zet de volledige originele
tekst op een afzonderlijke antwoordpagina. Je kunt het zelfstandige
HTML-bestand openen, offline lezen en zelf afdrukken.

Dit is geen automatische vertaling, media-uitlijning of taalbeoordeling.
De tekst komt letterlijk uit de door jou gekozen documenten en kan fout
zijn. Voor regels met minder dan drie woorden is alleen de tijdcode een
hint. Rechts-naar-links-scripts worden met `dir=auto` getoond.
Alle tekst wordt HTML-geëscaped. De uitvoer is scriptvrij,
heeft geen externe fonts, afbeeldingen of URL's en is begrensd
tot 100 KiB. Alleen geldige, veilige SRT/VTT-cues kunnen worden gebruikt.

**Opslag vereist twee bewuste acties:** de exportknop en vervolgens de
Android-bestandskiezer. Wanneer de selectie, inhoud of volgorde tijdens
de bestandskeuze is veranderd, weigert Tuddy het oude exportmoment.
Het origineel en het tijdelijke editorconcept worden niet overschreven.
De gekozen externe bestandsprovider kan het document alsnog met
cloudopslag synchroniseren; kies die locatie bewust. De app opent
nooit zelf een netwerkverbinding voor deze functie.

Deze debug-productfunctie is geen bewijs van fysieke offline
vliegtuigmodusacceptatie, volledig vrij gelicentieerde Kurdish/Pashto
ASR/MT, onafhankelijke native-speakerreview, production signing of
zCloud/VPS-admission. Incubate/Verifying blijven ongewijzigd.

## Terugkomen uit de Android-bestandskiezer

Het HTML-bestand wordt voortaan via de **bestandskiezer van de hoofdactiviteit**
bewaard. Android mag de tijdelijke ondertiteleditor sluiten wanneer de
bestandsprovider op de voorgrond komt; de bewuste export bevat dan alleen
een begrensde tekstsnapshot en precies het tijdelijke editorconcept van
het moment waarop je op Bewaar tikt. Na bevestigen of annuleren wordt
dat tijdelijke concept heropend. Tuddy meldt of bewaren is gelukt,
geannuleerd of geweigerd en schrijft niet automatisch een ander bestand.
De oude Undo/Redo-geschiedenis van de gesloten editor wordt niet
gereconstrueerd; de reeds gewijzigde tekst en tijdcodes blijven wel
onderdeel van het heropende concept. Bij procesafsluiting verdwijnt
deze tijdelijke informatie.
