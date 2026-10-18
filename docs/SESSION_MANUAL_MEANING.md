# Eigen interpretatie bij een live gesprekszin

## Waarom

Een automatisch herkende of lokaal vertaalde zin kan een betekenis hebben die
de gebruiker anders begrijpt. Dat rechtvaardigt geen automatische wijziging
van de vertaling of een claim dat een vertaling officieel correct is.

## Werking

1. Stop de microfoon. Kies bij een afgeronde gespreksregel **✎ Eigen uitleg**.
2. Schrijf maximaal 280 tekens die je zelf aan de zin wilt verbinden.
3. Bevestig, wijzig later of verwijder de notitie. De notitie staat apart van
   de oorspronkelijke zin en de on-device ML Kit-vertaling.
4. De eigen uitleg is afzonderlijk herkenbaar in de gesprekstijdlijn, de
   schermvullende gesprekskaart en de leesmodus.

De bewerkactie is gebonden aan **identiteit, oorspronkelijke tekst en
vertalingsrevisie** van precies de gekozen gespreksregel. Als die is
verwijderd, door de gebruiker gecorrigeerd of inmiddels is gewijzigd,
wordt het oude concept niet toegepast. Een correctie van de oorspronkelijke
woorden verwijdert automatisch de aan de oude woorden gebonden uitleg.
Bij het wissen van een afzonderlijke gespreksregel of het volledige
gesprek verdwijnt de bijbehorende uitleg eveneens. De app leest niet
automatisch de originele microfoondata na en presenteert de eigen
interpretatie nooit als door een mens geverifieerde vertaling.

## Gegevensbescherming

De notitie wordt **uitsluitend in het tijdelijke gespreksgeheugen**
bijgehouden. Deze functie voegt geen permissies, modellen, netwerktoegang,
cloudfallback, logbestanden, opslag, tracking of automatische export toe.
De notitie wordt **uitdrukkelijk niet** meegenomen in de bestaande TXT,
JSON, SRT of WebVTT conversatie-export, het deelmenu, het woordenboek
of het klembord. Wie een eigen uitleg elders nodig heeft, moet die
zelf afzonderlijk invoeren.

Vernieuwen van het Android Activity-geheugen of het wissen van de
gespreksregels verwijdert de notitie. Eerder gedeelde teksten buiten
Tuddy kunnen daarmee niet worden ingetrokken.

## Niet geleverd

Dit is geen gecontroleerde vertaalcorrectie en verandert geen
spraakherkenning, offline modelrechten, licenties, native-speakerreview,
ondertitel-woordtiming, debug-APK naar release-conversie of fysieke
Android-vliegtuigmodusacceptatie. Productstatus blijft Incubate.
