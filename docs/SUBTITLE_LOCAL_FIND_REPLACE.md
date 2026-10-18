# SRT/WebVTT: zoek en vervang in meerdere ondertitelregels

Open via **Gesprekstools → Open SRT/WebVTT…** een door jezelf
gekozen, lokaal leesbaar ondertitelbestand. Druk op **Zoek en vervang…**
in de tijdelijke ondertiteleditor. Vul een letterlijke zoekterm en
vervangende tekst in en kies desgewenst of hoofdletters moeten
onderscheiden worden.

De preview toont **hoeveel daadwerkelijke tekstvervangingen** in
**hoeveel afzonderlijke regels** worden toegepast. Bevestig met
**Pas toe in tijdelijk concept**. Een leeg vervangveld kan tekst
verwijderen, behalve wanneer de ondertitel daardoor ongeldig of
leeg zou worden; ook te lange regels, controlecodes en
documenten groter dan de bestaande 256 KiB-limiet worden
geweigerd. De invoer wordt **letterlijk** toegepast: geen
regex, geen automatische vertaling, geen semantische correctie,
geen gedeelde clipboardbewerking of online bewerking.

Bestaande tijdcodes, regelaantal en cue-volgorde veranderen niet.
Alle bewerkingen bestaan alleen in RAM. **Herstel** toont de
originele regels opnieuw. Alleen na **Bewaar kopie SRT…** of
**Bewaar kopie VTT…**, een expliciete waarschuwing en Androids
bestandslocatiekeuze wordt een nieuw bestand geschreven.
Het gekozen bronbestand blijft ongemoeid.

Deze functie is bewust beperkt tot handmatig gekozen tekst;
woorden in verschillende talen, eigennamen en vermeende
vertalingen worden nooit automatisch als correct beschouwd.
