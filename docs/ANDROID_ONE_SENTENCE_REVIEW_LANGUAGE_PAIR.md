# Eén gesproken zin controleren — inclusief teksttaalrichting

## De bestaande één-zinsmodus

Bij **Eén zin** stopt de Android-microfoon na één on-device
herkenningspoging. Een herkenningsvoorstel blijft uitsluitend als
tijdelijke RAM-conceptzin zichtbaar. Zonder jouw expliciete bevestiging
komt die zin **niet** in het gesprek en start er **geen** vertaling.
De gebruiker kan het voorstel aanpassen, een alternatief kiezen of
de zin verwijderen.

## Nieuw: de taal van precies deze zin beoordelen

1. Open het bestaande **Controleer je herkende zin**-venster na één
   gesproken zin. Androids voorgestelde bron- en doeltaal staan boven
   de herkende tekst.
2. Kies **Bron** of **Doel** om een zoekbare lijst met werkelijk door
   Tuddy toegelaten ML Kit-**tekst**talen te openen. Kies een taal via
   een menselijke naam of code; deze keuze verandert **uitsluitend
   deze conceptzin** en niet het microfoonpakket, instellingen van
   volgende zinnen of de taal van reeds bestaande gesprekregels.
3. Na een taalwijziging controleert Tuddy **read-only** of het exacte
   offline tekstmodelpaar momenteel geïnstalleerd is. Statussen zijn
   **beschikbaar**, **ontbreekt** of **onbekend**; het is geen download,
   geen netwerk-API en geen spraakmodelcontrole. Bij gelijke bron- en
   doeltaal vermeldt het scherm dat er geen tekstvertaling nodig is.
4. Bewerk zo nodig de herkende tekst of kies maximaal één van de drie
   bestaande Android-herkenningsalternatieven. Herstel desgewenst
   zowel de eerste tekst als de oorspronkelijk gekozen taalrichting.
5. Alleen bij **Bevestig en vertaal lokaal** valideert Tuddy nogmaals
   de tekst, de beide ondersteunde taalcodes en de actuele
   levenscyclus/generatie van het nog geopende voorstel. Daarna komt
   één gespreksregel met de **daadwerkelijk gekozen** bron- en doeltaal
   in het tijdelijke gesprek. De reeds bestaande ML Kit-only
   vertaalpipeline controleert het lokale model. Bij een ontbrekend
   model blijft de zin bestaan en verschijnt de bestaande herstelactie,
   zonder een automatische download.
6. Een gewijzigde tekst **of** taalrichting krijgt de
   `userCorrected`-status. Daardoor kunnen latere automatische
   taalherkenning, groepsherstel en asynchrone callbacks geen menselijke
   correctie stilzwijgend ongedaan maken. Handmatig opnieuw proberen
   blijft mogelijk. Androids originele ASR-richting is niet
   achteraf als opnieuw herkend aangemerkt.
7. **Gooi zin weg** of sluit het scherm: de zin wordt niet toegevoegd
   of vertaald en er wordt geen concept op schijf bewaard.

De woordenlijst van talen is uitsluitend ML Kit-tekstmodeldekking,
**geen** bewijs dat Android in die taal offline kan luisteren. Er
worden geen nog niet vrijgegeven Kurmanji-, Sorani- of Pasjtoe-
spraak/vertalingsmodellen geactiveerd. De bestaande fysieke Android
12+ vliegtuigmodusproef (NL→EN en EN→NL), modelrechtencontrole,
native-speakeracceptatie, exacte debug-APK-herkomst en getekende
productierelease blijven afzonderlijke gates.
