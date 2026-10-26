# Bronzin corrigeren zonder een intussen gewijzigde gespreksregel te overschrijven

De tijdelijke, lokale Tuddy-spraak- en tekstvertaling is asynchroon.
Een herkende bronzin kan veranderen door een bewuste taalwissel, een
nieuwe correctie, een hervertaling of een later teruggekeerd ML Kit-
resultaat terwijl het correctievenster nog openstaat.

## Gedeelde originele-zin-editor

Vanaf de gewone gespreksregel, een waarschuwing bij lage
Android-herkenningszekerheid, een aanvullende spraakreview of de
fullscreen gesprekslezer opent dezelfde actie **Herstel herkende zin**.

Tuddy onthoudt uitsluitend tijdelijk in RAM: het id van die specifieke
regel, de huidige gespreksgeneratie, de correctie-/vertaalrevisie,
de oorspronkelijke zin, de vertaling met haar huidige status en beide
taalcodes. Deze waarden worden niet naar bestanden, het netwerk,
analytics of een bewaarservice geschreven.

**Corrigeer en vertaal** is alleen beschikbaar zolang die gehele
actuele regel exact overeenkomt met de versie waarbij het venster
werd geopend, en de microfoon niet luistert. De app herhaalt dezelfde
controle **op het moment van opslaan**, zodat ook een late callback
tussen tekenen en aantikken geen stille overschrijving kan veroorzaken.

Wanneer de bronzin, vertaling, status, revisie, taalrichting of
gespreksgeneratie intussen wijzigt, verschijnt een duidelijke
waarschuwing en wordt opslaan geblokkeerd. Met **Laad nieuwste zin
opnieuw · concept vervangen** kan de gebruiker bewust het nieuwe
origineel en de nieuwe revisie ophalen. Die knop vervangt het getypte
concept dus nooit zonder expliciete actie. Na een geaccepteerde
correctie gebruikt Tuddy de bestaande lokale tekstmodelcontrole en
vertaling, inclusief revisie-fencing tegen late resultaten en de
tijdelijke **ongedaan maken**-mogelijkheid.

Ook sluiten, Stop/achtergrond, verwijderen, oudere regels uit het
RAM-geheugen laten vallen en de conversatie wissen laten geen open
broneditor achter. Er is geen modeldownload of cloudfallback nodig.

## Acceptatiegrenzen

Deze bronwijziging kan met de bestaande exacte GitHub-head Android-
build en lifecyclecheck worden gecontroleerd. Zonder geautoriseerde
fysieke Android 12+-sessie bewijst dat geen offline spraakuitkomst,
geen Nederlandse of Engelse vertaalkwaliteit en geen volledige
radio-isolatie. De fysieke acceptatie, rechten- en native-speaker-
controle van Kurmanji/Sorani/Pasjtoe-modellen en de ondertekende
productierelease blijven afzonderlijke gates.
