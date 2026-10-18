# Ondertitel-tijdlijn met twee handmatige ankers corrigeren

Open bewust een eigen SRT of WebVTT in de lokale ondertiteleditor.
Kies Tijdlijn uitlijnen met twee ankers. De app toont de huidige
starttijd van de eerste en laatste cue. Voer handmatig in op welke
starttijd de eerste en laatste regel moeten verschijnen.

Tuddy interpoleert daarna alle tussenliggende tijdcodes en ook
de duur van iedere regel op dezelfde lineaire schaal. Je ziet
vóór toepassen de oude/nieuwe ankers, de laatste eindtijd en
eventuele overlap. De oorspronkelijke tekst wordt niet gewijzigd.

De nieuwe tweede start moet ná de eerste liggen; de bestaande
cuevolgorde, positieve duren, nulgrens, 99:59:59,999-bovengrens
en SRT/VTT-bestandslimieten blijven gelden. Een te sterke compressie
kan door afronden ongeldige regelduren veroorzaken; die voorstellen
worden geweigerd. Een reeks met minder dan twee verschillende
startmomenten kan niet met twee ankers worden bewerkt.

Dit is geen automatische audio- of videosynchronisatie: beide
ankers zijn volledig door de gebruiker opgegeven. De editor
bewaart de hele wijziging als één tijdelijke herstelbare
Terug/Opnieuw-actie. Opslaan vereist daarna apart Bewaar kopie
met Android-bestandskeuze; een externe provider kan synchroniseren.
Geen nieuw netwerk, account, spraakmodel of microfoonpermissie.
