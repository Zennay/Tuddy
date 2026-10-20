# SRT/WebVTT plakken zonder bestand

Tuddy laat je handmatig een volledig SRT- of WebVTT-document plakken, zonder dat de app zelf je Android-klembord uitleest.

## Direct openen

1. Stop eerst de live microfoon en tik op **Plak eigen SRT/WebVTT-tekst en bekijk voorbeelden…**.
2. Plak de volledige ondertiteltekst in het invoerveld. Geldige tijdregels hebben een begin- en eindtijd. Zowel SRT als WebVTT is toegestaan.
3. Bekijk het aantal gevonden regels en de eerste vijf tijdgecodeerde fragmenten. Alleen als de volledige tekst door de bestaande begrensde parser wordt geaccepteerd, wordt **Open tijdelijk concept** actief.
4. Bevestig expliciet. De regels openen in de bestaande lokale ondertiteleditor. Het gesprek, het bronbestand en de live transcriptie veranderen niet.
5. Je kunt fragmenten aanpassen, afspelen met eigen apart gekozen lokale audio, terugdraaien, oefenen of als **nieuwe** SRT/VTT-kopie opslaan.

## Van losse tekst een ondertitelconcept maken

1. Kies **Gewone zinnen**. Je kunt zelf kiezen tussen **Per regel** en **Per zin (schatting)**.
2. Bij **Per regel** wordt iedere niet-lege tekstregel een cue. Bij **Per zin** wordt op leestekens gevolgd door witruimte gesplitst; dit is een beperkte tekstheuristiek en kan verkeerde grenzen kiezen.
3. Kies zelf de starttijd (0, 5, 10, 30 of 60 seconden) en de geschatte duur per cue (2, 3,5, 5 of 7 seconden).
4. Bekijk de resultaten, zoek over het hele document en blader in pagina's van 15 cues. De getoonde tijdcodes zijn **handmatig gesimuleerd**, niet gebaseerd op audio/spraakherkenning.
5. Open het concept alleen na de bevestigingsknop, bewerk tekst en tijden zo nodig, en bewaar een aparte SRT/VTT-kopie met de bestaande documentkiezer. Er vindt geen automatische bestandsopslag plaats.

## Handmatig tweetalige tekst uit twee kolommen

1. Kies **Gewone zinnen** en vervolgens **Twee handmatige tekstkolommen (tab gescheiden)**.
2. Plak per niet-lege regel twee velden uit bijvoorbeeld een eigen spreadsheet: `oorspronkelijke zin[TAB]zelfgeschreven betekenis`. Gebruik exact één tab om de kolommen te scheiden; twee lege/extra kolommen worden afgewezen.
3. Tuddy zet ieder door jou geschreven paar op één **tweeregelige** ondertitelkaart met dezelfde *geschatte* tijdsinstellingen. Bron en eigen betekenis blijven exact zoals aangeleverd, zonder automatische vertaling, taaldetectie of controle op juistheid.
4. Zoek lokaal door de complete preview, blader langs alle kaarten en open de volledige gekozen invoer pas na expliciete bevestiging. Vervolgens kun je in de bestaande editor aanpassen, combineren of apart als SRT/VTT opslaan.
5. Gebruik voor handmatige Kurmanji, Sorani of Pasjtoe gerust je eigen tekst, maar dit bevestigt **geen** offline ASR/MT-taalondersteuning of licentie-/native-speaker-review.

## Tijdcodes uit eigen spreadsheet bewaren (vier kolommen)

Heb je zelf nauwkeurige begin- en eindtijdcodes naast je tweetalige zinnen, dan hoeft Tuddy daar geen tijd te schatten.

1. Kies **Gewone zinnen** en **Vier kolommen: begin · einde · bron · betekenis**.
2. Plak per niet-lege rij exact vier velden, met tabs ertussen: `00:00:12.500[TAB]00:00:15.750[TAB]Eigen bronzin[TAB]Eigen betekenis`.
3. Alternatieve tijdnotatie is `mm:ss,mmm`; tijdvelden moeten binnen het geldige documentbereik vallen en **einde moet later zijn dan begin**. De volgorde moet chronologisch blijven. Overlappende tijden mogen zoals bij bestaande SRT/VTT voor overdub/captions, maar de editor laat die zichtbaar.
4. De app behoudt de opgegeven begin- en eindtijden zonder een audioanalyse, ASR of herschatting. De instelling voor geschatte starttijd en zinsduur wordt in deze modus niet getoond.
5. Bekijk de volledige regelpreview of kies eerst een exact bereik, open het concept en corrigeer desgewenst een bestaande tijdcode met de ondertiteleditor. Opslaan blijft expliciet via een nieuwe Android-bestandskeuze.

Deze eigen tijdcodes zijn **niet bewezen** aan een audio- of videobestand gesynchroniseerd; Tuddy kopieert uitsluitend de tijdcodes die je zelf invoert.

## Een geselecteerd bereik uit een grote tekst openen

- Na het plakken toont Tuddy hoeveel geldige regels de complete bron bevat.
- Vul bij **Van regel** en **Tot (leeg = alle)** de gewenste regelnummers in. Het begint bij 1; een lege eindwaarde kiest tot de laatste bronregel. Ongeldige of omgekeerde bereiken worden afgewezen.
- De zoekfunctie en bladzijden van 15 kaarten tonen alleen het gekozen bereik, maar bewaren de **oorspronkelijke bronregelnummers**. Zoeken verandert nooit welke regels worden geopend.
- Bevestig met **Open gekozen regels**. De app leest de volledige actuele bron opnieuw, controleert opnieuw de gekozen grenzen en neemt precies dat bereik over in de tijdelijke editor, ook als een zoekfilter actief is.
- De niet-geselecteerde regels blijven buiten het tijdelijke concept. Deze handeling verwijdert of verandert niets in het bronbestand; een export blijft een aparte keuze via Android.

## Gerichte tekstcorrecties na import

In de tijdelijke ondertiteleditor open je **Zoek en vervang** (via het documentmenu) als een herhaalde schrijf- of vertaalkwestie in veel cues voorkomt.

1. Selecteer de **oorspronkelijke eerste en laatste regel** waar correcties zijn toegestaan. Leeg einde betekent alle regels tot het einde. Ongeldige of omgekeerde bereiken worden geweigerd.
2. Kies wat aangepast mag worden: **alle tekstregels**, **alleen de eerste regel** (zelf aangeleverde brontekst), of **alleen de tweede regel** (zelf geschreven betekenis). De tweede-regeloptie slaat eentalige cues zonder tweede regel over; er wordt nooit zelf een betekenis verzonnen.
3. Voer een letterlijke zoekterm en vervanging in, eventueel hoofdlettergevoelig. Bekijk **hoeveel cues en treffers** worden aangepast en tot vijf voorbeelden van de exacte tekst **vóór en na**. Bij grotere aantallen toont Tuddy ook hoeveel andere regels veranderen. De preview is scrollbaar.
4. Bevestig afzonderlijk. Alle vervangingen vormen één bewerking in het tijdelijke concept; **Terug** maakt die ene wijziging ongedaan en **Opnieuw** kan haar terugzetten. Bestaande tijdcodes en regels buiten de gekozen selectie blijven onaangeroerd.
5. Bewaar pas later, indien gewenst, een nieuw SRT/VTT-bestand via de Android-documentkiezer. Originele bronbestanden of gesprekken veranderen niet.

Ook bij tweetalige kaarten bepaalt Tuddy **niet** welke taal de eerste of tweede regel werkelijk bevat; de volgorde blijft de verantwoordelijkheid van de gebruiker. Alle uitgaven blijven onder de bestaande 500-cue- en 256-KiB-limieten.

## Tweede spoor plakken en samenvoegen

1. Open een SRT/VTT-concept, vanuit een document of de plakroute.
2. Kies in de editor **Tweede ondertitelspoor zelf plakken…**.
3. Plak een tweede compleet document en beoordeel de korte preview.
4. Kies in de bestaande samenvoegpreview tussen **aansluiten**, **originele tijden behouden** of **overlappende tekstparen** (met optionele handmatige tijdsverschuiving).
5. Controleer het voorgestelde resultaat en geef apart toestemming om de samenvoeging alleen in het tijdelijke concept toe te passen. Undo/redo blijft bruikbaar.

## Grenzen en privacy

- Geen automatische clipboard-lezing, netwerk, ASR, vertaling, cloud of extra permissies.
- Maximaal 256 KiB UTF-8 en 500 cues per bron; misvormde tijdcodes en niet-ondersteunde inhoud worden geweigerd. In de plattetekstroute zijn per cue maximaal 800 tekens toegestaan, en de resulterende SRT én WebVTT moeten binnen de exportlimiet passen.
- Een te grote plakactie wordt expliciet geweigerd, niet stilzwijgend afgekapt of vervangen door eerder geldige invoer. Handmatige paren mogen elk maximaal 800 tekens per kolom bevatten; lege/extra kolommen worden niet omgezet in een onjuiste betekenis.
- De geplakte inhoud blijft alleen in de geopende UI-sessie en verdwijnt bij sluiten of achtergrond. Geen automatische opslag.
- Opslaan is een aparte handeling via de Android-bestandskiezer; een gekozen documentprovider kan zelf cloudsync aanbieden.
- WebVTT-/SRT-tijdcodes en combinaties zijn **handmatig**. De app claimt geen gemeten woord- of audiouitlijning.
- Er is geen claim van fysieke Android-vliegtuigmodusacceptatie, release-ondertekening of volledige Kurmanji/Sorani/Pasjtoe-ASR/MT.
